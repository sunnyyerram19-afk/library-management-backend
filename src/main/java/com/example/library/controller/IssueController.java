package com.example.library.controller;

import com.example.library.dto.IssueDto;
import com.example.library.dto.IssueRequest;
import com.example.library.service.IssueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/issues")
@RequiredArgsConstructor
public class IssueController {

    private final IssueService issueService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<IssueDto> all() {
        return issueService.listAll();
    }

    // A member sees only their own borrowed books.
    @GetMapping("/my")
    public List<IssueDto> mine(Authentication authentication) {
        return issueService.listForUser(authentication.getName());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public IssueDto issue(@Valid @RequestBody IssueRequest request) {
        return issueService.issueBook(request);
    }

    @PutMapping("/{id}/return")
    @PreAuthorize("hasRole('ADMIN')")
    public IssueDto returnBook(@PathVariable Long id) {
        return issueService.returnBook(id);
    }
}
