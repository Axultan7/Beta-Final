package com.microservices.sender.repository;

import com.microservices.sender.entity.UserRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRequestRepository extends JpaRepository<UserRequest, String> {

    Page<UserRequest> findByUserId(String userId, Pageable pageable);

    List<UserRequest> findByUserIdAndStatus(String userId, UserRequest.RequestStatus status);

    Page<UserRequest> findByStatus(UserRequest.RequestStatus status, Pageable pageable);

    Page<UserRequest> findByCategory(Integer category, Pageable pageable);
}
