package com.cardtrading.card.service;

import com.cardtrading.card.dto.CardDetailResponse;
import com.cardtrading.card.dto.CardRequest;
import com.cardtrading.card.dto.CardSummaryResponse;
import com.cardtrading.card.entity.Card;
import com.cardtrading.card.repository.CardRepository;
import com.cardtrading.shared.exception.BusinessRuleException;
import com.cardtrading.shared.exception.ResourceNotFoundException;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardService {

    private final CardRepository cardRepository;

    @Cacheable(value = "card:catalog", key = "#search + '-' + #rarity + '-' + #cardType + '-' + #edition + '-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<CardSummaryResponse> listCards(String search, String rarity, String cardType, String edition, Pageable pageable) {
        Specification<Card> spec = buildSpecification(search, rarity, cardType, edition);
        return cardRepository.findAll(spec, pageable).map(this::toSummary);
    }

    @Cacheable(value = "card:detail", key = "#cardId")
    public CardDetailResponse getCardById(UUID cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found"));
        return toDetail(card);
    }

    @Transactional
    @CacheEvict(value = {"card:catalog", "card:detail"}, allEntries = true)
    public CardDetailResponse createCard(CardRequest request) {
        if (cardRepository.existsByName(request.getName())) {
            throw new BusinessRuleException("A card with that name already exists");
        }

        Card card = Card.builder()
                .name(request.getName())
                .description(request.getDescription())
                .rarity(Card.Rarity.valueOf(request.getRarity()))
                .cardType(Card.CardType.valueOf(request.getCardType()))
                .edition(request.getEdition())
                .imageUrl(request.getImageUrl())
                .build();

        card = cardRepository.save(card);
        log.info("Card created: cardId={} name={}", card.getId(), card.getName());
        return toDetail(card);
    }

    @Transactional
    @CacheEvict(value = {"card:catalog", "card:detail"}, allEntries = true)
    public CardDetailResponse updateCard(UUID cardId, CardRequest request) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found"));

        if (request.getName() != null && !request.getName().equals(card.getName())) {
            if (cardRepository.existsByName(request.getName())) {
                throw new BusinessRuleException("A card with that name already exists");
            }
            card.setName(request.getName());
        }
        if (request.getDescription() != null) card.setDescription(request.getDescription());
        if (request.getRarity() != null) card.setRarity(Card.Rarity.valueOf(request.getRarity()));
        if (request.getCardType() != null) card.setCardType(Card.CardType.valueOf(request.getCardType()));
        if (request.getEdition() != null) card.setEdition(request.getEdition());
        if (request.getImageUrl() != null) card.setImageUrl(request.getImageUrl());

        card = cardRepository.save(card);
        log.info("Card updated: cardId={}", card.getId());
        return toDetail(card);
    }

    @Transactional
    @CacheEvict(value = {"card:catalog", "card:detail"}, allEntries = true)
    public void deleteCard(UUID cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found"));
        card.setDeletedAt(LocalDateTime.now());
        cardRepository.save(card);
        log.info("Card soft-deleted: cardId={}", cardId);
    }

    private Specification<Card> buildSpecification(String search, String rarity, String cardType, String edition) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + search.toLowerCase() + "%"));
            }
            if (rarity != null && !rarity.isBlank()) {
                predicates.add(cb.equal(root.get("rarity"), Card.Rarity.valueOf(rarity)));
            }
            if (cardType != null && !cardType.isBlank()) {
                predicates.add(cb.equal(root.get("cardType"), Card.CardType.valueOf(cardType)));
            }
            if (edition != null && !edition.isBlank()) {
                predicates.add(cb.equal(root.get("edition"), edition));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private CardSummaryResponse toSummary(Card card) {
        return CardSummaryResponse.builder()
                .id(card.getId())
                .name(card.getName())
                .rarity(card.getRarity().name())
                .cardType(card.getCardType().name())
                .edition(card.getEdition())
                .imageUrl(card.getImageUrl())
                .build();
    }

    private CardDetailResponse toDetail(Card card) {
        return CardDetailResponse.builder()
                .id(card.getId())
                .name(card.getName())
                .description(card.getDescription())
                .rarity(card.getRarity().name())
                .cardType(card.getCardType().name())
                .edition(card.getEdition())
                .imageUrl(card.getImageUrl())
                .createdAt(card.getCreatedAt())
                .updatedAt(card.getUpdatedAt())
                .build();
    }
}
