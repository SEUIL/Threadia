package com.threadia.backend.company.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.threadia.backend.company.domain.CompanyMember;
import com.threadia.backend.company.domain.CompanyMemberRole;
import com.threadia.backend.company.domain.CompanyMemberStatus;

public interface CompanyMemberRepository extends JpaRepository<CompanyMember, Long> {

	Optional<CompanyMember> findByCompany_IdAndUser_Id(Long companyId, Long userId);

	boolean existsByCompany_IdAndUser_IdAndRoleAndStatus(
			Long companyId,
			Long userId,
			CompanyMemberRole role,
			CompanyMemberStatus status);

	boolean existsByCompany_IdAndUser_IdAndStatus(Long companyId, Long userId, CompanyMemberStatus status);

	boolean existsByCompany_IdAndUser_Id(Long companyId, Long userId);

	Optional<CompanyMember> findByCompany_IdAndUser_Id(Long companyId, Long userId);
}
