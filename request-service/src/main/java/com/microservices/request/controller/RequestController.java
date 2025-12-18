package com.microservices.request.controller;

import com.microservices.request.service.RequestProcessorService;
import com.microservices.shared.dto.ApiResponse;
import com.microservices.shared.dto.UserRequestDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/requests")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Request Processor", description = "API for processing user requests")
public class RequestController {

    private final RequestProcessorService processorService;

    @PostMapping("/process")
    @Operation(summary = "Process a request", description = "Receive and process a user request")
    public ResponseEntity<ApiResponse<Void>> processRequest(@RequestBody UserRequestDTO request) {
        log.info("Received request for processing: id={}, category={}", request.getId(), request.getCategory());

        processorService.processRequest(request);

        return ResponseEntity.ok(ApiResponse.success("Запрос принят в обработку"));
    }
}
