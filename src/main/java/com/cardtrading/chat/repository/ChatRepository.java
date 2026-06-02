package com.cardtrading.chat.repository;

import com.cardtrading.chat.entity.Chat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatRepository extends JpaRepository<Chat, UUID> {

    @Query("SELECT c FROM Chat c WHERE c.buyer.id = :userId OR c.seller.id = :userId")
    Page<Chat> findByBuyerIdOrSellerId(UUID userId, Pageable pageable);

    Optional<Chat> findByAdId(UUID adId);
}
