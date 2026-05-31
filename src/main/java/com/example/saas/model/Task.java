package com.example.saas.model;

import com.example.saas.util.TenantCtx;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "tasks")
public class Task {

  @Id
  private UUID id;

  @Column(name = "project_id")
  private UUID projectId;

  @Column(name = "tenant_id")
  private UUID tenantId;

  private String title;

  @Column(columnDefinition = "text")
  private String description;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(columnDefinition = "task_status")
  private Enums.TaskStatus status = Enums.TaskStatus.todo;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(columnDefinition = "task_priority")
  private Enums.TaskPriority priority = Enums.TaskPriority.medium;

  @Column(name = "assigned_to_id")
  private UUID assignedToId;

  @Column(name = "created_by_id")
  private UUID createdById;

  @Column(name = "created_at")
  private Instant createdAt;

  @Column(name = "updated_at")
  private Instant updatedAt;

  @PrePersist
  void prePersist() {
    if (id == null) {
      id = UUID.randomUUID();
    }
    if (tenantId == null) {
      tenantId = TenantCtx.get();
    }
    Instant now = Instant.now();
    createdAt = now;
    updatedAt = now;
  }

  @PreUpdate
  void preUpdate() {
    updatedAt = Instant.now();
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

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Enums.TaskStatus getStatus() {
    return status;
  }

  public void setStatus(Enums.TaskStatus status) {
    this.status = status;
  }

  public Enums.TaskPriority getPriority() {
    return priority;
  }

  public void setPriority(Enums.TaskPriority priority) {
    this.priority = priority;
  }

  public UUID getAssignedToId() {
    return assignedToId;
  }

  public void setAssignedToId(UUID assignedToId) {
    this.assignedToId = assignedToId;
  }

  public UUID getCreatedById() {
    return createdById;
  }

  public void setCreatedById(UUID createdById) {
    this.createdById = createdById;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
