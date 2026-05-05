package com.cardtrading.inventory;

import com.cardtrading.inventory.controller.InventoryController;
import com.cardtrading.inventory.dto.UserCardDto;
import com.cardtrading.inventory.service.InventoryService;
import com.cardtrading.shared.exception.GlobalExceptionHandler;
import com.cardtrading.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class InventoryControllerTest {

    @Mock private InventoryService inventoryService;
    @InjectMocks private InventoryController inventoryController;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(inventoryController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("200 - returns paginated inventory")
    void shouldReturnInventory() throws Exception {
        UUID userId = UUID.randomUUID();
        UserCardDto dto = UserCardDto.builder()
                .cardId(UUID.randomUUID()).cardName("Blue Dragon").rarity("LEGENDARY")
                .quantity(3).acquiredAt(LocalDateTime.now()).build();

        when(inventoryService.getUserInventory(eq(userId), any()))
                .thenReturn(new PageImpl<>(List.of(dto), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/users/{userId}/inventory", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].cardName").value("Blue Dragon"))
                .andExpect(jsonPath("$.content[0].quantity").value(3));
    }

    @Test
    @DisplayName("200 - returns empty inventory")
    void shouldReturnEmptyInventory() throws Exception {
        UUID userId = UUID.randomUUID();
        when(inventoryService.getUserInventory(eq(userId), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/v1/users/{userId}/inventory", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("404 - unknown user")
    void shouldReturn404ForUnknownUser() throws Exception {
        UUID userId = UUID.randomUUID();
        when(inventoryService.getUserInventory(eq(userId), any()))
                .thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(get("/api/v1/users/{userId}/inventory", userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found"));
    }
}
