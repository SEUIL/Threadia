package com.threadia.backend.issue.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.threadia.backend.issue.domain.IssueStatusHistory;

public interface IssueStatusHistoryRepository extends JpaRepository<IssueStatusHistory, Long> {
}
