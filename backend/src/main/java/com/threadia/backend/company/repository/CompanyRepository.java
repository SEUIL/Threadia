package com.threadia.backend.company.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.threadia.backend.company.domain.Company;
import com.threadia.backend.company.domain.CompanyStatus;

import jakarta.persistence.LockModeType;

public interface CompanyRepository extends JpaRepository<Company, Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select company from Company company where company.id = :companyId")
	Optional<Company> findByIdForUpdate(@Param("companyId") Long companyId);

	@Query("""
			select company from Company company
			where (:status is null or company.status = :status)
			and (:q is null or lower(company.name) like concat('%', :q, '%') escape '\\')
			and (:cursorId is null or company.id < :cursorId)
			order by company.id desc
			""")
	List<Company> findPlatformCompanyPage(
			@Param("status") CompanyStatus status,
			@Param("q") String q,
			@Param("cursorId") Long cursorId,
			Pageable pageable);
}
