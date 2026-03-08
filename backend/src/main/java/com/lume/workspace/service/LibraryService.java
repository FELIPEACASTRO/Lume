package com.lume.workspace.service;

import com.lume.domain.exception.ResourceNotFoundException;
import com.lume.workspace.dto.LibraryEntryResponse;
import com.lume.workspace.entity.LibraryEntryJpaEntity;
import com.lume.workspace.repository.LibraryEntryJpaRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class LibraryService {

    private final LibraryEntryJpaRepository libraryEntryRepository;
    private final WorkspaceContextService workspaceContextService;

    public LibraryService(
            LibraryEntryJpaRepository libraryEntryRepository,
            WorkspaceContextService workspaceContextService
    ) {
        this.libraryEntryRepository = libraryEntryRepository;
        this.workspaceContextService = workspaceContextService;
    }

    public List<LibraryEntryResponse> findAll(String query, String category) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase();
        String normalizedCategory = category == null ? "" : category.trim().toLowerCase();

        return libraryEntryRepository.findByWorkspaceIdOrderByUpdatedAtDesc(workspaceContextService.getWorkspaceId())
                .stream()
                .filter(entry -> normalizedCategory.isBlank() || entry.getCategory().equalsIgnoreCase(normalizedCategory))
                .filter(entry -> normalizedQuery.isBlank() || matchesQuery(entry, normalizedQuery))
                .map(this::toResponse)
                .toList();
    }

    public LibraryEntryResponse findById(String id) {
        LibraryEntryJpaEntity entry = libraryEntryRepository.findByIdAndWorkspaceId(id, workspaceContextService.getWorkspaceId())
                .orElseThrow(() -> new ResourceNotFoundException("LibraryEntry", id));
        return toResponse(entry);
    }

    private boolean matchesQuery(LibraryEntryJpaEntity entry, String normalizedQuery) {
        String haystack = String.join(
                " ",
                entry.getTitle(),
                entry.getSummary(),
                entry.getOwnerName(),
                entry.getCategory(),
                String.join(" ", entry.getTags())
        ).toLowerCase();
        return haystack.contains(normalizedQuery);
    }

    private LibraryEntryResponse toResponse(LibraryEntryJpaEntity entry) {
        List<String> tags = entry.getTags().stream().sorted(Comparator.naturalOrder()).toList();
        return new LibraryEntryResponse(
                entry.getId(),
                entry.getTitle(),
                entry.getCategory(),
                entry.getStatusLabel(),
                entry.getAvailability(),
                entry.getOwnerName(),
                entry.getSourceLabel(),
                entry.getSummary(),
                tags
        );
    }
}
