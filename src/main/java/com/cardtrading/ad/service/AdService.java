package com.cardtrading.ad.service;

import com.cardtrading.ad.dto.AdResponse;
import com.cardtrading.ad.dto.CreateAdRequest;
import com.cardtrading.ad.dto.UpdateAdRequest;
import com.cardtrading.ad.entity.Ad;
import com.cardtrading.ad.entity.Ad.AdStatus;
import com.cardtrading.ad.entity.Ad.AdType;
import com.cardtrading.ad.repository.AdRepository;
import com.cardtrading.auth.entity.User;
import com.cardtrading.auth.repository.UserRepository;
import com.cardtrading.card.entity.Card;
import com.cardtrading.card.repository.CardRepository;
import com.cardtrading.shared.exception.BusinessRuleException;
import com.cardtrading.shared.exception.ResourceNotFoundException;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdService {

    private final AdRepository adRepository;
    private final CardRepository cardRepository;
    private final UserRepository userRepository;

    public Page<AdResponse> listAds(String type, String cardName, Pageable pageable) {
        Specification<Ad> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("status"), AdStatus.ACTIVE));

            if (type != null && !type.isBlank()) {
                predicates.add(cb.equal(root.get("type"), AdType.valueOf(type.toUpperCase())));
            }

            if (cardName != null && !cardName.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("cardName")), "%" + cardName.toLowerCase() + "%"));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return adRepository.findAll(spec, pageable).map(this::toResponse);
    }

    public AdResponse getAdById(UUID adId) {
        Ad ad = adRepository.findById(adId)
                .orElseThrow(() -> new ResourceNotFoundException("Ad not found: " + adId));
        return toResponse(ad);
    }

    public Page<AdResponse> listUserAds(UUID userId, Pageable pageable) {
        return adRepository.findByUserId(userId, pageable).map(this::toResponse);
    }

    @Transactional
    public AdResponse createAd(UUID userId, CreateAdRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        Card card = cardRepository.findById(request.getCardId())
                .orElseThrow(() -> new ResourceNotFoundException("Card not found: " + request.getCardId()));

        if (request.getType() == null || request.getType().isBlank()) {
            throw new BusinessRuleException("Type is required (SELL or TRADE)");
        }

        AdType adType;
        try {
            adType = AdType.valueOf(request.getType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException("Invalid type: " + request.getType() + ". Must be SELL or TRADE");
        }

        if (adType == AdType.SELL && (request.getPrice() == null || request.getPrice().compareTo(java.math.BigDecimal.ZERO) <= 0)) {
            throw new BusinessRuleException("Price is required and must be greater than 0 for SELL ads");
        }

        Ad ad = Ad.builder()
                .user(user)
                .type(adType)
                .card(card)
                .cardName(card.getName())
                .cardImageUrl(card.getImageUrl())
                .price(request.getPrice())
                .description(request.getDescription())
                .status(AdStatus.ACTIVE)
                .build();

        ad = adRepository.save(ad);
        return toResponse(ad);
    }

    @Transactional
    public AdResponse updateAd(UUID adId, UUID userId, UpdateAdRequest request) {
        Ad ad = adRepository.findById(adId)
                .orElseThrow(() -> new ResourceNotFoundException("Ad not found: " + adId));

        if (!ad.getUser().getId().equals(userId)) {
            throw new BusinessRuleException("You can only update your own ads");
        }

        if (request.getType() != null && !request.getType().isBlank()) {
            try {
                ad.setType(AdType.valueOf(request.getType().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new BusinessRuleException("Invalid type: " + request.getType());
            }
        }

        if (request.getDescription() != null) {
            ad.setDescription(request.getDescription());
        }

        if (request.getPrice() != null) {
            if (ad.getType() == AdType.SELL && request.getPrice().compareTo(java.math.BigDecimal.ZERO) <= 0) {
                throw new BusinessRuleException("Price must be greater than 0 for SELL ads");
            }
            ad.setPrice(request.getPrice());
        }

        ad = adRepository.save(ad);
        return toResponse(ad);
    }

    @Transactional
    public void deleteAd(UUID adId, UUID userId) {
        Ad ad = adRepository.findById(adId)
                .orElseThrow(() -> new ResourceNotFoundException("Ad not found: " + adId));

        if (!ad.getUser().getId().equals(userId)) {
            throw new BusinessRuleException("You can only delete your own ads");
        }

        ad.setStatus(AdStatus.CLOSED);
        adRepository.save(ad);
    }

    private AdResponse toResponse(Ad ad) {
        return AdResponse.builder()
                .id(ad.getId())
                .type(ad.getType().name())
                .userId(ad.getUser().getId())
                .username(ad.getUser().getUsername())
                .cardId(ad.getCard().getId())
                .cardName(ad.getCardName())
                .cardImageUrl(ad.getCardImageUrl())
                .price(ad.getPrice())
                .description(ad.getDescription())
                .status(ad.getStatus().name())
                .createdAt(ad.getCreatedAt())
                .build();
    }
}
