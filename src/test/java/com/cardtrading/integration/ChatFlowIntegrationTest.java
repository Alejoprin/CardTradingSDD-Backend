package com.cardtrading.integration;

import com.cardtrading.ad.dto.AdResponse;
import com.cardtrading.ad.dto.CreateAdRequest;
import com.cardtrading.auth.dto.LoginRequest;
import com.cardtrading.auth.dto.RegisterRequest;
import com.cardtrading.auth.dto.TokenResponse;
import com.cardtrading.chat.dto.ChatResponse;
import com.cardtrading.chat.dto.CreateChatRequest;
import com.cardtrading.chat.dto.MessageResponse;
import com.cardtrading.chat.dto.SendMessageRequest;
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

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class ChatFlowIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:15"))
            .withDatabaseName("cardtrading_chat_test")
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

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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

    private UUID seedCard(String name, String rarity) throws Exception {
        UUID setId = UUID.randomUUID();
        UUID gameId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();

        jdbcTemplate.update("INSERT INTO card_games (id, name, slug, is_active, created_at, updated_at) VALUES (?, ?, ?, true, NOW(), NOW())",
                gameId, "Test Game", "test-game");
        jdbcTemplate.update("INSERT INTO card_sets (id, game_id, name, code, created_at, updated_at) VALUES (?, ?, ?, ?, NOW(), NOW())",
                setId, gameId, "Test Set", "TS1");
        jdbcTemplate.update("INSERT INTO cards (id, set_id, name, rarity, created_at, updated_at) VALUES (?, ?, ?, ?, NOW(), NOW())",
                cardId, setId, name, rarity);

        return cardId;
    }

    @Test
    @DisplayName("Full chat flow: create ad, chat, and send messages")
    void fullChatFlow() throws Exception {
        String sellerToken = registerAndLogin("seller", "seller@test.com");
        String buyerToken = registerAndLogin("buyer", "buyer@test.com");

        UUID cardId = seedCard("Charizard", "LEGENDARY");

        String sellerResponse = mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + sellerToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        UUID sellerUserId = objectMapper.readTree(sellerResponse).get("id").asText().equals("") ? null : null;

        CreateAdRequest adRequest = new CreateAdRequest();
        adRequest.setType("SELL");
        adRequest.setCardId(cardId);
        adRequest.setPrice(BigDecimal.valueOf(25.00));
        adRequest.setDescription("Mint Charizard");

        MvcResult adResult = mockMvc.perform(post("/api/v1/ads")
                        .header("Authorization", "Bearer " + sellerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        AdResponse adResponse = objectMapper.readValue(adResult.getResponse().getContentAsString(), AdResponse.class);

        CreateChatRequest chatRequest = new CreateChatRequest();
        chatRequest.setAdId(adResponse.getId());

        MvcResult chatResult = mockMvc.perform(post("/api/v1/chats")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chatRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        ChatResponse chatResponse = objectMapper.readValue(chatResult.getResponse().getContentAsString(), ChatResponse.class);

        SendMessageRequest msgRequest = new SendMessageRequest();
        msgRequest.setContent("Hi, I'm interested in your Charizard!");

        MvcResult msgResult = mockMvc.perform(post("/api/v1/chats/{chatId}/messages", chatResponse.getId())
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(msgRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        MessageResponse msgResponse = objectMapper.readValue(msgResult.getResponse().getContentAsString(), MessageResponse.class);

        mockMvc.perform(get("/api/v1/chats/{chatId}/messages", chatResponse.getId())
                        .header("Authorization", "Bearer " + sellerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].content").value("Hi, I'm interested in your Charizard!"));
    }
}
