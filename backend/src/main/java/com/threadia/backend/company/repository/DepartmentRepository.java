package com.threadia.backend.company.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.threadia.backend.company.domain.Department;

import jakarta.persistence.LockModeType;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

	List<Department> findByCompany_IdOrderByNameAsc(Long companyId);

	boolean existsByCompany_IdAndName(Long companyId, String name);

	boolean existsByCompany_IdAndNameAndIdNot(Long companyId, String name, Long departmentId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select department from Department department join fetch department.company where department.id = :departmentId")
	Optional<Department> findByIdForUpdate(@Param("departmentId") Long departmentId);
}
