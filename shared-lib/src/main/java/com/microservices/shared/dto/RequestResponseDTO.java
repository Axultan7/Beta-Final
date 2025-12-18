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
@Schema(description = "Response to a user request")
public class RequestResponseDTO {

    @Schema(description = "Request ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private String requestId;

    @Schema(description = "Response status", example = "ACCEPTED")
    private String status;

    @Schema(description = "Response message", example = "Your request has been accepted and is being processed")
    private String message;

    @Schema(description = "Response timestamp")
    private LocalDateTime timestamp;

    public static RequestResponseDTO accepted(String requestId) {
        return RequestResponseDTO.builder()
                .requestId(requestId)
                .status("ACCEPTED")
                .message("Ваш запрос принят и обрабатывается")
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static RequestResponseDTO processed(String requestId, String message) {
        return RequestResponseDTO.builder()
                .requestId(requestId)
                .status("PROCESSED")
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
