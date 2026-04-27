package com.cardtrading.inventory.service;

import com.cardtrading.auth.entity.User;
import com.cardtrading.auth.repository.UserRepository;
import com.cardtrading.card.entity.Card;
import com.cardtrading.card.repository.CardRepository;
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

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final UserCardRepository userCardRepository;
    private final UserRepository userRepository;
    private final CardRepository cardRepository;

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

    private UserCardDto toDto(UserCard userCard) {
        return UserCardDto.builder()
                .cardId(userCard.getCard().getId())
                .cardName(userCard.getCard().getName())
                .rarity(userCard.getCard().getRarity().name())
                .quantity(userCard.getQuantity())
                .acquiredAt(userCard.getAcquiredAt())
                .acquiredFrom(userCard.getAcquiredFrom().name())
                .build();
    }
}
