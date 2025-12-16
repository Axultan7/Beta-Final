package com.microservices.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Gateway filter for rate limiting requests.
 */
@Slf4j
@Component
public class RateLimitingFilter extends AbstractGatewayFilterFactory<RateLimitingFilter.Config> {

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    @Value("${rate-limit.requests-per-minute:60}")
    private int requestsPerMinute;

    public RateLimitingFilter(ReactiveRedisTemplate<String, String> redisTemplate) {
        super(Config.class);
        this.redisTemplate = redisTemplate;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String clientIp = getClientIp(exchange);
            String rateLimitKey = "rate_limit:" + clientIp;

            return redisTemplate.opsForValue().increment(rateLimitKey)
                    .flatMap(count -> {
                        if (count == 1) {
                            // First request, set expiration
                            return redisTemplate.expire(rateLimitKey, Duration.ofMinutes(1))
                                    .then(chain.filter(exchange));
                        } else if (count <= requestsPerMinute) {
                            // Within limit
                            return chain.filter(exchange);
                        } else {
                            // Rate limit exceeded
                            log.warn("Rate limit exceeded for IP: {}", clientIp);
                            return onRateLimitExceeded(exchange);
                        }
                    })
                    .onErrorResume(e -> {
                        log.error("Rate limiting error: {}", e.getMessage());
                        // Allow request on Redis error
                        return chain.filter(exchange);
                    });
        };
    }

    private String getClientIp(ServerWebExchange exchange) {
        String forwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isEmpty()) {
            return forwardedFor.split(",")[0].trim();
        }
        var remoteAddress = exchange.getRequest().getRemoteAddress();
        return remoteAddress != null ? remoteAddress.getAddress().getHostAddress() : "unknown";
    }

    private Mono<Void> onRateLimitExceeded(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().add("Content-Type", "application/json");
        response.getHeaders().add("Retry-After", "60");

        String body = String.format(
                "{\"success\":false,\"message\":\"Rate limit exceeded. Please try again later.\",\"timestamp\":\"%s\"}",
                java.time.LocalDateTime.now()
        );

        return response.writeWith(Mono.just(
                response.bufferFactory().wrap(body.getBytes())
        ));
    }

    public static class Config {
        // Configuration properties can be added here
    }
}
