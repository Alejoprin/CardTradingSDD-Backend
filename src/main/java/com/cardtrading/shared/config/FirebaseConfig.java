package com.cardtrading.shared.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Configuration
@Slf4j
public class FirebaseConfig {

    @Value("${app.firebase.credentials-path:}")
    private String credentialsPath;

    @Value("${app.firebase.credentials-json:}")
    private String credentialsJson;

    @Value("${app.firebase.bucket}")
    private String bucket;

    @PostConstruct
    public void initialize() throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            return;
        }

        GoogleCredentials credentials;
        if (!credentialsPath.isBlank()) {
            try (InputStream is = new FileInputStream(credentialsPath)) {
                credentials = GoogleCredentials.fromStream(is);
            }
            log.info("Firebase initialized from credentials file");
        } else {
            try (InputStream is = new ByteArrayInputStream(credentialsJson.getBytes())) {
                credentials = GoogleCredentials.fromStream(is);
            }
            log.info("Firebase initialized from credentials JSON env var");
        }

        String cleanBucket = bucket.startsWith("gs://") ? bucket.substring(5) : bucket;
        FirebaseApp.initializeApp(FirebaseOptions.builder()
                .setCredentials(credentials)
                .setStorageBucket(cleanBucket)
                .build());
    }
}
