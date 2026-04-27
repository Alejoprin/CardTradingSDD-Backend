package com.cardtrading.auth.service;

import com.cardtrading.auth.dto.UpdateUserRequest;
import com.cardtrading.auth.dto.UserResponse;
import com.cardtrading.auth.entity.User;
import com.cardtrading.auth.repository.UserRepository;
import com.cardtrading.shared.exception.BusinessRuleException;
import com.cardtrading.shared.exception.ResourceNotFoundException;
import com.cardtrading.shared.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    public UserResponse getUserProfile(UUID requesterId, UUID targetId) {
        User target = userRepository.findById(targetId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        UserResponse.UserResponseBuilder builder = UserResponse.builder()
                .id(target.getId())
                .username(target.getUsername())
                .role(target.getRole().name())
                .createdAt(target.getCreatedAt());

        // Only show email to profile owner or admin
        if (requesterId.equals(targetId) || isAdmin(requesterId)) {
            builder.email(target.getEmail());
        }

        return builder.build();
    }

    @Transactional
    public UserResponse updateUserProfile(UUID requesterId, UUID targetId, UpdateUserRequest request) {
        if (!requesterId.equals(targetId)) {
            throw new UnauthorizedException("You are not authorized to update this profile");
        }

        User user = userRepository.findById(targetId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new BusinessRuleException("Username already taken");
            }
            user.setUsername(request.getUsername());
        }

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail().toLowerCase())) {
                throw new BusinessRuleException("Email already in use");
            }
            user.setEmail(request.getEmail().toLowerCase());
        }

        user = userRepository.save(user);
        log.info("User profile updated: userId={}", targetId);

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private boolean isAdmin(UUID userId) {
        return userRepository.findById(userId)
                .map(u -> u.getRole() == User.Role.ADMIN)
                .orElse(false);
    }
}
