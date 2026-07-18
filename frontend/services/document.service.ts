import apiClient from "@/lib/axios";
import type { ApiResponse } from "@/types/auth.types";
import type {
  DocumentData,
  DocumentIssueLinkData,
  DocumentIssueLinksData,
  DocumentsData,
  DocumentsOfIssueData,
  DocumentType,
  GraphData,
  IssuesOfDocumentData,
} from "@/types/document.types";

export const documentService = {
  list: (projectId: string | number, params?: { type?: DocumentType; q?: string; includeTrashed?: boolean }) =>
    apiClient.get<ApiResponse<DocumentsData>>(`/api/v1/projects/${projectId}/documents`, { params }),

  create: (projectId: string | number, body: { title: string; type: DocumentType; content: string }) =>
    apiClient.post<ApiResponse<DocumentData>>(`/api/v1/projects/${projectId}/documents`, body),

  detail: (documentId: string | number) =>
    apiClient.get<ApiResponse<DocumentData>>(`/api/v1/documents/${documentId}`),

  update: (documentId: string | number, body: { title: string; type: DocumentType; content: string }) =>
    apiClient.patch<ApiResponse<DocumentData>>(`/api/v1/documents/${documentId}`, body),

  trash: (documentId: string | number) =>
    apiClient.post<ApiResponse<DocumentData>>(`/api/v1/documents/${documentId}/trash`),

  restore: (documentId: string | number) =>
    apiClient.post<ApiResponse<DocumentData>>(`/api/v1/documents/${documentId}/restore`),

  graph: (projectId: string | number) =>
    apiClient.get<ApiResponse<GraphData>>(`/api/v1/projects/${projectId}/documents/graph`),

  createLink: (projectId: string | number, body: { documentId: number; issueId: number }) =>
    apiClient.post<ApiResponse<DocumentIssueLinkData>>(
      `/api/v1/projects/${projectId}/document-issue-links`,
      body
    ),

  listLinks: (projectId: string | number) =>
    apiClient.get<ApiResponse<DocumentIssueLinksData>>(
      `/api/v1/projects/${projectId}/document-issue-links`
    ),

  deleteLink: (linkId: string | number) =>
    apiClient.delete<ApiResponse<{ deleted: boolean }>>(`/api/v1/document-issue-links/${linkId}`),

  issuesOfDocument: (documentId: string | number) =>
    apiClient.get<ApiResponse<IssuesOfDocumentData>>(`/api/v1/documents/${documentId}/issues`),

  documentsOfIssue: (issueId: string | number) =>
    apiClient.get<ApiResponse<DocumentsOfIssueData>>(`/api/v1/issues/${issueId}/documents`),
};
