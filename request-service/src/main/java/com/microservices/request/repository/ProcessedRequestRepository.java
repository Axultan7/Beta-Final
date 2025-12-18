package com.microservices.request.repository;

import com.microservices.request.entity.ProcessedRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProcessedRequestRepository extends JpaRepository<ProcessedRequest, String> {

    Optional<ProcessedRequest> findByOriginalRequestId(String originalRequestId);

    Page<ProcessedRequest> findByUserId(String userId, Pageable pageable);

    Page<ProcessedRequest> findByStatus(ProcessedRequest.ProcessStatus status, Pageable pageable);
}
