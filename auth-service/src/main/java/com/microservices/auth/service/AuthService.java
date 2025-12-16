package com.microservices.auth.service;

import com.microservices.auth.entity.RefreshToken;
import com.microservices.auth.entity.Role;
import com.microservices.auth.entity.User;
import com.microservices.auth.mapper.UserMapper;
import com.microservices.auth.repository.RefreshTokenRepository;
import com.microservices.auth.repository.RoleRepository;
import com.microservices.auth.repository.UserRepository;
import com.microservices.shared.dto.*;
import com.microservices.shared.exception.AuthenticationException;
import com.microservices.shared.exception.InvalidTokenException;
import com.microservices.shared.exception.UserAlreadyExistsException;
import com.microservices.shared.exception.UserNotFoundException;
import com.microservices.shared.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for authentication operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${jwt.refresh-expiration:604800000}")
    private Long refreshTokenExpiration;

    private static final String REFRESH_TOKEN_CACHE_KEY = "refresh_token:";
    private static final String BLACKLIST_TOKEN_KEY = "blacklist:";

    /**
     * Registers a new user.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user with email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException(request.getEmail(), true);
        }

        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> {
                    Role newRole = Role.builder()
                            .name("ROLE_USER")
                            .description("Standard user role")
                            .build();
                    return roleRepository.save(newRole);
                });

        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        user.setRoles(roles);

        User savedUser = userRepository.save(user);
        log.info("User registered successfully with ID: {}", savedUser.getId());

        return generateAuthResponse(savedUser);
    }

    /**
     * Authenticates a user and returns tokens.
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for user: {}", request.getEmail());

        User user = userRepository.findByEmailAndDeletedAtIsNull(request.getEmail())
                .orElseThrow(() -> new AuthenticationException("Invalid email or password"));

        if (!user.getIsActive()) {
            throw new AuthenticationException("Account is deactivated");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AuthenticationException("Invalid email or password");
        }

        log.info("User logged in successfully: {}", user.getId());
        return generateAuthResponse(user);
    }

    /**
     * Logs out a user by invalidating their tokens.
     */
    @Transactional
    public void logout(String authHeader) {
        String token = extractToken(authHeader);
        log.info("Logging out user");

        String userId = jwtUtil.extractUserId(token);
        User user = userRepository.findById(java.util.UUID.fromString(userId))
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        refreshTokenRepository.revokeAllByUser(user);

        String blacklistKey = BLACKLIST_TOKEN_KEY + token;
        redisTemplate.opsForValue().set(blacklistKey, "true",
                Duration.ofMillis(jwtUtil.getAccessTokenExpirationSeconds() * 1000));

        log.info("User logged out successfully");
    }

    /**
     * Refreshes the access token using a valid refresh token.
     */
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        log.info("Refreshing access token");

        String refreshToken = request.getRefreshToken();

        if (!jwtUtil.validateToken(refreshToken) || !jwtUtil.isRefreshToken(refreshToken)) {
            throw new InvalidTokenException("Invalid refresh token");
        }

        RefreshToken storedToken = refreshTokenRepository.findByTokenAndRevokedFalse(refreshToken)
                .orElseThrow(() -> new InvalidTokenException("Refresh token not found or revoked"));

        if (!storedToken.isValid()) {
            throw new InvalidTokenException("Refresh token is expired or revoked");
        }

        User user = storedToken.getUser();
        if (!user.getIsActive()) {
            throw new AuthenticationException("Account is deactivated");
        }

        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        log.info("Access token refreshed successfully for user: {}", user.getId());
        return generateAuthResponse(user);
    }

    /**
     * Validates an access token.
     */
    public boolean validateToken(String authHeader) {
        String token = extractToken(authHeader);

        String blacklistKey = BLACKLIST_TOKEN_KEY + token;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey))) {
            return false;
        }

        return jwtUtil.validateToken(token);
    }

    /**
     * Gets user info from token.
     */
    public UserDTO getUserFromToken(String authHeader) {
        String token = extractToken(authHeader);

        if (!jwtUtil.validateToken(token)) {
            throw new InvalidTokenException("Invalid token");
        }

        String userId = jwtUtil.extractUserId(token);
        User user = userRepository.findByIdAndDeletedAtIsNull(java.util.UUID.fromString(userId))
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        return userMapper.toDTO(user);
    }

    /**
     * Generates authentication response with tokens.
     */
    private AuthResponse generateAuthResponse(User user) {
        Set<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        String accessToken = jwtUtil.generateAccessToken(
                user.getId().toString(),
                user.getEmail(),
                roles
        );

        String refreshToken = jwtUtil.generateRefreshToken(
                user.getId().toString(),
                user.getEmail()
        );

        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .user(user)
                .token(refreshToken)
                .expiresAt(LocalDateTime.now().plusSeconds(refreshTokenExpiration / 1000))
                .build();
        refreshTokenRepository.save(refreshTokenEntity);

        String cacheKey = REFRESH_TOKEN_CACHE_KEY + refreshToken;
        redisTemplate.opsForValue().set(cacheKey, user.getId().toString(),
                Duration.ofMillis(refreshTokenExpiration));

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getAccessTokenExpirationSeconds())
                .user(userMapper.toDTO(user))
                .build();
    }

    /**
     * Extracts token from Authorization header.
     */
    private String extractToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new InvalidTokenException("Invalid Authorization header");
        }
        return authHeader.substring(7);
    }
}
