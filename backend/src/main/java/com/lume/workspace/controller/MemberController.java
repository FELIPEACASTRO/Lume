package com.lume.workspace.controller;

import com.lume.workspace.dto.CreateMemberRequest;
import com.lume.workspace.dto.MemberResponse;
import com.lume.workspace.dto.UpdateMemberRequest;
import com.lume.workspace.service.MemberService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/members")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping
    public ResponseEntity<List<MemberResponse>> members() {
        return ResponseEntity.ok(memberService.listMembers());
    }

    @PostMapping
    public ResponseEntity<MemberResponse> create(@Valid @RequestBody CreateMemberRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(memberService.createMember(request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<MemberResponse> update(
            @PathVariable Long id,
            @RequestBody UpdateMemberRequest request
    ) {
        return ResponseEntity.ok(memberService.updateMember(id, request));
    }
}
