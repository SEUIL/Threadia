package com.threadia.backend.company.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.threadia.backend.company.domain.CompanyMember;

public interface CompanyMemberRepository extends JpaRepository<CompanyMember, Long> {
}
