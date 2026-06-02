package com.cardtrading.ad;

import com.cardtrading.ad.controller.AdController;
import com.cardtrading.ad.dto.AdResponse;
import com.cardtrading.ad.dto.CreateAdRequest;
import com.cardtrading.ad.service.AdService;
import com.cardtrading.shared.exception.GlobalExceptionHandler;
import com.cardtrading.shared.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdControllerTest {

    @Mock
    private AdService adService;

    @InjectMocks
    private AdController adController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private UUID userId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
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
    @DisplayName("GET /api/v1/ads")
    class ListAds {

        @Test
        @DisplayName("200 - list all active ads")
        void shouldReturn200() throws Exception {
            AdResponse ad = createAdResponse();
            Page<AdResponse> page = new PageImpl<>(List.of(ad), PageRequest.of(0, 20), 1);
            when(adService.listAds(any(), any(), any())).thenReturn(page);

            mockMvc.perform(get("/api/v1/ads")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(ad.getId().toString()))
                    .andExpect(jsonPath("$.content[0].type").value("SELL"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/ads")
    class CreateAd {

        @Test
        @DisplayName("201 - successful ad creation")
        void shouldReturn201() throws Exception {
            CreateAdRequest request = new CreateAdRequest();
            request.setType("SELL");
            request.setCardId(UUID.randomUUID());
            request.setPrice(BigDecimal.valueOf(25.00));
            request.setDescription("Mint condition");

            AdResponse response = createAdResponse();
            when(adService.createAd(eq(userId), any())).thenReturn(response);

            mockMvc.perform(post("/api/v1/ads")
                            .principal(authAs(userId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.type").value("SELL"));
        }

        @Test
        @DisplayName("400 - missing type")
        void shouldReturn400OnMissingType() throws Exception {
            CreateAdRequest request = new CreateAdRequest();
            request.setCardId(UUID.randomUUID());

            mockMvc.perform(post("/api/v1/ads")
                            .principal(authAs(userId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/ads/{adId}")
    class GetAd {

        @Test
        @DisplayName("200 - ad found")
        void shouldReturn200() throws Exception {
            AdResponse ad = createAdResponse();
            when(adService.getAdById(ad.getId())).thenReturn(ad);

            mockMvc.perform(get("/api/v1/ads/{adId}", ad.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(ad.getId().toString()));
        }

        @Test
        @DisplayName("404 - ad not found")
        void shouldReturn404() throws Exception {
            UUID adId = UUID.randomUUID();
            when(adService.getAdById(adId)).thenThrow(new ResourceNotFoundException("Ad not found"));

            mockMvc.perform(get("/api/v1/ads/{adId}", adId))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/ads/{adId}")
    class DeleteAd {

        @Test
        @DisplayName("204 - ad deleted")
        void shouldReturn204() throws Exception {
            UUID adId = UUID.randomUUID();
            mockMvc.perform(delete("/api/v1/ads/{adId}", adId)
                            .principal(authAs(userId)))
                    .andExpect(status().isNoContent());
        }
    }

    private AdResponse createAdResponse() {
        return AdResponse.builder()
                .id(UUID.randomUUID())
                .type("SELL")
                .userId(userId)
                .username("testuser")
                .cardId(UUID.randomUUID())
                .cardName("Charizard")
                .cardImageUrl("http://example.com/card.png")
                .price(BigDecimal.valueOf(25.00))
                .description("Mint condition")
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .build();
    }
}
