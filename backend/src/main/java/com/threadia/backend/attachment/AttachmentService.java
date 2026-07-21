package com.threadia.backend.attachment;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.http.InvalidMediaTypeException;

import com.threadia.backend.attachment.domain.Attachment;
import com.threadia.backend.attachment.domain.AttachmentStatus;
import com.threadia.backend.attachment.domain.AttachmentTargetType;
import com.threadia.backend.attachment.dto.AttachmentDownloadData;
import com.threadia.backend.attachment.dto.AttachmentResponse;
import com.threadia.backend.attachment.dto.AttachmentUploadData;
import com.threadia.backend.attachment.dto.AttachmentUploadRequest;
import com.threadia.backend.attachment.exception.AttachmentNotFoundException;
import com.threadia.backend.attachment.exception.AttachmentValidationException;
import com.threadia.backend.attachment.repository.AttachmentRepository;
import com.threadia.backend.auth.domain.User;
import com.threadia.backend.common.auth.AuthContext;
import com.threadia.backend.common.auth.AuthContextService;
import com.threadia.backend.company.domain.Company;
import com.threadia.backend.storage.PrivateObjectStorage;

@Service
public class AttachmentService {
	private final AuthContextService authContextService;
	private final AttachmentAccessService accessService;
	private final AttachmentRepository attachmentRepository;
	private final PrivateObjectStorage objectStorage;
	private final Clock clock;

	@Autowired
	public AttachmentService(AuthContextService authContextService, AttachmentAccessService accessService, AttachmentRepository attachmentRepository, PrivateObjectStorage objectStorage) {
		this(authContextService, accessService, attachmentRepository, objectStorage, Clock.systemUTC());
	}

	AttachmentService(AuthContextService authContextService, AttachmentAccessService accessService, AttachmentRepository attachmentRepository, PrivateObjectStorage objectStorage, Clock clock) {
		this.authContextService = authContextService; this.accessService = accessService; this.attachmentRepository = attachmentRepository;
		this.objectStorage = objectStorage; this.clock = clock;
	}

	@Transactional
	public AttachmentUploadData initiate(String token, AttachmentUploadRequest request) {
		AuthContext context = activeContext(token);
		User user = authContextService.requireActiveUser(context);
		Company company = accessService.requireMutationAccess(request.targetType(), request.targetId(), context);
		requireImageManagedAsset(request);
		String key = objectKey(company, request.targetType());
		Attachment attachment = attachmentRepository.save(new Attachment(company, user, request.targetType(), request.targetId(), key,
				request.filename(), request.contentType(), request.contentLength(), request.checksumSha256(), now()));
		URI uploadUrl = objectStorage.presignUpload(key, request.contentType(), request.checksumSha256());
		return new AttachmentUploadData(AttachmentResponse.from(attachment), uploadUrl);
	}

	@Transactional
	public AttachmentResponse complete(String token, Long attachmentId) {
		AuthContext context = activeContext(token);
		Attachment attachment = attachmentForUpdate(attachmentId);
		accessService.requireMutationAccess(attachment.getTargetType(), attachment.getTargetId(), context);
		if (attachment.getStatus() == AttachmentStatus.READY) return AttachmentResponse.from(attachment);
		if (attachment.getStatus() == AttachmentStatus.DELETED) throw new AttachmentValidationException("삭제된 첨부 파일은 완료할 수 없습니다.");
		PrivateObjectStorage.ObjectMetadata metadata = objectStorage.head(attachment.getObjectKey());
		if (metadata.size() != attachment.getContentLength() || !attachment.getContentType().equalsIgnoreCase(metadata.contentType())
				|| !attachment.getChecksumSha256().equals(metadata.checksumSha256())) {
			throw new AttachmentValidationException("업로드된 파일 metadata가 요청값과 일치하지 않습니다.");
		}
		attachment.complete(now());
		Long replacedAttachmentId = accessService.replaceManagedReference(attachment.getTargetType(), attachment.getTargetId(), attachment.getId());
		markReplacedManagedAttachmentDeleted(replacedAttachmentId, attachment.getId());
		return AttachmentResponse.from(attachment);
	}

	@Transactional(readOnly = true)
	public List<AttachmentResponse> list(String token, AttachmentTargetType targetType, Long targetId) {
		AuthContext context = activeContext(token);
		accessService.requireReadAccess(targetType, targetId, context);
		return attachmentRepository.findByTargetTypeAndTargetIdAndStatusOrderByCreatedAtAsc(targetType, targetId, AttachmentStatus.READY)
				.stream().map(AttachmentResponse::from).toList();
	}

	@Transactional(readOnly = true)
	public AttachmentDownloadData download(String token, Long attachmentId) {
		AuthContext context = activeContext(token);
		Attachment attachment = attachment(attachmentId);
		accessService.requireReadAccess(attachment.getTargetType(), attachment.getTargetId(), context);
		if (attachment.getStatus() != AttachmentStatus.READY) throw new AttachmentValidationException("완료된 첨부 파일만 다운로드할 수 있습니다.");
		return new AttachmentDownloadData(objectStorage.presignDownload(attachment.getObjectKey(), contentDisposition(attachment.getOriginalFilename())));
	}

	@Transactional
	public void delete(String token, Long attachmentId) {
		AuthContext context = activeContext(token);
		Attachment attachment = attachmentForUpdate(attachmentId);
		accessService.requireMutationAccess(attachment.getTargetType(), attachment.getTargetId(), context);
		if (attachment.getStatus() != AttachmentStatus.DELETED) {
			attachment.delete(now());
			accessService.clearManagedReferenceIfCurrent(attachment.getTargetType(), attachment.getTargetId(), attachment.getId());
		}
	}

	@Transactional
	public int expireStaleUploads(Duration maximumAge) {
		LocalDateTime cutoff = now().minus(maximumAge);
		List<Attachment> staleUploads = attachmentRepository.findByStatusAndCreatedAtBefore(AttachmentStatus.UPLOADING, cutoff);
		staleUploads.forEach(attachment -> attachment.delete(now()));
		return staleUploads.size();
	}

	@Transactional
	public int purgeLogicallyDeleted() {
		int purged = 0;
		for (Attachment attachment : attachmentRepository.findByStatusAndDeletedAtIsNotNull(AttachmentStatus.DELETED)) {
			objectStorage.delete(attachment.getObjectKey());
			attachmentRepository.delete(attachment);
			purged++;
		}
		return purged;
	}

	private AuthContext activeContext(String token) { AuthContext context = authContextService.fromAccessToken(token); authContextService.requireActiveUser(context); return context; }
	private Attachment attachment(Long id) { return attachmentRepository.findById(id).orElseThrow(() -> new AttachmentNotFoundException(id)); }
	private Attachment attachmentForUpdate(Long id) { return attachmentRepository.findByIdForUpdate(id).orElseThrow(() -> new AttachmentNotFoundException(id)); }
	private void markReplacedManagedAttachmentDeleted(Long replacedAttachmentId, Long replacementAttachmentId) {
		if (replacedAttachmentId == null || replacedAttachmentId.equals(replacementAttachmentId)) return;
		attachmentRepository.findByIdForUpdate(replacedAttachmentId).ifPresent(attachment -> attachment.delete(now()));
	}
	private static void requireImageManagedAsset(AttachmentUploadRequest request) {
		if ((request.targetType() == AttachmentTargetType.USER_AVATAR || request.targetType() == AttachmentTargetType.COMPANY_LOGO)
				&& !isImageContentType(request.contentType())) {
			throw new AttachmentValidationException("아바타와 회사 로고는 image/* Content-Type만 허용합니다.");
		}
	}

	private static boolean isImageContentType(String contentType) {
		try {
			return "image".equals(MediaType.parseMediaType(contentType).getType());
		} catch (InvalidMediaTypeException ex) {
			return false;
		}
	}
	private String objectKey(Company company, AttachmentTargetType type) {
		String tenant = company == null ? "users" : "companies/" + company.getId();
		return tenant + "/attachments/" + type.name().toLowerCase() + "/" + UUID.randomUUID();
	}
	private static String contentDisposition(String filename) {
		if (filename.codePoints().anyMatch(Character::isISOControl)
				|| filename.indexOf('/') >= 0 || filename.indexOf('\\') >= 0 || filename.indexOf(':') >= 0) {
			throw new AttachmentValidationException("파일명에 경로 또는 제어 문자를 포함할 수 없습니다.");
		}
		return ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString();
	}
	private LocalDateTime now() { return LocalDateTime.now(clock).atOffset(ZoneOffset.UTC).toLocalDateTime(); }
}
