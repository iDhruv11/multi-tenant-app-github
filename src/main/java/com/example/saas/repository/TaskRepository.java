package com.example.saas.repository;

import com.example.saas.model.Task;
import com.example.saas.model.Enums.TaskStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, UUID> {
    Optional<Task> findByIdAndTenantId(UUID id, UUID tenantId);

    List<Task> findByProjectIdAndTenantId(UUID projectId, UUID tenantId);

    Page<Task> findByTenantIdAndStatusOrderByCreatedAtDesc(UUID tenantId, TaskStatus status, Pageable pageable);

    List<Task> findByTenantId(UUID tenantId);
    long countByTenantIdAndStatus(UUID tenantId, TaskStatus status);
    long countByProjectIdAndStatus(UUID projectId, TaskStatus status);

    void deleteByProjectId(UUID projectId);

    void deleteByProjectIdAndAssignedToId(UUID projectId, UUID assignedToId);

    void deleteByAssignedToIdAndTenantId(UUID assignedToId, UUID tenantId);

    void deleteByCreatedByIdAndTenantId(UUID createdById, UUID tenantId);
}
