package com.microservices.sender.controller;

import com.microservices.sender.service.SenderService;
import com.microservices.shared.dto.ApiResponse;
import com.microservices.shared.dto.RequestResponseDTO;
import com.microservices.shared.dto.SendRequestDTO;
import com.microservices.shared.dto.UserRequestDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sender")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Sender", description = "API for sending user requests")
@SecurityRequirement(name = "Bearer Authentication")
public class SenderController {

    private final SenderService senderService;

    @PostMapping("/send")
    @Operation(summary = "Send a new request", description = "Send a new request with category and message")
    public ResponseEntity<ApiResponse<RequestResponseDTO>> sendRequest(
            @Valid @RequestBody SendRequestDTO request,
            Authentication authentication) {

        String userId = (String) authentication.getDetails();
        log.info("User {} sending request with category: {}", userId, request.getCategory());

        RequestResponseDTO response = senderService.sendRequest(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Запрос успешно отправлен", response));
    }

    @GetMapping("/requests")
    @Operation(summary = "Get user's requests", description = "Get all requests for the authenticated user")
    public ResponseEntity<ApiResponse<Page<UserRequestDTO>>> getMyRequests(
            @PageableDefault(size = 10) Pageable pageable,
            Authentication authentication) {

        String userId = (String) authentication.getDetails();
        Page<UserRequestDTO> requests = senderService.getUserRequests(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(requests));
    }

    @GetMapping("/requests/{requestId}")
    @Operation(summary = "Get request by ID", description = "Get a specific request by its ID")
    public ResponseEntity<ApiResponse<UserRequestDTO>> getRequest(
            @PathVariable String requestId) {

        UserRequestDTO request = senderService.getRequestById(requestId);
        return ResponseEntity.ok(ApiResponse.success(request));
    }

    @PutMapping("/requests/{requestId}/status")
    @Operation(summary = "Update request status", description = "Update status of a request (internal use)")
    public ResponseEntity<ApiResponse<Void>> updateRequestStatus(
            @PathVariable String requestId,
            @RequestParam String status,
            @RequestParam(required = false) String responseMessage) {

        senderService.updateRequestStatus(requestId, status, responseMessage);
        return ResponseEntity.ok(ApiResponse.success("Статус обновлен"));
    }
}
