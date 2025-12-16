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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Service for user management operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final PasswordEncoder passwordEncoder;

    private static final String CACHE_KEY = "user:";
    private static final long CACHE_TTL = 3600; // 1 hour in seconds

    /**
     * Creates a new user.
     */
    @Transactional
    public UserDTO createUser(CreateUserRequest request) {
        log.info("Creating new user with email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException(request.getEmail(), true);
        }

        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        Set<Role> roles = new HashSet<>();
        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            for (String roleName : request.getRoles()) {
                roleRepository.findByName(roleName)
                        .ifPresent(roles::add);
            }
        }

        if (roles.isEmpty()) {
            Role userRole = roleRepository.findByName("ROLE_USER")
                    .orElseGet(() -> {
                        Role newRole = Role.builder()
                                .name("ROLE_USER")
                                .description("Standard user role")
                                .build();
                        return roleRepository.save(newRole);
                    });
            roles.add(userRole);
        }

        user.setRoles(roles);
        User savedUser = userRepository.save(user);
        log.info("User created successfully with ID: {}", savedUser.getId());

        UserDTO dto = userMapper.toDTO(savedUser);
        cacheUser(dto);
        return dto;
    }

    /**
     * Gets a user by ID.
     */
    @Transactional(readOnly = true)
    public UserDTO getUserById(String id) {
        log.debug("Getting user by ID: {}", id);

        String cacheKey = CACHE_KEY + id;
        UserDTO cached = (UserDTO) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.debug("User found in cache: {}", id);
            return cached;
        }

        User user = userRepository.findByIdAndDeletedAtIsNull(UUID.fromString(id))
                .orElseThrow(() -> new UserNotFoundException(id, true));

        UserDTO dto = userMapper.toDTO(user);
        cacheUser(dto);
        return dto;
    }

    /**
     * Gets a user by email.
     */
    @Transactional(readOnly = true)
    public UserDTO getUserByEmail(String email) {
        log.debug("Getting user by email: {}", email);

        User user = userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new UserNotFoundException(email, false));

        return userMapper.toDTO(user);
    }

    /**
     * Gets all users with pagination.
     */
    @Transactional(readOnly = true)
    public Page<UserDTO> getAllUsers(Pageable pageable) {
        log.debug("Getting all users, page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());
        return userRepository.findByDeletedAtIsNull(pageable)
                .map(userMapper::toDTO);
    }

    /**
     * Searches users by query.
     */
    @Transactional(readOnly = true)
    public Page<UserDTO> searchUsers(String query, Pageable pageable) {
        log.debug("Searching users with query: {}", query);
        return userRepository.searchUsers(query, pageable)
                .map(userMapper::toDTO);
    }

    /**
     * Gets users by active status.
     */
    @Transactional(readOnly = true)
    public Page<UserDTO> getUsersByActiveStatus(Boolean isActive, Pageable pageable) {
        log.debug("Getting users by active status: {}", isActive);
        return userRepository.findByIsActive(isActive, pageable)
                .map(userMapper::toDTO);
    }

    /**
     * Updates a user.
     */
    @Transactional
    public UserDTO updateUser(String id, UpdateUserRequest request) {
        log.info("Updating user: {}", id);

        User user = userRepository.findByIdAndDeletedAtIsNull(UUID.fromString(id))
                .orElseThrow(() -> new UserNotFoundException(id, true));

        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getIsActive() != null) {
            user.setIsActive(request.getIsActive());
        }

        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            Set<Role> roles = new HashSet<>();
            for (String roleName : request.getRoles()) {
                roleRepository.findByName(roleName)
                        .ifPresent(roles::add);
            }
            if (!roles.isEmpty()) {
                user.setRoles(roles);
            }
        }

        user.setUpdatedAt(LocalDateTime.now());
        User updatedUser = userRepository.save(user);
        log.info("User updated successfully: {}", id);

        UserDTO dto = userMapper.toDTO(updatedUser);
        cacheUser(dto);
        return dto;
    }

    /**
     * Soft deletes a user.
     */
    @Transactional
    public void deleteUser(String id) {
        log.info("Deleting user: {}", id);

        User user = userRepository.findByIdAndDeletedAtIsNull(UUID.fromString(id))
                .orElseThrow(() -> new UserNotFoundException(id, true));

        user.setDeletedAt(LocalDateTime.now());
        user.setIsActive(false);
        userRepository.save(user);

        String cacheKey = CACHE_KEY + id;
        redisTemplate.delete(cacheKey);
        log.info("User deleted successfully: {}", id);
    }

    /**
     * Caches user data.
     */
    private void cacheUser(UserDTO user) {
        String cacheKey = CACHE_KEY + user.getId();
        redisTemplate.opsForValue().set(cacheKey, user, Duration.ofSeconds(CACHE_TTL));
    }

    /**
     * Invalidates user cache.
     */
    public void invalidateUserCache(String userId) {
        String cacheKey = CACHE_KEY + userId;
        redisTemplate.delete(cacheKey);
    }
}
