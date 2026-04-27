package com.cardtrading.auth;

import com.cardtrading.auth.controller.UserController;
import com.cardtrading.auth.dto.UpdateUserRequest;
import com.cardtrading.auth.dto.UserResponse;
import com.cardtrading.auth.service.UserService;
import com.cardtrading.shared.exception.BusinessRuleException;
import com.cardtrading.shared.exception.GlobalExceptionHandler;
import com.cardtrading.shared.exception.ResourceNotFoundException;
import com.cardtrading.shared.exception.UnauthorizedException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock private UserService userService;
    @InjectMocks private UserController userController;
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private UUID userId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        userId = UUID.randomUUID();
    }

    private UsernamePasswordAuthenticationToken authAs(UUID id) {
        return new UsernamePasswordAuthenticationToken(
                id.toString(), null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Nested
    @DisplayName("GET /api/v1/users/{userId}")
    class GetProfile {

        @Test
        @DisplayName("200 - returns user profile")
        void shouldReturnProfile() throws Exception {
            UserResponse response = UserResponse.builder()
                    .id(userId).username("testuser").email("test@example.com")
                    .role("USER").createdAt(LocalDateTime.now()).build();
            when(userService.getUserProfile(userId, userId)).thenReturn(response);

            mockMvc.perform(get("/api/v1/users/{userId}", userId)
                            .principal(authAs(userId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value("testuser"))
                    .andExpect(jsonPath("$.email").value("test@example.com"));
        }

        @Test
        @DisplayName("200 - email hidden for non-owner")
        void shouldHideEmailForNonOwner() throws Exception {
            UUID requesterId = UUID.randomUUID();
            UserResponse response = UserResponse.builder()
                    .id(userId).username("testuser").role("USER")
                    .createdAt(LocalDateTime.now()).build(); // no email
            when(userService.getUserProfile(requesterId, userId)).thenReturn(response);

            mockMvc.perform(get("/api/v1/users/{userId}", userId)
                            .principal(authAs(requesterId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").doesNotExist());
        }

        @Test
        @DisplayName("404 - user not found")
        void shouldReturn404() throws Exception {
            when(userService.getUserProfile(userId, userId))
                    .thenThrow(new ResourceNotFoundException("User not found"));

            mockMvc.perform(get("/api/v1/users/{userId}", userId)
                            .principal(authAs(userId)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/users/{userId}")
    class UpdateProfile {

        @Test
        @DisplayName("200 - successful update")
        void shouldUpdateProfile() throws Exception {
            UpdateUserRequest request = UpdateUserRequest.builder()
                    .username("newname").build();
            UserResponse response = UserResponse.builder()
                    .id(userId).username("newname").email("test@example.com")
                    .role("USER").createdAt(LocalDateTime.now()).build();
            when(userService.updateUserProfile(eq(userId), eq(userId), any())).thenReturn(response);

            mockMvc.perform(put("/api/v1/users/{userId}", userId)
                            .principal(authAs(userId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value("newname"));
        }

        @Test
        @DisplayName("403 - non-owner update")
        void shouldReturn403ForNonOwner() throws Exception {
            UUID otherUser = UUID.randomUUID();
            UpdateUserRequest request = UpdateUserRequest.builder().username("hacker").build();
            when(userService.updateUserProfile(eq(otherUser), eq(userId), any()))
                    .thenThrow(new UnauthorizedException("You are not authorized to update this profile"));

            mockMvc.perform(put("/api/v1/users/{userId}", userId)
                            .principal(authAs(otherUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("409 - duplicate username")
        void shouldReturn409OnDuplicate() throws Exception {
            UpdateUserRequest request = UpdateUserRequest.builder().username("taken").build();
            when(userService.updateUserProfile(eq(userId), eq(userId), any()))
                    .thenThrow(new BusinessRuleException("Username already taken"));

            mockMvc.perform(put("/api/v1/users/{userId}", userId)
                            .principal(authAs(userId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnprocessableEntity());
        }
    }
}
