package com.microservices.shared.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "User request information")
public class UserRequestDTO {

    @Schema(description = "Request ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private String id;

    @Schema(description = "User ID who sent the request", example = "550e8400-e29b-41d4-a716-446655440001")
    private String userId;

    @Schema(description = "Category number", example = "1")
    private Integer category;

    @Schema(description = "User message", example = "I need help with my order")
    private String message;

    @Schema(description = "Request status", example = "PENDING")
    private String status;

    @Schema(description = "Response message from system", example = "Your request has been accepted")
    private String responseMessage;

    @Schema(description = "Request creation time")
    private LocalDateTime createdAt;

    @Schema(description = "Request update time")
    private LocalDateTime updatedAt;
}
