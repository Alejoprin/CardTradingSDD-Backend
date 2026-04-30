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
import java.util.UUID;

@Repository
public interface TradeRepository extends JpaRepository<Trade, UUID> {

    @Query("SELECT t FROM Trade t WHERE (t.proposer.id = :userId OR t.receiver.id = :userId)")
    Page<Trade> findByProposerIdOrReceiverId(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT t FROM Trade t WHERE (t.proposer.id = :userId OR t.receiver.id = :userId) AND t.status = :status")
    Page<Trade> findByProposerIdOrReceiverIdAndStatus(@Param("userId") UUID userId,
                                                      @Param("status") Trade.TradeStatus status,
                                                      Pageable pageable);

    long countByProposerIdAndCreatedAtAfter(UUID proposerId, LocalDateTime after);

    Page<Trade> findByStatus(Trade.TradeStatus status, Pageable pageable);

    long countByStatus(Trade.TradeStatus status);

    @Query("SELECT t FROM Trade t WHERE t.status = 'PENDING' AND t.proposedAt < :expireBefore")
    List<Trade> findExpiredPendingTrades(@Param("expireBefore") LocalDateTime expireBefore);

    @Query("SELECT t FROM Trade t WHERE t.status = 'ACCEPTED' AND t.respondedAt < :stuckBefore")
    List<Trade> findStuckAcceptedTrades(@Param("stuckBefore") LocalDateTime stuckBefore);

    @Query("SELECT DISTINCT t FROM Trade t JOIN t.items i WHERE t.status = 'PENDING' AND i.userCard.id = :userCardId AND t.id <> :excludeTradeId")
    List<Trade> findPendingTradesByUserCardIdExcluding(@Param("userCardId") UUID userCardId,
                                                       @Param("excludeTradeId") UUID excludeTradeId);
}
