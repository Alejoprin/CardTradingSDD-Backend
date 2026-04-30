package com.cardtrading.admin.service;

import com.cardtrading.admin.dto.AdminUserResponse;
import com.cardtrading.admin.dto.BanUserResponse;
import com.cardtrading.admin.dto.StatsResponse;
import com.cardtrading.auth.entity.User;
import com.cardtrading.auth.repository.UserRepository;
import com.cardtrading.card.repository.CardRepository;
import com.cardtrading.shared.exception.BusinessRuleException;
import com.cardtrading.shared.exception.ResourceNotFoundException;
import com.cardtrading.trade.dto.TradeResponse;
import com.cardtrading.trade.entity.Trade;
import com.cardtrading.trade.repository.TradeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final UserRepository userRepository;
    private final CardRepository cardRepository;
    private final TradeRepository tradeRepository;

    public Page<AdminUserResponse> listUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::toAdminUserResponse);
    }

    @Transactional(readOnly = true)
    public Page<TradeResponse> listAllTrades(String status, Pageable pageable) {
        if (status != null && !status.isBlank()) {
            Trade.TradeStatus tradeStatus = Trade.TradeStatus.valueOf(status.toUpperCase());
            return tradeRepository.findByStatus(tradeStatus, pageable).map(this::toTradeResponse);
        }
        return tradeRepository.findAll(pageable).map(this::toTradeResponse);
    }

    @Transactional
    public BanUserResponse banUser(UUID userId, boolean banned, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.isBanned() == banned) {
            throw new BusinessRuleException(banned ? "User is already banned" : "User is not banned");
        }

        user.setBanned(banned);
        user = userRepository.save(user);

        log.info("User {} {}: userId={} reason={}", banned ? "banned" : "unbanned",
                user.getUsername(), userId, reason);

        return BanUserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .isBanned(user.isBanned())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    public StatsResponse getStats() {
        long totalUsers = userRepository.count();
        long bannedUsers = userRepository.countByBannedTrue();
        long totalCards = cardRepository.count();
        long totalTrades = tradeRepository.count();

        Map<String, Long> tradesByStatus = new LinkedHashMap<>();
        for (Trade.TradeStatus s : Trade.TradeStatus.values()) {
            tradesByStatus.put(s.name(), tradeRepository.countByStatus(s));
        }

        return StatsResponse.builder()
                .totalUsers(totalUsers)
                .activeUsers(totalUsers - bannedUsers)
                .bannedUsers(bannedUsers)
                .totalCards(totalCards)
                .totalTrades(totalTrades)
                .tradesByStatus(tradesByStatus)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    private AdminUserResponse toAdminUserResponse(User user) {
        return AdminUserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .isBanned(user.isBanned())
                .createdAt(user.getCreatedAt())
                .deletedAt(user.getDeletedAt())
                .build();
    }

    private TradeResponse toTradeResponse(Trade trade) {
        return TradeResponse.builder()
                .id(trade.getId())
                .proposerId(trade.getProposer().getId())
                .proposerUsername(trade.getProposer().getUsername())
                .receiverId(trade.getReceiver().getId())
                .receiverUsername(trade.getReceiver().getUsername())
                .status(trade.getStatus().name())
                .proposedAt(trade.getProposedAt())
                .createdAt(trade.getCreatedAt())
                .updatedAt(trade.getUpdatedAt())
                .build();
    }
}
