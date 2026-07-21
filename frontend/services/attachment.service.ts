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
