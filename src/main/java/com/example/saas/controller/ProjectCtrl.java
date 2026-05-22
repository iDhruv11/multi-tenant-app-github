package com.example.saas.controller;

import com.example.saas.dto.Dtos.CreateProjectRequest;
import com.example.saas.dto.Dtos.ProjectResponse;
import com.example.saas.service.ProjectService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectCtrl {

  private final ProjectService projectService;

  public ProjectCtrl(ProjectService projectService) {
    this.projectService = projectService;
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
}
