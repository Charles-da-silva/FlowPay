import type { AttendantStatus, ServiceCategory, ServiceRequestStatus } from "../types/api";

export const serviceCategoryLabels: Record<ServiceCategory, string> = {
  CARD_ISSUES: "Problemas com cartão",
  LOAN_CONTRACTING: "Contratação de empréstimo",
  OTHER_SUBJECTS: "Outros assuntos",
};

export const attendantStatusLabels: Record<AttendantStatus, string> = {
  AVAILABLE: "Disponível",
  BUSY: "Ocupado",
  PAUSED: "Em pausa",
  INACTIVE: "Inativo",
};

export const serviceRequestStatusLabels: Record<ServiceRequestStatus, string> = {
  WAITING: "Aguardando",
  IN_PROGRESS: "Em atendimento",
  COMPLETED: "Concluído",
};

export const sseStatusLabels = {
  connecting: "conectando",
  connected: "conectado",
  disconnected: "desconectado",
} as const;
