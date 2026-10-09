package com.example.library.service;

import com.example.library.dto.DashboardStats;
import com.example.library.entity.IssueStatus;
import com.example.library.repository.BookRepository;
import com.example.library.repository.IssueRecordRepository;
import com.example.library.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final BookRepository books;
    private final MemberRepository members;
    private final IssueRecordRepository issues;

    @Transactional(readOnly = true)
    public DashboardStats stats() {
        return new DashboardStats(
                books.count(),
                members.count(),
                issues.countByStatus(IssueStatus.ISSUED),
                issues.countByStatusAndDueDateBefore(IssueStatus.ISSUED, LocalDate.now()));
    }
}
