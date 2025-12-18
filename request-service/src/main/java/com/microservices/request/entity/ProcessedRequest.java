package com.microservices.request.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "processed_requests")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedRequest {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private String id;

    @Column(name = "original_request_id", nullable = false)
    private String originalRequestId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "category", nullable = false)
    private Integer category;

    @Column(name = "original_message", nullable = false, length = 2000)
    private String originalMessage;

    @Column(name = "response_message", length = 2000)
    private String responseMessage;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private ProcessStatus status = ProcessStatus.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
    }

    public enum ProcessStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED
    }
}
