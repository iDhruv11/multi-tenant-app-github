
package com.example.saas.service;

import com.example.saas.security.LoggedInUser;

import com.example.saas.dto.Dtos.AuditLogResponse;
import com.example.saas.dto.Dtos.DashboardResponse;
import com.example.saas.dto.Dtos.TaskResponse;
import com.example.saas.model.Enums.TaskStatus;
import com.example.saas.model.Enums.UserRole;
import com.example.saas.model.Project;
import com.example.saas.model.ProjectMember;
import com.example.saas.model.Task;
import com.example.saas.model.User;
import com.example.saas.repository.*;

import com.example.saas.util.TenantCtx;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

  private final ProjectRepository projectRepository;
  private final UserRepository userRepository;
  private final TaskRepository taskRepository;
  private final ActivityLogRepository activityLogRepository;
  private final ProjectMemberRepository memberRepository;

  public DashboardService(
      ProjectRepository projectRepository,
      UserRepository userRepository,
      TaskRepository taskRepository,
      ActivityLogRepository activityLogRepository,
      ProjectMemberRepository memberRepository) {
    this.projectRepository = projectRepository;
    this.userRepository = userRepository;
    this.taskRepository = taskRepository;
    this.activityLogRepository = activityLogRepository;
    this.memberRepository = memberRepository;
  }

  @Transactional(readOnly = true)
  public DashboardResponse get() {
    UUID tenantId = TenantCtx.get();
    UUID me = LoggedInUser.current().getUserId();
    UserRole tenantRole = LoggedInUser.current().getRole();

    long projects;
    long users;
    long open;
    long done;
    List<AuditLogResponse> recent;

    if (tenantRole == UserRole.admin) {
      projects = projectRepository.countByTenantIdAndDeletedAtIsNull(tenantId);
      users = userRepository.countByTenantId(tenantId);
      open = taskRepository.countByTenantIdAndStatus(tenantId, TaskStatus.todo)
          + taskRepository.countByTenantIdAndStatus(tenantId, TaskStatus.in_progress);
      done = taskRepository.countByTenantIdAndStatus(tenantId, TaskStatus.done);
      Map<UUID, String> names = userRepository.findByTenantId(tenantId).stream()
          .collect(Collectors.toMap(User::getId, u -> u.getFirstName() + " " + u.getLastName()));
      recent = activityLogRepository
          .findByTenantIdOrderByCreatedAtDesc(tenantId, PageRequest.of(0, 8))
          .map(log -> AuditLogResponse.from(log, names.getOrDefault(log.getActorId(), "User")))
          .getContent();
    } else if (tenantRole == UserRole.manager) {
      List<UUID> myProjectIds = myProjectIds(me, tenantId);
      projects = myProjectIds.size();
      users = userRepository.countByTenantId(tenantId);
      open = countTasksOnProjects(myProjectIds, true);
      done = countTasksOnProjects(myProjectIds, false);
      recent = List.of();
    } else {
      List<UUID> myProjectIds = myProjectIds(me, tenantId);
      projects = myProjectIds.size();
      users = 0;
      open = countAssignedTasks(me, tenantId, true);
      done = countAssignedTasks(me, tenantId, false);
      recent = List.of();
    }

    Map<UUID, String> projectNames = projectRepository.findByTenantIdAndDeletedAtIsNull(tenantId).stream()
        .collect(Collectors.toMap(Project::getId, Project::getName));
    List<TaskResponse> myTasks = taskRepository.findByTenantId(tenantId).stream()
        .filter(t -> projectNames.containsKey(t.getProjectId()))
        .filter(t -> me.equals(t.getAssignedToId()))
        .limit(10)
        .map(t -> {
          String assigneeName = null;
          if (t.getAssignedToId() != null) {
            assigneeName = userRepository
                .findByIdAndTenantId(t.getAssignedToId(), tenantId)
                .map(u -> u.getFirstName() + " " + u.getLastName())
                .orElse(null);
          }
          return TaskResponse.from(t, projectNames.getOrDefault(t.getProjectId(), ""), assigneeName);
        })
        .toList();

    return new DashboardResponse(projects, users, open, done, recent, myTasks);
  }

  private List<UUID> myProjectIds(UUID userId, UUID tenantId) {
    return memberRepository.findByUserIdAndTenantId(userId, tenantId).stream()
        .map(ProjectMember::getProjectId)
        .distinct()
        .toList();
  }

  private long countTasksOnProjects(List<UUID> projectIds, boolean openTasks) {
    long total = 0;
    for (UUID projectId : projectIds) {
      if (openTasks) {
        total += taskRepository.countByProjectIdAndStatus(projectId, TaskStatus.todo);
        total += taskRepository.countByProjectIdAndStatus(projectId, TaskStatus.in_progress);
      } else {
        total += taskRepository.countByProjectIdAndStatus(projectId, TaskStatus.done);
      }
    }
    return total;
  }

  private long countAssignedTasks(UUID userId, UUID tenantId, boolean openTasks) {
    return taskRepository.findByTenantId(tenantId).stream()
        .filter(t -> userId.equals(t.getAssignedToId()))
        .filter(t -> {
          if (openTasks) {
            return t.getStatus() == TaskStatus.todo || t.getStatus() == TaskStatus.in_progress;
          }
          return t.getStatus() == TaskStatus.done;
        })
        .count();
  }
}
