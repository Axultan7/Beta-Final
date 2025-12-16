package com.microservices.gateway.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.properties.SwaggerUiConfigParameters;
import org.springframework.boot.CommandLineRunner;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashSet;
import java.util.Set;

/**
 * OpenAPI/Swagger configuration for API Gateway.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter JWT token")
                        )
                );
    }

    @Bean
    public CommandLineRunner openApiGroups(
            RouteDefinitionLocator locator,
            SwaggerUiConfigParameters swaggerUiConfigParameters) {
        return args -> {
            Set<String> addedGroups = new HashSet<>();

            locator.getRouteDefinitions()
                    .filter(routeDefinition -> {
                        String id = routeDefinition.getId();
                        return id != null &&
                                (id.equals("auth-service") || id.equals("user-service")) &&
                                !addedGroups.contains(id);
                    })
                    .subscribe(routeDefinition -> {
                        String id = routeDefinition.getId();
                        swaggerUiConfigParameters.addGroup(id);
                        addedGroups.add(id);
                    });
        };
    }
}
