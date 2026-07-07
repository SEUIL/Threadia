package com.threadia.backend.company.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.threadia.backend.company.domain.CompanyMember;
import com.threadia.backend.company.domain.CompanyMemberRole;
import com.threadia.backend.company.domain.CompanyMemberStatus;

public interface CompanyMemberRepository extends JpaRepository<CompanyMember, Long> {

	boolean existsByCompany_IdAndUser_IdAndRoleAndStatus(
			Long companyId,
			Long userId,
			CompanyMemberRole role,
			CompanyMemberStatus status);

	boolean existsByCompany_IdAndUser_Id(Long companyId, Long userId);

	boolean existsByCompany_IdAndUser_IdAndStatus(Long companyId, Long userId, CompanyMemberStatus status);
}
