package com.threadia.backend.company.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

	long countByCompany_IdAndStatus(Long companyId, CompanyMemberStatus status);

	long countByDepartmentId(Long departmentId);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			update CompanyMember companyMember
			set companyMember.departmentId = null,
			    companyMember.updatedAt = :updatedAt
			where companyMember.departmentId = :departmentId
			""")
	int clearDepartmentId(@Param("departmentId") Long departmentId, @Param("updatedAt") java.time.LocalDateTime updatedAt);

	boolean existsByCompany_IdAndUser_Id(Long companyId, Long userId);
}
