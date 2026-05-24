package com.example.saas.service;

import com.example.saas.dto.Dtos.*;
import com.example.saas.exception.BadRequestException;
import com.example.saas.exception.ForbiddenException;
import com.example.saas.exception.NotFoundException;
import com.example.saas.model.Enums.ProjectMemberRole;
import com.example.saas.model.Enums.ProjectVisibility;
import com.example.saas.model.Enums.TaskStatus;
import com.example.saas.model.Enums.UserRole;
import com.example.saas.model.Project;
import com.example.saas.model.ProjectMember;
import com.example.saas.model.User;
import com.example.saas.repository.ProjectMemberRepository;
import com.example.saas.repository.ProjectRepository;
import com.example.saas.repository.TaskRepository;
import com.example.saas.repository.UserRepository;
import com.example.saas.security.LoggedInUser;
import com.example.saas.util.TenantCtx;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectService {

  private final ProjectRepository projectRepository;
  private final ProjectMemberRepository memberRepository;
  private final TaskRepository taskRepository;
  private final UserRepository userRepository;
  private final UserService userService;
  private final AuditService auditService;

  public ProjectService(
      ProjectRepository projectRepository,
      ProjectMemberRepository memberRepository,
      TaskRepository taskRepository,
      UserRepository userRepository,
      UserService userService,
      AuditService auditService) {
    this.projectRepository = projectRepository;
    this.memberRepository = memberRepository;
    this.taskRepository = taskRepository;
    this.userRepository = userRepository;
    this.userService = userService;
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

  @Transactional
  public ProjectResponse update(UUID projectId, UpdateProjectRequest req) {
    Project project = find(projectId);
    checkAccess(project);
    requireProjectOwner(project);
    if (req.name() != null) {
      project.setName(req.name());
    }
    if (req.description() != null) {
      project.setDescription(req.description());
    }
    if (req.visibility() != null) {
      project.setVisibility(req.visibility());
    }
    project = projectRepository.save(project);
    auditService.log("project", project.getId(), "updated", null);
    return toResponse(project, LoggedInUser.current().getUserId());
  }

  @Transactional
  public void delete(UUID projectId) {
    Project project = find(projectId);
    checkAccess(project);
    requireProjectOwner(project);
    taskRepository.deleteByProjectId(projectId);
    project.setDeletedAt(Instant.now());
    projectRepository.save(project);
    auditService.log("project", project.getId(), "deleted", null);
  }

  @Transactional(readOnly = true)
  public List<ProjectMemberResponse> listMembers(UUID projectId) {
    Project project = find(projectId);
    checkAccess(project);
    return memberRepository.findByProjectId(projectId).stream()
        .map(this::toMemberResponse)
        .toList();
  }

  @Transactional
  public void addMember(UUID projectId, UUID userId, ProjectMemberRole role) {
    Project project = find(projectId);
    checkAccess(project);
    requireCanManageMembers(project);
    userService.ensureActive(userId);
    if (memberRepository.existsByProjectIdAndUserId(projectId, userId)) {
      return;
    }
    ProjectMemberRole assignedRole = role;
    if (!isTenantAdmin()) {
      ProjectMemberRole myRole = roleOnProject(project, LoggedInUser.current().getUserId());
      if (myRole == ProjectMemberRole.editor) {
        assignedRole = ProjectMemberRole.viewer;
      }
    }
    ProjectMember member = new ProjectMember();
    member.setProjectId(projectId);
    member.setUserId(userId);
    member.setRole(assignedRole);
    memberRepository.save(member);
    auditService.log("project_member", member.getId(), "added", null);
  }

  @Transactional
  public void updateMemberRole(UUID projectId, UUID userId, ProjectMemberRole role) {
    Project project = find(projectId);
    checkAccess(project);
    requireProjectOwner(project);
    ProjectMember member = memberRepository
        .findByProjectIdAndUserId(projectId, userId)
        .orElseThrow(() -> new NotFoundException("Member not found"));
    if (member.getRole() == ProjectMemberRole.owner) {
      long owners = memberRepository.countByProjectIdAndRole(projectId, ProjectMemberRole.owner);
      if (owners <= 1 && role != ProjectMemberRole.owner) {
        throw new BadRequestException("The project must have an owner");
      }
    }
    member.setRole(role);
    memberRepository.save(member);
    auditService.log("project_member", member.getId(), "updated", null);
  }

  @Transactional
  public void removeMember(UUID projectId, UUID userId) {
    Project project = find(projectId);
    checkAccess(project);
    ProjectMember member = memberRepository
        .findByProjectIdAndUserId(projectId, userId)
        .orElseThrow(() -> new NotFoundException("Member not found"));

    if (!isTenantAdmin()) {
      UUID me = LoggedInUser.current().getUserId();
      ProjectMemberRole myRole = roleOnProject(project, me);
      if (myRole == ProjectMemberRole.editor) {
        if (member.getRole() != ProjectMemberRole.viewer) {
          throw new ForbiddenException("Editor can only remove viewers");
        }
      } else if (myRole != ProjectMemberRole.owner) {
        throw new ForbiddenException("No permission to remove members");
      }
    }

    if (member.getRole() == ProjectMemberRole.owner) {
      long owners = memberRepository.countByProjectIdAndRole(projectId, ProjectMemberRole.owner);
      if (owners <= 1) {
        throw new BadRequestException("The project must have an owner");
      }
    }

    memberRepository.delete(member);
    taskRepository.deleteByProjectIdAndAssignedToId(projectId, userId);
    auditService.log("project_member", member.getId(), "removed", null);
  }

  @Transactional(readOnly = true)
  public Project requireAccess(UUID projectId) {
    Project project = find(projectId);
    checkAccess(project);
    return project;
  }

  @Transactional(readOnly = true)
  public void requireProjectActivityAccess(UUID projectId) {
    Project project = find(projectId);
    checkAccess(project);
    requireProjectOwner(project);
  }

  @Transactional(readOnly = true)
  public boolean canAccessProject(UUID projectId, UUID userId) {
    return projectRepository
        .findByIdAndTenantIdAndDeletedAtIsNull(projectId, TenantCtx.get())
        .map(p -> canAccess(p, userId))
        .orElse(false);
  }

  @Transactional(readOnly = true)
  public ProjectMemberRole roleOnProject(UUID projectId, UUID userId) {
    Project project = projectRepository
        .findByIdAndTenantIdAndDeletedAtIsNull(projectId, TenantCtx.get())
        .orElse(null);
    if (project == null) {
      return null;
    }
    return roleOnProject(project, userId);
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

  @Transactional(readOnly = true)
  public void requireProjectEditor(Project project) {
    if (isTenantAdmin()) {
      return;
    }
    ProjectMemberRole role = roleOnProject(project, LoggedInUser.current().getUserId());
    if (role != ProjectMemberRole.owner && role != ProjectMemberRole.editor) {
      throw new ForbiddenException("Need editor or owner on project");
    }
  }

  private ProjectResponse toResponse(Project project, UUID userId) {
    long members = memberRepository.countByProjectId(project.getId());
    long open = taskRepository.countByProjectIdAndStatus(project.getId(), TaskStatus.todo)
        + taskRepository.countByProjectIdAndStatus(project.getId(), TaskStatus.in_progress);
    long done = taskRepository.countByProjectIdAndStatus(project.getId(), TaskStatus.done);
    return ProjectResponse.from(project, members, open, done, roleOnProject(project, userId));
  }

  private ProjectMemberResponse toMemberResponse(ProjectMember member) {
    User user = userRepository
        .findByIdAndTenantId(member.getUserId(), TenantCtx.get())
        .orElseThrow(() -> new NotFoundException("User not found"));
    return new ProjectMemberResponse(
        user.getId(),
        user.getFirstName(),
        user.getLastName(),
        user.getEmail(),
        user.getRole(),
        member.getRole(),
        user.getStatus());
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

  private void requireProjectOwner(Project project) {
    if (isTenantAdmin()) {
      return;
    }
    ProjectMemberRole role = roleOnProject(project, LoggedInUser.current().getUserId());
    if (role != ProjectMemberRole.owner) {
      throw new ForbiddenException("Project owner required");
    }
  }

  private void requireTenantAdmin() {
    if (!isTenantAdmin()) {
      throw new ForbiddenException("Admin only");
    }
  }

  private void requireCanManageMembers(Project project) {
    if (isTenantAdmin()) {
      return;
    }
    ProjectMemberRole role = roleOnProject(project, LoggedInUser.current().getUserId());
    if (role != ProjectMemberRole.owner && role != ProjectMemberRole.editor) {
      throw new ForbiddenException("Need editor or owner on project");
    }
  }
}
