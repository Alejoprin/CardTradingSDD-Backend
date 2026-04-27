package com.cardtrading.admin.controller;

import com.cardtrading.admin.dto.*;
import com.cardtrading.admin.service.AdminService;
import com.cardtrading.trade.dto.TradeResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users")
    public ResponseEntity<Page<AdminUserResponse>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        return ResponseEntity.ok(adminService.listUsers(pageable));
    }

    @GetMapping("/trades")
    public ResponseEntity<Page<TradeResponse>> listAllTrades(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        return ResponseEntity.ok(adminService.listAllTrades(status, pageable));
    }

    @PutMapping("/users/{userId}/ban")
    public ResponseEntity<BanUserResponse> banUser(
            @PathVariable UUID userId,
            @Valid @RequestBody BanUserRequest request) {
        BanUserResponse response = adminService.banUser(userId, request.getBanned(), request.getReason());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats")
    public ResponseEntity<StatsResponse> getStats() {
        return ResponseEntity.ok(adminService.getStats());
    }
}
