package com.cardtrading.card.service;

import com.cardtrading.card.dto.CardDetailResponse;
import com.cardtrading.card.dto.CardRequest;
import com.cardtrading.card.dto.CardSummaryResponse;
import com.cardtrading.card.entity.Card;
import com.cardtrading.card.entity.CardSet;
import com.cardtrading.card.repository.CardRepository;
import com.cardtrading.card.repository.CardSetRepository;
import com.cardtrading.shared.exception.BusinessRuleException;
import com.cardtrading.shared.exception.ResourceNotFoundException;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.cardtrading.shared.dto.RestPage;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardService {

    private final CardRepository cardRepository;
    private final CardSetRepository cardSetRepository;
    private final ImageStorageService imageStorageService;

    @Cacheable(value = "card:catalog", key = "#search + '-' + #rarity + '-' + #setId + '-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    @Transactional(readOnly = true)
    public Page<CardSummaryResponse> listCards(String search, String rarity, UUID setId, Pageable pageable) {
        Specification<Card> spec = buildSpecification(search, rarity, setId);
        return new RestPage<>(cardRepository.findAll(spec, pageable).map(this::toSummary));
    }

    @Cacheable(value = "card:detail", key = "#cardId")
    @Transactional(readOnly = true)
    public CardDetailResponse getCardById(UUID cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found"));
        return toDetail(card);
    }

    @Transactional
    @CacheEvict(value = {"card:catalog", "card:detail"}, allEntries = true)
    public CardDetailResponse createCard(CardRequest request, MultipartFile image) {
        CardSet set = cardSetRepository.findById(request.getSetId())
                .orElseThrow(() -> new ResourceNotFoundException("Card set not found"));

        if (cardRepository.existsByNameAndSetId(request.getName(), set.getId())) {
            throw new BusinessRuleException("A card with that name already exists in this set");
        }

        String imageUrl = (image != null && !image.isEmpty()) ? imageStorageService.store(image) : null;

        Card card = Card.builder()
                .set(set)
                .name(request.getName())
                .cardNumber(request.getCardNumber())
                .rarity(Card.Rarity.valueOf(request.getRarity()))
                .attributes(request.getAttributes())
                .imageUrl(imageUrl)
                .marketPrice(request.getMarketPrice())
                .build();

        card = cardRepository.save(card);
        log.info("Card created: cardId={} name={}", card.getId(), card.getName());
        return toDetail(card);
    }

    @Transactional
    @CacheEvict(value = {"card:catalog", "card:detail"}, allEntries = true)
    public CardDetailResponse updateCard(UUID cardId, CardRequest request, MultipartFile image) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found"));

        if (request.getSetId() != null) {
            CardSet set = cardSetRepository.findById(request.getSetId())
                    .orElseThrow(() -> new ResourceNotFoundException("Card set not found"));
            card.setSet(set);
        }
        if (request.getName() != null) card.setName(request.getName());
        if (request.getCardNumber() != null) card.setCardNumber(request.getCardNumber());
        if (request.getRarity() != null) card.setRarity(Card.Rarity.valueOf(request.getRarity()));
        if (request.getAttributes() != null) card.setAttributes(request.getAttributes());
        if (request.getMarketPrice() != null) card.setMarketPrice(request.getMarketPrice());

        if (image != null && !image.isEmpty()) {
            imageStorageService.delete(card.getImageUrl());
            card.setImageUrl(imageStorageService.store(image));
        }

        card = cardRepository.save(card);
        log.info("Card updated: cardId={}", card.getId());
        return toDetail(card);
    }

    @Transactional
    @CacheEvict(value = {"card:catalog", "card:detail"}, allEntries = true)
    public void deleteCard(UUID cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found"));
        imageStorageService.delete(card.getImageUrl());
        cardRepository.delete(card);
        log.info("Card deleted: cardId={}", cardId);
    }

    private Specification<Card> buildSpecification(String search, String rarity, UUID setId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + search.toLowerCase() + "%"));
            }
            if (rarity != null && !rarity.isBlank()) {
                predicates.add(cb.equal(root.get("rarity"), Card.Rarity.valueOf(rarity.toUpperCase())));
            }
            if (setId != null) {
                predicates.add(cb.equal(root.get("set").get("id"), setId));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private CardSummaryResponse toSummary(Card card) {
        return CardSummaryResponse.builder()
                .id(card.getId())
                .setId(card.getSet().getId())
                .setName(card.getSet().getName())
                .gameName(card.getSet().getGame().getName())
                .name(card.getName())
                .cardNumber(card.getCardNumber())
                .rarity(card.getRarity().name())
                .imageUrl(card.getImageUrl())
                .imageSmallUrl(card.getImageSmallUrl())
                .marketPrice(card.getMarketPrice())
                .build();
    }

    private CardDetailResponse toDetail(Card card) {
        return CardDetailResponse.builder()
                .id(card.getId())
                .setId(card.getSet().getId())
                .setName(card.getSet().getName())
                .setCode(card.getSet().getCode())
                .gameId(card.getSet().getGame().getId())
                .gameName(card.getSet().getGame().getName())
                .name(card.getName())
                .cardNumber(card.getCardNumber())
                .rarity(card.getRarity().name())
                .attributes(card.getAttributes())
                .imageUrl(card.getImageUrl())
                .imageSmallUrl(card.getImageSmallUrl())
                .marketPrice(card.getMarketPrice())
                .lastPriceUpdate(card.getLastPriceUpdate())
                .createdAt(card.getCreatedAt())
                .updatedAt(card.getUpdatedAt())
                .build();
    }
}
