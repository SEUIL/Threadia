"use client";

/*
 * S15 시스템 설정 (WBS 4.10.5) — 8개 고정 key catalog.
 *
 * key별 value shape가 전부 다르고(§10-B.1) 8종 전용 폼은 이번 1차 구현
 * 범위를 넘어서, JSON 원문 편집 방식을 택했다. 조회는 PLATFORM_ADMIN도
 * 가능하지만 PUT은 SUPER_ADMIN 전용(backend requireSuperAdminFresh)이라
 * PLATFORM_ADMIN에게는 편집 버튼을 아예 숨긴다. expectedVersion은 화면에
 *떠 있는 값을 그대로 보내 optimistic concurrency를 건다 — 충돌 시
 * 409 SYSTEM_SETTING_VERSION_CONFLICT 메시지를 보여주고 재조회를 유도한다.
 */
import { useCallback, useEffect, useState } from "react";
import { systemSettingsService } from "@/services/system-settings.service";
import { platformErrorMessage, isValidPlatformReason } from "@/lib/platform";
import { usePlatformAdmin } from "@/contexts/PlatformAdminContext";
import { SYSTEM_SETTING_KEY_LABEL } from "@/types/system-settings.types";
import type { SystemSettingResponse } from "@/types/system-settings.types";

type LoadState = "loading" | "ready" | "error";

export default function PlatformSystemSettingsPage() {
  const { isSuperAdmin } = usePlatformAdmin();
  const [settings, setSettings] = useState<SystemSettingResponse[]>([]);
  const [state, setState] = useState<LoadState>("loading");
  const [error, setError] = useState<string | null>(null);
  const [editingKey, setEditingKey] = useState<string | null>(null);
  const [editValue, setEditValue] = useState("");
  const [editReason, setEditReason] = useState("");
  const [editBusy, setEditBusy] = useState(false);
  const [editError, setEditError] = useState<string | null>(null);

  const fetchSettings = useCallback(async () => {
    setState("loading");
    setError(null);
    try {
      const { data } = await systemSettingsService.list();
      setSettings(data.data.settings);
      setState("ready");
    } catch (err) {
      setError(platformErrorMessage(err, "시스템 설정을 불러오지 못했습니다."));
      setState("error");
    }
  }, []);

  useEffect(() => {
    (async () => {
      await fetchSettings();
    })();
  }, [fetchSettings]);

  const startEdit = (setting: SystemSettingResponse) => {
    setEditingKey(setting.key);
    setEditValue(JSON.stringify(setting.value, null, 2));
    setEditReason("");
    setEditError(null);
  };

  const cancelEdit = () => {
    setEditingKey(null);
    setEditError(null);
  };

  const saveEdit = async (setting: SystemSettingResponse) => {
    setEditError(null);
    let parsedValue: unknown;
    try {
      parsedValue = JSON.parse(editValue);
    } catch {
      setEditError("value가 올바른 JSON 형식이 아닙니다.");
      return;
    }
    if (!isValidPlatformReason(editReason)) {
      setEditError("사유는 3~500자로 입력해 주세요.");
      return;
    }
    setEditBusy(true);
    try {
      const { data } = await systemSettingsService.replace(setting.key, {
        value: parsedValue,
        expectedVersion: setting.version,
        reason: editReason.trim(),
      });
      setSettings((prev) => prev.map((s) => (s.key === setting.key ? data.data : s)));
      setEditingKey(null);
    } catch (err) {
      setEditError(platformErrorMessage(err, "설정 변경에 실패했습니다."));
    } finally {
      setEditBusy(false);
    }
  };

  if (state === "loading") return <p className="text-sm text-muted">불러오는 중...</p>;
  if (state === "error") return <p className="text-sm text-destructive">{error}</p>;

  return (
    <div>
      <h2 className="text-lg font-semibold text-foreground">시스템 설정</h2>
      {!isSuperAdmin && (
        <p className="mt-1 text-xs text-muted">조회만 가능합니다. 값 변경은 SUPER_ADMIN만 할 수 있습니다.</p>
      )}

      <ul className="mt-4 space-y-3">
        {settings.map((setting) => (
          <li key={setting.key} className="rounded-xl border border-border bg-surface p-5">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-semibold text-foreground">{SYSTEM_SETTING_KEY_LABEL[setting.key]}</p>
                <p className="text-xs text-muted">
                  {setting.key} · v{setting.version} · {new Date(setting.updatedAt).toLocaleString("ko-KR")}
                </p>
              </div>
              {isSuperAdmin && editingKey !== setting.key && (
                <button
                  onClick={() => startEdit(setting)}
                  className="rounded-md px-3 py-1.5 text-xs font-medium text-primary transition-colors duration-fast hover:bg-primary-subtle"
                >
                  수정
                </button>
              )}
            </div>

            {editingKey === setting.key ? (
              <div className="mt-3 space-y-2">
                <textarea
                  aria-label={`${setting.key} value(JSON)`}
                  value={editValue}
                  onChange={(e) => setEditValue(e.target.value)}
                  rows={8}
                  disabled={editBusy}
                  spellCheck={false}
                  className="w-full rounded-md border border-border bg-surface px-3 py-2 font-mono text-xs text-foreground focus:border-ring focus:outline-none disabled:opacity-50"
                />
                <input
                  aria-label="설정 변경 사유"
                  value={editReason}
                  onChange={(e) => setEditReason(e.target.value)}
                  placeholder="변경 사유(3~500자)"
                  disabled={editBusy}
                  className="w-full rounded-md border border-border bg-surface px-3 py-1.5 text-xs text-foreground placeholder:text-placeholder focus:border-ring focus:outline-none disabled:opacity-50"
                />
                {editError && <p className="text-xs text-destructive">{editError}</p>}
                <div className="flex gap-2">
                  <button
                    onClick={() => saveEdit(setting)}
                    disabled={editBusy}
                    className="rounded-md btn-gradient px-4 py-1.5 text-xs font-semibold text-primary-foreground disabled:opacity-50"
                  >
                    저장
                  </button>
                  <button
                    onClick={cancelEdit}
                    disabled={editBusy}
                    className="rounded-md px-3 py-1.5 text-xs font-medium text-muted hover:bg-surface"
                  >
                    취소
                  </button>
                </div>
              </div>
            ) : (
              <pre className="mt-3 overflow-x-auto rounded-md bg-border/30 px-3 py-2 font-mono text-xs text-foreground">
                {JSON.stringify(setting.value, null, 2)}
              </pre>
            )}
          </li>
        ))}
      </ul>
    </div>
  );
}
