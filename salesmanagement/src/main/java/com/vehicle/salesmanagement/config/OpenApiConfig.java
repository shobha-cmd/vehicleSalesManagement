package com.vehicle.salesmanagement.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Vehicle Sales Management API")
                        .description("API for managing vehicle orders, finance, and dispatch/delivery workflows")
                        .version("1.0.0"))
                .tags(Arrays.asList(
                        new Tag().name("Vehicle Order Management").description("Endpoints for managing vehicle orders"),
                        new Tag().name("Vehicle Model Management").description("Endpoints for managing vehicle models and variants")
                ));
    }
}