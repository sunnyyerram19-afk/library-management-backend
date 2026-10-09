package com.example.library.repository;

import com.example.library.entity.IssueRecord;
import com.example.library.entity.IssueStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface IssueRecordRepository extends JpaRepository<IssueRecord, Long> {

    @EntityGraph(attributePaths = {"book", "member"})
    List<IssueRecord> findAllByOrderByIdDesc();

    @EntityGraph(attributePaths = {"book", "member"})
    List<IssueRecord> findByMemberIdOrderByIdDesc(Long memberId);

    boolean existsByBookId(Long bookId);

    boolean existsByMemberId(Long memberId);

    boolean existsByBookIdAndMemberIdAndStatus(Long bookId, Long memberId, IssueStatus status);

    long countByStatus(IssueStatus status);

    long countByStatusAndDueDateBefore(IssueStatus status, LocalDate date);
}
