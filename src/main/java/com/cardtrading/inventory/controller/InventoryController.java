package com.cardtrading.inventory.controller;

import com.cardtrading.inventory.dto.UserCardDto;
import com.cardtrading.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
        Page<UserCardDto> inventory = inventoryService.getUserInventory(userId, pageable);
        return ResponseEntity.ok(inventory);
    }
}
