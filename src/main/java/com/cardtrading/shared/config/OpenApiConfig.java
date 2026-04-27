package com.cardtrading.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;  // ← AÑADIR
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;  // ← AÑADIR

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Card Trading Platform API")
                        .description("REST API for collectible card trading")
                        .version("v1"))
                .servers(List.of(  // ← AÑADIR ESTO
                        new Server()
                                .url("https://cardtradingsdd-backend-production.up.railway.app")
                                .description("Production server"),
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local development")
                ))  // ← FIN AÑADIDO
                .addSecurityItem(new SecurityRequirement().addList("Bearer"))
                .components(new Components()
                        .addSecuritySchemes("Bearer", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}