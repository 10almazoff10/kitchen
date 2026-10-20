package ru.prokin.kitchen.entity;

import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import org.springframework.util.StringUtils;
import ru.prokin.kitchen.util.GravatarUtil;

import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "app_users")
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String username;

    private String password;
    private String fullName;

    @Column(name = "email")
    private String email;

    @Column(name = "ad_username", unique = true)
    private String adUsername;

    @Column(name = "legacy_username")
    private String legacyUsername;

    // constructors, getters, setters
    public User() {}

    public User(String username, String password, String fullName) {
        this.username = username;
        this.password = password;
        this.fullName = fullName;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    @Override
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    @Override
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getAdUsername() { return adUsername; }
    public void setAdUsername(String adUsername) { this.adUsername = adUsername; }

    public String getLegacyUsername() { return legacyUsername; }
    public void setLegacyUsername(String legacyUsername) { this.legacyUsername = legacyUsername; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getEffectiveEmail() {
        if (StringUtils.hasText(email)) {
            return email.trim().toLowerCase();
        }
        String identifier = StringUtils.hasText(adUsername) ? adUsername : username;
        if (identifier == null) {
            return "";
        }
        identifier = identifier.trim().toLowerCase();
        if (identifier.contains("@")) {
            return identifier;
        }
        return identifier + "@example.com";
    }

    public String getAvatarUrl() {
        return GravatarUtil.getGravatarUrl(getEffectiveEmail(), 160);
    }

    public boolean isAdLinked() {
        return adUsername != null && !adUsername.trim().isEmpty();
    }

    // UserDetails методы
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public boolean isAccountNonExpired() { return true; }
    @Override
    public boolean isAccountNonLocked() { return true; }
    @Override
    public boolean isCredentialsNonExpired() { return true; }
    @Override
    public boolean isEnabled() { return true; }
}