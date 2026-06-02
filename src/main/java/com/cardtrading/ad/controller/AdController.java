package com.cardtrading.ad.controller;

import com.cardtrading.ad.dto.AdResponse;
import com.cardtrading.ad.dto.CreateAdRequest;
import com.cardtrading.ad.dto.UpdateAdRequest;
import com.cardtrading.ad.service.AdService;
import com.cardtrading.shared.dto.RestPage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ads")
@RequiredArgsConstructor
public class AdController {

    private final AdService adService;

    @GetMapping
    public ResponseEntity<RestPage<AdResponse>> listAds(
            @RequestParam(required = false) String type,
            @RequestParam(name = "cardName", required = false) String cardName,
            Pageable pageable) {
        Page<AdResponse> page = adService.listAds(type, cardName, pageable);
        return ResponseEntity.ok(new RestPage<>(page));
    }

    @PostMapping
    public ResponseEntity<AdResponse> createAd(
            @Valid @RequestBody CreateAdRequest request,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        AdResponse response = adService.createAd(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{adId}")
    public ResponseEntity<AdResponse> getAd(@PathVariable UUID adId) {
        return ResponseEntity.ok(adService.getAdById(adId));
    }

    @PutMapping("/{adId}")
    public ResponseEntity<AdResponse> updateAd(
            @PathVariable UUID adId,
            @Valid @RequestBody UpdateAdRequest request,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(adService.updateAd(adId, userId, request));
    }

    @DeleteMapping("/{adId}")
    public ResponseEntity<Void> deleteAd(
            @PathVariable UUID adId,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        adService.deleteAd(adId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<RestPage<AdResponse>> listUserAds(
            @PathVariable UUID userId,
            Pageable pageable) {
        Page<AdResponse> page = adService.listUserAds(userId, pageable);
        return ResponseEntity.ok(new RestPage<>(page));
    }
}
