package com.example.saas.repository;

import com.example.saas.model.Enums.ProjectMemberRole;
import com.example.saas.model.ProjectMember;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, UUID> {
    Optional<ProjectMember> findByProjectIdAndUserId(UUID projectId, UUID userId);

    boolean existsByProjectIdAndUserId(UUID projectId, UUID userId);

    List<ProjectMember> findByProjectId(UUID projectId);

    List<ProjectMember> findByUserIdAndTenantId(UUID userId, UUID tenantId);

    long countByProjectId(UUID projectId);

    long countByProjectIdAndRole(UUID projectId, ProjectMemberRole role);

    void deleteByUserIdAndTenantId(UUID userId, UUID tenantId);

    boolean existsByUserIdAndTenantIdAndRoleIn(UUID userId, UUID tenantId, Collection<ProjectMemberRole> roles);
}
