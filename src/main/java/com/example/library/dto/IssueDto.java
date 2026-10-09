package com.example.library.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record IssueDto(
        Long id,
        Long bookId,
        String bookTitle,
        Long memberId,
        String memberName,
        LocalDate issueDate,
        LocalDate dueDate,
        LocalDate returnDate,
        BigDecimal fine,
        String status,
        boolean overdue) {
}
