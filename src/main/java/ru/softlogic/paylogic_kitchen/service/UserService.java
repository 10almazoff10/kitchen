package ru.softlogic.paylogic_kitchen.service;

import ru.softlogic.paylogic_kitchen.entity.User;
import ru.softlogic.paylogic_kitchen.exception.UserRegistrationException;
import ru.softlogic.paylogic_kitchen.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService implements UserDetailsService {

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private PasswordEncoder passwordEncoder; // будет внедрён автоматически

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepo.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    public User createUser(String username, String password, String fullName) {
        if (userRepo.findByUsername(username).isPresent()) {
            throw new UserRegistrationException("Пользователь с логином '" + username + "' уже существует.");
        }

        if (username.length() < 3 || username.length() > 30) {
            throw new UserRegistrationException("Логин должен быть от 3 до 30 символов.");
        }

        if (password.length() < 6) {
            throw new UserRegistrationException("Пароль должен быть не менее 6 символов.");
        }

        if (fullName.trim().isEmpty()) {
            throw new UserRegistrationException("Полное имя не может быть пустым.");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setFullName(fullName);
        return userRepo.save(user);
    }
}