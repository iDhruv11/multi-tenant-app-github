
package com.example.saas.service;

import com.example.saas.security.LoggedInUser;

import com.example.saas.dto.Dtos.AuditLogResponse;
import com.example.saas.exception.ForbiddenException;
import com.example.saas.model.ActivityLog;
import com.example.saas.model.Enums.ProjectMemberRole;
import com.example.saas.model.Enums.UserRole;
import com.example.saas.model.Task;
import com.example.saas.model.User;
import com.example.saas.model.ProjectMember;
import com.example.saas.repository.ActivityLogRepository;
import com.example.saas.repository.ProjectMemberRepository;
import com.example.saas.repository.TaskRepository;
import com.example.saas.repository.UserRepository;

import com.example.saas.util.TenantCtx;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

  private final ActivityLogRepository activityLogRepository;
  private final UserRepository userRepository;
  private final TaskRepository taskRepository;
  private final ProjectMemberRepository memberRepository;

  public AuditService(
      ActivityLogRepository activityLogRepository,
      UserRepository userRepository,
      TaskRepository taskRepository,
      ProjectMemberRepository memberRepository) {
    this.activityLogRepository = activityLogRepository;
    this.userRepository = userRepository;
    this.taskRepository = taskRepository;
    this.memberRepository = memberRepository;
  }

  @Transactional
  public void log(String entityType, UUID entityId, String action, String changesJson) {
    ActivityLog log = new ActivityLog();
    log.setEntityType(entityType);
    log.setEntityId(entityId);
    log.setAction(action);
    log.setActorId(LoggedInUser.current().getUserId());
    log.setChanges(changesJson);
    activityLogRepository.save(log);
  }

  @Transactional(readOnly = true)
  public Page<AuditLogResponse> list(Pageable pageable) {
    requireAdmin();
    return mapPage(activityLogRepository.findByTenantIdOrderByCreatedAtDesc(TenantCtx.get(), pageable));
  }

  @Transactional(readOnly = true)
  public Page<AuditLogResponse> listForProject(UUID projectId, Pageable pageable) {
    requireProjectOwner(projectId);
    UUID tenantId = TenantCtx.get();
    List<UUID> taskIds = taskRepository.findByProjectIdAndTenantId(projectId, tenantId).stream()
        .map(Task::getId)
        .toList();
    List<ActivityLog> logs = new ArrayList<>();
    logs.addAll(activityLogRepository.findByTenantIdAndEntityTypeAndEntityIdOrderByCreatedAtDesc(
        tenantId, "project", projectId));
    if (!taskIds.isEmpty()) {
      logs.addAll(activityLogRepository.findByTenantIdAndEntityTypeAndEntityIdInOrderByCreatedAtDesc(
          tenantId, "task", taskIds));
    }
    List<UUID> memberIds = memberRepository.findByProjectId(projectId).stream()
        .map(ProjectMember::getId)
        .toList();
    if (!memberIds.isEmpty()) {
      logs.addAll(activityLogRepository.findByTenantIdAndEntityTypeAndEntityIdInOrderByCreatedAtDesc(
          tenantId, "project_member", memberIds));
    }
    logs.sort(Comparator.comparing(ActivityLog::getCreatedAt).reversed());
    int start = (int) pageable.getOffset();
    int end = Math.min(start + pageable.getPageSize(), logs.size());
    List<ActivityLog> slice = start >= logs.size() ? List.of() : logs.subList(start, end);
    return mapPage(new PageImpl<>(slice, pageable, logs.size()));
  }

  private Page<AuditLogResponse> mapPage(Page<ActivityLog> page) {
    Map<UUID, String> names = userRepository.findByTenantId(TenantCtx.get()).stream()
        .collect(Collectors.toMap(User::getId, u -> u.getFirstName() + " " + u.getLastName()));
    return page.map(log -> AuditLogResponse.from(log, names.getOrDefault(log.getActorId(), "User")));
  }

  private void requireAdmin() {
    if (LoggedInUser.current().getRole() != UserRole.admin) {
      throw new ForbiddenException("Admin only");
    }
  }

  private void requireProjectOwner(UUID projectId) {
    if (LoggedInUser.current().getRole() == UserRole.admin) {
      return;
    }
    UUID userId = LoggedInUser.current().getUserId();
    memberRepository
        .findByProjectIdAndUserId(projectId, userId)
        .filter(m -> m.getRole() == ProjectMemberRole.owner)
        .orElseThrow(() -> new ForbiddenException("Project owner required"));
  }
}
