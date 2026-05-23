package com.example.saas.service;

import com.example.saas.dto.Dtos.CreateTaskRequest;
import com.example.saas.dto.Dtos.TaskResponse;
import com.example.saas.model.Enums.ProjectMemberRole;
import com.example.saas.model.Enums.TaskStatus;
import com.example.saas.model.Project;
import com.example.saas.model.Task;
import com.example.saas.repository.ProjectMemberRepository;
import com.example.saas.repository.ProjectRepository;
import com.example.saas.repository.TaskRepository;
import com.example.saas.security.LoggedInUser;
import com.example.saas.util.TenantCtx;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskService {

  private final TaskRepository taskRepository;
  private final ProjectRepository projectRepository;
  private final ProjectMemberRepository memberRepository;
  private final ProjectService projectService;
  private final UserService userService;
  private final AuditService auditService;

  public TaskService(
      TaskRepository taskRepository,
      ProjectRepository projectRepository,
      ProjectMemberRepository memberRepository,
      ProjectService projectService,
      UserService userService,
      AuditService auditService) {
    this.taskRepository = taskRepository;
    this.projectRepository = projectRepository;
    this.memberRepository = memberRepository;
    this.projectService = projectService;
    this.userService = userService;
    this.auditService = auditService;
  }

  @Transactional
  public TaskResponse create(UUID projectId, CreateTaskRequest req) {
    Project project = projectService.requireAccess(projectId);
    projectService.requireProjectEditor(project);

    Task task = new Task();
    task.setProjectId(projectId);
    task.setTitle(req.title());
    task.setDescription(req.description());
    if (req.priority() != null) {
      task.setPriority(req.priority());
    }
    task.setCreatedById(LoggedInUser.current().getUserId());
    task = taskRepository.save(task);

    auditService.log("task", task.getId(), "created", null);
    return toResponse(task);
  }

  @Transactional(readOnly = true)
  public List<TaskResponse> listByProject(UUID projectId) {
    Project project = projectService.requireAccess(projectId);
    UUID me = LoggedInUser.current().getUserId();
    ProjectMemberRole role = projectService.roleOnProject(project, me);

    return taskRepository.findByProjectIdAndTenantId(projectId, TenantCtx.get()).stream()
        .filter(t -> canSeeTaskInProject(t, project, role, me))
        .map(this::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<TaskResponse> listAll(TaskStatus status, UUID assigneeId) {
    UUID me = LoggedInUser.current().getUserId();
    Map<UUID, String> names = projectNames();

    return taskRepository.findByTenantId(TenantCtx.get()).stream()
        .filter(t -> names.containsKey(t.getProjectId()))
        .filter(t -> canSeeTaskOnTasksPage(t, me))
        .filter(t -> status == null || t.getStatus() == status)
        .filter(t -> assigneeId == null || assigneeId.equals(t.getAssignedToId()))
        .map(t -> toResponse(t, names.getOrDefault(t.getProjectId(), "")))
        .toList();
  }

  private boolean canSeeTaskInProject(Task task, Project project, ProjectMemberRole role, UUID userId) {
    if (projectService.isTenantAdmin()) {
      return true;
    }
    if (role == ProjectMemberRole.owner || role == ProjectMemberRole.editor) {
      return true;
    }
    if (role == ProjectMemberRole.viewer) {
      return userId.equals(task.getAssignedToId());
    }
    return false;
  }

  private boolean canSeeTaskOnTasksPage(Task task, UUID userId) {
    if (projectService.isTenantAdmin()) {
      return true;
    }
    if (!projectService.canAccessProject(task.getProjectId(), userId)) {
      return false;
    }
    ProjectMemberRole role = projectService.roleOnProject(task.getProjectId(), userId);
    if (role == ProjectMemberRole.owner || role == ProjectMemberRole.editor) {
      return true;
    }
    if (role == ProjectMemberRole.viewer) {
      return userId.equals(task.getAssignedToId());
    }
    return false;
  }

  private TaskResponse toResponse(Task task) {
    String projectName = projectRepository
        .findByIdAndTenantIdAndDeletedAtIsNull(task.getProjectId(), TenantCtx.get())
        .map(Project::getName)
        .orElse("");
    return toResponse(task, projectName);
  }

  private TaskResponse toResponse(Task task, String projectName) {
    String assigneeName = null;
    if (task.getAssignedToId() != null) {
      assigneeName = userService.displayName(task.getAssignedToId());
    }
    return TaskResponse.from(task, projectName, assigneeName);
  }

  private Map<UUID, String> projectNames() {
    return projectRepository.findByTenantIdAndDeletedAtIsNull(TenantCtx.get()).stream()
        .collect(Collectors.toMap(Project::getId, Project::getName));
  }
}
