// Tipos do frontend (TypeScript) que espelham o backend.
// Comentario: manter estes tipos bem definidos ajuda muito em entrevistas,
// porque voce mostra que entende o contrato (API) entre front e back.

export type AttendantStatus = "AVAILABLE" | "BUSY" | "PAUSED" | "INACTIVE";
export type ServiceCategory = "CARD_ISSUES" | "LOAN_CONTRACTING" | "OTHER_SUBJECTS";
export type ServiceRequestStatus = "WAITING" | "IN_PROGRESS" | "COMPLETED";

export interface AttendantResponse {
  id: number;
  name: string;
  badge: string;
  status: AttendantStatus;
  activeServiceRequests: number;
  availableSince: string | null;
  pausedSince: string | null;
  maxSimultaneousCustomers: number;
  categories: ServiceCategory[];
}

export interface ServiceRequestResponse {
  id: number;
  customerName: string;
  category: ServiceCategory;
  status: ServiceRequestStatus;
  attendantId: number | null;
  attendantName: string | null;
  createdAt: string;
  startedAt: string | null;
  finishedAt: string | null;
  queuedAt: string | null;
}

export interface AttendantDailySummaryResponse {
  attendantId: number;
  attendantName: string;
  attendantBadge: string;
  serviceRequests: number;
  averageServiceSeconds: number;
  pauseCount: number;
  totalPauseSeconds: number;
}

export interface DashboardSummaryResponse {
  totalAttendants: number;
  availableAttendants: number;
  busyAttendants: number;
  pausedAttendants: number;
  inactiveAttendants: number;
  totalServiceRequests: number;
  waitingServiceRequests: number;
  inProgressServiceRequests: number;
  completedServiceRequests: number;
  todayServiceRequests: number;
  todayWaitedServiceRequests: number;
  todayServiceLevel: number;
  todayByAttendant: AttendantDailySummaryResponse[];
}

export interface CategoryReportResponse {
  category: ServiceCategory;
  totalServiceRequests: number;
  waitedServiceRequests: number;
  inProgressServiceRequests: number;
  completedServiceRequests: number;
  averageServiceSeconds: number;
  averageWaitSeconds: number;
}

export interface DailyReportResponse {
  startDate: string;
  endDate: string;
  generatedAt: string;
  totalServiceRequests: number;
  waitedServiceRequests: number;
  inProgressServiceRequests: number;
  completedServiceRequests: number;
  serviceLevel: number;
  averageServiceSeconds: number;
  categories: CategoryReportResponse[];
  attendants: AttendantDailySummaryResponse[];
}
