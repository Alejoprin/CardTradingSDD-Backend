package com.cardtrading.card.repository;

import com.cardtrading.card.entity.CustomCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomCardRepository extends JpaRepository<CustomCard, UUID> {
    Optional<CustomCard> findByIdAndOwnerId(UUID id, UUID ownerId);
}
