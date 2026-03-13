package com.lume.workspace.controller;

import com.lume.workspace.dto.SearchResultsResponse;
import com.lume.workspace.service.SearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/search")
public class VersionedSearchController {

    private final SearchService searchService;

    public VersionedSearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/results")
    public ResponseEntity<SearchResultsResponse> searchResults(
            @RequestParam(name = "q", required = false) String query
    ) {
        return ResponseEntity.ok(searchService.searchResults(query));
    }
}
