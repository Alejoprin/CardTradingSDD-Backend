package com.cardtrading.inventory.service;

import com.cardtrading.auth.entity.User;
import com.cardtrading.auth.repository.UserRepository;
import com.cardtrading.card.entity.Card;
import com.cardtrading.card.entity.CustomCard;
import com.cardtrading.card.repository.CardRepository;
import com.cardtrading.card.repository.CustomCardRepository;
import com.cardtrading.card.service.ImageStorageService;
import com.cardtrading.inventory.dto.AddCatalogCardRequest;
import com.cardtrading.inventory.dto.CardOwnerDto;
import com.cardtrading.inventory.dto.CustomCardRequest;
import com.cardtrading.inventory.dto.UserCardDto;
import com.cardtrading.inventory.entity.UserCard;
import com.cardtrading.inventory.repository.UserCardRepository;
import com.cardtrading.shared.exception.BusinessRuleException;
import com.cardtrading.shared.exception.ResourceNotFoundException;
import com.cardtrading.trade.entity.Trade;
import com.cardtrading.trade.repository.TradeItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final UserCardRepository userCardRepository;
    private final UserRepository userRepository;
    private final CardRepository cardRepository;
    private final CustomCardRepository customCardRepository;
    private final TradeItemRepository tradeItemRepository;
    private final ImageStorageService imageStorageService;

    @Transactional(readOnly = true)
    public Page<UserCardDto> getUserInventory(UUID userId, Pageable pageable) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found");
        }
        return userCardRepository.findByUserId(userId, pageable).map(this::toDto);
    }

    @Transactional
    public UserCardDto addCatalogCard(UUID userId, UUID requesterId, AddCatalogCardRequest request) {
        if (!userId.equals(requesterId)) {
            throw new BusinessRuleException("You can only add cards to your own inventory");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Card card = cardRepository.findById(request.getCardId())
                .orElseThrow(() -> new ResourceNotFoundException("Card not found"));

        UserCard.CardCondition condition = UserCard.CardCondition.valueOf(request.getCondition().toUpperCase());

        // Buscar si ya existe una carta con las mismas características
        Optional<UserCard> existingUserCard = userCardRepository.findByUserAndCardAndCondition(user, card, condition);

        UserCard userCard;
        if (existingUserCard.isPresent()) {
            // Si existe, incrementar la cantidad
            userCard = existingUserCard.get();
            userCard.setQuantity(userCard.getQuantity() + request.getQuantity());
            log.info("Card quantity updated: userId={} cardId={} oldQty={} newQty={}",
                    userId, card.getId(), userCard.getQuantity() - request.getQuantity(), userCard.getQuantity());
        } else {
            // Si no existe, crear nueva entrada
            userCard = UserCard.builder()
                    .user(user)
                    .card(card)
                    .quantity(request.getQuantity())
                    .condition(condition)
                    .notes(request.getNotes())
                    .forTrade(false)
                    .forSale(false)
                    .build();
            log.info("Card added to inventory: userId={} cardId={} qty={}", userId, card.getId(), request.getQuantity());
        }

        userCard = userCardRepository.save(userCard);
        return toDto(userCard);
    }
    @Transactional
    public UserCardDto addCustomCard(UUID userId, UUID requesterId, CustomCardRequest request, MultipartFile image) {
        if (!userId.equals(requesterId)) {
            throw new BusinessRuleException("You can only add cards to your own inventory");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Card.Rarity rarity = Card.Rarity.valueOf(request.getRarity().toUpperCase());
        UserCard.CardCondition condition = UserCard.CardCondition.valueOf(request.getCondition().toUpperCase());

        String imageUrl = null;
        if (image != null && !image.isEmpty()) {
            imageUrl = imageStorageService.store(image);
        }

        CustomCard customCard = CustomCard.builder()
                .owner(user)
                .name(request.getName())
                .cardNumber(request.getCardNumber())
                .rarity(rarity)
                .attributes(request.getAttributes())
                .imageUrl(imageUrl)
                .notes(request.getNotes())
                .build();
        customCard = customCardRepository.save(customCard);

        UserCard userCard = UserCard.builder()
                .user(user)
                .customCard(customCard)
                .quantity(request.getQuantity())
                .condition(condition)
                .forTrade(false)
                .forSale(false)
                .build();
        userCard = userCardRepository.save(userCard);

        log.info("Custom card added to inventory: userId={} customCardId={}", userId, customCard.getId());
        return toDto(userCard);
    }

    @Transactional
    public void transferUserCard(UUID userCardId, UUID toUserId, int quantity) {
        UserCard source = userCardRepository.findById(userCardId)
                .orElseThrow(() -> new ResourceNotFoundException("User card not found"));
        User newOwner = userRepository.findById(toUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (quantity == source.getQuantity()) {
            UUID catalogCardId = source.getCard() != null ? source.getCard().getId() : null;
            UUID customCardId = source.getCustomCard() != null ? source.getCustomCard().getId() : null;

            Optional<UserCard> existing = catalogCardId != null
                    ? userCardRepository.findByUserIdAndCardIdAndCondition(toUserId, catalogCardId, source.getCondition())
                    : userCardRepository.findByUserIdAndCustomCardIdAndCondition(toUserId, customCardId, source.getCondition());

            if (existing.isPresent()) {
                existing.get().setQuantity(existing.get().getQuantity() + quantity);
                userCardRepository.save(existing.get());
                userCardRepository.delete(source);
            } else {
                source.setUser(newOwner);
                source.setForTrade(false);
                source.setForSale(false);
                userCardRepository.save(source);
            }
        } else {
            // Partial transfer: reduce source quantity, add to receiver's inventory
            source.setQuantity(source.getQuantity() - quantity);
            userCardRepository.save(source);

            UUID catalogCardId = source.getCard() != null ? source.getCard().getId() : null;
            UUID customCardId = source.getCustomCard() != null ? source.getCustomCard().getId() : null;

            Optional<UserCard> existing = catalogCardId != null
                    ? userCardRepository.findByUserIdAndCardIdAndCondition(toUserId, catalogCardId, source.getCondition())
                    : userCardRepository.findByUserIdAndCustomCardIdAndCondition(toUserId, customCardId, source.getCondition());

            existing.ifPresentOrElse(
                            found -> {
                                found.setQuantity(found.getQuantity() + quantity);
                                userCardRepository.save(found);
                            },
                            () -> {
                                UserCard received = UserCard.builder()
                                        .user(newOwner)
                                        .card(source.getCard())
                                        .customCard(source.getCustomCard())
                                        .quantity(quantity)
                                        .condition(source.getCondition())
                                        .forTrade(false)
                                        .forSale(false)
                                        .build();
                                userCardRepository.save(received);
                            }
                    );
        }

        log.info("UserCard transferred: userCardId={} toUserId={} quantity={}", userCardId, toUserId, quantity);
    }

    @Transactional
    public void removeUserCard(UUID userCardId, UUID requesterId) {
        UserCard userCard = userCardRepository.findByIdAndUserId(userCardId, requesterId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found in your inventory"));

        if (tradeItemRepository.existsByUserCardIdAndTradeStatusIn(userCardId,
                List.of(Trade.TradeStatus.PENDING, Trade.TradeStatus.ACCEPTED))) {
            throw new BusinessRuleException("Cannot remove a card that is part of an active trade");
        }

        userCardRepository.delete(userCard);
        log.info("UserCard removed: userCardId={} userId={}", userCardId, requesterId);
    }

    @Transactional(readOnly = true)
    public List<CardOwnerDto> getCardOwners(UUID cardId, UUID excludeUserId) {
        if (!cardRepository.existsById(cardId)) {
            throw new ResourceNotFoundException("Card not found");
        }
        return userCardRepository.findByCardIdExcludingUser(cardId, excludeUserId)
                .stream()
                .map(uc -> CardOwnerDto.builder()
                        .userId(uc.getUser().getId())
                        .username(uc.getUser().getUsername())
                        .userCardId(uc.getId())
                        .condition(uc.getCondition().name())
                        .quantity(uc.getQuantity())
                        .build())
                .collect(Collectors.toList());
    }

    private UserCardDto toDto(UserCard userCard) {
        UserCardDto.UserCardDtoBuilder builder = UserCardDto.builder()
                .userCardId(userCard.getId())
                .quantity(userCard.getQuantity())
                .condition(userCard.getCondition().name())
                .forTrade(userCard.isForTrade())
                .forSale(userCard.isForSale())
                .notes(userCard.getNotes())
                .acquiredAt(userCard.getAcquiredAt());

        if (userCard.getCustomCard() != null) {
            CustomCard cc = userCard.getCustomCard();
            builder.customCardId(cc.getId())
                    .custom(true)
                    .cardName(cc.getName())
                    .cardNumber(cc.getCardNumber())
                    .rarity(cc.getRarity().name())
                    .imageUrl(cc.getImageUrl())
                    .imageSmallUrl(cc.getImageSmallUrl());
        } else {
            Card card = userCard.getCard();
            builder.cardId(card.getId())
                    .custom(false)
                    .cardName(card.getName())
                    .cardNumber(card.getCardNumber())
                    .rarity(card.getRarity().name())
                    .imageUrl(card.getImageUrl())
                    .imageSmallUrl(card.getImageSmallUrl())
                    .marketPrice(card.getMarketPrice())
                    .setName(card.getSet().getName())
                    .gameName(card.getSet().getGame().getName());
        }

        return builder.build();
    }
}
