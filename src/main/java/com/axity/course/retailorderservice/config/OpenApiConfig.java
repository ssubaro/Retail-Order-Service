package com.axity.course.retailorderservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI retailOrderServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("RetailOrder Service API")
                        .description("Microservicio de catálogo de productos, inventario y pedidos del "
                                + "proyecto integrador de retail.")
                        .version("v1")
                        .contact(new Contact().name("Proyecto Integrador - Curso Backend Especializado")));
    }
}
