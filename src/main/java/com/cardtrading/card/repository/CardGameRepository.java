package com.cardtrading.card.repository;

import com.cardtrading.card.entity.CardGame;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CardGameRepository extends JpaRepository<CardGame, UUID> {
    Optional<CardGame> findBySlug(String slug);
}
