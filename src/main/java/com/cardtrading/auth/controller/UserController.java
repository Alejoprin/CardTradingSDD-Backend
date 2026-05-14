package com.cardtrading.auth.controller;

import com.cardtrading.auth.dto.UpdateUserRequest;
import com.cardtrading.auth.dto.UserResponse;
import com.cardtrading.auth.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserProfile(
            @PathVariable UUID userId,
            Authentication authentication) {
        UUID requesterId = UUID.fromString(authentication.getName());
        UserResponse response = userService.getUserProfile(requesterId, userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{userId}")
    public ResponseEntity<UserResponse> updateUserProfile(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserRequest request,
            Authentication authentication) {
        UUID requesterId = UUID.fromString(authentication.getName());
        UserResponse response = userService.updateUserProfile(requesterId, userId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/{userId}/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserResponse> uploadProfileImage(
            @PathVariable UUID userId,
            @RequestPart("image") MultipartFile image,
            Authentication authentication) {
        UUID requesterId = UUID.fromString(authentication.getName());
        UserResponse response = userService.uploadProfileImage(requesterId, userId, image);
        return ResponseEntity.ok(response);
    }
}
