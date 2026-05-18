
package com.example.saas.model;

import com.example.saas.util.TenantCtx;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "project_members")
public class ProjectMember {

  @Id
  private UUID id;

  @Column(name = "project_id")
  private UUID projectId;

  @Column(name = "user_id")
  private UUID userId;

  @Column(name = "tenant_id")
  private UUID tenantId;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(columnDefinition = "project_member_role")
  private Enums.ProjectMemberRole role;

  @Column(name = "joined_at")
  private Instant joinedAt;

  @PrePersist
  void prePersist() {
    if (id == null) {
      id = UUID.randomUUID();
    }
    if (tenantId == null) {
      tenantId = TenantCtx.get();
    }
    if (joinedAt == null) {
      joinedAt = Instant.now();
    }
  }

  public UUID getId() {
    return id;
  }

  public UUID getProjectId() {
    return projectId;
  }

  public void setProjectId(UUID projectId) {
    this.projectId = projectId;
  }

  public UUID getUserId() {
    return userId;
  }

  public void setUserId(UUID userId) {
    this.userId = userId;
  }

  public Enums.ProjectMemberRole getRole() {
    return role;
  }

  public void setRole(Enums.ProjectMemberRole role) {
    this.role = role;
  }
}
