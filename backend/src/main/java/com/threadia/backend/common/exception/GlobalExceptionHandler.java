package com.threadia.backend.common.exception;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.threadia.backend.common.error.ErrorBody;
import com.threadia.backend.common.error.ErrorResponse;
import com.threadia.backend.company.exception.CompanyMemberConflictException;
import com.threadia.backend.company.exception.CompanyMemberNotFoundException;
import com.threadia.backend.company.exception.CompanyNotFoundException;
import com.threadia.backend.company.exception.DepartmentConflictException;
import com.threadia.backend.company.exception.DepartmentNotFoundException;
import com.threadia.backend.config.jwt.JwtAuthenticationException;
import com.threadia.backend.invitation.exception.InvitationConflictException;
import com.threadia.backend.invitation.exception.InvitationNotFoundException;
import com.threadia.backend.project.exception.ProjectConflictException;
import com.threadia.backend.project.exception.ProjectNotFoundException;
import com.threadia.backend.project.exception.ProjectValidationException;

// 앱 전체 컨트롤러에서 발생한 예외를 공통 에러 응답 형식으로 변환한다.
// 컨트롤러/서비스 코드는 예외를 던지기만 하면 되고, HTTP status/에러 body 조립은 여기서 한 번에 처리한다.
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	// MySQL ER_DUP_ENTRY. unique 제약 위반(중복 키)일 때만 이 코드가 붙는다. NOT NULL(1048)·FK(1452) 등과 구분하는 기준.
	private static final int MYSQL_DUPLICATE_ENTRY_ERROR_CODE = 1062;

	// @Valid 검증 실패(SignupRequest 등) -> 400, 필드별 실패 메시지를 details에 담는다.
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
		Map<String, Object> details = new LinkedHashMap<>();
		for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
			details.put(fieldError.getField(), fieldError.getDefaultMessage());
		}
		ErrorBody body = ErrorBody.of("VALIDATION_ERROR", "요청 형식이 올바르지 않습니다.", details);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse.of(body));
	}

	// signup 이메일 중복 -> 409. 원문 email은 응답에는 담지 않고 log에만 남긴다.
	@ExceptionHandler(EmailAlreadyExistsException.class)
	public ResponseEntity<ErrorResponse> handleEmailAlreadyExists(EmailAlreadyExistsException ex) {
		log.info("signup 중복 이메일 시도: {}", ex.getEmail());
		ErrorBody body = ErrorBody.of("CONFLICT", ex.getMessage());
		return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.of(body));
	}

	// JWT 토큰 문제(누락/만료/변조)와 로그인 credential 실패/비활성 계정을 모두 401로 통일해 응답한다.
	@ExceptionHandler({JwtAuthenticationException.class, AuthenticationFailedException.class})
	public ResponseEntity<ErrorResponse> handleUnauthorized(RuntimeException ex) {
		ErrorBody body = ErrorBody.of("UNAUTHORIZED", ex.getMessage());
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse.of(body));
	}

	@ExceptionHandler(ForbiddenException.class)
	public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException ex) {
		ErrorBody body = ErrorBody.of("FORBIDDEN", ex.getMessage());
		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ErrorResponse.of(body));
	}

	// 요청 body가 비어있거나 JSON 형식이 깨진 경우(정의되지 않은 필드 포함) -> 400. 원인 메시지는 details에 담지 않는다(내부 파싱 정보 노출 금지).
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResponse> handleMessageNotReadable(HttpMessageNotReadableException ex) {
		ErrorBody body = ErrorBody.of("VALIDATION_ERROR", "요청 형식이 올바르지 않습니다.");
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse.of(body));
	}

	@ExceptionHandler(ProjectValidationException.class)
	public ResponseEntity<ErrorResponse> handleProjectValidation(ProjectValidationException ex) {
		ErrorBody body = ErrorBody.of("VALIDATION_ERROR", ex.getMessage());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse.of(body));
	}

	@ExceptionHandler(UserNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex) {
		ErrorBody body = ErrorBody.of("NOT_FOUND", ex.getMessage());
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse.of(body));
	}

	@ExceptionHandler({
			CompanyNotFoundException.class,
			CompanyMemberNotFoundException.class,
			DepartmentNotFoundException.class,
			ProjectNotFoundException.class,
			com.threadia.backend.invitation.exception.CompanyNotFoundException.class})
	public ResponseEntity<ErrorResponse> handleCompanyNotFound(RuntimeException ex) {
		ErrorBody body = ErrorBody.of("NOT_FOUND", ex.getMessage());
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse.of(body));
	}

	@ExceptionHandler({DepartmentConflictException.class, CompanyMemberConflictException.class, ProjectConflictException.class})
	public ResponseEntity<ErrorResponse> handleCompanyConflict(RuntimeException ex) {
		ErrorBody body = ErrorBody.of("CONFLICT", ex.getMessage());
		return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.of(body));
	}

	@ExceptionHandler(InvitationNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleInvitationNotFound(InvitationNotFoundException ex) {
		ErrorBody body = ErrorBody.of("NOT_FOUND", ex.getMessage());
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse.of(body));
	}

	@ExceptionHandler(InvitationConflictException.class)
	public ResponseEntity<ErrorResponse> handleInvitationConflict(InvitationConflictException ex) {
		ErrorBody body = ErrorBody.of(ex.getCode(), ex.getMessage());
		return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.of(body));
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<ErrorResponse> handleMissingRequestParameter(MissingServletRequestParameterException ex) {
		ErrorBody body = ErrorBody.of("VALIDATION_ERROR", "요청 파라미터가 누락되었습니다.");
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse.of(body));
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ErrorResponse> handleArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
		ErrorBody body = ErrorBody.of("VALIDATION_ERROR", "요청 경로 변수가 올바르지 않습니다.");
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse.of(body));
	}

	// 무결성 위반 중 "중복 키(unique 제약)"만 409로 변환한다. 예: existsByEmail 확인 이후 저장 사이의
	// 동시 가입 경합을 uq_users_email이 막아준 경우. NOT NULL/FK 위반 등은 클라이언트가 고칠 수 있는 값 문제가
	// 아니라 서버/데이터 버그에 가까우므로 409로 위장하지 않고 500(catch-all)으로 흘려보낸다.
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
		if (isDuplicateKeyViolation(ex)) {
			ErrorBody body = ErrorBody.of("CONFLICT", "이미 존재하는 데이터입니다.");
			return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.of(body));
		}
		return handleUnexpected(ex);
	}

	// 원인 체인에서 SQLException을 찾아 MySQL 중복 키 에러코드인지 확인한다. JPA/JDBC 경로 모두 근본 SQLException을 보존한다.
	private static boolean isDuplicateKeyViolation(Throwable ex) {
		for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
			if (cause instanceof SQLException sqlException
					&& sqlException.getErrorCode() == MYSQL_DUPLICATE_ENTRY_ERROR_CODE) {
				return true;
			}
		}
		return false;
	}

	// Content-Type이 컨트롤러가 소비 가능한 타입(application/json)이 아닌 경우 -> 415. 이 예외를 별도 처리하지
	// 않으면 catch-all(Exception)로 흘러가 클라이언트 요청 오류(잘못된 Content-Type)가 500(서버 오류)로 응답된다.
	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	public ResponseEntity<ErrorResponse> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex) {
		ErrorBody body = ErrorBody.of("UNSUPPORTED_MEDIA_TYPE", "지원하지 않는 Content-Type입니다.");
		return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(ErrorResponse.of(body));
	}

	// 위에서 처리하지 못한 나머지 예외는 500으로 통일하고, 원인은 log에만 남기고 응답에는 노출하지 않는다.
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
		log.error("처리되지 않은 예외가 발생했습니다.", ex);
		ErrorBody body = ErrorBody.of("INTERNAL_ERROR", "서버 오류가 발생했습니다.");
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErrorResponse.of(body));
	}
}
