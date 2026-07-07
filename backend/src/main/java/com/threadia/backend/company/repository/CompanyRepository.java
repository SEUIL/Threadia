package com.threadia.backend.company.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.threadia.backend.company.domain.Company;

import jakarta.persistence.LockModeType;

public interface CompanyRepository extends JpaRepository<Company, Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select company from Company company where company.id = :companyId")
	Optional<Company> findByIdForUpdate(@Param("companyId") Long companyId);
}
