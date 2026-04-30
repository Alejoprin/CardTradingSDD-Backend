package com.cardtrading.inventory.controller;

import com.cardtrading.inventory.dto.AddCatalogCardRequest;
import com.cardtrading.inventory.dto.CustomCardRequest;
import com.cardtrading.inventory.dto.UserCardDto;
import com.cardtrading.inventory.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/{userId}/inventory")
    public ResponseEntity<Page<UserCardDto>> getUserInventory(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        return ResponseEntity.ok(inventoryService.getUserInventory(userId, pageable));
    }

    @PostMapping("/{userId}/inventory")
    public ResponseEntity<UserCardDto> addCatalogCard(
            @PathVariable UUID userId,
            @Valid @RequestBody AddCatalogCardRequest request,
            Authentication authentication) {
        UUID requesterId = UUID.fromString(authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(inventoryService.addCatalogCard(userId, requesterId, request));
    }

    @PostMapping(value = "/{userId}/inventory/custom", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserCardDto> addCustomCard(
            @PathVariable UUID userId,
            @RequestPart("data") @Valid CustomCardRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image,
            Authentication authentication) {
        UUID requesterId = UUID.fromString(authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(inventoryService.addCustomCard(userId, requesterId, request, image));
    }

    @DeleteMapping("/{userId}/inventory/{userCardId}")
    public ResponseEntity<Void> removeCard(
            @PathVariable UUID userId,
            @PathVariable UUID userCardId,
            Authentication authentication) {
        UUID requesterId = UUID.fromString(authentication.getName());
        inventoryService.removeUserCard(userCardId, requesterId);
        return ResponseEntity.noContent().build();
    }
}
