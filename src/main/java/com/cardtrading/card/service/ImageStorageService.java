package com.cardtrading.card.service;

import com.cardtrading.shared.exception.BusinessRuleException;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageStorageService {

    private static final List<String> ALLOWED_TYPES = List.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB

    public static final String FOLDER_CARDS = "cards";
    public static final String FOLDER_USERS = "User_Img";

    private final Cloudinary cloudinary;

    public String storeCardImage(MultipartFile file) {
        return store(file, FOLDER_CARDS);
    }

    public String storeUserImage(MultipartFile file) {
        return store(file, FOLDER_USERS);
    }

    public void delete(String imageUrl) {
        if (imageUrl == null) return;

        // Extract public_id from Cloudinary URL:
        // https://res.cloudinary.com/{cloud}/image/upload/v{version}/{public_id}.{ext}
        String marker = "/image/upload/";
        int idx = imageUrl.indexOf(marker);
        if (idx == -1) return;

        String afterUpload = imageUrl.substring(idx + marker.length());
        // Strip version segment if present (e.g. "v1234567890/")
        if (afterUpload.startsWith("v") && afterUpload.contains("/")) {
            afterUpload = afterUpload.substring(afterUpload.indexOf('/') + 1);
        }
        // Strip file extension
        int dotIdx = afterUpload.lastIndexOf('.');
        String publicId = dotIdx != -1 ? afterUpload.substring(0, dotIdx) : afterUpload;

        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            log.info("Image deleted from Cloudinary: {}", publicId);
        } catch (Exception e) {
            log.warn("Could not delete image from Cloudinary: {}", publicId);
        }
    }

    private String store(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException("Image file is required");
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new BusinessRuleException("Only JPEG, PNG and WebP images are allowed");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new BusinessRuleException("Image must not exceed 5 MB");
        }

        String publicId = folder + "/" + UUID.randomUUID();

        try {
            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "public_id", publicId,
                    "overwrite", false
            ));
            String url = (String) result.get("secure_url");
            log.info("Image uploaded to Cloudinary [{}]: {}", folder, url);
            return url;
        } catch (IOException e) {
            log.error("Failed to upload image to Cloudinary: {}", e.getMessage());
            throw new BusinessRuleException("Could not upload image file");
        }
    }
}
