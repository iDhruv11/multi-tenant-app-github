package com.example.saas.controller;

import com.example.saas.dto.Dtos.*;
import com.example.saas.model.Enums.TaskStatus;
import com.example.saas.service.TaskService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
public class TaskCtrl {

    private final TaskService taskService;

    public TaskCtrl(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping("/api/v1/projects/{projectId}/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse create(@PathVariable UUID projectId, @Valid @RequestBody CreateTaskRequest body) {
        return taskService.create(projectId, body);
    }

    @GetMapping("/api/v1/projects/{projectId}/tasks")
    public List<TaskResponse> listByProject(@PathVariable UUID projectId) {
        return taskService.listByProject(projectId);
    }

    @GetMapping("/api/v1/tasks")
    public List<TaskResponse> listAll(
            @RequestParam(required = false) TaskStatus status, @RequestParam(required = false) UUID assigneeId) {
        return taskService.listAll(status, assigneeId);
    }

    @GetMapping("/api/v1/tasks/{taskId}")
    public TaskResponse get(@PathVariable UUID taskId) {
        return taskService.get(taskId);
    }

    @PutMapping("/api/v1/tasks/{taskId}")
    public TaskResponse update(@PathVariable UUID taskId, @Valid @RequestBody UpdateTaskRequest body) {
        return taskService.update(taskId, body);
    }

    @DeleteMapping("/api/v1/tasks/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID taskId) {
        taskService.delete(taskId);
    }

    @PostMapping("/api/v1/tasks/{taskId}/assign")
    public TaskResponse assign(@PathVariable UUID taskId, @Valid @RequestBody AssignTaskRequest body) {
        return taskService.assign(taskId, body.userId());
    }
}
