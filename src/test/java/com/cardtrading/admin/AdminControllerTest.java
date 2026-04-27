package com.cardtrading.admin;

import com.cardtrading.admin.controller.AdminController;
import com.cardtrading.admin.dto.*;
import com.cardtrading.admin.service.AdminService;
import com.cardtrading.shared.exception.BusinessRuleException;
import com.cardtrading.shared.exception.GlobalExceptionHandler;
import com.cardtrading.shared.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock private AdminService adminService;
    @InjectMocks private AdminController adminController;
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
    }

    @Test
    @DisplayName("GET /admin/users - 200")
    void shouldListUsers() throws Exception {
        AdminUserResponse user = AdminUserResponse.builder()
                .id(UUID.randomUUID()).username("testuser").email("test@test.com")
                .role("USER").isBanned(false).createdAt(LocalDateTime.now()).build();

        when(adminService.listUsers(any()))
                .thenReturn(new PageImpl<>(List.of(user), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").value("testuser"));
    }

    @Test
    @DisplayName("PUT /admin/users/{id}/ban - 200 ban user")
    void shouldBanUser() throws Exception {
        UUID userId = UUID.randomUUID();
        BanUserRequest request = BanUserRequest.builder().banned(true).reason("Spam").build();
        BanUserResponse response = BanUserResponse.builder()
                .id(userId).username("baduser").isBanned(true)
                .updatedAt(LocalDateTime.now()).build();

        when(adminService.banUser(userId, true, "Spam")).thenReturn(response);

        mockMvc.perform(put("/api/v1/admin/users/{userId}/ban", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isBanned").value(true));
    }

    @Test
    @DisplayName("PUT /admin/users/{id}/ban - 404 not found")
    void shouldReturn404ForUnknownUser() throws Exception {
        UUID userId = UUID.randomUUID();
        BanUserRequest request = BanUserRequest.builder().banned(true).build();

        when(adminService.banUser(eq(userId), eq(true), isNull()))
                .thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(put("/api/v1/admin/users/{userId}/ban", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /admin/users/{id}/ban - 409 already banned")
    void shouldReturn409AlreadyBanned() throws Exception {
        UUID userId = UUID.randomUUID();
        BanUserRequest request = BanUserRequest.builder().banned(true).build();

        when(adminService.banUser(eq(userId), eq(true), isNull()))
                .thenThrow(new BusinessRuleException("User is already banned"));

        mockMvc.perform(put("/api/v1/admin/users/{userId}/ban", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("GET /admin/stats - 200")
    void shouldReturnStats() throws Exception {
        Map<String, Long> tradesByStatus = new LinkedHashMap<>();
        tradesByStatus.put("PENDING", 5L);
        tradesByStatus.put("COMPLETED", 10L);

        StatsResponse stats = StatsResponse.builder()
                .totalUsers(100).activeUsers(95).bannedUsers(5)
                .totalCards(500).totalTrades(15)
                .tradesByStatus(tradesByStatus)
                .generatedAt(LocalDateTime.now()).build();

        when(adminService.getStats()).thenReturn(stats);

        mockMvc.perform(get("/api/v1/admin/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(100))
                .andExpect(jsonPath("$.totalCards").value(500));
    }
}
