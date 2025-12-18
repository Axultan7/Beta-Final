package com.microservices.sender;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.microservices.sender", "com.microservices.shared"})
@OpenAPIDefinition(
        info = @Info(
                title = "Sender Service API",
                version = "1.0.0",
                description = "Service for sending user requests",
                contact = @Contact(name = "API Support", email = "support@example.com")
        ),
        servers = {
                @Server(url = "http://localhost:8083", description = "Local Development Server"),
                @Server(url = "http://sender-service:8083", description = "Docker Environment")
        }
)
public class SenderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SenderServiceApplication.class, args);
    }
}
