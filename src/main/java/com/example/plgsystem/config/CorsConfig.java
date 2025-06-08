package com.example.plgsystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(@NonNull CorsRegistry registry) { // Añade @NonNull aquí
                registry.addMapping("/**") // Aplica a todas las rutas
                        .allowedOriginPatterns("*") // Permite todos los orígenes. Más flexible que allowedOrigins("*")
                        .allowedMethods("*") // Permite todos los métodos (GET, POST, PUT, DELETE, etc.)
                        .allowedHeaders("*") // Permite todas las cabeceras
                        .allowCredentials(false); // Importante cuando allowedOriginPatterns es "*" y no necesitas cookies/auth headers de cross-origin
                // Si necesitaras credenciales, tendrías que especificar los orígenes
                // y poner esto a true, pero "*" con credenciales es inseguro y a menudo bloqueado.
            }
        };
    }
}