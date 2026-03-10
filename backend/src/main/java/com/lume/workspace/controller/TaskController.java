package com.lume.workspace.controller;

import com.lume.workspace.dto.CreateTaskRequest;
import com.lume.workspace.dto.TaskDetailResponse;
import com.lume.workspace.dto.TaskSummaryResponse;
import com.lume.workspace.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public ResponseEntity<List<TaskSummaryResponse>> tasks(
            @RequestParam(name = "project", required = false) String projectId
    ) {
        return ResponseEntity.ok(taskService.listTasks(projectId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskDetailResponse> task(@PathVariable String id) {
        return ResponseEntity.ok(taskService.getTask(id));
    }

    @PostMapping
    public ResponseEntity<TaskDetailResponse> createTask(
            @Valid @RequestBody CreateTaskRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.createTask(request));
    }
}
