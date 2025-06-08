package com.example.plgsystem.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${plgsystem.openapi.dev-url:http://localhost:8080}")
    private String devUrl;

    @Value("${plgsystem.openapi.prod-url:}")
    private String prodUrl;

    @Bean
    public OpenAPI myOpenAPI() {
        Server devServer = new Server();
        devServer.setUrl(devUrl);
        devServer.setDescription("Server URL in Development environment");

        Contact contact = new Contact();
        contact.setEmail("plgsystem@example.com");
        contact.setName("PLG System Team");
        contact.setUrl("https://www.example.com");

        License mitLicense = new License().name("MIT License").url("https://choosealicense.com/licenses/mit/");

        Info info = new Info()
                .title("PLG System Management API")
                .version("1.0")
                .contact(contact)
                .description("API para la gestión del sistema PLG (Propane Liquid Gas). " +
                           "Incluye gestión de vehículos, mantenimientos, bloqueos, incidentes y órdenes.")
                .termsOfService("https://www.example.com/terms")
                .license(mitLicense);

        OpenAPI openAPI = new OpenAPI().info(info);
        
        if (!prodUrl.isEmpty()) {
            Server prodServer = new Server();
            prodServer.setUrl(prodUrl);
            prodServer.setDescription("Server URL in Production environment");
            openAPI.servers(List.of(devServer, prodServer));
        } else {
            openAPI.servers(List.of(devServer));
        }

        return openAPI;
    }
}
