export type AttachmentTargetType =
  | "DOCUMENT"
  | "BINARY_DOCUMENT"
  | "ISSUE"
  | "MESSAGE"
  | "THREAD_COMMENT"
  | "DIRECT_MESSAGE"
  | "USER_AVATAR"
  | "COMPANY_LOGO";

export type AttachmentStatus = "UPLOADING" | "READY" | "DELETED";

export interface AttachmentResponse {
  id: number;
  targetType: AttachmentTargetType;
  targetId: number;
  filename: string;
  contentType: string;
  contentLength: number;
  status: AttachmentStatus;
  completedAt: string | null;
  createdAt: string;
}

export interface InitiateAttachmentRequest {
  targetType: AttachmentTargetType;
  targetId: number;
  filename: string;
  contentType: string;
  contentLength: number;
  checksumSha256: string;
}

export interface InitiateAttachmentData {
  attachment: AttachmentResponse;
  uploadUrl: string;
}

export interface AttachmentDownloadData {
  downloadUrl: string;
}
