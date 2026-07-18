"use client";

/*
 * S10 쓰레드 진입 — 대상(targetType/targetId)만 알고 threadId를 모르는 곳에서
 * 쓰레드로 들어오는 공통 경로다. `POST /projects/{projectId}/threads`는
 * get-or-create라 이미 있으면 기존 대표 Thread를, 없으면 새로 만들어 반환한다
 * (API-명세서 §13.7). 여기서 그 Thread의 id를 받아 정식 상세 경로
 * (`../[threadId]`)로 옮긴다 — 그래야 새로고침·공유 링크가 그대로 동작한다.
 *
 * 사용처: 문서 편집기(S08)의 "쓰레드 열기", 이슈 상세 모달(S05)의 "쓰레드 열기".
 */
import { Suspense, useEffect, useState } from "react";
import { useParams, useRouter, useSearchParams } from "next/navigation";
import { invitationErrorMessage } from "@/lib/invitation";
import { threadService } from "@/services/thread.service";
import type { ThreadTargetType } from "@/types/thread.types";

const VALID_TARGET_TYPES: ThreadTargetType[] = ["ISSUE", "DOCUMENT", "MESSAGE"];

function OpenThreadContent() {
  const params = useParams<{ companyId: string; projectId: string }>();
  const router = useRouter();
  const searchParams = useSearchParams();
  const [error, setError] = useState<string | null>(null);

  const targetType = searchParams.get("targetType");
  const targetId = searchParams.get("targetId");

  useEffect(() => {
    let cancelled = false;
    (async () => {
      const isValidTarget =
        targetType !== null &&
        (VALID_TARGET_TYPES as string[]).includes(targetType) &&
        targetId !== null &&
        Number.isFinite(Number(targetId));
      if (!isValidTarget) {
        if (!cancelled) setError("잘못된 쓰레드 진입 경로입니다.");
        return;
      }

      try {
        const { data } = await threadService.create(params.projectId, {
          targetType: targetType as ThreadTargetType,
          targetId: Number(targetId),
        });
        if (!cancelled) {
          router.replace(`/${params.companyId}/${params.projectId}/threads/${data.data.thread.id}`);
        }
      } catch (err) {
        if (!cancelled) setError(invitationErrorMessage(err, "쓰레드를 여는 데 실패했습니다."));
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [params.companyId, params.projectId, router, targetType, targetId]);

  if (error) {
    return (
      <div className="mx-auto max-w-2xl px-6 py-10 text-center">
        <p className="text-sm text-destructive">{error}</p>
      </div>
    );
  }

  return <div className="px-6 py-8 text-sm text-muted">쓰레드를 여는 중...</div>;
}

export default function OpenThreadPage() {
  return (
    <Suspense fallback={<div className="px-6 py-8 text-sm text-muted">쓰레드를 여는 중...</div>}>
      <OpenThreadContent />
    </Suspense>
  );
}
