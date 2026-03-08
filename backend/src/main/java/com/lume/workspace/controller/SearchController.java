package com.lume.workspace.controller;

import com.lume.workspace.dto.SearchResultResponse;
import com.lume.workspace.service.SearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    public ResponseEntity<List<SearchResultResponse>> search(
            @RequestParam(name = "q", required = false) String query
    ) {
        return ResponseEntity.ok(searchService.search(query));
    }
}
