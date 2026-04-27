package com.cardtrading.trade.repository;

import com.cardtrading.trade.entity.Trade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TradeRepository extends JpaRepository<Trade, UUID> {

    Page<Trade> findByOffererId(UUID offererId, Pageable pageable);

    Page<Trade> findByReceiverId(UUID receiverId, Pageable pageable);

    @Query("SELECT t FROM Trade t WHERE (t.offerer.id = :userId OR t.receiver.id = :userId)")
    Page<Trade> findByOffererIdOrReceiverId(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT t FROM Trade t WHERE (t.offerer.id = :userId OR t.receiver.id = :userId) AND t.status = :status")
    Page<Trade> findByOffererIdOrReceiverIdAndStatus(@Param("userId") UUID userId,
                                                      @Param("status") Trade.TradeStatus status,
                                                      Pageable pageable);

    long countByOffererIdAndCreatedAtAfter(UUID offererId, LocalDateTime after);

    Optional<Trade> findByIdempotencyKey(String idempotencyKey);

    @Query("SELECT t FROM Trade t WHERE t.status = 'PENDING' AND t.createdAt < :expireBefore")
    List<Trade> findExpiredPendingTrades(@Param("expireBefore") LocalDateTime expireBefore);
}
