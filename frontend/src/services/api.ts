import { httpDelete, httpGet, httpPatch, httpPost, httpPut } from "./http";
import type {
  AttendantResponse,
  DashboardSummaryResponse,
  DailyReportResponse,
  ServiceRequestResponse,
} from "../types/api";

export const api = {
  attendants: {
    list: () => httpGet<AttendantResponse[]>("/api/attendants"),
    create: (body: { name: string; badge: string; categories: string[] }) =>
      httpPost<AttendantResponse, typeof body>("/api/attendants", body),
    update: (id: number, body: { name: string; categories: string[] }) =>
      httpPut<AttendantResponse, typeof body>(`/api/attendants/${id}`, body),
    delete: (id: number) => httpDelete(`/api/attendants/${id}`),
    pause: (id: number) => httpPatch<AttendantResponse>(`/api/attendants/${id}/pause`),
    resume: (id: number) => httpPatch<AttendantResponse>(`/api/attendants/${id}/resume`),
  },
  serviceRequests: {
    list: () => httpGet<ServiceRequestResponse[]>("/api/service-requests"),
    listQueue: () => httpGet<ServiceRequestResponse[]>("/api/service-requests/queue"),
    create: (body: { customerName: string; category: string }) =>
      httpPost<ServiceRequestResponse, typeof body>("/api/service-requests", body),
    finish: (id: number) => httpPatch<ServiceRequestResponse>(`/api/service-requests/${id}/finish`),
  },
  dashboard: {
    summary: () => httpGet<DashboardSummaryResponse>("/api/dashboard/summary"),
  },
  reports: {
    daily: (startDate: string, endDate: string) =>
      httpGet<DailyReportResponse>(`/api/reports/daily?startDate=${startDate}&endDate=${endDate}`),
  },
};
