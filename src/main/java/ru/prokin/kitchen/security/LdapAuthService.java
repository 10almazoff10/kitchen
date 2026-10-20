package ru.prokin.kitchen.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import ru.prokin.kitchen.dto.AuthResult;
import ru.prokin.kitchen.entity.User;
import ru.prokin.kitchen.repository.OrderRepository;
import ru.prokin.kitchen.repository.UserOrderRepository;
import ru.prokin.kitchen.repository.UserRepository;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import java.util.Hashtable;
import java.util.Optional;

@Service
public class LdapAuthService {

    private static final Logger logger = LoggerFactory.getLogger(LdapAuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OrderRepository orderRepository;
    private final UserOrderRepository userOrderRepository;

    @Value("${ldap.enabled:false}")
    private boolean ldapEnabled;

    @Value("${ldap.url:ldap://localhost:389}")
    private String ldapUrl;

    @Value("${ldap.domain:example.com}")
    private String ldapDomain;

    @Value("${ldap.base-dn:dc=example,dc=com}")
    private String ldapBaseDn;

    @Value("${ldap.user-dn-pattern:uid={0},ou=users,dc=example,dc=com}")
    private String ldapUserDnPattern;

    public record LdapUser(String accountName, String fullName, String email) {
        public LdapUser(String accountName, String fullName) {
            this(accountName, fullName, null);
        }
    }

    public LdapAuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            OrderRepository orderRepository,
            UserOrderRepository userOrderRepository
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.orderRepository = orderRepository;
        this.userOrderRepository = userOrderRepository;
    }

    @Transactional
    public AuthResult authenticate(String username, String password) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new BadCredentialsException("Имя пользователя и пароль не могут быть пустыми");
        }

        String trimmed = username.trim();

        // 1. Проверяем локальную базу пользователей (старые/legacy пользователи Kitchen)
        Optional<User> localUserOpt = userRepository.findByAnyUsername(trimmed);
        if (localUserOpt.isPresent()) {
            User localUser = localUserOpt.get();
            if (localUser.getPassword() != null &&
                    (passwordEncoder.matches(password, localUser.getPassword()) || password.equals(localUser.getPassword()))) {
                boolean requiresLink = !localUser.isAdLinked();
                logger.info("Local/legacy authentication successful for user '{}' (id={}), requiresAdLink={}",
                        localUser.getUsername(), localUser.getId(), requiresLink);
                return new AuthResult(localUser, requiresLink);
            }
        }

        // 2. Если локальный пароль не подошел или пользователя нет, проверяем Active Directory (LDAP)
        if (ldapEnabled) {
            try {
                LdapUser ldapUser = queryLdap(trimmed, password);
                String accountName = ldapUser.accountName();

                // Ищем существующего пользователя по привязанному adUsername или username
                Optional<User> linkedUserOpt = userRepository.findByAdUsername(accountName);
                if (linkedUserOpt.isEmpty()) {
                    linkedUserOpt = userRepository.findByUsername(accountName);
                }

                if (linkedUserOpt.isPresent()) {
                    User user = linkedUserOpt.get();
                    if (!user.isAdLinked()) {
                        user.setAdUsername(accountName);
                    }
                    if (StringUtils.hasText(ldapUser.fullName()) && !ldapUser.fullName().equals(user.getFullName())) {
                        user.setFullName(ldapUser.fullName());
                    }
                    if (StringUtils.hasText(ldapUser.email())) {
                        user.setEmail(ldapUser.email());
                    }
                    user.setPassword(passwordEncoder.encode(password));
                    User saved = userRepository.save(user);
                    logger.info("AD login successful for existing linked user '{}' (id={})", saved.getUsername(), saved.getId());
                    return new AuthResult(saved, false);
                } else {
                    // Новый сотрудник (в старой базе его не было) - автоматически создаем учетную запись
                    User newUser = new User();
                    newUser.setUsername(accountName);
                    newUser.setAdUsername(accountName);
                    newUser.setFullName(StringUtils.hasText(ldapUser.fullName()) ? ldapUser.fullName() : accountName);
                    if (StringUtils.hasText(ldapUser.email())) {
                        newUser.setEmail(ldapUser.email());
                    }
                    newUser.setPassword(passwordEncoder.encode(password));
                    User saved = userRepository.save(newUser);
                    logger.info("AD login: provisioned new employee '{}' (id={})", saved.getUsername(), saved.getId());
                    return new AuthResult(saved, false);
                }
            } catch (BadCredentialsException e) {
                throw e;
            } catch (Exception e) {
                logger.warn("AD authentication failed for user '{}': {}", trimmed, e.getMessage());
                throw new BadCredentialsException("Неверный логин или пароль");
            }
        } else {
            return authenticateFallback(trimmed, password);
        }
    }

    public LdapUser queryLdap(String username, String password) {
        logger.info("Attempting LDAP/AD authentication for user: {}", username);

        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, ldapUrl);
        env.put(Context.SECURITY_AUTHENTICATION, "simple");

        String accountName = cleanAccountName(username);

        String principal;
        if (StringUtils.hasText(ldapUserDnPattern) && ldapUserDnPattern.contains("{0}")) {
            principal = ldapUserDnPattern.replace("{0}", accountName);
        } else if (username.contains("@")) {
            principal = username;
        } else if (StringUtils.hasText(ldapDomain)) {
            principal = accountName + "@" + ldapDomain;
        } else {
            principal = username;
        }

        env.put(Context.SECURITY_PRINCIPAL, principal);
        env.put(Context.SECURITY_CREDENTIALS, password);

        DirContext ctx = null;
        try {
            ctx = new InitialDirContext(env);
            logger.info("LDAP bind successful for user: {}", username);

            String fullName = extractFullNameFromLdap(ctx, accountName, username);
            if (!StringUtils.hasText(fullName)) {
                fullName = accountName;
            }
            String email = extractEmailFromLdap(ctx, accountName, username);
            if (!StringUtils.hasText(email)) {
                email = accountName + "@" + (StringUtils.hasText(ldapDomain) ? ldapDomain : "example.com");
            }

            return new LdapUser(accountName, fullName, email);
        } catch (javax.naming.AuthenticationException e) {
            logger.warn("LDAP authentication failed for user {}: bad credentials", username);
            throw new BadCredentialsException("Неверный логин или пароль Active Directory");
        } catch (Exception e) {
            logger.error("LDAP connection error during auth for user {}: {}", username, e.getMessage());
            throw new BadCredentialsException("Ошибка связи с контроллером домена AD: " + e.getMessage());
        } finally {
            if (ctx != null) {
                try {
                    ctx.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    @Transactional
    public User linkAdAccount(User currentUser, String rawAdUsername, String adPassword) {
        if (!StringUtils.hasText(rawAdUsername) || !StringUtils.hasText(adPassword)) {
            throw new BadCredentialsException("Доменный логин и пароль AD не могут быть пустыми");
        }

        String adAccountName = cleanAccountName(rawAdUsername);
        logger.info("Linking AD account '{}' to user '{}' (id={})", adAccountName, currentUser.getUsername(), currentUser.getId());

        // 1. Проверяем валидность учетных данных в Active Directory
        LdapUser ldapUser;
        if (ldapEnabled) {
            ldapUser = queryLdap(adAccountName, adPassword);
        } else {
            String fallbackEmail = adAccountName + "@" + (StringUtils.hasText(ldapDomain) ? ldapDomain : "example.com");
            ldapUser = new LdapUser(adAccountName, adAccountName, fallbackEmail);
        }

        // 2. Проверяем, не привязана ли эта учетная запись AD к ДРУГОМУ пользователю
        Optional<User> existingLinked = userRepository.findByAdUsername(adAccountName);
        if (existingLinked.isPresent() && !existingLinked.get().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Учетная запись AD '" + adAccountName + "' уже привязана к пользователю: " + existingLinked.get().getFullName());
        }

        // 3. Проверяем, существует ли уже пользователь с таким username в БД
        Optional<User> existingUserWithSameName = userRepository.findByUsername(adAccountName);
        if (existingUserWithSameName.isPresent() && !existingUserWithSameName.get().getId().equals(currentUser.getId())) {
            User orphan = existingUserWithSameName.get();
            long ordersCount = orderRepository.countByCreatedBy(orphan);
            long itemsCount = userOrderRepository.countByUser(orphan);
            if (ordersCount == 0 && itemsCount == 0) {
                logger.info("Removing unlinked empty user '{}' (id={}) to allow linking to legacy user '{}' (id={})",
                        adAccountName, orphan.getId(), currentUser.getUsername(), currentUser.getId());
                userRepository.delete(orphan);
                userRepository.flush();
            } else {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Пользователь с логином '" + adAccountName + "' уже существует и имеет активные заказы. Обратитесь к администратору.");
            }
        }

        // 4. Привязываем AD к текущему пользователю
        if (currentUser.getLegacyUsername() == null) {
            currentUser.setLegacyUsername(currentUser.getUsername());
        }
        currentUser.setAdUsername(adAccountName);
        currentUser.setUsername(adAccountName);
        if (StringUtils.hasText(ldapUser.fullName())) {
            currentUser.setFullName(ldapUser.fullName());
        }
        if (StringUtils.hasText(ldapUser.email())) {
            currentUser.setEmail(ldapUser.email());
        }
        currentUser.setPassword(passwordEncoder.encode(adPassword));

        User updated = userRepository.save(currentUser);
        logger.info("Successfully linked AD '{}' to user id={}, legacyUsername='{}'",
                adAccountName, updated.getId(), updated.getLegacyUsername());
        return updated;
    }

    public String cleanAccountName(String username) {
        if (!StringUtils.hasText(username)) return "";
        String clean = username.trim();
        if (clean.contains("\\")) {
            clean = clean.substring(clean.lastIndexOf('\\') + 1);
        }
        if (clean.contains("@")) {
            clean = clean.substring(0, clean.indexOf('@'));
        }
        return clean.trim();
    }

    private String extractFullNameFromLdap(DirContext ctx, String accountName, String fullUsername) {
        try {
            SearchControls controls = new SearchControls();
            controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            controls.setReturningAttributes(new String[]{"displayName", "cn", "name", "givenName", "sn"});

            String filter = "(|(sAMAccountName=" + accountName + ")(userPrincipalName=" + fullUsername + ")(uid=" + accountName + ")(cn=" + accountName + "))";
            NamingEnumeration<SearchResult> answer = ctx.search(ldapBaseDn, filter, controls);

            if (answer != null && answer.hasMore()) {
                SearchResult result = answer.next();
                Attributes attrs = result.getAttributes();
                if (attrs.get("displayName") != null) {
                    return (String) attrs.get("displayName").get();
                }
                if (attrs.get("cn") != null) {
                    return (String) attrs.get("cn").get();
                }
                if (attrs.get("name") != null) {
                    return (String) attrs.get("name").get();
                }
                if (attrs.get("givenName") != null && attrs.get("sn") != null) {
                    return attrs.get("givenName").get() + " " + attrs.get("sn").get();
                }
            }
        } catch (Exception e) {
            logger.warn("Could not query LDAP attributes for user {}: {}", fullUsername, e.getMessage());
        }
        return null;
    }

    private String extractEmailFromLdap(DirContext ctx, String accountName, String fullUsername) {
        try {
            SearchControls controls = new SearchControls();
            controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            controls.setReturningAttributes(new String[]{"mail", "userPrincipalName"});

            String filter = "(|(sAMAccountName=" + accountName + ")(userPrincipalName=" + fullUsername + ")(uid=" + accountName + ")(cn=" + accountName + "))";
            NamingEnumeration<SearchResult> answer = ctx.search(ldapBaseDn, filter, controls);

            if (answer != null && answer.hasMore()) {
                SearchResult result = answer.next();
                Attributes attrs = result.getAttributes();
                if (attrs.get("mail") != null) {
                    return (String) attrs.get("mail").get();
                }
                if (attrs.get("userPrincipalName") != null) {
                    String upn = (String) attrs.get("userPrincipalName").get();
                    if (upn != null && upn.contains("@")) {
                        return upn;
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Could not query LDAP email for user {}: {}", fullUsername, e.getMessage());
        }
        return null;
    }

    private AuthResult authenticateFallback(String username, String password) {
        logger.info("Using local DB/fallback authentication for user: {}", username);

        Optional<User> userOpt = userRepository.findByAnyUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.getPassword() != null &&
                    (passwordEncoder.matches(password, user.getPassword()) || password.equals(user.getPassword()))) {
                return new AuthResult(user, !user.isAdLinked());
            }
            throw new BadCredentialsException("Неверный логин или пароль");
        }

        // В mock/локальном режиме, если пользователь не найден, создаём локального пользователя для удобства разработки
        logger.info("Fallback user '{}' not found in DB. Auto-creating for local development.", username);
        User newUser = new User();
        newUser.setUsername(username);
        newUser.setFullName(username);
        newUser.setEmail(username.contains("@") ? username : username + "@example.com");
        newUser.setPassword(passwordEncoder.encode(password));
        User saved = userRepository.save(newUser);
        return new AuthResult(saved, false);
    }

    public boolean isLdapEnabled() {
        return ldapEnabled;
    }
}
