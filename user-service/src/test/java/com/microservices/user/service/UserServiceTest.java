package com.microservices.user.service;

import com.microservices.shared.dto.CreateUserRequest;
import com.microservices.shared.dto.UpdateUserRequest;
import com.microservices.shared.dto.UserDTO;
import com.microservices.shared.exception.UserAlreadyExistsException;
import com.microservices.shared.exception.UserNotFoundException;
import com.microservices.user.entity.Role;
import com.microservices.user.entity.User;
import com.microservices.user.mapper.UserMapper;
import com.microservices.user.repository.RoleRepository;
import com.microservices.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserService.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private Role testRole;
    private UserDTO testUserDTO;
    private CreateUserRequest createUserRequest;
    private UpdateUserRequest updateUserRequest;

    @BeforeEach
    void setUp() {
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

        testUserDTO = UserDTO.builder()
                .id(testUser.getId().toString())
                .email(testUser.getEmail())
                .firstName(testUser.getFirstName())
                .lastName(testUser.getLastName())
                .phone(testUser.getPhone())
                .isActive(true)
                .roles(Set.of("ROLE_USER"))
                .build();

        createUserRequest = CreateUserRequest.builder()
                .email("new@example.com")
                .password("password123")
                .firstName("Jane")
                .lastName("Smith")
                .phone("+0987654321")
                .build();

        updateUserRequest = UpdateUserRequest.builder()
                .firstName("Updated")
                .lastName("Name")
                .phone("+1111111111")
                .build();
    }

    @Test
    @DisplayName("Should create user successfully")
    void createUser_Success() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(testRole));
        when(userMapper.toEntity(any(CreateUserRequest.class))).thenReturn(testUser);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userMapper.toDTO(any(User.class))).thenReturn(testUserDTO);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // Act
        UserDTO result = userService.createUser(createUserRequest);

        // Assert
        assertNotNull(result);
        assertEquals(testUserDTO.getEmail(), result.getEmail());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when creating user with existing email")
    void createUser_EmailExists_ThrowsException() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        // Act & Assert
        assertThrows(UserAlreadyExistsException.class,
                () -> userService.createUser(createUserRequest));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should get user by ID from cache")
    void getUserById_FromCache_Success() {
        // Arrange
        String userId = testUser.getId().toString();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(testUserDTO);

        // Act
        UserDTO result = userService.getUserById(userId);

        // Assert
        assertNotNull(result);
        assertEquals(testUserDTO.getEmail(), result.getEmail());
        verify(userRepository, never()).findByIdAndDeletedAtIsNull(any());
    }

    @Test
    @DisplayName("Should get user by ID from database when not in cache")
    void getUserById_FromDatabase_Success() {
        // Arrange
        String userId = testUser.getId().toString();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(userRepository.findByIdAndDeletedAtIsNull(any(UUID.class))).thenReturn(Optional.of(testUser));
        when(userMapper.toDTO(any(User.class))).thenReturn(testUserDTO);

        // Act
        UserDTO result = userService.getUserById(userId);

        // Assert
        assertNotNull(result);
        assertEquals(testUserDTO.getEmail(), result.getEmail());
        verify(userRepository).findByIdAndDeletedAtIsNull(any(UUID.class));
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void getUserById_NotFound_ThrowsException() {
        // Arrange
        String userId = UUID.randomUUID().toString();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(userRepository.findByIdAndDeletedAtIsNull(any(UUID.class))).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class,
                () -> userService.getUserById(userId));
    }

    @Test
    @DisplayName("Should get all users with pagination")
    void getAllUsers_Success() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        List<User> users = Collections.singletonList(testUser);
        Page<User> userPage = new PageImpl<>(users, pageable, 1);

        when(userRepository.findByDeletedAtIsNull(pageable)).thenReturn(userPage);
        when(userMapper.toDTO(any(User.class))).thenReturn(testUserDTO);

        // Act
        Page<UserDTO> result = userService.getAllUsers(pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(testUserDTO.getEmail(), result.getContent().get(0).getEmail());
    }

    @Test
    @DisplayName("Should update user successfully")
    void updateUser_Success() {
        // Arrange
        String userId = testUser.getId().toString();
        when(userRepository.findByIdAndDeletedAtIsNull(any(UUID.class))).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userMapper.toDTO(any(User.class))).thenReturn(testUserDTO);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // Act
        UserDTO result = userService.updateUser(userId, updateUserRequest);

        // Assert
        assertNotNull(result);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent user")
    void updateUser_NotFound_ThrowsException() {
        // Arrange
        String userId = UUID.randomUUID().toString();
        when(userRepository.findByIdAndDeletedAtIsNull(any(UUID.class))).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class,
                () -> userService.updateUser(userId, updateUserRequest));
    }

    @Test
    @DisplayName("Should delete user successfully")
    void deleteUser_Success() {
        // Arrange
        String userId = testUser.getId().toString();
        when(userRepository.findByIdAndDeletedAtIsNull(any(UUID.class))).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(redisTemplate.delete(anyString())).thenReturn(true);

        // Act
        userService.deleteUser(userId);

        // Assert
        verify(userRepository).save(any(User.class));
        verify(redisTemplate).delete(anyString());
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent user")
    void deleteUser_NotFound_ThrowsException() {
        // Arrange
        String userId = UUID.randomUUID().toString();
        when(userRepository.findByIdAndDeletedAtIsNull(any(UUID.class))).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class,
                () -> userService.deleteUser(userId));
    }

    @Test
    @DisplayName("Should search users successfully")
    void searchUsers_Success() {
        // Arrange
        String query = "John";
        Pageable pageable = PageRequest.of(0, 10);
        List<User> users = Collections.singletonList(testUser);
        Page<User> userPage = new PageImpl<>(users, pageable, 1);

        when(userRepository.searchUsers(query, pageable)).thenReturn(userPage);
        when(userMapper.toDTO(any(User.class))).thenReturn(testUserDTO);

        // Act
        Page<UserDTO> result = userService.searchUsers(query, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }
}
