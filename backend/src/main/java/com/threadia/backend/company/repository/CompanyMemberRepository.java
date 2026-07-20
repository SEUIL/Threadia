package com.threadia.backend.company.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.threadia.backend.company.domain.CompanyMember;
import com.threadia.backend.company.domain.CompanyMemberRole;
import com.threadia.backend.company.domain.CompanyMemberStatus;

import jakarta.persistence.LockModeType;

public interface CompanyMemberRepository extends JpaRepository<CompanyMember, Long> {

	Optional<CompanyMember> findByCompany_IdAndUser_Id(Long companyId, Long userId);

	@Query("""
			select companyMember
			from CompanyMember companyMember
			join fetch companyMember.company
			where companyMember.user.id = :userId
			  and companyMember.status = :status
			order by companyMember.company.id asc, companyMember.id asc
			""")
	List<CompanyMember> findByUserIdAndStatusWithCompanyOrderByCompanyIdAscCompanyMemberIdAsc(
			@Param("userId") Long userId,
			@Param("status") CompanyMemberStatus status);

	@Query("""
			select companyMember
			from CompanyMember companyMember
			join fetch companyMember.company
			join fetch companyMember.user
			where companyMember.company.id = :companyId
			  and (:status is null or companyMember.status = :status)
			  and (:departmentId is null or companyMember.departmentId = :departmentId)
			  and (:q is null
			       or lower(companyMember.user.email) like concat(concat('%', :q), '%')
			       or lower(companyMember.user.nickname) like concat(concat('%', :q), '%'))
			order by companyMember.id asc
			""")
	List<CompanyMember> findByCompanyIdWithFiltersOrderByIdAsc(
			@Param("companyId") Long companyId,
			@Param("status") CompanyMemberStatus status,
			@Param("departmentId") Long departmentId,
			@Param("q") String q);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select companyMember
			from CompanyMember companyMember
			join fetch companyMember.company
			join fetch companyMember.user
			where companyMember.id = :companyMemberId
			  and companyMember.company.id = :companyId
			""")
	Optional<CompanyMember> findByIdAndCompanyIdForUpdate(
			@Param("companyMemberId") Long companyMemberId,
			@Param("companyId") Long companyId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select companyMember
			from CompanyMember companyMember
			where companyMember.company.id = :companyId
			  and companyMember.user.id = :userId
			""")
	Optional<CompanyMember> findByCompany_IdAndUser_IdForUpdate(
			@Param("companyId") Long companyId,
			@Param("userId") Long userId);

	boolean existsByCompany_IdAndUser_IdAndRoleAndStatus(
			Long companyId,
			Long userId,
			CompanyMemberRole role,
			CompanyMemberStatus status);

	boolean existsByCompany_IdAndUser_IdAndStatus(Long companyId, Long userId, CompanyMemberStatus status);

	List<CompanyMember> findByCompany_IdAndUser_IdInAndStatus(
			Long companyId,
			List<Long> userIds,
			CompanyMemberStatus status);

	long countByCompany_IdAndStatus(Long companyId, CompanyMemberStatus status);

	long countByCompany_IdAndRoleAndStatus(Long companyId, CompanyMemberRole role, CompanyMemberStatus status);

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
