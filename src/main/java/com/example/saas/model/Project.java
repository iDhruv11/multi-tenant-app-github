package com.example.saas.model;

import com.example.saas.util.TenantCtx;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "projects")
public class Project {

  @Id
  private UUID id;

  @Column(name = "tenant_id", nullable = false)
  private UUID tenantId;

  private String name;

  @Column(columnDefinition = "text")
  private String description;

  @Convert(converter = VisibilityConverter.class)
  @Column(columnDefinition = "project_visibility")
  @org.hibernate.annotations.ColumnTransformer(write = "?::project_visibility")
  private Enums.ProjectVisibility visibility = Enums.ProjectVisibility.PRIVATE;

  @Column(name = "owner_id")
  private UUID ownerId;

  @Column(name = "created_at")
  private Instant createdAt;

  @Column(name = "updated_at")
  private Instant updatedAt;

  @Column(name = "deleted_at")
  private Instant deletedAt;

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

  public UUID getTenantId() {
    return tenantId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Enums.ProjectVisibility getVisibility() {
    return visibility;
  }

  public void setVisibility(Enums.ProjectVisibility visibility) {
    this.visibility = visibility;
  }

  public UUID getOwnerId() {
    return ownerId;
  }

  public void setOwnerId(UUID ownerId) {
    this.ownerId = ownerId;
  }

  public Instant getDeletedAt() {
    return deletedAt;
  }

  public void setDeletedAt(Instant deletedAt) {
    this.deletedAt = deletedAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  @Converter
  static class VisibilityConverter implements AttributeConverter<Enums.ProjectVisibility, String> {
    @Override
    public String convertToDatabaseColumn(Enums.ProjectVisibility attribute) {
      return attribute == null ? null : attribute.getDbValue();
    }

    @Override
    public Enums.ProjectVisibility convertToEntityAttribute(String dbData) {
      return dbData == null ? null : Enums.ProjectVisibility.fromDb(dbData);
    }
  }
}
