package com.lume.workspace.controller;

import com.lume.workspace.dto.LibraryEntryResponse;
import com.lume.workspace.service.LibraryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/library")
public class LibraryController {

    private final LibraryService libraryService;

    public LibraryController(LibraryService libraryService) {
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
}
