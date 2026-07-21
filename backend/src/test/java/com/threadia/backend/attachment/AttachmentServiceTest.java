package com.threadia.backend.attachment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.Duration;
import java.util.Optional;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.mockito.ArgumentCaptor;

import com.threadia.backend.attachment.domain.Attachment;
import com.threadia.backend.attachment.domain.AttachmentStatus;
import com.threadia.backend.attachment.domain.AttachmentTargetType;
import com.threadia.backend.attachment.dto.AttachmentUploadRequest;
import com.threadia.backend.attachment.exception.AttachmentValidationException;
import com.threadia.backend.attachment.repository.AttachmentRepository;
import com.threadia.backend.auth.domain.User;
import com.threadia.backend.common.auth.AuthContext;
import com.threadia.backend.common.auth.AuthContextService;
import com.threadia.backend.common.exception.ForbiddenException;
import com.threadia.backend.company.domain.Company;
import com.threadia.backend.storage.PrivateObjectStorage;

class AttachmentServiceTest {
	private final AuthContextService authContextService = mock(AuthContextService.class);
	private final AttachmentAccessService accessService = mock(AttachmentAccessService.class);
	private final AttachmentRepository attachmentRepository = mock(AttachmentRepository.class);
	private final PrivateObjectStorage objectStorage = mock(PrivateObjectStorage.class);
	private final AuthContext context = mock(AuthContext.class);
	private final User user = mock(User.class);
	private final Company company = mock(Company.class);
	private AttachmentService service;

	@BeforeEach
	void setUp() {
		service = new AttachmentService(authContextService, accessService, attachmentRepository, objectStorage,
				Clock.fixed(Instant.parse("2026-07-20T00:00:00Z"), ZoneOffset.UTC));
		when(authContextService.fromAccessToken("token")).thenReturn(context);
		when(authContextService.requireActiveUser(context)).thenReturn(user);
		when(company.getId()).thenReturn(7L);
		when(attachmentRepository.save(any(Attachment.class))).thenAnswer(invocation -> {
			Attachment attachment = invocation.getArgument(0);
			ReflectionTestUtils.setField(attachment, "id", 11L);
			return attachment;
		});
	}

	@Test
	void initiateCreatesTenantScopedPrivateObjectAndPresignedUpload() {
		AttachmentUploadRequest request = request();
		when(accessService.requireMutationAccess(AttachmentTargetType.DOCUMENT, 3L, context)).thenReturn(company);
		when(objectStorage.presignUpload(any(), eq("application/pdf"), eq(request.checksumSha256()))).thenReturn(URI.create("https://storage.example/upload"));

		var result = service.initiate("token", request);

		assertThat(result.attachment().status()).isEqualTo(AttachmentStatus.UPLOADING);
		assertThat(result.attachment().id()).isEqualTo(11L);
		assertThat(result.uploadUrl()).hasToString("https://storage.example/upload");
		verify(accessService).requireMutationAccess(AttachmentTargetType.DOCUMENT, 3L, context);
	}

	@Test
	void initiateDoesNotPresignWhenTargetAccessIsDenied() {
		when(accessService.requireMutationAccess(any(), any(), eq(context))).thenThrow(new ForbiddenException("프로젝트 접근 권한이 없습니다."));

		assertThatThrownBy(() -> service.initiate("token", request())).isInstanceOf(ForbiddenException.class);

		verify(objectStorage, never()).presignUpload(any(), any(), any());
		verify(attachmentRepository, never()).save(any());
	}

	@Test
	void avatarInitiationRejectsNonImageMimeBeforeSavingOrPresigning() {
		AttachmentUploadRequest request = uploadRequest(AttachmentTargetType.USER_AVATAR, 1L, "application/pdf");
		when(accessService.requireMutationAccess(AttachmentTargetType.USER_AVATAR, 1L, context)).thenReturn(null);

		assertThatThrownBy(() -> service.initiate("token", request))
				.isInstanceOf(AttachmentValidationException.class)
				.hasMessage("아바타와 회사 로고는 image/* Content-Type만 허용합니다.");

		verify(attachmentRepository, never()).save(any());
		verify(objectStorage, never()).presignUpload(any(), any(), any());
	}

	@Test
	void companyLogoInitiationRejectsNonImageMimeBeforeSavingOrPresigning() {
		AttachmentUploadRequest request = uploadRequest(AttachmentTargetType.COMPANY_LOGO, 7L, "application/pdf");
		when(accessService.requireMutationAccess(AttachmentTargetType.COMPANY_LOGO, 7L, context)).thenReturn(company);

		assertThatThrownBy(() -> service.initiate("token", request)).isInstanceOf(AttachmentValidationException.class);

		verify(attachmentRepository, never()).save(any());
		verify(objectStorage, never()).presignUpload(any(), any(), any());
	}

	@Test
	void avatarInitiationAcceptsImageMime() {
		AttachmentUploadRequest request = uploadRequest(AttachmentTargetType.USER_AVATAR, 1L, "image/png");
		when(accessService.requireMutationAccess(AttachmentTargetType.USER_AVATAR, 1L, context)).thenReturn(null);
		when(objectStorage.presignUpload(any(), eq("image/png"), eq(request.checksumSha256()))).thenReturn(URI.create("https://storage.example/upload"));

		service.initiate("token", request);

		verify(attachmentRepository).save(any(Attachment.class));
		verify(objectStorage).presignUpload(any(), eq("image/png"), eq(request.checksumSha256()));
	}

	@Test
	void documentInitiationPreservesArbitraryValidMimeBehavior() {
		AttachmentUploadRequest request = uploadRequest(AttachmentTargetType.DOCUMENT, 3L, "application/vnd.ms-excel");
		when(accessService.requireMutationAccess(AttachmentTargetType.DOCUMENT, 3L, context)).thenReturn(company);
		when(objectStorage.presignUpload(any(), eq("application/vnd.ms-excel"), eq(request.checksumSha256()))).thenReturn(URI.create("https://storage.example/upload"));

		service.initiate("token", request);

		verify(attachmentRepository).save(any(Attachment.class));
		verify(objectStorage).presignUpload(any(), eq("application/vnd.ms-excel"), eq(request.checksumSha256()));
	}

	@Test
	void completionVerifiesHeadMetadataBeforeMarkingReady() {
		Attachment attachment = attachment();
		when(attachmentRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(attachment));
		when(accessService.requireMutationAccess(AttachmentTargetType.DOCUMENT, 3L, context)).thenReturn(company);
		when(objectStorage.head(attachment.getObjectKey())).thenReturn(new PrivateObjectStorage.ObjectMetadata(12L, "application/pdf", request().checksumSha256()));

		var result = service.complete("token", 11L);

		assertThat(result.status()).isEqualTo(AttachmentStatus.READY);
		verify(accessService).replaceManagedReference(AttachmentTargetType.DOCUMENT, 3L, 11L);
	}

	@Test
	void completionRejectsMetadataMismatchAndKeepsUploadPending() {
		Attachment attachment = attachment();
		when(attachmentRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(attachment));
		when(accessService.requireMutationAccess(AttachmentTargetType.DOCUMENT, 3L, context)).thenReturn(company);
		when(objectStorage.head(attachment.getObjectKey())).thenReturn(new PrivateObjectStorage.ObjectMetadata(13L, "application/pdf", request().checksumSha256()));

		assertThatThrownBy(() -> service.complete("token", 11L)).isInstanceOf(AttachmentValidationException.class);
		assertThat(attachment.getStatus()).isEqualTo(AttachmentStatus.UPLOADING);
	}

	@Test
	void listUsesReadAccessRatherThanMutationAccess() {
		when(accessService.requireReadAccess(AttachmentTargetType.DOCUMENT, 3L, context)).thenReturn(company);
		when(attachmentRepository.findByTargetTypeAndTargetIdAndStatusOrderByCreatedAtAsc(AttachmentTargetType.DOCUMENT, 3L, AttachmentStatus.READY)).thenReturn(List.of());

		assertThat(service.list("token", AttachmentTargetType.DOCUMENT, 3L)).isEmpty();

		verify(accessService).requireReadAccess(AttachmentTargetType.DOCUMENT, 3L, context);
		verify(accessService, never()).requireMutationAccess(AttachmentTargetType.DOCUMENT, 3L, context);
	}

	@Test
	void orphanCleanupDeletesPrivateObjectThenMetadata() {
		Attachment attachment = attachment();
		attachment.delete(java.time.LocalDateTime.now());
		when(attachmentRepository.findByStatusAndDeletedAtIsNotNull(AttachmentStatus.DELETED)).thenReturn(List.of(attachment));

		assertThat(service.purgeLogicallyDeleted()).isEqualTo(1);

		verify(objectStorage).delete(attachment.getObjectKey());
		verify(attachmentRepository).delete(attachment);
	}

	@Test
	void staleUploadingAttachmentBecomesEligibleBeforeItsObjectIsPurgedIdempotently() {
		Attachment attachment = attachment();
		when(attachmentRepository.findByStatusAndCreatedAtBefore(AttachmentStatus.UPLOADING, java.time.LocalDateTime.of(2026, 7, 19, 23, 0))).thenReturn(List.of(attachment), List.of());

		assertThat(service.expireStaleUploads(Duration.ofHours(1))).isEqualTo(1);
		assertThat(attachment.getStatus()).isEqualTo(AttachmentStatus.DELETED);
		verify(objectStorage, never()).delete(any());
		assertThat(service.expireStaleUploads(Duration.ofHours(1))).isZero();

		when(attachmentRepository.findByStatusAndDeletedAtIsNotNull(AttachmentStatus.DELETED)).thenReturn(List.of(attachment), List.of());
		assertThat(service.purgeLogicallyDeleted()).isEqualTo(1);
		assertThat(service.purgeLogicallyDeleted()).isZero();
		verify(objectStorage).delete(attachment.getObjectKey());
	}

	@Test
	void completingManagedAvatarReplacementLogicallyDeletesPreviousReadyAttachment() {
		Attachment replacement = new Attachment(null, user, AttachmentTargetType.USER_AVATAR, 1L, "users/attachments/user_avatar/replacement", "new.png", "image/png", 12L, request().checksumSha256(), java.time.LocalDateTime.now());
		Attachment previous = new Attachment(null, user, AttachmentTargetType.USER_AVATAR, 1L, "users/attachments/user_avatar/previous", "old.png", "image/png", 12L, request().checksumSha256(), java.time.LocalDateTime.now());
		ReflectionTestUtils.setField(replacement, "id", 11L);
		ReflectionTestUtils.setField(previous, "id", 10L);
		previous.complete(java.time.LocalDateTime.now());
		when(attachmentRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(replacement));
		when(attachmentRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(previous));
		when(accessService.requireMutationAccess(AttachmentTargetType.USER_AVATAR, 1L, context)).thenReturn(null);
		when(accessService.replaceManagedReference(AttachmentTargetType.USER_AVATAR, 1L, 11L)).thenReturn(10L);
		when(objectStorage.head(replacement.getObjectKey())).thenReturn(new PrivateObjectStorage.ObjectMetadata(12L, "image/png", request().checksumSha256()));

		service.complete("token", 11L);

		assertThat(previous.getStatus()).isEqualTo(AttachmentStatus.DELETED);
	}

	@Test
	void deletingCurrentManagedAvatarClearsReferenceInSameTransaction() {
		Attachment avatar = new Attachment(null, user, AttachmentTargetType.USER_AVATAR, 1L, "users/attachments/user_avatar/current", "avatar.png", "image/png", 12L, request().checksumSha256(), java.time.LocalDateTime.now());
		ReflectionTestUtils.setField(avatar, "id", 11L);
		when(attachmentRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(avatar));
		when(accessService.requireMutationAccess(AttachmentTargetType.USER_AVATAR, 1L, context)).thenReturn(null);

		service.delete("token", 11L);

		verify(accessService).clearManagedReferenceIfCurrent(AttachmentTargetType.USER_AVATAR, 1L, 11L);
		assertThat(avatar.getStatus()).isEqualTo(AttachmentStatus.DELETED);
	}

	@Test
	void downloadRejectsLegacyControlCharacterFilenameBeforePresigning() {
		Attachment attachment = attachment();
		attachment.complete(java.time.LocalDateTime.now());
		ReflectionTestUtils.setField(attachment, "originalFilename", "bad\r\nname.txt");
		when(attachmentRepository.findById(11L)).thenReturn(Optional.of(attachment));
		when(accessService.requireReadAccess(AttachmentTargetType.DOCUMENT, 3L, context)).thenReturn(company);

		assertThatThrownBy(() -> service.download("token", 11L)).isInstanceOf(AttachmentValidationException.class);
		verify(objectStorage, never()).presignDownload(any(), any());
	}

	@Test
	void downloadUsesUtf8ContentDispositionForValidLegacyFilename() {
		Attachment attachment = attachment();
		attachment.complete(java.time.LocalDateTime.now());
		ReflectionTestUtils.setField(attachment, "originalFilename", "보고서.pdf");
		when(attachmentRepository.findById(11L)).thenReturn(Optional.of(attachment));
		when(accessService.requireReadAccess(AttachmentTargetType.DOCUMENT, 3L, context)).thenReturn(company);
		when(objectStorage.presignDownload(any(), any())).thenReturn(URI.create("https://storage.example/download"));

		service.download("token", 11L);

		ArgumentCaptor<String> disposition = ArgumentCaptor.forClass(String.class);
		verify(objectStorage).presignDownload(eq(attachment.getObjectKey()), disposition.capture());
		assertThat(disposition.getValue()).contains("filename*=").doesNotContain("\r", "\n");
	}

	private Attachment attachment() {
		Attachment attachment = new Attachment(company, user, AttachmentTargetType.DOCUMENT, 3L, "companies/7/attachments/document/object", "brief.pdf", "application/pdf", 12L, request().checksumSha256(), java.time.LocalDateTime.now());
		ReflectionTestUtils.setField(attachment, "id", 11L);
		return attachment;
	}

	private AttachmentUploadRequest request() {
		return uploadRequest(AttachmentTargetType.DOCUMENT, 3L, "application/pdf");
	}

	private AttachmentUploadRequest uploadRequest(AttachmentTargetType targetType, Long targetId, String contentType) {
		return new AttachmentUploadRequest(targetType, targetId, "brief.pdf", contentType, 12L, "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWYwMTIzNDU2Nzg5YWJjZGVm");
	}
}
