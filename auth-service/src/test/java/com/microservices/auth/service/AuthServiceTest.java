package com.microservices.auth.service;

import com.microservices.auth.entity.Role;
import com.microservices.auth.entity.User;
import com.microservices.auth.mapper.UserMapper;
import com.microservices.auth.repository.RefreshTokenRepository;
import com.microservices.auth.repository.RoleRepository;
import com.microservices.auth.repository.UserRepository;
import com.microservices.shared.dto.*;
import com.microservices.shared.exception.AuthenticationException;
import com.microservices.shared.exception.UserAlreadyExistsException;
import com.microservices.shared.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthService.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserMapper userMapper;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private Role testRole;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshTokenExpiration", 604800000L);

        testRole = Role.builder()
                .id(UUID.randomUUID())
                .name("ROLE_USER")
                .description("Standard user role")
                .build();

        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .password("encodedPassword")
                .firstName("John")
                .lastName("Doe")
                .phone("+1234567890")
                .isActive(true)
                .roles(new HashSet<>(Collections.singletonList(testRole)))
                .createdAt(LocalDateTime.now())
                .build();

        registerRequest = RegisterRequest.builder()
                .email("test@example.com")
                .password("password123")
                .firstName("John")
                .lastName("Doe")
                .phone("+1234567890")
                .build();

        loginRequest = LoginRequest.builder()
                .email("test@example.com")
                .password("password123")
                .build();
    }

    @Test
    @DisplayName("Should register a new user successfully")
    void register_Success() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(testRole));
        when(userMapper.toEntity(any(RegisterRequest.class))).thenReturn(testUser);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtUtil.generateAccessToken(anyString(), anyString(), anySet())).thenReturn("accessToken");
        when(jwtUtil.generateRefreshToken(anyString(), anyString())).thenReturn("refreshToken");
        when(jwtUtil.getAccessTokenExpirationSeconds()).thenReturn(900L);
        when(refreshTokenRepository.save(any())).thenReturn(null);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(userMapper.toDTO(any(User.class))).thenReturn(UserDTO.builder()
                .id(testUser.getId().toString())
                .email(testUser.getEmail())
                .firstName(testUser.getFirstName())
                .lastName(testUser.getLastName())
                .build());

        // Act
        AuthResponse response = authService.register(registerRequest);

        // Assert
        assertNotNull(response);
        assertEquals("accessToken", response.getAccessToken());
        assertEquals("refreshToken", response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        assertNotNull(response.getUser());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when registering with existing email")
    void register_EmailExists_ThrowsException() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        // Act & Assert
        assertThrows(UserAlreadyExistsException.class,
                () -> authService.register(registerRequest));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should login user successfully")
    void login_Success() {
        // Arrange
        when(userRepository.findByEmailAndDeletedAtIsNull(anyString())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtUtil.generateAccessToken(anyString(), anyString(), anySet())).thenReturn("accessToken");
        when(jwtUtil.generateRefreshToken(anyString(), anyString())).thenReturn("refreshToken");
        when(jwtUtil.getAccessTokenExpirationSeconds()).thenReturn(900L);
        when(refreshTokenRepository.save(any())).thenReturn(null);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(userMapper.toDTO(any(User.class))).thenReturn(UserDTO.builder()
                .id(testUser.getId().toString())
                .email(testUser.getEmail())
                .build());

        // Act
        AuthResponse response = authService.login(loginRequest);

        // Assert
        assertNotNull(response);
        assertEquals("accessToken", response.getAccessToken());
        assertEquals("refreshToken", response.getRefreshToken());
    }

    @Test
    @DisplayName("Should throw exception for invalid credentials")
    void login_InvalidCredentials_ThrowsException() {
        // Arrange
        when(userRepository.findByEmailAndDeletedAtIsNull(anyString())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        // Act & Assert
        assertThrows(AuthenticationException.class,
                () -> authService.login(loginRequest));
    }

    @Test
    @DisplayName("Should throw exception for non-existent user")
    void login_UserNotFound_ThrowsException() {
        // Arrange
        when(userRepository.findByEmailAndDeletedAtIsNull(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(AuthenticationException.class,
                () -> authService.login(loginRequest));
    }

    @Test
    @DisplayName("Should throw exception for inactive user")
    void login_InactiveUser_ThrowsException() {
        // Arrange
        testUser.setIsActive(false);
        when(userRepository.findByEmailAndDeletedAtIsNull(anyString())).thenReturn(Optional.of(testUser));

        // Act & Assert
        assertThrows(AuthenticationException.class,
                () -> authService.login(loginRequest));
    }

    @Test
    @DisplayName("Should validate token successfully")
    void validateToken_Success() {
        // Arrange
        String authHeader = "Bearer validToken";
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        when(jwtUtil.validateToken("validToken")).thenReturn(true);

        // Act
        boolean result = authService.validateToken(authHeader);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Should return false for blacklisted token")
    void validateToken_Blacklisted_ReturnsFalse() {
        // Arrange
        String authHeader = "Bearer blacklistedToken";
        when(redisTemplate.hasKey(anyString())).thenReturn(true);

        // Act
        boolean result = authService.validateToken(authHeader);

        // Assert
        assertFalse(result);
    }
}
