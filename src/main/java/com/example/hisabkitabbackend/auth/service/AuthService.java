package com.example.hisabkitabbackend.auth.service;

import com.example.hisabkitabbackend.auth.dto.AuthResponse;
import com.example.hisabkitabbackend.auth.dto.LoginRequest;
import com.example.hisabkitabbackend.auth.dto.RefreshTokenRequest;
import com.example.hisabkitabbackend.auth.dto.RegisterRequest;
import com.example.hisabkitabbackend.auth.exception.AuthException;
import com.example.hisabkitabbackend.user.RefreshToken;
import com.example.hisabkitabbackend.user.RefreshTokenRepository;
import com.example.hisabkitabbackend.user.User;
import com.example.hisabkitabbackend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new AuthException("Email already registered");
        }

        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .phone(request.phone())
                .build();

        user = userRepository.save(user);

        String accessToken = jwtService.generateAccessToken(user.getEmail());
        String refreshToken = jwtService.generateRefreshToken(user.getEmail());

        saveRefreshToken(user, refreshToken);

        return new AuthResponse(accessToken, refreshToken, user.getId(), user.getEmail());
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new AuthException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new AuthException("Invalid email or password");
        }

        if (!user.getIsActive()) {
            throw new AuthException("Account is disabled");
        }

        String accessToken = jwtService.generateAccessToken(user.getEmail());
        String refreshToken = jwtService.generateRefreshToken(user.getEmail());

        saveRefreshToken(user, refreshToken);

        return new AuthResponse(accessToken, refreshToken, user.getId(), user.getEmail());
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.refreshToken();

        RefreshToken storedToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new AuthException("Invalid refresh token"));

        if (storedToken.isExpired() || storedToken.isRevoked()) {
            throw new AuthException("Refresh token is expired or revoked");
        }

        String email = jwtService.extractEmail(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException("User not found"));

        String newAccessToken = jwtService.generateAccessToken(user.getEmail());
        String newRefreshToken = jwtService.generateRefreshToken(user.getEmail());

        storedToken.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(storedToken);

        saveRefreshToken(user, newRefreshToken);

        return new AuthResponse(newAccessToken, newRefreshToken, user.getId(), user.getEmail());
    }

    @Transactional
    public void logout(RefreshTokenRequest request) {
        String refreshToken = request.refreshToken();

        RefreshToken storedToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new AuthException("Invalid refresh token"));

        storedToken.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(storedToken);
    }

    private void saveRefreshToken(User user, String token) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(token)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public void revokeAllUserTokens(UUID userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }
}
