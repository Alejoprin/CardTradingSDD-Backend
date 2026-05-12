package com.cardtrading.shared.security;

import com.cardtrading.auth.entity.User;
import com.cardtrading.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;


@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String sub = (String) attributes.get("sub");

        log.info("OAuth2 login attempt for email: {}", email);

        User user = userRepository.findByEmail(email).orElseGet(() -> {
            log.info("User not found, creating new user for email: {}", email);
            User saved = userRepository.save(User.builder()
                    .email(email)
                    .username(name != null ? name : email.split("@")[0])
                    .provider("GOOGLE")
                    .providerId(sub)
                    .build());
            log.info("User created with id: {}", saved.getId());
            return saved;
        });

        log.info("OAuth2 loadUser completed for: {}", user.getId());
        return oAuth2User;
    }
}