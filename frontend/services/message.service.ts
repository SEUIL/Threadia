import apiClient from "@/lib/axios";
import type { ApiResponse } from "@/types/auth.types";
import type { MessageData } from "@/types/message.types";

export const messageService = {
  /** Thread 컨텍스트 배너용 단건 조회. 프로젝트 채팅(S09) 목록/전송은 별도 WBS(4.7.2.2). */
  detail: (messageId: string | number) =>
    apiClient.get<ApiResponse<MessageData>>(`/api/v1/messages/${messageId}`),
};
