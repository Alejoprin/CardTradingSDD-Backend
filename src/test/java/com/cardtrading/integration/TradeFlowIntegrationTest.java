package com.cardtrading.integration;

import com.cardtrading.auth.dto.LoginRequest;
import com.cardtrading.auth.dto.RegisterRequest;
import com.cardtrading.auth.dto.TokenResponse;
import com.cardtrading.trade.dto.CreateTradeRequest;
import com.cardtrading.trade.dto.TradeItemRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class TradeFlowIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:15"))
            .withDatabaseName("cardtrading_trade_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    @SuppressWarnings("resource")
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7"))
            .withExposedPorts(6379);

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbcTemplate;

    private String registerAndLogin(String username, String email) throws Exception {
        RegisterRequest reg = RegisterRequest.builder()
                .username(username).email(email).password("Password1").build();
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated());

        LoginRequest login = LoginRequest.builder().email(email).password("Password1").build();
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readValue(result.getResponse().getContentAsString(), TokenResponse.class)
                .getAccessToken();
    }

    private UUID seedCard(String name, String rarity) {
        UUID cardId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO cards (id, name, rarity, card_type, created_at, updated_at) VALUES (?, ?, ?, 'MONSTER', NOW(), NOW())",
                cardId, name, rarity);
        return cardId;
    }

    private void seedInventory(UUID userId, UUID cardId, int quantity) {
        jdbcTemplate.update(
                "INSERT INTO user_cards (id, user_id, card_id, quantity, acquired_at, acquired_from, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, NOW(), 'SYSTEM', NOW(), NOW())",
                UUID.randomUUID(), userId, cardId, quantity);
    }

    private UUID getUserIdByEmail(String email) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = ?", UUID.class, email);
    }

    @Test
    @DisplayName("Full trade flow: create -> accept -> inventory exchanged")
    void fullTradeFlow() throws Exception {
        String tokenA = registerAndLogin("traderA", "traderA@test.com");
        String tokenB = registerAndLogin("traderB", "traderB@test.com");

        UUID userA = getUserIdByEmail("traderA@test.com");
        UUID userB = getUserIdByEmail("traderB@test.com");

        UUID cardA = seedCard("Dragon Sword", "LEGENDARY");
        UUID cardB = seedCard("Shield Wall", "RARE");

        seedInventory(userA, cardA, 3);
        seedInventory(userB, cardB, 2);

        // Create trade: A offers Dragon Sword, requests Shield Wall
        CreateTradeRequest tradeRequest = CreateTradeRequest.builder()
                .receiverId(userB)
                .offeredCards(List.of(TradeItemRequest.builder().cardId(cardA).quantity(1).build()))
                .requestedCards(List.of(TradeItemRequest.builder().cardId(cardB).quantity(1).build()))
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/trades")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tradeRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();

        String tradeId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("id").asText();

        // B accepts the trade
        mockMvc.perform(put("/api/v1/trades/{tradeId}/accept", tradeId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        // Give Kafka consumer time to process
        Thread.sleep(3000);

        // Verify trade is COMPLETED
        mockMvc.perform(get("/api/v1/trades/{tradeId}", tradeId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }
}
