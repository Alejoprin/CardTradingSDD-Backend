package com.cardtrading.shared.security;

import com.cardtrading.auth.entity.User;
import com.cardtrading.auth.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final String REFRESH_COOKIE = "refresh_token";
    private static final int COOKIE_MAX_AGE = 7 * 24 * 60 * 60;

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Value("${app.frontend.base-path:}")
    private String frontendBasePath;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found after OAuth2 login"));

        String accessToken  = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String refreshToken = jwtService.generateRefreshToken(user.getId(), user.getEmail());

        // Setear refresh token como cookie HttpOnly — igual que AuthController
        ResponseCookie refreshCookie = ResponseCookie.from(REFRESH_COOKIE, refreshToken)
                .httpOnly(true)
                .secure(true)
                .path("/api/v1/auth")
                .maxAge(COOKIE_MAX_AGE)
                .sameSite("None")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        // Solo el accessToken va en la URL — el refreshToken ya está en la cookie
        String frontendUrl = allowedOrigins.split(",")[0].trim();
        String redirectUrl = frontendUrl + frontendBasePath + "/#/oauth2/callback?accessToken=" + accessToken;
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}