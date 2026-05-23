package com.example.saas.controller;

import com.example.saas.dto.Dtos.*;
import com.example.saas.service.AuditService;
import com.example.saas.service.ProjectService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectCtrl {

  private final ProjectService projectService;
  private final AuditService auditService;

  public ProjectCtrl(ProjectService projectService, AuditService auditService) {
    this.projectService = projectService;
    this.auditService = auditService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ProjectResponse create(@Valid @RequestBody CreateProjectRequest body) {
    return projectService.create(body);
  }

  @GetMapping
  public List<ProjectResponse> list() {
    return projectService.list();
  }

  @GetMapping("/{projectId}")
  public ProjectResponse get(@PathVariable UUID projectId) {
    return projectService.get(projectId);
  }

  @PutMapping("/{projectId}")
  public ProjectResponse update(@PathVariable UUID projectId, @Valid @RequestBody UpdateProjectRequest body) {
    return projectService.update(projectId, body);
  }

  @DeleteMapping("/{projectId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID projectId) {
    projectService.delete(projectId);
  }

  @GetMapping("/{projectId}/members")
  public List<ProjectMemberResponse> listMembers(@PathVariable UUID projectId) {
    return projectService.listMembers(projectId);
  }

  @GetMapping("/{projectId}/activity")
  public Page<AuditLogResponse> activity(
      @PathVariable UUID projectId, @PageableDefault(size = 20) Pageable pageable) {
    return auditService.listForProject(projectId, pageable);
  }

  @PostMapping("/{projectId}/members")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void addMember(@PathVariable UUID projectId, @Valid @RequestBody AddMemberRequest body) {
    projectService.addMember(projectId, body.userId(), body.role());
  }

  @PutMapping("/{projectId}/members/{userId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void updateMember(
      @PathVariable UUID projectId,
      @PathVariable UUID userId,
      @Valid @RequestBody UpdateMemberRequest body) {
    projectService.updateMemberRole(projectId, userId, body.role());
  }

  @DeleteMapping("/{projectId}/members/{userId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void removeMember(@PathVariable UUID projectId, @PathVariable UUID userId) {
    projectService.removeMember(projectId, userId);
  }
}
