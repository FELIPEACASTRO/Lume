package com.lume.workspace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTaskRequest(
        @NotBlank(message = "O prompt e obrigatorio")
        @Size(max = 4000, message = "O prompt nao pode ultrapassar 4000 caracteres")
        String prompt,
        @NotBlank(message = "O tipo da tarefa e obrigatorio")
        String taskType,
        String projectId,
        String providerCode,
        String modelCode,
        String versionLabel
) {
    public CreateTaskRequest(String prompt, String taskType, String projectId) {
        this(prompt, taskType, projectId, null, null, null);
    }
}
