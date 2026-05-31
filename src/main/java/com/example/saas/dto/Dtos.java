package com.example.saas.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.example.saas.model.ActivityLog;
import com.example.saas.model.Project;
import com.example.saas.model.Task;
import com.example.saas.model.User;
import com.example.saas.model.Enums.ProjectMemberRole;
import com.example.saas.model.Enums.ProjectVisibility;
import com.example.saas.model.Enums.TaskPriority;
import com.example.saas.model.Enums.TaskStatus;
import com.example.saas.model.Enums.UserRole;
import com.example.saas.model.Enums.UserStatus;

public final class Dtos {

  private Dtos() {
  }

  public record ApiResponse<T>(
      T data,
      Instant timestamp,
      String requestId) {
  }

  public record RegisterRequest(
      @NotBlank @Size(max = 255) String tenantName,
      @NotBlank @Size(max = 50) String tenantSlug,
      @NotBlank @Email String email,
      @NotBlank @Size(min = 8) String password,
      @NotBlank String firstName,
      @NotBlank String lastName) {
  }

  public record AuthResponse(
      String accessToken,
      String refreshToken,
      UUID sessionId,
      long accessTokenExpiresInSeconds) {
  }

  public record DashboardResponse(
      long projectCount,
      long userCount,
      long openTaskCount,
      long completedTaskCount,
      List<AuditLogResponse> recentActivity,
      List<TaskResponse> myTasks) {
  }

  public record AuditLogResponse(
      UUID id,
      String entityType,
      UUID entityId,
      String action,
      UUID actorId,
      String actorName,
      String changes,
      Instant createdAt) {
    public static AuditLogResponse from(ActivityLog log, String actorName) {
      return new AuditLogResponse(
          log.getId(), log.getEntityType(), log.getEntityId(), log.getAction(),
          log.getActorId(), actorName, log.getChanges(), log.getCreatedAt());
    }
  }

  public record UserResponse(
      UUID id,
      String email,
      String firstName,
      String lastName,
      UserRole role,
      UserStatus status,
      Instant createdAt) {
    public static UserResponse from(User u) {
      return new UserResponse(
          u.getId(), u.getEmail(), u.getFirstName(), u.getLastName(), u.getRole(), u.getStatus(), u.getCreatedAt());
    }
  }

  public record TaskResponse(
      UUID id,
      UUID projectId,
      String projectName,
      String title,
      String description,
      TaskStatus status,
      TaskPriority priority,
      UUID assignedToId,
      String assignedToName,
      UUID createdById,
      Instant createdAt) {
    public static TaskResponse from(Task t, String projectName) {
      return from(t, projectName, null);
    }

    public static TaskResponse from(Task t, String projectName, String assignedToName) {
      return new TaskResponse(
          t.getId(), t.getProjectId(), projectName, t.getTitle(), t.getDescription(),
          t.getStatus(), t.getPriority(), t.getAssignedToId(), assignedToName,
          t.getCreatedById(), t.getCreatedAt());
    }
  }

  public record ProjectResponse(
      UUID id,
      String name,
      String description,
      ProjectVisibility visibility,
      UUID ownerId,
      Instant createdAt,
      long memberCount,
      long openTaskCount,
      long doneTaskCount,
      ProjectMemberRole myRole) {
    public static ProjectResponse from(
        Project p, long members, long open, long done, ProjectMemberRole myRole) {
      return new ProjectResponse(
          p.getId(), p.getName(), p.getDescription(), p.getVisibility(), p.getOwnerId(),
          p.getCreatedAt(), members, open, done, myRole);
    }
  }

  public record ProjectMemberResponse(
      UUID userId,
      String firstName,
      String lastName,
      String email,
      UserRole tenantRole,
      ProjectMemberRole projectRole,
      UserStatus status) {
  }

  public record CreateTaskRequest(@NotBlank String title, String description, TaskPriority priority) {
  }

  public record UpdateTaskRequest(String title, String description, TaskStatus status, TaskPriority priority) {
  }

  public record AssignTaskRequest(UUID userId) {
  }

  public record AddMemberRequest(@NotNull UUID userId, @NotNull ProjectMemberRole role) {
  }

  public record UpdateMemberRequest(@NotNull ProjectMemberRole role) {
  }

  public record CreateProjectRequest(@NotBlank String name, String description, ProjectVisibility visibility) {
  }

  public record UpdateProjectRequest(String name, String description, ProjectVisibility visibility) {
  }

  public record CreateUserRequest(
      @NotBlank @Email String email,
      @NotBlank @Size(min = 8) String password,
      @NotBlank String firstName,
      @NotBlank String lastName,
      @NotNull UserRole role) {
  }

  public record UpdateUserRequest(String firstName, String lastName, UserRole role, UserStatus status) {
  }

  public record ProjectSummary(UUID id, String name) {
  }

  public record LoginRequest(
      @NotBlank String tenantSlug, @NotBlank @Email String email, @NotBlank String password) {
  }

  public record RefreshRequest(@NotBlank String refreshToken) {
  }

  public record LogoutRequest(@NotBlank String refreshToken) {
  }

  public record TenantResponse(UUID id, String name, String slug) {
  }

  public record SessionResponse(UUID id, String device, String browser, Instant lastActive, boolean current) {
  }

}
