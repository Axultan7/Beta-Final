package com.microservices.gateway.config;

import com.microservices.gateway.filter.AuthenticationFilter;
import com.microservices.gateway.filter.RateLimitingFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Gateway route configuration.
 */
@Configuration
@RequiredArgsConstructor
public class GatewayConfig {

    private final AuthenticationFilter authenticationFilter;
    private final RateLimitingFilter rateLimitingFilter;

    @Value("${services.auth-service.url:http://localhost:8081}")
    private String authServiceUrl;

    @Value("${services.user-service.url:http://localhost:8082}")
    private String userServiceUrl;

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Auth Service Routes
                .route("auth-service", r -> r
                        .path("/api/v1/auth/**")
                        .filters(f -> f
                                .filter(rateLimitingFilter.apply(new RateLimitingFilter.Config()))
                                .filter(authenticationFilter.apply(new AuthenticationFilter.Config()))
                        )
                        .uri(authServiceUrl)
                )

                // User Service Routes
                .route("user-service", r -> r
                        .path("/api/v1/users/**")
                        .filters(f -> f
                                .filter(rateLimitingFilter.apply(new RateLimitingFilter.Config()))
                                .filter(authenticationFilter.apply(new AuthenticationFilter.Config()))
                        )
                        .uri(userServiceUrl)
                )

                // Auth Service Swagger
                .route("auth-service-swagger", r -> r
                        .path("/auth-service/v3/api-docs/**")
                        .filters(f -> f.rewritePath("/auth-service/v3/api-docs/(?<segment>.*)", "/v3/api-docs/${segment}"))
                        .uri(authServiceUrl)
                )

                // User Service Swagger
                .route("user-service-swagger", r -> r
                        .path("/user-service/v3/api-docs/**")
                        .filters(f -> f.rewritePath("/user-service/v3/api-docs/(?<segment>.*)", "/v3/api-docs/${segment}"))
                        .uri(userServiceUrl)
                )

                .build();
    }
}
