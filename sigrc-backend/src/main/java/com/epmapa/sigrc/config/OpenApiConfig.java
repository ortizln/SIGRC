package com.epmapa.sigrc.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        var securityScheme = new SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT")
            .description("Ingrese el token JWT obtenido en el login");

        return new OpenAPI()
            .info(new Info()
                .title("SIGRC - Sistema Institucional de Gestión de Requerimientos, Cambios y Auditoría Tecnológica")
                .description("API REST del sistema integral de gestión de tickets, cambios y auditoría para EPMAPA-T")
                .version("1.0.0")
                .contact(new Contact()
                    .name("Dirección de Sistemas EPMAPA-T")
                    .email("sistemas@epmapa.gob.ec")))
            .components(new Components()
                .addSecuritySchemes("bearer-jwt", securityScheme))
            .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));
    }
}
