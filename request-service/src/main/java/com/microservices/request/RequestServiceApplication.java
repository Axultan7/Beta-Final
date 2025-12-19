package com.microservices.request;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@ComponentScan(basePackages = {"com.microservices.request", "com.microservices.shared"})
@OpenAPIDefinition(
        info = @Info(
                title = "Request Service API",
                version = "1.0.0",
                description = "Service for processing user requests",
                contact = @Contact(name = "API Support", email = "support@example.com")
        ),
        servers = {
                @Server(url = "http://localhost:8085", description = "API Gateway"),
                @Server(url = "http://localhost:8084", description = "Local Development Server")
        }
)
public class RequestServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RequestServiceApplication.class, args);
    }
}
