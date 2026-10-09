package com.example.library.service;

import com.example.library.dto.IssueDto;
import com.example.library.dto.IssueRequest;
import com.example.library.entity.Book;
import com.example.library.entity.IssueRecord;
import com.example.library.entity.IssueStatus;
import com.example.library.entity.Member;
import com.example.library.exception.ConflictException;
import com.example.library.exception.ResourceNotFoundException;
import com.example.library.repository.BookRepository;
import com.example.library.repository.IssueRecordRepository;
import com.example.library.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IssueService {

    private final IssueRecordRepository issues;
    private final BookRepository books;
    private final MemberRepository members;

    @Value("${library.loan-days}")
    private int loanDays;

    @Value("${library.fine-per-day}")
    private int finePerDay;

    @Transactional(readOnly = true)
    public List<IssueDto> listAll() {
        return issues.findAllByOrderByIdDesc().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<IssueDto> listForUser(String username) {
        return members.findByUserUsername(username)
                .map(m -> issues.findByMemberIdOrderByIdDesc(m.getId()).stream().map(this::toDto).toList())
                .orElse(List.of());
    }

    @Transactional
    public IssueDto issueBook(IssueRequest req) {
        Book book = books.findById(req.bookId())
                .orElseThrow(() -> new ResourceNotFoundException("Book not found"));
        Member member = members.findById(req.memberId())
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

        if (book.getAvailableCopies() <= 0) {
            throw new ConflictException("No copies of this book are available right now");
        }
        if (issues.existsByBookIdAndMemberIdAndStatus(book.getId(), member.getId(), IssueStatus.ISSUED)) {
            throw new ConflictException("This member already has this book");
        }

        book.setAvailableCopies(book.getAvailableCopies() - 1);

        LocalDate today = LocalDate.now();
        IssueRecord record = new IssueRecord();
        record.setBook(book);
        record.setMember(member);
        record.setIssueDate(today);
        record.setDueDate(today.plusDays(loanDays));
        record.setFine(BigDecimal.ZERO);
        record.setStatus(IssueStatus.ISSUED);
        return toDto(issues.save(record));
    }

    @Transactional
    public IssueDto returnBook(Long issueId) {
        IssueRecord record = issues.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue record not found"));
        if (record.getStatus() == IssueStatus.RETURNED) {
            throw new ConflictException("This book has already been returned");
        }

        LocalDate today = LocalDate.now();
        record.setReturnDate(today);
        record.setFine(calculateFine(record.getDueDate(), today));
        record.setStatus(IssueStatus.RETURNED);

        Book book = record.getBook();
        book.setAvailableCopies(book.getAvailableCopies() + 1);
        return toDto(record);
    }

    private BigDecimal calculateFine(LocalDate dueDate, LocalDate returnedOn) {
        long daysLate = ChronoUnit.DAYS.between(dueDate, returnedOn);
        if (daysLate <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(finePerDay).multiply(BigDecimal.valueOf(daysLate));
    }

    private IssueDto toDto(IssueRecord r) {
        LocalDate today = LocalDate.now();
        boolean overdue = r.getStatus() == IssueStatus.ISSUED && today.isAfter(r.getDueDate());
        // While a book is still out and late, show the fine that has built up so far.
        BigDecimal fine = overdue ? calculateFine(r.getDueDate(), today) : r.getFine();
        return new IssueDto(
                r.getId(),
                r.getBook().getId(),
                r.getBook().getTitle(),
                r.getMember().getId(),
                r.getMember().getName(),
                r.getIssueDate(),
                r.getDueDate(),
                r.getReturnDate(),
                fine,
                r.getStatus().name(),
                overdue);
    }
}
