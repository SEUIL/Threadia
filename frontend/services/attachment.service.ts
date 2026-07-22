/*
 * 첨부 파일 API 클라이언트 — WBS 4.11(PR #215) private S3 attachment.
 *
 * 업로드는 서버를 거치지 않는 3단계 presigned 흐름이다(파일 바이트가 우리
 * 백엔드를 통과하지 않고 브라우저 → S3로 직접 간다):
 *   1) initiate: 서버에 메타데이터(파일명/타입/크기/체크섬)를 등록하고
 *      presigned PUT URL을 받는다. 이 시점 attachment는 UPLOADING 상태.
 *   2) presigned PUT: 브라우저가 S3(로컬은 MinIO)로 파일 바이트를 직접 올린다.
 *      apiClient(axios)가 아니라 raw fetch를 쓴다 — 이 요청은 우리 백엔드가
 *      아니라 스토리지로 가므로 Authorization 헤더/baseURL을 붙이면 안 된다.
 *   3) complete: 업로드가 끝났음을 서버에 알려 READY 상태로 전이시킨다.
 * 2)나 3)이 실패하면 1)에서 만든 UPLOADING 레코드가 고아로 남으므로,
 * catch에서 remove로 정리한다(정리 자체가 실패해도 원래 에러를 던진다 —
 * 고아 레코드는 서버의 orphan cleanup job이 결국 회수한다).
 */
import apiClient from "@/lib/axios";
import type { ApiResponse } from "@/types/auth.types";
import type {
  AttachmentDownloadData,
  AttachmentResponse,
  AttachmentTargetType,
  InitiateAttachmentData,
  InitiateAttachmentRequest,
} from "@/types/attachment.types";

const MAX_ATTACHMENT_BYTES = 104857600;
const INVALID_FILENAME = /[/:\\\p{Cc}]/u;

export function validateAttachmentFile(file: File): void {
  if (file.size < 1 || file.size > MAX_ATTACHMENT_BYTES) {
    throw new Error("파일 크기는 1바이트 이상 100MB 이하여야 합니다.");
  }
  if (!file.type) {
    throw new Error("파일의 MIME type을 확인할 수 없습니다.");
  }
  if (!file.name || INVALID_FILENAME.test(file.name)) {
    throw new Error("파일명에는 /, :, \\, 제어 문자를 사용할 수 없습니다.");
  }
}

async function checksumSha256(file: File): Promise<string> {
  const digest = await crypto.subtle.digest("SHA-256", await file.arrayBuffer());
  const bytes = new Uint8Array(digest);
  let binary = "";
  for (const byte of bytes) binary += String.fromCharCode(byte);
  return btoa(binary);
}

export const attachmentService = {
  list: (targetType: AttachmentTargetType, targetId: number) =>
    apiClient.get<ApiResponse<AttachmentResponse[]>>("/api/v1/attachments", {
      params: { targetType, targetId },
    }),

  initiate: (body: InitiateAttachmentRequest) =>
    apiClient.post<ApiResponse<InitiateAttachmentData>>("/api/v1/attachments/uploads", body),

  complete: (attachmentId: number) =>
    apiClient.post<ApiResponse<AttachmentResponse>>(`/api/v1/attachments/${attachmentId}/complete`),

  download: (attachmentId: number) =>
    apiClient.get<ApiResponse<AttachmentDownloadData>>(`/api/v1/attachments/${attachmentId}/download`),

  remove: (attachmentId: number) => apiClient.delete(`/api/v1/attachments/${attachmentId}`),

  upload: async (targetType: AttachmentTargetType, targetId: number, file: File) => {
    validateAttachmentFile(file);
    const checksum = await checksumSha256(file);
    const { data } = await attachmentService.initiate({
      targetType,
      targetId,
      filename: file.name,
      contentType: file.type,
      contentLength: file.size,
      checksumSha256: checksum,
    });
    const initiated = data.data.attachment;

    try {
      const response = await fetch(data.data.uploadUrl, {
        method: "PUT",
        headers: {
          "Content-Type": file.type,
          "x-amz-checksum-sha256": checksum,
        },
        body: file,
      });
      if (!response.ok) throw new Error(`파일 업로드에 실패했습니다. (${response.status})`);
      const completed = await attachmentService.complete(initiated.id);
      return completed.data.data;
    } catch (error) {
      try {
        await attachmentService.remove(initiated.id);
      } catch (cleanupError) {
        console.warn("Initiated attachment cleanup failed", cleanupError);
      }
      throw error;
    }
  },
};
