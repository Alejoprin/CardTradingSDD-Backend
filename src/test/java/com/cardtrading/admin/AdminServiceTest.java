package com.cardtrading.admin;

import com.cardtrading.admin.dto.BanUserResponse;
import com.cardtrading.admin.dto.StatsResponse;
import com.cardtrading.admin.service.AdminService;
import com.cardtrading.auth.entity.User;
import com.cardtrading.auth.repository.UserRepository;
import com.cardtrading.card.repository.CardRepository;
import com.cardtrading.shared.exception.BusinessRuleException;
import com.cardtrading.shared.exception.ResourceNotFoundException;
import com.cardtrading.trade.repository.TradeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private CardRepository cardRepository;
    @Mock private TradeRepository tradeRepository;

    @InjectMocks private AdminService adminService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(UUID.randomUUID()).username("testuser")
                .email("test@test.com").password("enc").role(User.Role.USER)
                .banned(false).build();
    }

    @Nested
    @DisplayName("banUser()")
    class BanUser {

        @Test
        @DisplayName("should ban user successfully")
        void shouldBan() {
            when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
            when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            BanUserResponse response = adminService.banUser(testUser.getId(), true, "Spam");

            assertThat(response.isBanned()).isTrue();
        }

        @Test
        @DisplayName("should throw when already banned")
        void shouldThrowAlreadyBanned() {
            testUser.setBanned(true);
            when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));

            assertThatThrownBy(() -> adminService.banUser(testUser.getId(), true, null))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("User is already banned");
        }

        @Test
        @DisplayName("should unban user")
        void shouldUnban() {
            testUser.setBanned(true);
            when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
            when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            BanUserResponse response = adminService.banUser(testUser.getId(), false, null);

            assertThat(response.isBanned()).isFalse();
        }

        @Test
        @DisplayName("should throw for unknown user")
        void shouldThrowNotFound() {
            UUID unknown = UUID.randomUUID();
            when(userRepository.findById(unknown)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adminService.banUser(unknown, true, null))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getStats()")
    class GetStats {

        @Test
        @DisplayName("should return aggregate stats")
        void shouldReturnStats() {
            when(userRepository.count()).thenReturn(10L);
            when(userRepository.findAll()).thenReturn(List.of(testUser));
            when(cardRepository.count()).thenReturn(50L);
            when(tradeRepository.count()).thenReturn(5L);
            when(tradeRepository.findAll()).thenReturn(List.of());

            StatsResponse stats = adminService.getStats();

            assertThat(stats.getTotalUsers()).isEqualTo(10);
            assertThat(stats.getTotalCards()).isEqualTo(50);
            assertThat(stats.getTotalTrades()).isEqualTo(5);
        }
    }
}
