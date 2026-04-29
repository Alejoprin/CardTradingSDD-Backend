package com.cardtrading.inventory.service;

import com.cardtrading.auth.entity.User;
import com.cardtrading.auth.repository.UserRepository;
import com.cardtrading.card.entity.Card;
import com.cardtrading.card.repository.CardRepository;
import com.cardtrading.card.service.ImageStorageService;
import com.cardtrading.inventory.dto.AddCatalogCardRequest;
import com.cardtrading.inventory.dto.CustomCardRequest;
import com.cardtrading.inventory.dto.UserCardDto;
import com.cardtrading.inventory.entity.UserCard;
import com.cardtrading.inventory.repository.UserCardRepository;
import com.cardtrading.shared.exception.BusinessRuleException;
import com.cardtrading.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final UserCardRepository userCardRepository;
    private final UserRepository userRepository;
    private final CardRepository cardRepository;
    private final ImageStorageService imageStorageService;

    public Page<UserCardDto> getUserInventory(UUID userId, Pageable pageable) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found");
        }
        return userCardRepository.findByUserId(userId, pageable).map(this::toDto);
    }

    @Transactional
    public void addCard(UUID userId, UUID cardId, int quantity, UserCard.AcquisitionSource source) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found"));

        userCardRepository.findByUserIdAndCardId(userId, cardId)
                .ifPresentOrElse(
                        existing -> {
                            existing.setQuantity(existing.getQuantity() + quantity);
                            userCardRepository.save(existing);
                            log.info("Updated inventory: userId={} cardId={} newQty={}", userId, cardId, existing.getQuantity());
                        },
                        () -> {
                            UserCard userCard = UserCard.builder()
                                    .user(user)
                                    .card(card)
                                    .quantity(quantity)
                                    .acquiredAt(LocalDateTime.now())
                                    .acquiredFrom(source)
                                    .build();
                            userCardRepository.save(userCard);
                            log.info("Added to inventory: userId={} cardId={} qty={}", userId, cardId, quantity);
                        }
                );
    }

    @Transactional
    public void removeCard(UUID userId, UUID cardId, int quantity) {
        UserCard userCard = userCardRepository.findByUserIdAndCardId(userId, cardId)
                .orElseThrow(() -> new BusinessRuleException("User does not own this card"));

        int newQuantity = userCard.getQuantity() - quantity;
        if (newQuantity < 0) {
            throw new BusinessRuleException("Insufficient card quantity");
        } else if (newQuantity == 0) {
            userCardRepository.delete(userCard);
            log.info("Removed from inventory: userId={} cardId={}", userId, cardId);
        } else {
            userCard.setQuantity(newQuantity);
            userCardRepository.save(userCard);
            log.info("Decremented inventory: userId={} cardId={} newQty={}", userId, cardId, newQuantity);
        }
    }

    @Transactional
    public UserCardDto addCatalogCard(UUID userId, UUID requesterId, AddCatalogCardRequest request) {
        if (!userId.equals(requesterId)) {
            throw new BusinessRuleException("You can only add cards to your own inventory");
        }
        Card card = cardRepository.findById(request.getCardId())
                .orElseThrow(() -> new ResourceNotFoundException("Card not found"));
        if (card.getOwnerId() != null) {
            throw new BusinessRuleException("Cannot add a custom card from another user's inventory");
        }
        addCard(userId, card.getId(), request.getQuantity(), UserCard.AcquisitionSource.MANUAL);
        UserCard userCard = userCardRepository.findByUserIdAndCardId(userId, card.getId()).orElseThrow();
        return toDto(userCard);
    }

    @Transactional
    public UserCardDto addCustomCard(UUID userId, UUID requesterId, CustomCardRequest request, MultipartFile image) {
        if (!userId.equals(requesterId)) {
            throw new BusinessRuleException("You can only add cards to your own inventory");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String imageUrl = (image != null && !image.isEmpty()) ? imageStorageService.store(image) : null;

        Card card = Card.builder()
                .name(request.getName())
                .description(request.getDescription())
                .rarity(Card.Rarity.valueOf(request.getRarity()))
                .cardType(Card.CardType.valueOf(request.getCardType()))
                .edition(request.getEdition())
                .imageUrl(imageUrl)
                .ownerId(userId)
                .build();
        card = cardRepository.save(card);

        UserCard userCard = UserCard.builder()
                .user(user)
                .card(card)
                .quantity(request.getQuantity())
                .acquiredFrom(UserCard.AcquisitionSource.MANUAL)
                .build();
        userCard = userCardRepository.save(userCard);
        log.info("Custom card added: userId={} cardId={}", userId, card.getId());
        return toDto(userCard);
    }

    private UserCardDto toDto(UserCard userCard) {
        Card card = userCard.getCard();
        return UserCardDto.builder()
                .cardId(card.getId())
                .cardName(card.getName())
                .description(card.getDescription())
                .rarity(card.getRarity().name())
                .cardType(card.getCardType().name())
                .edition(card.getEdition())
                .imageUrl(card.getImageUrl())
                .isCustom(card.getOwnerId() != null)
                .quantity(userCard.getQuantity())
                .acquiredAt(userCard.getAcquiredAt())
                .acquiredFrom(userCard.getAcquiredFrom().name())
                .build();
    }
}
