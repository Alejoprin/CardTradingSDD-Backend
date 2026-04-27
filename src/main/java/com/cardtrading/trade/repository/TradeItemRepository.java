package com.cardtrading.trade.repository;

import com.cardtrading.trade.entity.TradeItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TradeItemRepository extends JpaRepository<TradeItem, UUID> {

    List<TradeItem> findByTradeId(UUID tradeId);

    List<TradeItem> findByTradeIdAndSide(UUID tradeId, TradeItem.TradeSide side);
}
