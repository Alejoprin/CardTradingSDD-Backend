package com.cardtrading.card.service;

import com.cardtrading.shared.exception.BusinessRuleException;
import com.google.cloud.storage.Acl;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.firebase.cloud.StorageClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class ImageStorageService {

    private static final List<String> ALLOWED_TYPES = List.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB
    private static final String GCS_BASE_URL = "https://storage.googleapis.com/";

    @Value("${app.firebase.bucket}")
    private String bucket;

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
        String blobName = "cards/" + UUID.randomUUID() + "." + extension;

        try {
            com.google.cloud.storage.Storage storage = StorageClient.getInstance().bucket().getStorage();
            BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(bucket, blobName))
                    .setContentType(file.getContentType())
                    .setAcl(List.of(Acl.of(Acl.User.ofAllUsers(), Acl.Role.READER)))
                    .build();
            storage.create(blobInfo, file.getBytes());
        } catch (IOException e) {
            log.error("Failed to upload image to Firebase Storage: {}", e.getMessage());
            throw new BusinessRuleException("Could not upload image file");
        }

        String url = GCS_BASE_URL + bucket + "/" + blobName;
        log.info("Image uploaded: {}", url);
        return url;
    }

    public void delete(String imageUrl) {
        if (imageUrl == null) return;
        String prefix = GCS_BASE_URL + bucket + "/";
        if (!imageUrl.startsWith(prefix)) return;

        String blobName = imageUrl.substring(prefix.length());
        try {
            StorageClient.getInstance().bucket().getStorage()
                    .delete(BlobId.of(bucket, blobName));
        } catch (Exception e) {
            log.warn("Could not delete image from Firebase Storage: {}", blobName);
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "jpg";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
