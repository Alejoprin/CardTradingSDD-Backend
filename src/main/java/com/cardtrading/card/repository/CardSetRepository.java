package com.cardtrading.card.repository;

import com.cardtrading.card.entity.CardSet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CardSetRepository extends JpaRepository<CardSet, UUID> {
    Page<CardSet> findByGameId(UUID gameId, Pageable pageable);
}
