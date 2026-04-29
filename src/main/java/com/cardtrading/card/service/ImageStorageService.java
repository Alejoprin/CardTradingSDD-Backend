package com.cardtrading.card.service;

import com.cardtrading.shared.exception.BusinessRuleException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class ImageStorageService {

    private static final List<String> ALLOWED_TYPES = List.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    @Value("${app.upload.base-url:/uploads}")
    private String baseUrl;

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException("Image file is required");
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new BusinessRuleException("Only JPEG, PNG and WebP images are allowed");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new BusinessRuleException("Image must not exceed 5 MB");
        }

        String extension = getExtension(file.getOriginalFilename());
        String filename = UUID.randomUUID() + "." + extension;

        try {
            Path cardsDir = Paths.get(uploadDir, "cards");
            Files.createDirectories(cardsDir);
            file.transferTo(cardsDir.resolve(filename).toFile());
        } catch (IOException e) {
            log.error("Failed to store image: {}", e.getMessage());
            throw new BusinessRuleException("Could not store image file");
        }

        return baseUrl + "/cards/" + filename;
    }

    public void delete(String imageUrl) {
        if (imageUrl == null || !imageUrl.startsWith(baseUrl)) {
            return;
        }
        String relativePath = imageUrl.substring(baseUrl.length());
        Path filePath = Paths.get(uploadDir, relativePath);
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.warn("Could not delete image file: {}", filePath);
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "jpg";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
