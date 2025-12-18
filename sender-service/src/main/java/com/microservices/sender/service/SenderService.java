package com.microservices.sender.service;

import com.microservices.sender.entity.UserRequest;
import com.microservices.sender.mapper.UserRequestMapper;
import com.microservices.sender.repository.UserRequestRepository;
import com.microservices.shared.dto.RequestResponseDTO;
import com.microservices.shared.dto.SendRequestDTO;
import com.microservices.shared.dto.UserRequestDTO;
import com.microservices.shared.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
@RequiredArgsConstructor
public class SenderService {

    private final UserRequestRepository requestRepository;
    private final UserRequestMapper requestMapper;
    private final RestTemplate restTemplate;

    @Value("${request-service.url}")
    private String requestServiceUrl;

    @Transactional
    public RequestResponseDTO sendRequest(String userId, SendRequestDTO sendRequest) {
        log.info("Creating new request for user: {}, category: {}", userId, sendRequest.getCategory());

        UserRequest userRequest = UserRequest.builder()
                .userId(userId)
                .category(sendRequest.getCategory())
                .message(sendRequest.getMessage())
                .status(UserRequest.RequestStatus.PENDING)
                .build();

        userRequest = requestRepository.save(userRequest);
        log.info("Request saved with id: {}", userRequest.getId());

        // Send to request-service for processing
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            UserRequestDTO requestDTO = requestMapper.toDTO(userRequest);
            HttpEntity<UserRequestDTO> entity = new HttpEntity<>(requestDTO, headers);

            String url = requestServiceUrl + "/api/v1/requests/process";
            restTemplate.postForEntity(url, entity, Void.class);

            log.info("Request {} sent to request-service for processing", userRequest.getId());
        } catch (Exception e) {
            log.error("Failed to send request to request-service: {}", e.getMessage());
        }

        return RequestResponseDTO.accepted(userRequest.getId());
    }

    @Transactional(readOnly = true)
    public UserRequestDTO getRequestById(String requestId) {
        UserRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new UserNotFoundException("Request not found with id: " + requestId));
        return requestMapper.toDTO(request);
    }

    @Transactional(readOnly = true)
    public Page<UserRequestDTO> getUserRequests(String userId, Pageable pageable) {
        return requestRepository.findByUserId(userId, pageable)
                .map(requestMapper::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<UserRequestDTO> getAllRequests(Pageable pageable) {
        return requestRepository.findAll(pageable)
                .map(requestMapper::toDTO);
    }

    @Transactional
    public void updateRequestStatus(String requestId, String status, String responseMessage) {
        UserRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new UserNotFoundException("Request not found with id: " + requestId));

        request.setStatus(UserRequest.RequestStatus.valueOf(status));
        request.setResponseMessage(responseMessage);
        requestRepository.save(request);

        log.info("Request {} status updated to: {}", requestId, status);
    }
}
