package ru.prokin.kitchen.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.prokin.kitchen.dto.AuthResponse;
import ru.prokin.kitchen.dto.AuthResult;
import ru.prokin.kitchen.dto.LinkAdRequest;
import ru.prokin.kitchen.dto.LoginRequest;
import ru.prokin.kitchen.dto.UpdateProfileRequest;
import ru.prokin.kitchen.dto.UserDto;
import ru.prokin.kitchen.entity.User;
import ru.prokin.kitchen.repository.UserRepository;
import ru.prokin.kitchen.security.JwtTokenProvider;
import ru.prokin.kitchen.security.LdapAuthService;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final LdapAuthService ldapAuthService;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResult authResult = ldapAuthService.authenticate(request.getUsername(), request.getPassword());
        User user = authResult.getUser();
        String token = jwtTokenProvider.generateToken(user);

        AuthResponse response = AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .user(UserDto.fromEntity(user))
                .requiresAdLink(authResult.isRequiresAdLink())
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/link-ad")
    public ResponseEntity<AuthResponse> linkAd(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody LinkAdRequest request
    ) {
        if (currentUser == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).build();
        }

        User updatedUser = ldapAuthService.linkAdAccount(currentUser, request.getAdUsername(), request.getAdPassword());
        String newToken = jwtTokenProvider.generateToken(updatedUser);

        AuthResponse response = AuthResponse.builder()
                .token(newToken)
                .tokenType("Bearer")
                .user(UserDto.fromEntity(updatedUser))
                .requiresAdLink(false)
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(UserDto.fromEntity(user));
    }

    @PutMapping("/profile")
    public ResponseEntity<UserDto> updateProfile(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        if (currentUser == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).build();
        }

        if (org.springframework.util.StringUtils.hasText(request.getEmail())) {
            currentUser.setEmail(request.getEmail().trim().toLowerCase());
        }
        if (org.springframework.util.StringUtils.hasText(request.getFullName())) {
            currentUser.setFullName(request.getFullName().trim());
        }

        User saved = userRepository.save(currentUser);
        return ResponseEntity.ok(UserDto.fromEntity(saved));
    }
}
