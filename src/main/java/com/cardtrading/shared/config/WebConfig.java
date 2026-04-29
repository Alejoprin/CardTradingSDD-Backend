package com.cardtrading.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

import static org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO;

@Configuration
// Esta anotación le dice a Spring que use un formato compatible con JSON para las páginas
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
public class WebConfig {
    // No necesitas escribir nada más aquí por ahora
}