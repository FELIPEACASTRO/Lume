package com.lume.workspace.dto;

import java.util.List;

public record LibraryEntryResponse(
        String id,
        String title,
        String category,
        String status,
        String availability,
        String owner,
        String sourceLabel,
        String summary,
        List<String> tags
) {
}
