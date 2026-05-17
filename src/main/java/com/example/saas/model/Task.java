package com.example.saas.model;

import com.example.saas.util.TenantCtx;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

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

  private String status = "todo";

  private String priority = "medium";

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

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getPriority() {
    return priority;
  }

  public void setPriority(String priority) {
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

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
