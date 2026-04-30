package com.cardtrading.card.repository;

import com.cardtrading.card.entity.Card;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CardRepository extends JpaRepository<Card, UUID>, JpaSpecificationExecutor<Card> {
    Page<Card> findBySetId(UUID setId, Pageable pageable);
    boolean existsByNameAndSetId(String name, UUID setId);
}
