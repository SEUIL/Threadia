/*
 * Department API 클라이언트 — WBS 3.1.3(PR #54) 기준.
 */
import apiClient from "@/lib/axios";
import type { ApiResponse } from "@/types/auth.types";
import type { DepartmentData, DepartmentDeleteData, DepartmentsData } from "@/types/company.types";

export const departmentService = {
  list: (companyId: string | number) =>
    apiClient.get<ApiResponse<DepartmentsData>>(`/api/v1/companies/${companyId}/departments`),

  create: (companyId: string | number, body: { name: string }) =>
    apiClient.post<ApiResponse<DepartmentData>>(
      `/api/v1/companies/${companyId}/departments`,
      body
    ),

  update: (departmentId: number, body: { name: string }) =>
    apiClient.patch<ApiResponse<DepartmentData>>(`/api/v1/departments/${departmentId}`, body),

  remove: (departmentId: number) =>
    apiClient.delete<ApiResponse<DepartmentDeleteData>>(`/api/v1/departments/${departmentId}`),
};
