package com.example.saas.repository;

import com.example.saas.model.Session;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionRepository extends JpaRepository<Session, UUID> {
    Optional<Session> findByRefreshToken(String refreshToken);
    List<Session> findByUserIdAndTenantIdOrderByCreatedAtDesc(UUID userId, UUID tenantId);

    void deleteByUserId(UUID userId);
}
