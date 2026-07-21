"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { invitationErrorMessage } from "@/lib/invitation";
import { attachmentService } from "@/services/attachment.service";
import type { AttachmentResponse, AttachmentTargetType } from "@/types/attachment.types";

type AttachmentControlsProps = {
  targetType: AttachmentTargetType;
  targetId: number | null | undefined;
  canMutate?: boolean;
  compact?: boolean;
  label?: string;
  onChanged?: (attachments: AttachmentResponse[]) => void;
};

const ATTACHMENT_REFRESH_EVENT = "threadia:attachment-refresh";

type AttachmentRefreshDetail = { targetType: AttachmentTargetType; targetId: number };

function publishAttachmentRefresh(targetType: AttachmentTargetType, targetId: number) {
  window.dispatchEvent(
    new CustomEvent<AttachmentRefreshDetail>(ATTACHMENT_REFRESH_EVENT, { detail: { targetType, targetId } })
  );
}

function useAttachmentRefresh(targetType: AttachmentTargetType, targetId: number | null | undefined) {
  const [revision, setRevision] = useState(0);

  useEffect(() => {
    const onRefresh = (event: Event) => {
      const detail = (event as CustomEvent<AttachmentRefreshDetail>).detail;
      if (detail.targetType === targetType && detail.targetId === targetId) {
        setRevision((current) => current + 1);
      }
    };
    window.addEventListener(ATTACHMENT_REFRESH_EVENT, onRefresh);
    return () => window.removeEventListener(ATTACHMENT_REFRESH_EVENT, onRefresh);
  }, [targetId, targetType]);

  return revision;
}

export function AttachmentImage({
  targetType,
  targetId,
  fallbackUrl,
  alt,
  className,
}: {
  targetType: AttachmentTargetType;
  targetId: number | null | undefined;
  fallbackUrl?: string | null;
  alt: string;
  className: string;
}) {
  const [imageUrl, setImageUrl] = useState<string | null>(null);
  const [failedUrl, setFailedUrl] = useState<string | null>(null);
  const refreshRevision = useAttachmentRefresh(targetType, targetId);
  const requestSequence = useRef(0);

  useEffect(() => {
    let active = true;
    const requestId = ++requestSequence.current;
    if (!targetId || !Number.isInteger(targetId)) {
      return;
    }
    (async () => {
      try {
        const { data } = await attachmentService.list(targetType, targetId);
        const image = data.data.find((attachment) => attachment.contentType.startsWith("image/"));
        if (!image) {
          if (active && requestId === requestSequence.current) setImageUrl(null);
          return;
        }
        const download = await attachmentService.download(image.id);
        if (active && requestId === requestSequence.current) setImageUrl(download.data.data.downloadUrl);
      } catch {
        if (active && requestId === requestSequence.current) setImageUrl(null);
      }
    })();
    return () => {
      active = false;
    };
  }, [refreshRevision, targetId, targetType]);

  const src = imageUrl ?? fallbackUrl ?? null;
  if (!src || src === failedUrl) return null;
  return (
    // eslint-disable-next-line @next/next/no-img-element -- presigned URLs use dynamic storage hosts.
    <img src={src} alt={alt} className={className} onError={() => setFailedUrl(src)} />
  );
}

export function AttachmentControls({
  targetType,
  targetId,
  canMutate = false,
  compact = false,
  label = "첨부 파일",
  onChanged,
}: AttachmentControlsProps) {
  const [attachments, setAttachments] = useState<AttachmentResponse[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);
  const requestSequence = useRef(0);
  const isPersisted = Number.isInteger(targetId) && Number(targetId) > 0;
  const refreshRevision = useAttachmentRefresh(targetType, targetId);
  const imageOnly = targetType === "USER_AVATAR" || targetType === "COMPANY_LOGO";
  const displayedAttachments = attachments;

  const load = useCallback(async () => {
    if (!isPersisted || !targetId) return;
    const requestId = ++requestSequence.current;
    try {
      const { data } = await attachmentService.list(targetType, targetId);
      if (requestId !== requestSequence.current) return;
      setAttachments(data.data);
      onChanged?.(data.data);
    } catch (loadError) {
      if (requestId !== requestSequence.current) return;
      setError(invitationErrorMessage(loadError, "첨부 파일을 불러오지 못했습니다."));
    }
  }, [isPersisted, onChanged, targetId, targetType]);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      void load();
    }, 0);
    return () => window.clearTimeout(timer);
  }, [load, refreshRevision]);

  const upload = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    event.target.value = "";
    if (!file || !targetId || busy) return;
    setBusy(true);
    setError(null);
    try {
      await attachmentService.upload(targetType, targetId, file);
      publishAttachmentRefresh(targetType, targetId);
      await load();
    } catch (uploadError) {
      setError(invitationErrorMessage(uploadError, "파일 업로드에 실패했습니다."));
    } finally {
      setBusy(false);
    }
  };

  const download = async (attachmentId: number) => {
    setError(null);
    try {
      const { data } = await attachmentService.download(attachmentId);
      window.open(data.data.downloadUrl, "_blank", "noopener,noreferrer");
    } catch (downloadError) {
      setError(invitationErrorMessage(downloadError, "파일 다운로드에 실패했습니다."));
    }
  };

  const remove = async (attachmentId: number) => {
    if (!targetId) return;
    const persistedTargetId = targetId;
    setBusy(true);
    setError(null);
    try {
      await attachmentService.remove(attachmentId);
      publishAttachmentRefresh(targetType, persistedTargetId);
      await load();
    } catch (deleteError) {
      setError(invitationErrorMessage(deleteError, "첨부 파일 삭제에 실패했습니다."));
    } finally {
      setBusy(false);
    }
  };

  if (!isPersisted) return null;
  return (
    <section className={compact ? "mt-2" : "mt-5 rounded-xl border border-border bg-surface p-4"}>
      <div className="flex items-center justify-between gap-2">
        <h2 className="text-xs font-semibold text-foreground">{label}</h2>
        {canMutate && (
          <>
            <input ref={inputRef} type="file" accept={imageOnly ? "image/*" : undefined} className="sr-only" onChange={(event) => void upload(event)} />
            <button
              type="button"
              onClick={() => inputRef.current?.click()}
              disabled={busy}
              className="text-xs font-medium text-primary hover:opacity-70 disabled:opacity-50"
            >
              {busy ? "처리 중..." : "파일 추가"}
            </button>
          </>
        )}
      </div>
      {error && <p className="mt-2 text-xs text-destructive">{error}</p>}
      {displayedAttachments.length > 0 && (
        <ul className="mt-2 divide-y divide-border">
          {displayedAttachments.map((attachment) => (
            <li key={attachment.id} className="flex items-center gap-2 py-1.5 text-xs">
              <button type="button" onClick={() => void download(attachment.id)} className="min-w-0 flex-1 truncate text-left text-primary hover:opacity-70">
                {attachment.filename}
              </button>
              <span className="shrink-0 text-muted">{Math.ceil(attachment.contentLength / 1024)}KB</span>
              {canMutate && <button type="button" onClick={() => void remove(attachment.id)} disabled={busy} className="shrink-0 text-destructive hover:opacity-70 disabled:opacity-50">삭제</button>}
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
