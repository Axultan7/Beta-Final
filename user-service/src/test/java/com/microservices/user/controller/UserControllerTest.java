package com.microservices.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservices.shared.dto.CreateUserRequest;
import com.microservices.shared.dto.UpdateUserRequest;
import com.microservices.shared.dto.UserDTO;
import com.microservices.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for UserController.
 */
@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    private UserDTO testUserDTO;
    private CreateUserRequest createUserRequest;
    private UpdateUserRequest updateUserRequest;
    private String testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID().toString();

        testUserDTO = UserDTO.builder()
                .id(testUserId)
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .phone("+1234567890")
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
    @DisplayName("Should get all users successfully")
    void getAllUsers_Success() throws Exception {
        Page<UserDTO> userPage = new PageImpl<>(
                Collections.singletonList(testUserDTO),
                PageRequest.of(0, 20),
                1
        );

        when(userService.getAllUsers(any())).thenReturn(userPage);

        mockMvc.perform(get("/api/v1/users")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].email").value("test@example.com"));
    }

    @Test
    @DisplayName("Should get user by ID successfully")
    void getUserById_Success() throws Exception {
        when(userService.getUserById(testUserId)).thenReturn(testUserDTO);

        mockMvc.perform(get("/api/v1/users/{id}", testUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.firstName").value("John"));
    }

    @Test
    @DisplayName("Should create user successfully")
    void createUser_Success() throws Exception {
        when(userService.createUser(any(CreateUserRequest.class))).thenReturn(testUserDTO);

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createUserRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User created successfully"));
    }

    @Test
    @DisplayName("Should return validation error for invalid email")
    void createUser_InvalidEmail_ValidationError() throws Exception {
        createUserRequest.setEmail("invalid-email");

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createUserRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return validation error for short password")
    void createUser_ShortPassword_ValidationError() throws Exception {
        createUserRequest.setPassword("12345");

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createUserRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should update user successfully")
    void updateUser_Success() throws Exception {
        when(userService.updateUser(eq(testUserId), any(UpdateUserRequest.class))).thenReturn(testUserDTO);

        mockMvc.perform(put("/api/v1/users/{id}", testUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateUserRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User updated successfully"));
    }

    @Test
    @DisplayName("Should delete user successfully")
    void deleteUser_Success() throws Exception {
        doNothing().when(userService).deleteUser(testUserId);

        mockMvc.perform(delete("/api/v1/users/{id}", testUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User deleted successfully"));
    }

    @Test
    @DisplayName("Should search users successfully")
    void searchUsers_Success() throws Exception {
        Page<UserDTO> userPage = new PageImpl<>(
                Collections.singletonList(testUserDTO),
                PageRequest.of(0, 20),
                1
        );

        when(userService.searchUsers(anyString(), any())).thenReturn(userPage);

        mockMvc.perform(get("/api/v1/users/search")
                        .param("query", "John")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }
}
