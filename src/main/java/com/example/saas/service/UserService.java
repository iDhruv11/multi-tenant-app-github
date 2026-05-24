package com.example.saas.service;

import com.example.saas.security.LoggedInUser;

import com.example.saas.dto.Dtos.CreateUserRequest;
import com.example.saas.dto.Dtos.ProjectSummary;
import com.example.saas.dto.Dtos.UpdateUserRequest;
import com.example.saas.dto.Dtos.UserRemovalPreview;
import com.example.saas.dto.Dtos.UserResponse;
import com.example.saas.exception.BadRequestException;
import com.example.saas.exception.ForbiddenException;
import com.example.saas.exception.NotFoundException;
import com.example.saas.model.Enums.ProjectMemberRole;
import com.example.saas.model.Enums.UserRole;
import com.example.saas.model.Enums.UserStatus;
import com.example.saas.model.Project;
import com.example.saas.model.ProjectMember;
import com.example.saas.model.User;
import com.example.saas.repository.ActivityLogRepository;
import com.example.saas.repository.ProjectMemberRepository;
import com.example.saas.repository.ProjectRepository;
import com.example.saas.repository.SessionRepository;
import com.example.saas.repository.TaskRepository;
import com.example.saas.repository.UserRepository;

import com.example.saas.util.TenantCtx;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuditService auditService;
  private final TaskRepository taskRepository;
  private final ProjectMemberRepository memberRepository;
  private final ProjectRepository projectRepository;
  private final SessionRepository sessionRepository;
  private final ActivityLogRepository activityLogRepository;

  public UserService(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      AuditService auditService,
      TaskRepository taskRepository,
      ProjectMemberRepository memberRepository,
      ProjectRepository projectRepository,
      SessionRepository sessionRepository,
      ActivityLogRepository activityLogRepository) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.auditService = auditService;
    this.taskRepository = taskRepository;
    this.memberRepository = memberRepository;
    this.projectRepository = projectRepository;
    this.sessionRepository = sessionRepository;
    this.activityLogRepository = activityLogRepository;
  }

  @Transactional
  public UserResponse create(CreateUserRequest req) {
    requireAdmin();
    UUID tenantId = TenantCtx.get();
    if (userRepository.existsByEmailAndTenantId(req.email(), tenantId)) {
      throw new BadRequestException("Email already used");
    }
    User user = new User();
    user.setEmail(req.email());
    user.setPasswordHash(passwordEncoder.encode(req.password()));
    user.setFirstName(req.firstName());
    user.setLastName(req.lastName());
    user.setRole(req.role());
    user = userRepository.save(user);
    auditService.log("user", user.getId(), "created", null);
    return UserResponse.from(user);
  }

  @Transactional(readOnly = true)
  public List<UserResponse> list() {
    UUID me = LoggedInUser.current().getUserId();
    UserRole role = LoggedInUser.current().getRole();
    if (role == UserRole.member && !canMemberListUsers(me)) {
      throw new ForbiddenException("Members cannot view users");
    }
    return userRepository.findByTenantId(TenantCtx.get()).stream()
        .map(UserResponse::from)
        .toList();
  }

  @Transactional(readOnly = true)
  public UserResponse get(UUID userId) {
    return UserResponse.from(findInTenant(userId));
  }

  @Transactional(readOnly = true)
  public UserRemovalPreview previewRemoval(UUID userId) {
    requireAdmin();
    User user = findInTenant(userId);
    List<ProjectSummary> projectsToDelete = soleOwnerProjects(userId);
    if (wouldRemoveLastActiveAdmin(user, null, UserStatus.disabled)) {
      return new UserRemovalPreview(projectsToDelete, true, "At least one active admin must remain");
    }
    return new UserRemovalPreview(projectsToDelete, false, null);
  }

  @Transactional
  public UserResponse update(UUID userId, UpdateUserRequest req) {
    requireAdmin();
    User user = findInTenant(userId);
    UserRole newRole = req.role() != null ? req.role() : user.getRole();
    UserStatus newStatus = req.status() != null ? req.status() : user.getStatus();

    if (wouldRemoveLastActiveAdmin(user, newRole, newStatus)) {
      throw new BadRequestException("At least one active admin must remain");
    }

    if (req.status() != null && req.status() == UserStatus.disabled && user.getStatus() == UserStatus.active) {
      detachUserFromTenant(userId, false);
    }

    if (req.firstName() != null) {
      user.setFirstName(req.firstName());
    }
    if (req.lastName() != null) {
      user.setLastName(req.lastName());
    }
    if (req.role() != null) {
      user.setRole(req.role());
    }
    if (req.status() != null) {
      user.setStatus(req.status());
    }
    user = userRepository.save(user);
    auditService.log("user", user.getId(), "updated", null);
    return UserResponse.from(user);
  }

  @Transactional
  public void delete(UUID userId) {
    requireAdmin();
    User user = findInTenant(userId);
    if (wouldRemoveLastActiveAdmin(user, null, UserStatus.disabled)) {
      throw new BadRequestException("At least one active admin must remain");
    }

    detachUserFromTenant(userId, true);
    sessionRepository.deleteByUserId(userId);
    activityLogRepository.deleteByActorIdAndTenantId(userId, TenantCtx.get());
    userRepository.delete(user);
    auditService.log("user", user.getId(), "deleted", null);
  }

  public void ensureActive(UUID userId) {
    User user = findInTenant(userId);
    if (user.getStatus() != UserStatus.active) {
      throw new BadRequestException("User is not active");
    }
  }

  public void ensureInTenant(UUID userId) {
    findInTenant(userId);
  }

  public String displayName(UUID userId) {
    User user = findInTenant(userId);
    return user.getFirstName() + " " + user.getLastName();
  }

  User findInTenant(UUID userId) {
    return userRepository
        .findByIdAndTenantId(userId, TenantCtx.get())
        .orElseThrow(() -> new NotFoundException("User not found"));
  }

  @Transactional(readOnly = true)
  public UserResponse me() {
    return UserResponse.from(findInTenant(LoggedInUser.current().getUserId()));
  }

  private boolean canMemberListUsers(UUID userId) {
    return memberRepository.existsByUserIdAndTenantIdAndRoleIn(
        userId, TenantCtx.get(), List.of(ProjectMemberRole.owner, ProjectMemberRole.editor));
  }

  private boolean wouldRemoveLastActiveAdmin(User user, UserRole newRole, UserStatus newStatus) {
    if (user.getRole() != UserRole.admin || user.getStatus() != UserStatus.active) {
      return false;
    }
    UserRole roleAfter = newRole != null ? newRole : user.getRole();
    UserStatus statusAfter = newStatus != null ? newStatus : user.getStatus();
    if (roleAfter == UserRole.admin && statusAfter == UserStatus.active) {
      return false;
    }
    return countOtherActiveAdmins(user.getId()) < 1;
  }

  private long countOtherActiveAdmins(UUID excludeId) {
    return userRepository.findByTenantId(TenantCtx.get()).stream()
        .filter(u -> !u.getId().equals(excludeId))
        .filter(u -> u.getRole() == UserRole.admin && u.getStatus() == UserStatus.active)
        .count();
  }

  private List<ProjectSummary> soleOwnerProjects(UUID userId) {
    UUID tenantId = TenantCtx.get();
    List<ProjectSummary> result = new ArrayList<>();
    for (ProjectMember member : memberRepository.findByUserIdAndTenantId(userId, tenantId)) {
      if (member.getRole() != ProjectMemberRole.owner) {
        continue;
      }
      long owners = memberRepository.countByProjectIdAndRole(member.getProjectId(), ProjectMemberRole.owner);
      if (owners <= 1) {
        projectRepository
            .findByIdAndTenantIdAndDeletedAtIsNull(member.getProjectId(), tenantId)
            .ifPresent(p -> result.add(new ProjectSummary(p.getId(), p.getName())));
      }
    }
    return result;
  }

  private void detachUserFromTenant(UUID userId, boolean hardDelete) {
    UUID tenantId = TenantCtx.get();
    List<ProjectMember> memberships = new ArrayList<>(memberRepository.findByUserIdAndTenantId(userId, tenantId));

    for (ProjectMember member : memberships) {
      UUID projectId = member.getProjectId();
      if (member.getRole() == ProjectMemberRole.owner) {
        long owners = memberRepository.countByProjectIdAndRole(projectId, ProjectMemberRole.owner);
        if (owners <= 1) {
          taskRepository.deleteByProjectId(projectId);
          projectRepository
              .findByIdAndTenantIdAndDeletedAtIsNull(projectId, tenantId)
              .ifPresent(p -> {
                p.setDeletedAt(Instant.now());
                projectRepository.save(p);
              });
          memberRepository.delete(member);
          continue;
        }
        projectRepository.findByIdAndTenantIdAndDeletedAtIsNull(projectId, tenantId).ifPresent(p -> {
          if (userId.equals(p.getOwnerId())) {
            memberRepository.findByProjectId(projectId).stream()
                .filter(m -> !m.getUserId().equals(userId))
                .filter(m -> m.getRole() == ProjectMemberRole.owner)
                .findFirst()
                .ifPresent(other -> {
                  p.setOwnerId(other.getUserId());
                  projectRepository.save(p);
                });
          }
        });
      }
      taskRepository.deleteByProjectIdAndAssignedToId(projectId, userId);
      memberRepository.delete(member);
    }

    taskRepository.deleteByAssignedToIdAndTenantId(userId, tenantId);

    if (hardDelete) {
      taskRepository.deleteByCreatedByIdAndTenantId(userId, tenantId);
      for (Project project : projectRepository.findByOwnerIdAndTenantId(userId, tenantId)) {
        if (project.getDeletedAt() != null) {
          continue;
        }
        taskRepository.deleteByProjectId(project.getId());
        project.setDeletedAt(Instant.now());
        projectRepository.save(project);
      }
    }
  }

  private void requireAdmin() {
    if (LoggedInUser.current().getRole() != UserRole.admin) {
      throw new ForbiddenException("Admin only");
    }
  }
}
