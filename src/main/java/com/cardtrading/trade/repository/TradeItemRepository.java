package com.cardtrading.trade.repository;

import com.cardtrading.trade.entity.Trade;
import com.cardtrading.trade.entity.TradeItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TradeItemRepository extends JpaRepository<TradeItem, UUID> {
    List<TradeItem> findByTradeId(UUID tradeId);

    @Query("SELECT COUNT(ti) > 0 FROM TradeItem ti WHERE ti.userCard.id = :userCardId AND ti.trade.status IN :statuses")
    boolean existsByUserCardIdAndTradeStatusIn(@Param("userCardId") UUID userCardId,
                                               @Param("statuses") List<Trade.TradeStatus> statuses);
}
