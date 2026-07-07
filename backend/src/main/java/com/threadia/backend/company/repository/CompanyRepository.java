package com.threadia.backend.company.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.threadia.backend.company.domain.Company;

public interface CompanyRepository extends JpaRepository<Company, Long> {
}
