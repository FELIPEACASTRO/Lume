package com.lume.workspace.dto;

import jakarta.validation.constraints.Size;

public record UpdateMemberRequest(
        @Size(max = 200) String name,
        @Size(max = 320) String email,
        @Size(max = 128) String password,
        @Size(max = 40) String roleCode,
        Boolean active
) {
}
