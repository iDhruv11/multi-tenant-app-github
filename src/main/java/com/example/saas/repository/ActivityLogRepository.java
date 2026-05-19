package com.example.saas.repository;

import com.example.saas.model.ActivityLog;
import java.util.UUID;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, UUID> {
    Page<ActivityLog> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);

    List<ActivityLog> findByTenantIdAndEntityTypeAndEntityIdOrderByCreatedAtDesc(
            UUID tenantId, String entityType, UUID entityId);
    List<ActivityLog> findByTenantIdAndEntityTypeAndEntityIdInOrderByCreatedAtDesc(
            UUID tenantId, String entityType, List<UUID> entityIds);

    void deleteByActorIdAndTenantId(UUID actorId, UUID tenantId);
}
