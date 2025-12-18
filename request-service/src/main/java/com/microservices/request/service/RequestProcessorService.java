package com.microservices.request.service;

import com.microservices.request.entity.ProcessedRequest;
import com.microservices.request.repository.ProcessedRequestRepository;
import com.microservices.shared.dto.UserRequestDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class RequestProcessorService {

    private final ProcessedRequestRepository processedRequestRepository;
    private final RestTemplate restTemplate;

    @Value("${request.processing.delay-seconds:10}")
    private int processingDelaySeconds;

    @Value("${sender-service.url:http://localhost:8083}")
    private String senderServiceUrl;

    private static final Map<Integer, String> CATEGORY_RESPONSES = Map.of(
            1, "Ваш запрос по категории 1 (Техническая поддержка) успешно обработан. Наши специалисты уже работают над решением.",
            2, "Ваш запрос по категории 2 (Финансовые вопросы) принят в обработку. Ожидайте ответа от финансового отдела.",
            3, "Ваш запрос по категории 3 (Общие вопросы) обработан. Благодарим за обращение!",
            4, "Ваш запрос по категории 4 (Жалобы и предложения) зарегистрирован. Мы ценим вашу обратную связь.",
            5, "Ваш запрос по категории 5 (Партнерство) передан в отдел развития бизнеса.",
            6, "Ваш запрос по категории 6 (Возврат товара) принят. Ожидайте инструкции по возврату.",
            7, "Ваш запрос по категории 7 (Доставка) обрабатывается логистическим отделом.",
            8, "Ваш запрос по категории 8 (Гарантия) передан в сервисный центр.",
            9, "Ваш запрос по категории 9 (Консультация) принят. Специалист свяжется с вами.",
            10, "Ваш запрос по категории 10 (Другое) обработан. Спасибо за обращение!"
    );

    @Transactional
    public void processRequest(UserRequestDTO requestDTO) {
        log.info("Received request for processing: {}", requestDTO.getId());

        ProcessedRequest processedRequest = ProcessedRequest.builder()
                .originalRequestId(requestDTO.getId())
                .userId(requestDTO.getUserId())
                .category(requestDTO.getCategory())
                .originalMessage(requestDTO.getMessage())
                .status(ProcessedRequest.ProcessStatus.PROCESSING)
                .build();

        processedRequestRepository.save(processedRequest);

        // Update sender-service that request is being processed
        updateSenderServiceStatus(requestDTO.getId(), "PROCESSING", "Ваш запрос обрабатывается...");

        // Process asynchronously
        processAsync(processedRequest.getId(), requestDTO);
    }

    @Async
    public void processAsync(String processedRequestId, UserRequestDTO originalRequest) {
        log.info("Starting async processing for request: {}", originalRequest.getId());

        try {
            // Wait for specified delay (10 seconds by default)
            Thread.sleep(processingDelaySeconds * 1000L);

            // Generate response based on category
            String responseMessage = CATEGORY_RESPONSES.getOrDefault(
                    originalRequest.getCategory(),
                    "Ваш запрос успешно обработан. Благодарим за обращение!"
            );

            // Update processed request
            processedRequestRepository.findById(processedRequestId).ifPresent(pr -> {
                pr.setStatus(ProcessedRequest.ProcessStatus.COMPLETED);
                pr.setResponseMessage(responseMessage);
                pr.setProcessedAt(LocalDateTime.now());
                processedRequestRepository.save(pr);
            });

            // Send response back to sender-service
            updateSenderServiceStatus(originalRequest.getId(), "PROCESSED", responseMessage);

            log.info("Request {} processed successfully", originalRequest.getId());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Processing interrupted for request: {}", originalRequest.getId());

            processedRequestRepository.findById(processedRequestId).ifPresent(pr -> {
                pr.setStatus(ProcessedRequest.ProcessStatus.FAILED);
                pr.setResponseMessage("Обработка прервана");
                processedRequestRepository.save(pr);
            });

            updateSenderServiceStatus(originalRequest.getId(), "FAILED", "Ошибка обработки запроса");

        } catch (Exception e) {
            log.error("Error processing request {}: {}", originalRequest.getId(), e.getMessage());

            processedRequestRepository.findById(processedRequestId).ifPresent(pr -> {
                pr.setStatus(ProcessedRequest.ProcessStatus.FAILED);
                pr.setResponseMessage("Ошибка: " + e.getMessage());
                processedRequestRepository.save(pr);
            });

            updateSenderServiceStatus(originalRequest.getId(), "FAILED", "Ошибка обработки запроса");
        }
    }

    private void updateSenderServiceStatus(String requestId, String status, String message) {
        try {
            String url = senderServiceUrl + "/api/v1/sender/requests/" + requestId + "/status?status=" + status;
            if (message != null) {
                url += "&responseMessage=" + java.net.URLEncoder.encode(message, "UTF-8");
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            restTemplate.put(url, entity);
            log.info("Updated sender-service with status {} for request {}", status, requestId);

        } catch (Exception e) {
            log.error("Failed to update sender-service for request {}: {}", requestId, e.getMessage());
        }
    }
}
