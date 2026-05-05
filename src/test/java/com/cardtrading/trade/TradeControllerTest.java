package com.cardtrading.trade;

import com.cardtrading.shared.exception.BusinessRuleException;
import com.cardtrading.shared.exception.GlobalExceptionHandler;
import com.cardtrading.shared.exception.ResourceNotFoundException;
import com.cardtrading.shared.exception.UnauthorizedException;
import com.cardtrading.trade.controller.TradeController;
import com.cardtrading.trade.dto.*;
import com.cardtrading.trade.service.TradeService;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class TradeControllerTest {

    @Mock private TradeService tradeService;
    @InjectMocks private TradeController tradeController;
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private UUID userId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(tradeController)
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
    @DisplayName("POST /api/v1/trades")
    class CreateTrade {

        @Test
        @DisplayName("201 - successful trade creation")
        void shouldReturn201() throws Exception {
            UUID receiverId = UUID.randomUUID();
            TradeItemRequest offeredItem = new TradeItemRequest();
            offeredItem.setUserCardId(UUID.randomUUID());
            offeredItem.setQuantity(1);
            TradeItemRequest requestedItem = new TradeItemRequest();
            requestedItem.setUserCardId(UUID.randomUUID());
            requestedItem.setQuantity(1);
            CreateTradeRequest request = new CreateTradeRequest();
            request.setReceiverId(receiverId);
            request.setOfferedCards(List.of(offeredItem));
            request.setRequestedCards(List.of(requestedItem));

            TradeResponse response = TradeResponse.builder()
                    .id(UUID.randomUUID()).proposerId(userId).receiverId(receiverId)
                    .status("PENDING").createdAt(LocalDateTime.now()).build();

            when(tradeService.createTrade(eq(userId), any())).thenReturn(response);

            mockMvc.perform(post("/api/v1/trades")
                            .principal(authAs(userId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("PENDING"));
        }

        @Test
        @DisplayName("422 - self-trade")
        void shouldReturn422OnSelfTrade() throws Exception {
            TradeItemRequest offeredItem = new TradeItemRequest();
            offeredItem.setUserCardId(UUID.randomUUID());
            offeredItem.setQuantity(1);
            TradeItemRequest requestedItem = new TradeItemRequest();
            requestedItem.setUserCardId(UUID.randomUUID());
            requestedItem.setQuantity(1);
            CreateTradeRequest request = new CreateTradeRequest();
            request.setReceiverId(userId);
            request.setOfferedCards(List.of(offeredItem));
            request.setRequestedCards(List.of(requestedItem));

            when(tradeService.createTrade(eq(userId), any()))
                    .thenThrow(new BusinessRuleException("You cannot trade with yourself"));

            mockMvc.perform(post("/api/v1/trades")
                            .principal(authAs(userId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnprocessableEntity());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/trades/{tradeId}/accept")
    class AcceptTrade {

        @Test
        @DisplayName("200 - successful accept")
        void shouldReturn200() throws Exception {
            UUID tradeId = UUID.randomUUID();
            TradeResponse response = TradeResponse.builder()
                    .id(tradeId).status("ACCEPTED").respondedAt(LocalDateTime.now())
                    .message("Trade accepted. Inventory update is being processed.").build();

            when(tradeService.acceptTrade(tradeId, userId)).thenReturn(response);

            mockMvc.perform(put("/api/v1/trades/{tradeId}/accept", tradeId)
                            .principal(authAs(userId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ACCEPTED"));
        }

        @Test
        @DisplayName("403 - not receiver")
        void shouldReturn403NotReceiver() throws Exception {
            UUID tradeId = UUID.randomUUID();
            when(tradeService.acceptTrade(tradeId, userId))
                    .thenThrow(new UnauthorizedException("Only the receiver can accept this trade"));

            mockMvc.perform(put("/api/v1/trades/{tradeId}/accept", tradeId)
                            .principal(authAs(userId)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("409 - not PENDING")
        void shouldReturn409NotPending() throws Exception {
            UUID tradeId = UUID.randomUUID();
            when(tradeService.acceptTrade(tradeId, userId))
                    .thenThrow(new BusinessRuleException("Trade cannot be accepted: current status is CANCELLED"));

            mockMvc.perform(put("/api/v1/trades/{tradeId}/accept", tradeId)
                            .principal(authAs(userId)))
                    .andExpect(status().isUnprocessableEntity());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/trades/{tradeId}/reject")
    class RejectTrade {

        @Test
        @DisplayName("200 - successful reject")
        void shouldReturn200() throws Exception {
            UUID tradeId = UUID.randomUUID();
            TradeResponse response = TradeResponse.builder()
                    .id(tradeId).status("REJECTED").updatedAt(LocalDateTime.now()).build();

            when(tradeService.rejectTrade(eq(tradeId), eq(userId), isNull())).thenReturn(response);

            mockMvc.perform(put("/api/v1/trades/{tradeId}/reject", tradeId)
                            .principal(authAs(userId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("REJECTED"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/trades/{tradeId}")
    class CancelTrade {

        @Test
        @DisplayName("200 - successful cancel")
        void shouldReturn200() throws Exception {
            UUID tradeId = UUID.randomUUID();
            TradeResponse response = TradeResponse.builder()
                    .id(tradeId).status("CANCELLED").updatedAt(LocalDateTime.now()).build();

            when(tradeService.cancelTrade(tradeId, userId)).thenReturn(response);

            mockMvc.perform(delete("/api/v1/trades/{tradeId}", tradeId)
                            .principal(authAs(userId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELLED"));
        }

        @Test
        @DisplayName("403 - not offerer")
        void shouldReturn403() throws Exception {
            UUID tradeId = UUID.randomUUID();
            when(tradeService.cancelTrade(tradeId, userId))
                    .thenThrow(new UnauthorizedException("Only the offerer can cancel this trade"));

            mockMvc.perform(delete("/api/v1/trades/{tradeId}", tradeId)
                            .principal(authAs(userId)))
                    .andExpect(status().isUnauthorized());
        }
    }
}
