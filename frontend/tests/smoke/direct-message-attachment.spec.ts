import { expect, test, type Page } from "@playwright/test";
import type { AttachmentResponse, InitiateAttachmentRequest } from "../../types/attachment.types";

const apiResponse = (data: unknown) => ({ data, meta: { requestId: "direct-message-attachment-smoke" } });
const USER = { id: 1, email: "me@test.dev", nickname: "나", avatarUrl: null };
const OTHER_USER = { id: 2, email: "other@test.dev", nickname: "상대 사용자", avatarUrl: null };

async function mockApi(page: Page) {
  const attachmentTargets: string[] = [];
  const attachmentCalls: string[] = [];
  const attachments: AttachmentResponse[] = [];
  const markReadBodies: Array<{ lastReadDirectMessageId?: number }> = [];

  await page.route("**/attachment-upload/**", async (route) => {
    attachmentCalls.push(`put:${route.request().url()}`);
    await route.fulfill({ status: 200 });
  });
  await page.route("**/api/v1/**", async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const origin = request.headers().origin ?? "http://127.0.0.1:3001";
    const headers = {
      "access-control-allow-origin": origin,
      "access-control-allow-methods": "GET,POST,DELETE,OPTIONS",
      "access-control-allow-headers": "authorization,content-type",
      "access-control-allow-credentials": "true",
    };
    const json = async (data: unknown, status = 200) => route.fulfill({ status, contentType: "application/json", headers, body: JSON.stringify(apiResponse(data)) });

    if (request.method() === "OPTIONS") {
      await route.fulfill({ status: 204, headers });
      return;
    }
    if (request.method() === "POST" && url.pathname === "/api/v1/auth/refresh") {
      await json({ accessToken: "test-token", user: USER });
      return;
    }
    if (request.method() === "GET" && url.pathname === "/api/v1/users/me/company-memberships") {
      await json({ companyMemberships: [{ companyMemberId: 1, companyId: 1, companyName: "회사", role: "MEMBER", status: "ACTIVE" }] });
      return;
    }
    if (request.method() === "GET" && url.pathname === "/api/v1/companies/1/direct-messages/rooms") {
      await json({ rooms: [{ id: 8, companyId: 1, otherUser: OTHER_USER, unreadCount: 1, createdAt: "2026-07-20T10:00:00" }] });
      return;
    }
    if (request.method() === "GET" && url.pathname === "/api/v1/direct-messages/rooms/8/messages") {
      await json({ messages: [{ id: 45, roomId: 8, sender: USER, content: "이미 저장된 DM", createdAt: "2026-07-20T10:00:00" }] });
      return;
    }
    if (request.method() === "POST" && url.pathname === "/api/v1/direct-messages/rooms/8/mark-read") {
      markReadBodies.push(request.postDataJSON() as { lastReadDirectMessageId?: number });
      await json({ roomId: 8, lastReadDirectMessageId: 45, unreadCount: 0, readAt: "2026-07-20T10:00:01" });
      return;
    }
    if (request.method() === "POST" && url.pathname === "/api/v1/direct-messages/rooms/8/messages") {
      await json({ message: { id: 46, roomId: 8, sender: USER, content: (request.postDataJSON() as { content: string }).content, createdAt: "2026-07-20T10:01:00" } });
      return;
    }
    if (request.method() === "GET" && url.pathname === "/api/v1/attachments") {
      const targetType = url.searchParams.get("targetType");
      const targetId = url.searchParams.get("targetId");
      attachmentTargets.push(`${targetType}:${targetId}`);
      await json(targetType === "DIRECT_MESSAGE" ? attachments.filter((attachment) => attachment.targetId === Number(targetId)) : []);
      return;
    }
    if (request.method() === "POST" && url.pathname === "/api/v1/attachments/uploads") {
      const body = request.postDataJSON() as InitiateAttachmentRequest;
      const attachment: AttachmentResponse = { id: 101, targetType: body.targetType, targetId: body.targetId, filename: body.filename, contentType: body.contentType, contentLength: body.contentLength, status: "UPLOADING", completedAt: null, createdAt: "2026-07-20T10:01:00" };
      attachments.push(attachment);
      attachmentCalls.push(`initiate:${body.targetType}:${body.targetId}`);
      await json({ attachment, uploadUrl: "http://127.0.0.1:3001/attachment-upload/101" }, 201);
      return;
    }
    if (request.method() === "POST" && url.pathname === "/api/v1/attachments/101/complete") {
      attachments[0].status = "READY";
      attachments[0].completedAt = "2026-07-20T10:01:01";
      attachmentCalls.push("complete:101");
      await json(attachments[0]);
      return;
    }
    if (request.method() === "GET" && url.pathname === "/api/v1/attachments/101/download") {
      attachmentCalls.push("download:101");
      await json({ downloadUrl: "https://storage.test/message.txt" });
      return;
    }
    if (request.method() === "DELETE" && url.pathname === "/api/v1/attachments/101") {
      attachments.length = 0;
      attachmentCalls.push("delete:101");
      await route.fulfill({ status: 204, headers });
      return;
    }
    await route.abort();
  });

  return { attachmentCalls, attachmentTargets, markReadBodies };
}

test("persisted direct message uses the DIRECT_MESSAGE attachment target for list, upload, download, and delete", async ({ page }) => {
  const mock = await mockApi(page);
  await page.goto("/1/dm/8");

  await expect(page.getByText("이미 저장된 DM")).toBeVisible();
  await expect.poll(() => mock.attachmentTargets).toContain("DIRECT_MESSAGE:45");
  await expect.poll(() => mock.markReadBodies.some((body) => body.lastReadDirectMessageId === 45)).toBe(true);

  const message = page.getByText("이미 저장된 DM").locator("..");
  await message.locator("input[type=file]").setInputFiles({ name: "message.txt", mimeType: "text/plain", buffer: Buffer.from("dm attachment") });
  await expect.poll(() => mock.attachmentCalls).toContain("initiate:DIRECT_MESSAGE:45");
  await expect.poll(() => mock.attachmentCalls).toContain("complete:101");
  await expect(message.getByRole("button", { name: "message.txt" })).toBeVisible();

  await message.getByRole("button", { name: "message.txt" }).click();
  await expect.poll(() => mock.attachmentCalls).toContain("download:101");
  await message.getByRole("button", { name: "삭제" }).click();
  await expect.poll(() => mock.attachmentCalls).toContain("delete:101");
});

test("new direct message receives a persisted numeric ID before its attachment control is rendered", async ({ page }) => {
  const mock = await mockApi(page);
  await page.goto("/1/dm/8");
  await expect(page.getByText("이미 저장된 DM")).toBeVisible();

  await page.getByLabel("메시지 입력").fill("새 DM");
  await page.getByRole("button", { name: "전송" }).click();

  await expect(page.getByText("새 DM")).toBeVisible();
  await expect.poll(() => mock.attachmentTargets).toContain("DIRECT_MESSAGE:46");
});
