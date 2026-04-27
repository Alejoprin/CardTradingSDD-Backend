package com.cardtrading.inventory.repository;

import com.cardtrading.inventory.entity.UserCard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserCardRepository extends JpaRepository<UserCard, UUID> {

    Optional<UserCard> findByUserIdAndCardId(UUID userId, UUID cardId);

    Page<UserCard> findByUserId(UUID userId, Pageable pageable);
}
