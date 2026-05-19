package com.example.saas.repository;

import com.example.saas.model.Project;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, UUID> {
    Optional<Project> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    List<Project> findByTenantIdAndDeletedAtIsNull(UUID tenantId);

    long countByTenantIdAndDeletedAtIsNull(UUID tenantId);

    List<Project> findByOwnerIdAndTenantId(UUID ownerId, UUID tenantId);
}
