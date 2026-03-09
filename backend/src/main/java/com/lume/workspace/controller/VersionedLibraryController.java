package com.lume.workspace.controller;

import com.lume.workspace.dto.ArtifactVersionResponse;
import com.lume.workspace.dto.CreateArtifactVersionRequest;
import com.lume.workspace.dto.LibraryEntryResponse;
import com.lume.workspace.service.LibraryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/v1/library", "/api/v1/library"})
public class VersionedLibraryController {

    private final LibraryService libraryService;

    public VersionedLibraryController(LibraryService libraryService) {
        this.libraryService = libraryService;
    }

    @GetMapping("/entries")
    public ResponseEntity<List<LibraryEntryResponse>> findAll(
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(name = "category", required = false) String category
    ) {
        return ResponseEntity.ok(libraryService.findAll(query, category));
    }

    @GetMapping("/entries/{id}")
    public ResponseEntity<LibraryEntryResponse> findById(@PathVariable String id) {
        return ResponseEntity.ok(libraryService.findById(id));
    }

    @GetMapping("/entries/{id}/versions")
    public ResponseEntity<List<ArtifactVersionResponse>> listVersions(@PathVariable String id) {
        return ResponseEntity.ok(libraryService.listVersions(id));
    }

    @PostMapping("/entries/{id}/versions")
    public ResponseEntity<ArtifactVersionResponse> createVersion(
            @PathVariable String id,
            @Valid @RequestBody CreateArtifactVersionRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(libraryService.createVersion(id, request));
    }
}
