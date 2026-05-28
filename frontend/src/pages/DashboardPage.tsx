import { useEffect, useState } from "react";
import { api } from "../services/api";
import { getApiBaseUrl } from "../services/http";
import type { AttendantResponse, DailyReportResponse, ServiceCategory, ServiceRequestResponse } from "../types/api";
import { useDashboardSse } from "../hooks/useDashboardSse";
import { StatCard } from "../components/ui/StatCard";
import { Section } from "../components/ui/Section";
import { CreateAttendantForm } from "../components/forms/CreateAttendantForm";
import { CreateServiceRequestForm } from "../components/forms/CreateServiceRequestForm";
import { attendantStatusLabels, serviceCategoryLabels, sseStatusLabels } from "../utils/labels";

const categories: ServiceCategory[] = ["CARD_ISSUES", "LOAN_CONTRACTING", "OTHER_SUBJECTS"];
const namePattern = /^[\p{L} ]+$/u;

function formatDuration(start: string | null, end?: string | null, now = Date.now()) {
  if (!start) return "-";

  const startTime = new Date(start).getTime();
  const endTime = end ? new Date(end).getTime() : now;
  const totalSeconds = Math.max(0, Math.floor((endTime - startTime) / 1000));
  return formatSeconds(totalSeconds);
}

function formatSeconds(value: number) {
  const totalSeconds = Math.max(0, Math.floor(value));
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const seconds = totalSeconds % 60;

  if (hours > 0) return `${hours}h ${minutes}m`;
  return `${minutes}m ${seconds}s`;
}

function formatServiceLevel(value: number) {
  const truncated = Math.floor(value * 100) / 100;
  return `${truncated.toLocaleString("pt-BR", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })}%`;
}

function isValidPersonName(value: string) {
  const trimmed = value.trim().replace(/\s+/g, " ");
  return trimmed.length >= 2 && namePattern.test(trimmed);
}

function todayInputValue() {
  return new Date().toISOString().slice(0, 10);
}

function getDaysDifference(startDate: string, endDate: string): number {
  const start = new Date(startDate).getTime();
  const end = new Date(endDate).getTime();
  return Math.max(1, Math.floor((end - start) / (1000 * 60 * 60 * 24)) + 1);
}

function getCurrentMonth() {
  const today = new Date();
  const year = today.getFullYear();
  const month = String(today.getMonth() + 1).padStart(2, "0");
  const day = "01";
  return `${year}-${month}-${day}`;
}

function getMonthEnd() {
  const today = new Date();
  const year = today.getFullYear();
  const month = today.getMonth() + 1;
  const lastDay = new Date(year, month, 0).getDate();
  return `${year}-${String(month).padStart(2, "0")}-${String(lastDay).padStart(2, "0")}`;
}

interface Quarter {
  name: string;
  startMonth: number;
  endMonth: number;
}

const QUARTERS: Quarter[] = [
  { name: "Q1", startMonth: 2, endMonth: 4 },
  { name: "Q2", startMonth: 5, endMonth: 7 },
  { name: "Q3", startMonth: 8, endMonth: 10 },
  { name: "Q4", startMonth: 11, endMonth: 1 },
];

function getQuarterDates(quarter: Quarter): { start: string; end: string } | null {
  const today = new Date();
  const currentMonth = today.getMonth() + 1;
  const currentYear = today.getFullYear();
  let startYear = currentYear;
  let endYear = currentYear;

  if (quarter.endMonth < quarter.startMonth) {
    if (currentMonth >= quarter.startMonth) {
      endYear = currentYear + 1;
    } else {
      startYear = currentYear - 1;
    }
  }

  const startDate = `${startYear}-${String(quarter.startMonth).padStart(2, "0")}-01`;
  const endDate = `${endYear}-${String(quarter.endMonth).padStart(2, "0")}-${new Date(endYear, quarter.endMonth, 0).getDate()}`;

  return { start: startDate, end: endDate };
}

function isQuarterActive(quarter: Quarter): boolean {
  const today = new Date();
  const currentMonth = today.getMonth() + 1;

  if (quarter.endMonth < quarter.startMonth) {
    return currentMonth >= quarter.startMonth || currentMonth <= quarter.endMonth;
  }
  return currentMonth >= quarter.startMonth && currentMonth <= quarter.endMonth;
}

function hasQuarterPassed(quarter: Quarter): boolean {
  const today = new Date();
  const currentMonth = today.getMonth() + 1;

  if (quarter.endMonth < quarter.startMonth) {
    return currentMonth > quarter.endMonth && currentMonth < quarter.startMonth;
  }
  return currentMonth > quarter.endMonth;
}

export function DashboardPage() {
  const { summary, status } = useDashboardSse();
  const [attendants, setAttendants] = useState<AttendantResponse[]>([]);
  const [queue, setQueue] = useState<ServiceRequestResponse[]>([]);
  const [activeRequests, setActiveRequests] = useState<ServiceRequestResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [now, setNow] = useState(Date.now());
  const [formPanel, setFormPanel] = useState<"service" | "agent" | null>(null);
  const [finishingId, setFinishingId] = useState<number | null>(null);
  const [confirmFinishId, setConfirmFinishId] = useState<number | null>(null);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editName, setEditName] = useState("");
  const [editCategories, setEditCategories] = useState<ServiceCategory[]>([]);
  const [savingAttendantId, setSavingAttendantId] = useState<number | null>(null);
  const [deletingAttendantId, setDeletingAttendantId] = useState<number | null>(null);
  const [confirmDeleteId, setConfirmDeleteId] = useState<number | null>(null);
  const [pauseActionId, setPauseActionId] = useState<number | null>(null);
  const [attendantError, setAttendantError] = useState<string | null>(null);
  const [reportStartDate, setReportStartDate] = useState(todayInputValue());
  const [reportEndDate, setReportEndDate] = useState(todayInputValue());
  const [dailyReport, setDailyReport] = useState<DailyReportResponse | null>(null);
  const [reportLoading, setReportLoading] = useState(false);

  const loadData = async () => {
    try {
      const [a, q, requests] = await Promise.all([
        api.attendants.list(),
        api.serviceRequests.listQueue(),
        api.serviceRequests.list(),
      ]);
      setAttendants(a);
      setQueue(q);
      setActiveRequests(requests.filter((request) => request.status === "IN_PROGRESS"));
    } catch (err) {
      console.error("Erro ao carregar dados:", err);
    } finally {
      setLoading(false);
    }
  };

  const loadDailyReport = async (startDate = reportStartDate, endDate = reportEndDate) => {
    setReportLoading(true);
    try {
      setDailyReport(await api.reports.daily(startDate, endDate));
    } catch (err) {
      console.error("Erro ao carregar relatorio:", err);
    } finally {
      setReportLoading(false);
    }
  };

  useEffect(() => {
    setLoading(true);
    loadData();
    loadDailyReport();
  }, []);

  useEffect(() => {
    const intervalId = window.setInterval(() => setNow(Date.now()), 1000);
    return () => window.clearInterval(intervalId);
  }, []);

  useEffect(() => {
    if (summary) {
      loadData();
      loadDailyReport();
    }
  }, [summary]);

  useEffect(() => {
    loadDailyReport(reportStartDate, reportEndDate);
  }, [reportStartDate, reportEndDate]);

  useEffect(() => {
    if (!attendantError) return;
    const timeoutId = window.setTimeout(() => setAttendantError(null), 5000);
    return () => window.clearTimeout(timeoutId);
  }, [attendantError]);

  const startEdit = (attendant: AttendantResponse) => {
    setAttendantError(null);
    setConfirmDeleteId(null);
    setEditingId(attendant.id);
    setEditName(attendant.name);
    setEditCategories(attendant.categories);
  };

  const toggleEditCategory = (category: ServiceCategory) => {
    setEditCategories((prev) =>
      prev.includes(category) ? prev.filter((item) => item !== category) : [...prev, category]
    );
  };

  const saveAttendant = async (id: number) => {
    setAttendantError(null);

    if (!isValidPersonName(editName)) {
      setAttendantError("Use apenas letras e espaços no nome do agente.");
      return;
    }

    if (editCategories.length === 0) {
      setAttendantError("Selecione pelo menos uma categoria.");
      return;
    }

    setSavingAttendantId(id);
    try {
      await api.attendants.update(id, { name: editName.trim().replace(/\s+/g, " "), categories: editCategories });
      setEditingId(null);
      await loadData();
    } catch (err) {
      setAttendantError(err instanceof Error ? err.message : "Erro ao editar agente.");
    } finally {
      setSavingAttendantId(null);
    }
  };

  const deleteAttendant = async (attendant: AttendantResponse) => {
    setAttendantError(null);

    if (attendant.activeServiceRequests > 0) {
      setAttendantError("Finalize os atendimentos em andamento antes de excluir este agente.");
      return;
    }

    setDeletingAttendantId(attendant.id);
    try {
      await api.attendants.delete(attendant.id);
      setConfirmDeleteId(null);
      await loadData();
    } catch (err) {
      setAttendantError(err instanceof Error ? err.message : "Erro ao excluir agente.");
    } finally {
      setDeletingAttendantId(null);
    }
  };

  const togglePause = async (attendant: AttendantResponse) => {
    setAttendantError(null);
    setPauseActionId(attendant.id);

    try {
      if (attendant.status === "PAUSED") {
        await api.attendants.resume(attendant.id);
      } else {
        await api.attendants.pause(attendant.id);
      }
      await loadData();
    } catch (err) {
      setAttendantError(err instanceof Error ? err.message : "Erro ao alterar pausa.");
    } finally {
      setPauseActionId(null);
    }
  };

  const handleFinishRequest = async (id: number) => {
    setFinishingId(id);
    try {
      await api.serviceRequests.finish(id);
      await loadData();
      setConfirmFinishId(null);
    } catch (err) {
      console.error("Erro ao finalizar atendimento:", err);
    } finally {
      setFinishingId(null);
    }
  };

  const exportDailyReport = () => {
    window.open(
      `${getApiBaseUrl()}/api/reports/daily.csv?startDate=${reportStartDate}&endDate=${reportEndDate}`,
      "_blank",
      "noopener,noreferrer"
    );
  };

  return (
    <div className="min-h-screen bg-slate-300">
      <header className="border-b border-slate-300 bg-white shadow-md">
        <div className="mx-auto max-w-7xl px-3 py-4 sm:px-4">
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <h1 className="text-xl font-semibold text-slate-950">FlowPay - Painel de monitoramento de fila</h1>
              <p className="mt-1 text-sm text-slate-500">
                Distribuição e monitoramento de atendimentos em tempo real.
              </p>
            </div>
            <div
              className={`w-fit rounded-full px-3 py-1 text-xs font-medium ${
                status === "connected"
                  ? "bg-emerald-100 text-emerald-800"
                  : status === "connecting"
                    ? "bg-amber-100 text-amber-800"
                    : "bg-rose-100 text-rose-800"
              }`}
              title="Status da conexão em tempo real"
            >
              Tempo real: {sseStatusLabels[status]}
            </div>
          </div>
        </div>
      </header>

      <main className="mx-auto max-w-7xl px-3 py-5 sm:px-4 sm:py-6">
        <section className="rounded-xl border border-slate-300 bg-white p-3 shadow-md ring-1 ring-white sm:p-4">
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-5">
            <StatCard label="Service Level" value={summary ? formatServiceLevel(summary.todayServiceLevel) : "-"} />
            <StatCard label="Atendimentos hoje" value={summary?.todayServiceRequests ?? "-"} />
            <StatCard label="Entraram em espera" value={summary?.todayWaitedServiceRequests ?? "-"} />
            <button
              type="button"
              onClick={() => setFormPanel((current) => (current === "service" ? null : "service"))}
              className="rounded-lg border border-emerald-300 bg-emerald-50 px-4 py-3 text-sm font-semibold text-emerald-800 shadow-sm transition hover:bg-emerald-100"
            >
              Novo atendimento
            </button>
            <button
              type="button"
              onClick={() => setFormPanel((current) => (current === "agent" ? null : "agent"))}
              className="rounded-lg border border-blue-300 bg-blue-100 px-4 py-3 text-sm font-semibold text-blue-800 shadow-sm transition hover:bg-blue-200"
            >
              Logar agente
            </button>
          </div>

          {formPanel ? (
            <div className="mt-4">
              {formPanel === "service" ? (
                <CreateServiceRequestForm
                  onCancel={() => setFormPanel(null)}
                  onSuccess={() => {
                    setFormPanel(null);
                    loadData();
                  }}
                />
              ) : (
                <CreateAttendantForm
                  onCancel={() => setFormPanel(null)}
                  onSuccess={() => {
                    setFormPanel(null);
                    loadData();
                  }}
                />
              )}
            </div>
          ) : null}
        </section>

        <div className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-5">
          <StatCard label="Agentes logados" value={summary?.totalAttendants ?? "-"} />
          <StatCard label="Agentes disponíveis" value={summary?.availableAttendants ?? "-"} />
          <StatCard label="Agentes em atendimento" value={summary?.busyAttendants ?? "-"} />
          <StatCard label="Clientes na fila" value={summary?.waitingServiceRequests ?? "-"} />
          <StatCard label="Clientes em atendimento" value={summary?.inProgressServiceRequests ?? "-"} />
        </div>

        <div className="mt-6 space-y-6">
          <Section
            title="Fila de espera"
            description="Clientes aguardando disponibilidade de agentes elegíveis."
            right={<span className="text-sm text-slate-500">{loading ? "carregando..." : `${queue.length} clientes`}</span>}
          >
            <div className="overflow-x-auto rounded-lg border border-slate-200">
              <table className="w-full min-w-[680px] text-left text-sm">
                <thead className="bg-slate-50 text-slate-600">
                  <tr>
                    <th className="px-3 py-2">Cliente</th>
                    <th className="px-3 py-2">Categoria</th>
                    <th className="px-3 py-2 text-center">Tempo em espera</th>
                  </tr>
                </thead>
                <tbody className="bg-white">
                  {queue.map((request) => (
                    <tr key={request.id} className="border-t border-slate-100">
                      <td className="px-3 py-2 font-medium text-slate-900">{request.customerName}</td>
                      <td className="px-3 py-2 text-slate-600">{serviceCategoryLabels[request.category]}</td>
                      <td className="px-3 py-2 text-center text-slate-600">{formatDuration(request.queuedAt ?? request.createdAt, null, now)}</td>
                    </tr>
                  ))}
                  {queue.length === 0 ? (
                    <tr>
                      <td className="px-3 py-6 text-slate-500" colSpan={3}>Nenhum cliente em fila.</td>
                    </tr>
                  ) : null}
                </tbody>
              </table>
            </div>
          </Section>

          <Section
            title="Em atendimento"
            description="Clientes já atribuídos a agentes."
            right={<span className="text-sm text-slate-500">{loading ? "carregando..." : `${activeRequests.length} ativos`}</span>}
          >
            <div className="overflow-x-auto rounded-lg border border-slate-200">
              <table className="w-full min-w-[760px] text-left text-sm">
                <thead className="bg-slate-50 text-slate-600">
                  <tr>
                    <th className="px-3 py-2">Cliente</th>
                    <th className="px-3 py-2">Categoria</th>
                    <th className="px-3 py-2">Agente</th>
                    <th className="px-3 py-2 text-center">Tempo</th>
                    <th className="px-3 py-2">Ação</th>
                  </tr>
                </thead>
                <tbody className="bg-white">
                  {activeRequests.map((request) => (
                    <tr key={request.id} className="border-t border-slate-100">
                      <td className="px-3 py-2 font-medium text-slate-900">{request.customerName}</td>
                      <td className="px-3 py-2 text-slate-600">{serviceCategoryLabels[request.category]}</td>
                      <td className="px-3 py-2 text-slate-600">{request.attendantName ?? "-"}</td>
                      <td className="px-3 py-2 text-center text-slate-600">{formatDuration(request.startedAt, request.finishedAt, now)}</td>
                      <td className="px-3 py-2">
                        {confirmFinishId === request.id ? (
                          <div className="flex gap-2">
                            <button onClick={() => handleFinishRequest(request.id)} disabled={finishingId === request.id} className="rounded bg-green-600 px-2 py-1 text-xs text-white hover:bg-green-700 disabled:bg-slate-400">
                              {finishingId === request.id ? "..." : "Confirmar"}
                            </button>
                            <button onClick={() => setConfirmFinishId(null)} disabled={finishingId === request.id} className="rounded bg-slate-300 px-2 py-1 text-xs text-slate-700 hover:bg-slate-400 disabled:bg-slate-400">
                              Cancelar
                            </button>
                          </div>
                        ) : (
                          <button onClick={() => setConfirmFinishId(request.id)} className="rounded bg-blue-600 px-3 py-1 text-xs text-white hover:bg-blue-700">
                            Finalizar
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                  {activeRequests.length === 0 ? (
                    <tr>
                      <td className="px-3 py-6 text-slate-500" colSpan={5}>Nenhum atendimento em andamento.</td>
                    </tr>
                  ) : null}
                </tbody>
              </table>
            </div>
          </Section>
        </div>

        <div className="mt-6">
          <Section
            title="Agentes logados"
            description="Informações dos agentes logados, seus status e ações possíveis"
            right={<span className="text-sm text-slate-500">{loading ? "carregando..." : `${attendants.length} agentes`}</span>}
          >
            {attendantError ? <div className="mb-3 rounded bg-rose-50 px-3 py-2 text-sm text-rose-700">{attendantError}</div> : null}
            <div className="overflow-x-auto rounded-lg border border-slate-200">
              <table className="w-full min-w-[900px] text-left text-sm">
                <thead className="bg-slate-50 text-slate-600">
                  <tr>
                    <th className="px-3 py-2">Badge</th>
                    <th className="px-3 py-2">Nome</th>
                    <th className="px-3 py-2">Status</th>
                    <th className="px-3 py-2 text-center">Atendimentos</th>
                    <th className="px-3 py-2 text-center">Tempo (disponível ou pausa)</th>
                    <th className="px-3 py-2">Categorias de atendimento</th>
                    <th className="px-3 py-2">Ações</th>
                  </tr>
                </thead>
                <tbody className="bg-white">
                  {attendants.map((attendant) => {
                    const isEditing = editingId === attendant.id;
                    const isConfirmingDelete = confirmDeleteId === attendant.id;
                    const timeStart = attendant.status === "PAUSED" ? attendant.pausedSince : attendant.availableSince;

                    return (
                      <tr key={attendant.id} className="border-t border-slate-100 align-top">
                        <td className="px-3 py-2 font-mono text-xs font-semibold text-slate-700">{attendant.badge}</td>
                        <td className="px-3 py-2 font-medium text-slate-900">
                          {isEditing ? <input value={editName} onChange={(event) => setEditName(event.target.value)} className="w-36 rounded border border-slate-300 px-2 py-1 text-sm" /> : attendant.name}
                        </td>
                        <td className="px-3 py-2 text-slate-600">{attendantStatusLabels[attendant.status]}</td>
                        <td className="px-3 py-2 text-center text-slate-600">{attendant.activeServiceRequests}/{attendant.maxSimultaneousCustomers}</td>
                        <td className="px-3 py-2 text-center text-slate-600">{formatDuration(timeStart, null, now)}</td>
                        <td className="px-3 py-2 text-slate-500">
                          {isEditing ? (
                            <div className="space-y-1">
                              {categories.map((category) => (
                                <label key={category} className="flex items-center gap-2">
                                  <input type="checkbox" checked={editCategories.includes(category)} onChange={() => toggleEditCategory(category)} />
                                  <span>{serviceCategoryLabels[category]}</span>
                                </label>
                              ))}
                            </div>
                          ) : (
                            attendant.categories.map((category) => serviceCategoryLabels[category]).join(", ")
                          )}
                        </td>
                        <td className="px-3 py-2">
                          {isEditing ? (
                            <div className="flex flex-wrap gap-2">
                              <button onClick={() => saveAttendant(attendant.id)} disabled={savingAttendantId === attendant.id} className="rounded bg-green-600 px-2 py-1 text-xs text-white hover:bg-green-700 disabled:bg-slate-400">
                                {savingAttendantId === attendant.id ? "Salvando..." : "Salvar"}
                              </button>
                              <button onClick={() => setEditingId(null)} disabled={savingAttendantId === attendant.id} className="rounded bg-slate-300 px-2 py-1 text-xs text-slate-700 hover:bg-slate-400 disabled:bg-slate-400">
                                Cancelar
                              </button>
                            </div>
                          ) : isConfirmingDelete ? (
                            <div className="flex flex-wrap gap-2">
                              <button onClick={() => deleteAttendant(attendant)} disabled={deletingAttendantId === attendant.id} className="rounded bg-rose-600 px-2 py-1 text-xs text-white hover:bg-rose-700 disabled:bg-slate-400">
                                {deletingAttendantId === attendant.id ? "Excluindo..." : "Confirmar"}
                              </button>
                              <button onClick={() => setConfirmDeleteId(null)} disabled={deletingAttendantId === attendant.id} className="rounded bg-slate-300 px-2 py-1 text-xs text-slate-700 hover:bg-slate-400 disabled:bg-slate-400">
                                Cancelar
                              </button>
                            </div>
                          ) : (
                            <div className="flex flex-wrap gap-2">
                              <button onClick={() => startEdit(attendant)} className="rounded bg-blue-600 px-2 py-1 text-xs text-white hover:bg-blue-700">Editar</button>
                              <button onClick={() => togglePause(attendant)} disabled={(attendant.status !== "AVAILABLE" && attendant.status !== "PAUSED") || pauseActionId === attendant.id} className={`rounded px-2 py-1 text-xs text-white ${attendant.status === "PAUSED" ? "bg-green-600 hover:bg-green-700" : "bg-amber-500 hover:bg-amber-600"} disabled:bg-slate-300`}>
                                {pauseActionId === attendant.id ? "..." : attendant.status === "PAUSED" ? "Voltar" : "Pausa"}
                              </button>
                              <button onClick={() => { setAttendantError(null); setEditingId(null); setConfirmDeleteId(attendant.id); }} className="rounded bg-rose-600 px-2 py-1 text-xs text-white hover:bg-rose-700">Deslogar</button>
                            </div>
                          )}
                        </td>
                      </tr>
                    );
                  })}
                  {attendants.length === 0 ? (
                    <tr>
                      <td className="px-3 py-6 text-slate-500" colSpan={7}>Nenhum agente cadastrado ainda.</td>
                    </tr>
                  ) : null}
                </tbody>
              </table>
            </div>
          </Section>
        </div>

        <div className="mt-6">
          <Section title="Histórico dos agentes" description="Produção e pausas acumuladas no dia.">
            <div className="overflow-x-auto rounded-lg border border-slate-200">
              <table className="w-full min-w-[700px] text-left text-sm">
                <thead className="bg-slate-50 text-slate-600">
                  <tr>
                    <th className="px-3 py-2">Badge</th>
                    <th className="px-3 py-2">Agente</th>
                    <th className="px-3 py-2 text-center">Atendimentos</th>
                    <th className="px-3 py-2 text-center">Tempo médio</th>
                    <th className="px-3 py-2 text-center">Pausas</th>
                    <th className="px-3 py-2 text-center">Tempo médio por dia</th>
                  </tr>
                </thead>
                <tbody className="bg-white">
                  {summary?.todayByAttendant.map((item) => (
                    <tr key={item.attendantId} className="border-t border-slate-100">
                      <td className="px-3 py-2 font-mono text-xs font-semibold text-slate-700">{item.attendantBadge}</td>
                      <td className="px-3 py-2 font-medium text-slate-900">{item.attendantName}</td>
                      <td className="px-3 py-2 text-center text-slate-600">{item.serviceRequests}</td>
                      <td className="px-3 py-2 text-center text-slate-600">{formatSeconds(item.averageServiceSeconds)}</td>
                      <td className="px-3 py-2 text-center text-slate-600">{item.pauseCount}</td>
                      <td className="px-3 py-2 text-center text-slate-600">{formatSeconds(item.totalPauseSeconds)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </Section>
        </div>
        <div className="mt-6">
          <Section
            title="Relatórios gerenciais"
            description="Visão consolidada para acompanhamento de desempenho e exportação para análise."
            right={
              <div className="flex flex-col gap-3">
                <div className="flex flex-wrap gap-2">
                  <button
                    type="button"
                    onClick={() => {
                      setReportStartDate(getCurrentMonth());
                      setReportEndDate(getMonthEnd());
                    }}
                    className="rounded border border-slate-300 bg-slate-50 px-3 py-1 text-xs font-medium text-slate-700 hover:bg-slate-100"
                  >
                    Mês atual
                  </button>
                  {QUARTERS.map((quarter) => {
                    const dates = getQuarterDates(quarter);
                    const isDisabled = !isQuarterActive(quarter) && hasQuarterPassed(quarter);
                    return (
                      <button
                        key={quarter.name}
                        type="button"
                        onClick={() => {
                          if (dates) {
                            setReportStartDate(dates.start);
                            setReportEndDate(dates.end);
                          }
                        }}
                        disabled={isDisabled}
                        className={`rounded border px-3 py-1 text-xs font-medium ${
                          isDisabled
                            ? "border-slate-200 bg-slate-100 text-slate-400"
                            : "border-slate-300 bg-slate-50 text-slate-700 hover:bg-slate-100"
                        }`}
                      >
                        {quarter.name}
                      </button>
                    );
                  })}
                </div>
                <div className="flex flex-col gap-2 sm:flex-row sm:items-center">
                  <label className="flex flex-col gap-1 text-xs font-medium text-slate-600">
                    Início
                    <input
                      type="date"
                      value={reportStartDate}
                      max={reportEndDate}
                      onChange={(event) => {
                        const newStart = event.target.value;
                        setReportStartDate(newStart);
                      }}
                      className="rounded border border-slate-300 px-2 py-1 text-sm text-slate-700"
                    />
                  </label>
                  <label className="flex flex-col gap-1 text-xs font-medium text-slate-600">
                    Fim
                    <input
                      type="date"
                      value={reportEndDate}
                      min={reportStartDate}
                      onChange={(event) => {
                        const newEnd = event.target.value;
                        setReportEndDate(newEnd);
                      }}
                      className="rounded border border-slate-300 px-2 py-1 text-sm text-slate-700"
                    />
                  </label>
                  <button
                    type="button"
                    onClick={exportDailyReport}
                    className="rounded bg-slate-900 px-3 py-2 text-sm font-medium text-white hover:bg-slate-700 sm:self-end"
                  >
                    Exportar CSV
                  </button>
                </div>
              </div>
            }
          >
            <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
              <StatCard label="Atendimentos no período" value={dailyReport?.totalServiceRequests ?? "-"} />
              <StatCard label="Entraram em espera" value={dailyReport?.waitedServiceRequests ?? "-"} />
              <StatCard label="Service Level" value={dailyReport ? formatServiceLevel(dailyReport.serviceLevel)  : "-"} />
              <div className="rounded-lg border border-slate-200 bg-slate-50 p-3 sm:p-4">
                <p className="text-xs font-semibold text-slate-600">Dias com piores Service Level</p>
                <div className="mt-2 space-y-1">
                  {dailyReport?.worstDays.map((day) => (
                    <div key={day.date} className="text-sm text-slate-900">
                      <span className="font-mono text-xs text-slate-500">{day.date}</span>
                      <span className="ml-2 font-semibold">{formatServiceLevel(day.serviceLevel)}</span>
                    </div>
                  )) ?? <span className="text-sm text-slate-500">-</span>}
                </div>
              </div>
            </div>

            <div className="mt-4 grid gap-4">
              <div className="overflow-x-auto rounded-lg border border-slate-300">
                <table className="w-full min-w-full text-left text-sm">
                  <thead className="bg-slate-50 text-slate-600">
                    <tr>
                      <th className="px-3 py-2 w-32">Categoria</th>
                      <th className="px-3 py-2 text-center">Atendimentos</th>
                      <th className="px-3 py-2 text-center">Tempo médio</th>
                      <th className="px-3 py-2 text-center">Em espera Tempo médio</th>
                    </tr>
                  </thead>
                  <tbody className="bg-white">
                    {dailyReport?.categories.map((item) => (
                      <tr key={item.category} className="border-t border-slate-100">
                        <td className="px-3 py-2 font-medium text-slate-900">{serviceCategoryLabels[item.category]}</td>
                        <td className="px-3 py-2 text-center text-slate-600">{item.totalServiceRequests}</td>
                        <td className="px-3 py-2 text-center text-slate-600">{formatSeconds(item.averageServiceSeconds)}</td>
                        <td className="px-3 py-2 text-center text-slate-600">{item.waitedServiceRequests}</td>
                      </tr>
                    ))}
                    {!dailyReport || reportLoading ? (
                      <tr>
                        <td className="px-3 py-6 text-slate-500" colSpan={5}>Carregando relatório...</td>
                      </tr>
                    ) : null}
                  </tbody>
                </table>
              </div>

              <div className="overflow-x-auto rounded-lg border border-slate-300">
                <table className="w-full min-w-full text-left text-sm">
                  <thead className="bg-slate-50 text-slate-600">
                    <tr>
                      <th className="px-3 py-2">Agente</th>
                      <th className="px-3 py-2 text-center">Atendimentos</th>
                      <th className="px-3 py-2 text-center">Tempo médio</th>
                      <th className="px-3 py-2 text-center">Pausas</th>
                      <th className="px-3 py-2 text-center">Tempo médio por dia</th>
                    </tr>
                  </thead>
                  
                  <tbody className="bg-white">
                    {dailyReport?.attendants.map((item) => (
                      <tr key={item.attendantId} className="border-t border-slate-100">
                        <td className="px-3 py-2 font-medium text-slate-900">{item.attendantName}</td>
                        <td className="px-3 py-2 text-center text-slate-600">{item.serviceRequests}</td>
                        <td className="px-3 py-2 text-center text-slate-600">{formatSeconds(item.averageServiceSeconds)}</td>
                        <td className="px-3 py-2 text-center text-slate-600">{item.pauseCount}</td>
                        <td className="px-3 py-2 text-center text-slate-600">{formatSeconds(item.totalPauseSeconds / getDaysDifference(reportStartDate, reportEndDate))}</td>
                      </tr>
                    ))}
                    {!dailyReport || reportLoading ? (
                      <tr>
                        <td className="px-3 py-6 text-slate-500" colSpan={5}>Carregando relatório...</td>
                      </tr>
                    ) : null}
                  </tbody>
                </table>
              </div>
            </div>
          </Section>
        </div>
      </main>
    </div>
  );
}
