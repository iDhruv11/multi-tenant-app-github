package com.example.saas.service;

import com.example.saas.dto.Dtos.*;
import com.example.saas.exception.ForbiddenException;
import com.example.saas.exception.NotFoundException;
import com.example.saas.model.Enums.ProjectMemberRole;
import com.example.saas.model.Enums.ProjectVisibility;
import com.example.saas.model.Enums.TaskStatus;
import com.example.saas.model.Enums.UserRole;
import com.example.saas.model.Project;
import com.example.saas.model.ProjectMember;
import com.example.saas.repository.ProjectMemberRepository;
import com.example.saas.repository.ProjectRepository;
import com.example.saas.repository.TaskRepository;
import com.example.saas.security.LoggedInUser;
import com.example.saas.util.TenantCtx;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectService {

  private final ProjectRepository projectRepository;
  private final ProjectMemberRepository memberRepository;
  private final TaskRepository taskRepository;
  private final AuditService auditService;

  public ProjectService(
      ProjectRepository projectRepository,
      ProjectMemberRepository memberRepository,
      TaskRepository taskRepository,
      AuditService auditService) {
    this.projectRepository = projectRepository;
    this.memberRepository = memberRepository;
    this.taskRepository = taskRepository;
    this.auditService = auditService;
  }

  @Transactional
  public ProjectResponse create(CreateProjectRequest req) {
    requireTenantAdmin();
    var me = LoggedInUser.current();

    Project project = new Project();
    project.setName(req.name());
    project.setDescription(req.description());
    project.setVisibility(req.visibility() != null ? req.visibility() : ProjectVisibility.PRIVATE);
    project.setOwnerId(me.getUserId());
    project = projectRepository.save(project);

    // Automatically assign creator as Owner in project metadata mapping
    ProjectMember member = new ProjectMember();
    member.setProjectId(project.getId());
    member.setUserId(me.getUserId());
    member.setRole(ProjectMemberRole.owner);
    memberRepository.save(member);

    auditService.log("project", project.getId(), "created", null);
    return toResponse(project, me.getUserId());
  }

  @Transactional(readOnly = true)
  public ProjectResponse get(UUID projectId) {
    Project project = find(projectId);
    checkAccess(project);
    return toResponse(project, LoggedInUser.current().getUserId());
  }

  @Transactional(readOnly = true)
  public List<ProjectResponse> list() {
    UUID me = LoggedInUser.current().getUserId();
    return projectRepository.findByTenantIdAndDeletedAtIsNull(TenantCtx.get()).stream()
        .filter(p -> canAccess(p, me))
        .map(p -> toResponse(p, me))
        .toList();
  }

  @Transactional(readOnly = true)
  public ProjectMemberRole roleOnProject(Project project, UUID userId) {
    return memberRepository
        .findByProjectIdAndUserId(project.getId(), userId)
        .map(ProjectMember::getRole)
        .orElse(null);
  }

  @Transactional(readOnly = true)
  public boolean isTenantAdmin() {
    return LoggedInUser.current().getRole() == UserRole.admin;
  }

  private ProjectResponse toResponse(Project project, UUID userId) {
    long members = memberRepository.countByProjectId(project.getId());
    long open = taskRepository.countByProjectIdAndStatus(project.getId(), TaskStatus.todo)
        + taskRepository.countByProjectIdAndStatus(project.getId(), TaskStatus.in_progress);
    long done = taskRepository.countByProjectIdAndStatus(project.getId(), TaskStatus.done);
    return ProjectResponse.from(project, members, open, done, roleOnProject(project, userId));
  }

  private Project find(UUID projectId) {
    return projectRepository
        .findByIdAndTenantIdAndDeletedAtIsNull(projectId, TenantCtx.get())
        .orElseThrow(() -> new NotFoundException("Project not found"));
  }

  private void checkAccess(Project project) {
    if (!canAccess(project, LoggedInUser.current().getUserId())) {
      throw new ForbiddenException("No access to this project");
    }
  }

  private boolean canAccess(Project project, UUID userId) {
    if (project.getOwnerId().equals(userId)) {
      return true;
    }
    if (project.getVisibility() == ProjectVisibility.INTERNAL) {
      return true;
    }
    return memberRepository.findByProjectIdAndUserId(project.getId(), userId).isPresent();
  }

  private void requireTenantAdmin() {
    if (!isTenantAdmin()) {
      throw new ForbiddenException("Admin only");
    }
  }
}
