package com.microservices.shared.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Request for sending a user message")
public class SendRequestDTO {

    @Schema(description = "Category number (1-10)", example = "1")
    @Min(value = 1, message = "Category must be at least 1")
    @Max(value = 10, message = "Category must be at most 10")
    private Integer category;

    @Schema(description = "User message", example = "I need help with my order")
    @NotBlank(message = "Message cannot be blank")
    @Size(min = 1, max = 2000, message = "Message must be between 1 and 2000 characters")
    private String message;
}
