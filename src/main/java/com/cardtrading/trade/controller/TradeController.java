package com.cardtrading.trade.controller;

import com.cardtrading.trade.dto.CreateTradeRequest;
import com.cardtrading.trade.dto.RejectTradeRequest;
import com.cardtrading.trade.dto.TradeResponse;
import com.cardtrading.trade.service.TradeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/trades")
@RequiredArgsConstructor
public class TradeController {

    private final TradeService tradeService;

    @PostMapping
    public ResponseEntity<TradeResponse> createTrade(
            @Valid @RequestBody CreateTradeRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            Authentication authentication) {
        UUID offererId = UUID.fromString(authentication.getName());
        TradeResponse response = tradeService.createTrade(offererId, request, idempotencyKey);

        if (response.getMessage() != null && response.getMessage().contains("Duplicate")) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<TradeResponse>> listTrades(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String direction,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<TradeResponse> trades = tradeService.listUserTrades(userId, status, direction, pageable);
        return ResponseEntity.ok(trades);
    }

    @GetMapping("/{tradeId}")
    public ResponseEntity<TradeResponse> getTrade(
            @PathVariable UUID tradeId,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        TradeResponse response = tradeService.getTradeById(tradeId, userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{tradeId}/accept")
    public ResponseEntity<TradeResponse> acceptTrade(
            @PathVariable UUID tradeId,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        TradeResponse response = tradeService.acceptTrade(tradeId, userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{tradeId}/reject")
    public ResponseEntity<TradeResponse> rejectTrade(
            @PathVariable UUID tradeId,
            @RequestBody(required = false) RejectTradeRequest request,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        String reason = request != null ? request.getReason() : null;
        TradeResponse response = tradeService.rejectTrade(tradeId, userId, reason);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{tradeId}")
    public ResponseEntity<TradeResponse> cancelTrade(
            @PathVariable UUID tradeId,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        TradeResponse response = tradeService.cancelTrade(tradeId, userId);
        return ResponseEntity.ok(response);
    }
}
