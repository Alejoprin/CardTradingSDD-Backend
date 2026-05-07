package com.cardtrading.card.controller;

import com.cardtrading.card.dto.*;
import com.cardtrading.card.service.CardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cards")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;

    @GetMapping
    public ResponseEntity<Page<CardSummaryResponse>> listCards(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String rarity,
            @RequestParam(required = false) UUID setId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        return ResponseEntity.ok(cardService.listCards(search, rarity, setId, pageable));
    }

    @GetMapping("/sets")
    public ResponseEntity<List<CardSetDto>> getAllSets(
            @RequestParam(required = false) UUID gameId) {
        return ResponseEntity.ok(cardService.getAllSets(gameId));
    }

    @GetMapping("/games")
    public ResponseEntity<List<CardGameDto>> getAllGames() {
        return ResponseEntity.ok(cardService.getAllGames());
    }


    @GetMapping("/{cardId}")
    public ResponseEntity<CardDetailResponse> getCard(@PathVariable UUID cardId) {
        return ResponseEntity.ok(cardService.getCardById(cardId));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CardDetailResponse> createCard(@Valid @RequestBody CardRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cardService.createCard(request, null));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CardDetailResponse> createCardWithImage(
            @RequestPart("data") @Valid CardRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cardService.createCard(request, image));
    }

    @PutMapping(value = "/{cardId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CardDetailResponse> updateCard(
            @PathVariable UUID cardId,
            @Valid @RequestBody CardRequest request) {
        return ResponseEntity.ok(cardService.updateCard(cardId, request, null));
    }

    @PutMapping(value = "/{cardId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CardDetailResponse> updateCardWithImage(
            @PathVariable UUID cardId,
            @RequestPart("data") @Valid CardRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        return ResponseEntity.ok(cardService.updateCard(cardId, request, image));
    }

    @DeleteMapping("/{cardId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCard(@PathVariable UUID cardId) {
        cardService.deleteCard(cardId);
        return ResponseEntity.noContent().build();
    }
}
