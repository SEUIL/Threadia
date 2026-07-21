"use client";

/*
 * AttachmentControls / AttachmentImage — components/attachments/AttachmentControls.tsx
 *
 * WBS 4.11(PR #215)에서 만든 S3 private attachment의 공용 UI. 여러 대상
 * (DOCUMENT/ISSUE/MESSAGE/THREAD_COMMENT/DIRECT_MESSAGE/USER_AVATAR/
 * COMPANY_LOGO)이 같은 컴포넌트로 첨부 목록 조회·업로드·다운로드·삭제를
 * 한다. targetType + targetId 조합으로 어느 대상의 첨부인지 구분한다.
 *
 * 새로고침 이벤트(pub/sub): 같은 (targetType, targetId)를 보는 인스턴스가
 * 화면에 둘 이상일 수 있다 — 예를 들어 USER_AVATAR는 프로필 수정 모달의
 * AttachmentControls(업로드/삭제)와 레일 아바타의 AttachmentImage(표시)가
 * 동시에 뜬다. 한쪽에서 업로드/삭제하면 다른 쪽도 즉시 다시 그려야 하므로,
 * 공유 부모로 상태를 끌어올리는 대신 window CustomEvent로 "이 대상 갱신됨"을
 * 브로드캐스트하고 각 인스턴스가 자기 대상이면 refetch한다(컴포넌트 트리
 * 위치가 서로 멀어서 Context보다 이벤트가 단순하다).
 */
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

// 바이트를 사람이 읽는 단위로. 예전엔 항상 KB로만 표기해서(Math.ceil(bytes/1024))
// 5MB 파일이 "5120KB"처럼 보였다 — 1MB 이상은 MB, 1KB 미만은 B로 자동 전환한다.
function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes}B`;
  if (bytes < 1024 * 1024) return `${Math.round(bytes / 1024)}KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)}MB`;
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
  // 요청이 여러 개 겹칠 때(대상 전환/빠른 refresh) 늦게 도착한 옛 응답이
  // 최신 이미지를 덮지 않게 하는 stale 가드 — 응답 시점의 requestId가 지금도
  // 최신인지 확인한다.
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
  // 실수 클릭으로 첨부가 바로 지워지지 않도록 인라인 2단계 확인을 둔다 — 이
  // 코드베이스는 window.confirm 대신 members/page.tsx(구성원 제거)와 같은
  // "삭제 클릭 → 확인/취소" 인라인 패턴을 쓴다. 확인 대기 중인 첨부의 id를
  // 저장한다(한 번에 하나만).
  const [confirmDeleteId, setConfirmDeleteId] = useState<number | null>(null);
  const inputRef = useRef<HTMLInputElement>(null);
  const requestSequence = useRef(0);
  const isPersisted = Number.isInteger(targetId) && Number(targetId) > 0;
  const refreshRevision = useAttachmentRefresh(targetType, targetId);
  const imageOnly = targetType === "USER_AVATAR" || targetType === "COMPANY_LOGO";

  // onChanged는 호출부가 인라인 함수로 넘길 수 있어(매 렌더 새 identity) load의
  // 의존성에 그대로 두면 effect가 매 렌더 재실행된다. ref에 최신값을 담아두고
  // load는 이 ref만 읽어서, load 자체는 targetType/targetId가 바뀔 때만 새로
  // 만들어지게 한다.
  const onChangedRef = useRef(onChanged);
  useEffect(() => {
    onChangedRef.current = onChanged;
  });

  const load = useCallback(async () => {
    if (!isPersisted || !targetId) return;
    const requestId = ++requestSequence.current;
    try {
      const { data } = await attachmentService.list(targetType, targetId);
      if (requestId !== requestSequence.current) return;
      setAttachments(data.data);
      onChangedRef.current?.(data.data);
    } catch (loadError) {
      if (requestId !== requestSequence.current) return;
      setError(invitationErrorMessage(loadError, "첨부 파일을 불러오지 못했습니다."));
    }
  }, [isPersisted, targetId, targetType]);

  // load()를 effect에서 바로 호출하면 react-hooks/set-state-in-effect에 걸린다
  // (load가 setState를 하므로) — AuthContext/members 등과 동일하게 async IIFE로
  // 감싸 microtask로 미룬다. 예전엔 같은 목적으로 setTimeout(0)을 썼는데, 이
  // 프로젝트에서 이미 쓰는 IIFE 패턴으로 통일했다.
  useEffect(() => {
    void (async () => {
      await load();
    })();
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
      setConfirmDeleteId(null);
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
            <input
              ref={inputRef}
              type="file"
              accept={imageOnly ? "image/*" : undefined}
              className="sr-only"
              onChange={(event) => void upload(event)}
            />
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
      {attachments.length > 0 && (
        <ul className="mt-2 divide-y divide-border">
          {attachments.map((attachment) => (
            <li key={attachment.id} className="flex items-center gap-2 py-1.5 text-xs">
              <button
                type="button"
                onClick={() => void download(attachment.id)}
                className="min-w-0 flex-1 truncate text-left text-primary hover:opacity-70"
              >
                {attachment.filename}
              </button>
              <span className="shrink-0 text-muted">{formatFileSize(attachment.contentLength)}</span>
              {canMutate &&
                (confirmDeleteId === attachment.id ? (
                  <span className="flex shrink-0 items-center gap-1">
                    <span className="text-muted">삭제?</span>
                    <button
                      type="button"
                      onClick={() => void remove(attachment.id)}
                      disabled={busy}
                      className="font-semibold text-destructive hover:opacity-70 disabled:opacity-50"
                    >
                      {busy ? "삭제 중..." : "확인"}
                    </button>
                    <button
                      type="button"
                      onClick={() => setConfirmDeleteId(null)}
                      disabled={busy}
                      className="text-muted hover:opacity-70 disabled:opacity-50"
                    >
                      취소
                    </button>
                  </span>
                ) : (
                  <button
                    type="button"
                    onClick={() => setConfirmDeleteId(attachment.id)}
                    className="shrink-0 text-destructive hover:opacity-70"
                  >
                    삭제
                  </button>
                ))}
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
