package com.threadia.backend.company.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.threadia.backend.auth.domain.User;
import com.threadia.backend.auth.domain.UserStatus;
import com.threadia.backend.common.auth.AuthContext;
import com.threadia.backend.common.auth.AuthContextService;
import com.threadia.backend.company.domain.Company;
import com.threadia.backend.company.domain.Department;
import com.threadia.backend.company.dto.DepartmentData;
import com.threadia.backend.company.dto.DepartmentDeleteData;
import com.threadia.backend.company.dto.DepartmentRequest;
import com.threadia.backend.company.dto.DepartmentsData;
import com.threadia.backend.company.exception.DepartmentConflictException;
import com.threadia.backend.company.repository.CompanyMemberRepository;
import com.threadia.backend.company.repository.CompanyRepository;
import com.threadia.backend.company.repository.DepartmentRepository;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

	private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-07-07T00:00:00Z"), ZoneOffset.UTC);

	@Mock
	private AuthContextService authContextService;

	@Mock
	private CompanyAccessService companyAccessService;

	@Mock
	private CompanyRepository companyRepository;

	@Mock
	private DepartmentRepository departmentRepository;

	@Mock
	private CompanyMemberRepository companyMemberRepository;

	private DepartmentService departmentService;

	@BeforeEach
	void setUp() {
		departmentService = new DepartmentService(
				authContextService,
				companyAccessService,
				companyRepository,
				departmentRepository,
				companyMemberRepository,
				FIXED_CLOCK);
	}

	@Test
	void listRequiresCompanyAdminAndReturnsDepartments() {
		AuthContext authContext = authContext();
		User admin = user(1L);
		Company company = company(10L, admin);
		when(authContextService.fromAccessToken("admin-token")).thenReturn(authContext);
		when(authContextService.requireActiveUser(authContext)).thenReturn(admin);
		when(companyRepository.findById(10L)).thenReturn(Optional.of(company));
		when(departmentRepository.findByCompany_IdOrderByNameAsc(10L))
				.thenReturn(List.of(department(1L, company, "디자인팀")));

		DepartmentsData data = departmentService.list("admin-token", 10L);

		InOrder inOrder = inOrder(authContextService, companyRepository, companyAccessService);
		inOrder.verify(authContextService).fromAccessToken("admin-token");
		inOrder.verify(authContextService).requireActiveUser(authContext);
		inOrder.verify(companyRepository).findById(10L);
		inOrder.verify(companyAccessService).requireCompanyAdmin(10L, authContext);
		assertThat(data.departments()).hasSize(1);
		assertThat(data.departments().get(0).name()).isEqualTo("디자인팀");
	}

	@Test
	void createSavesDepartmentWhenNameIsUnique() {
		AuthContext authContext = authContext();
		User admin = user(1L);
		Company company = company(10L, admin);
		when(authContextService.fromAccessToken("admin-token")).thenReturn(authContext);
		when(authContextService.requireActiveUser(authContext)).thenReturn(admin);
		when(companyRepository.findById(10L)).thenReturn(Optional.of(company));
		when(departmentRepository.existsByCompany_IdAndName(10L, "디자인팀")).thenReturn(false);
		when(departmentRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
			Department department = invocation.getArgument(0);
			setId(department, 1L);
			return department;
		});

		DepartmentData data = departmentService.create("admin-token", 10L, new DepartmentRequest(" 디자인팀 "));

		InOrder inOrder = inOrder(authContextService, companyRepository, companyAccessService);
		inOrder.verify(authContextService).fromAccessToken("admin-token");
		inOrder.verify(authContextService).requireActiveUser(authContext);
		inOrder.verify(companyRepository).findById(10L);
		inOrder.verify(companyAccessService).requireCompanyAdmin(10L, authContext);
		ArgumentCaptor<Department> departmentCaptor = ArgumentCaptor.forClass(Department.class);
		verify(departmentRepository).save(departmentCaptor.capture());
		assertThat(departmentCaptor.getValue().getName()).isEqualTo("디자인팀");
		assertThat(data.department().id()).isEqualTo(1L);
		assertThat(data.department().name()).isEqualTo("디자인팀");
	}

	@Test
	void createThrowsConflictWhenNameAlreadyExistsInCompany() {
		AuthContext authContext = authContext();
		User admin = user(1L);
		Company company = company(10L, admin);
		when(authContextService.fromAccessToken("admin-token")).thenReturn(authContext);
		when(authContextService.requireActiveUser(authContext)).thenReturn(admin);
		when(companyRepository.findById(10L)).thenReturn(Optional.of(company));
		when(departmentRepository.existsByCompany_IdAndName(10L, "디자인팀")).thenReturn(true);

		assertThatThrownBy(() -> departmentService.create("admin-token", 10L, new DepartmentRequest("디자인팀")))
				.isInstanceOf(DepartmentConflictException.class);
	}

	@Test
	void updateRequiresCompanyAdminFromDepartmentCompanyAndUpdatesName() {
		AuthContext authContext = authContext();
		User admin = user(1L);
		Company company = company(10L, admin);
		Department department = department(1L, company, "디자인팀");
		when(authContextService.fromAccessToken("admin-token")).thenReturn(authContext);
		when(authContextService.requireActiveUser(authContext)).thenReturn(admin);
		when(departmentRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(department));
		when(departmentRepository.existsByCompany_IdAndNameAndIdNot(10L, "UX팀", 1L)).thenReturn(false);

		DepartmentData data = departmentService.update("admin-token", 1L, new DepartmentRequest("UX팀"));

		verify(companyAccessService).requireCompanyAdmin(10L, authContext);
		assertThat(department.getName()).isEqualTo("UX팀");
		assertThat(data.department().name()).isEqualTo("UX팀");
	}

	@Test
	void deleteCountsMembersClearsDepartmentAndDeletesDepartment() {
		AuthContext authContext = authContext();
		User admin = user(1L);
		Company company = company(10L, admin);
		Department department = department(1L, company, "디자인팀");
		when(authContextService.fromAccessToken("admin-token")).thenReturn(authContext);
		when(authContextService.requireActiveUser(authContext)).thenReturn(admin);
		when(departmentRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(department));
		when(companyMemberRepository.countByDepartmentId(1L)).thenReturn(3L);

		DepartmentDeleteData data = departmentService.delete("admin-token", 1L);

		InOrder inOrder = inOrder(companyAccessService, companyMemberRepository, departmentRepository);
		inOrder.verify(companyAccessService).requireCompanyAdmin(10L, authContext);
		inOrder.verify(companyMemberRepository).countByDepartmentId(1L);
		inOrder.verify(companyMemberRepository).clearDepartmentId(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.any());
		inOrder.verify(departmentRepository).delete(department);
		assertThat(data.deleted()).isTrue();
		assertThat(data.affectedMemberCount()).isEqualTo(3L);
	}

	private static AuthContext authContext() {
		return new AuthContext(1L, "admin@example.com");
	}

	private static User user(Long id) {
		LocalDateTime now = LocalDateTime.now(FIXED_CLOCK);
		User user = new User("admin@example.com", "hashed", "admin", null, UserStatus.ACTIVE, now, now);
		setId(user, id);
		return user;
	}

	private static Company company(Long id, User createdBy) {
		LocalDateTime now = LocalDateTime.now(FIXED_CLOCK);
		Company company = new Company("Threadia Inc.", "Admin", "workspace", "IT", null, "ACTIVE", createdBy, now, now);
		setId(company, id);
		return company;
	}

	private static Department department(Long id, Company company, String name) {
		LocalDateTime now = LocalDateTime.now(FIXED_CLOCK);
		Department department = new Department(company, name, now, now);
		setId(department, id);
		return department;
	}

	private static void setId(Object entity, Long id) {
		try {
			Field field = entity.getClass().getDeclaredField("id");
			field.setAccessible(true);
			field.set(entity, id);
		} catch (ReflectiveOperationException exception) {
			throw new IllegalStateException(exception);
		}
	}
}
