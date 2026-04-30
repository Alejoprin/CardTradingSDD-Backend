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
            Authentication authentication) {
        UUID proposerId = UUID.fromString(authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(tradeService.createTrade(proposerId, request));
    }

    @GetMapping
    public ResponseEntity<Page<TradeResponse>> listTrades(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        return ResponseEntity.ok(tradeService.listUserTrades(userId, status, pageable));
    }

    @GetMapping("/{tradeId}")
    public ResponseEntity<TradeResponse> getTrade(
            @PathVariable UUID tradeId,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(tradeService.getTradeById(tradeId, userId));
    }

    @PutMapping("/{tradeId}/accept")
    public ResponseEntity<TradeResponse> acceptTrade(
            @PathVariable UUID tradeId,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(tradeService.acceptTrade(tradeId, userId));
    }

    @PutMapping("/{tradeId}/reject")
    public ResponseEntity<TradeResponse> rejectTrade(
            @PathVariable UUID tradeId,
            @RequestBody(required = false) RejectTradeRequest request,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        String reason = request != null ? request.getReason() : null;
        return ResponseEntity.ok(tradeService.rejectTrade(tradeId, userId, reason));
    }

    @DeleteMapping("/{tradeId}")
    public ResponseEntity<TradeResponse> cancelTrade(
            @PathVariable UUID tradeId,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(tradeService.cancelTrade(tradeId, userId));
    }
}
