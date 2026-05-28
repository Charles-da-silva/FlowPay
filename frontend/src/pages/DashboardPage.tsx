import { useEffect, useState } from "react";
import { api } from "../services/api";
import type { AttendantResponse, ServiceCategory, ServiceRequestResponse } from "../types/api";
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

  useEffect(() => {
    setLoading(true);
    loadData();
  }, []);

  useEffect(() => {
    const intervalId = window.setInterval(() => setNow(Date.now()), 1000);
    return () => window.clearInterval(intervalId);
  }, []);

  useEffect(() => {
    if (summary) loadData();
  }, [summary]);

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

  return (
    <div className="min-h-screen bg-slate-100">
      <header className="border-b border-slate-200 bg-white shadow-sm">
        <div className="mx-auto max-w-7xl px-4 py-4">
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

      <main className="mx-auto max-w-7xl px-4 py-6">
        <section className="rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-5">
            <StatCard label="Service Level" value={summary ? formatServiceLevel(summary.todayServiceLevel) : "-"} />
            <StatCard label="Atendimentos hoje" value={summary?.todayServiceRequests ?? "-"} />
            <StatCard label="Entraram em espera" value={summary?.todayWaitedServiceRequests ?? "-"} />
            <button
              type="button"
              onClick={() => setFormPanel((current) => (current === "service" ? null : "service"))}
              className="rounded-lg border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm font-semibold text-emerald-800 shadow-sm transition hover:bg-emerald-100"
            >
              Novo atendimento
            </button>
            <button
              type="button"
              onClick={() => setFormPanel((current) => (current === "agent" ? null : "agent"))}
              className="rounded-lg border border-blue-200 bg-blue-50 px-4 py-3 text-sm font-semibold text-blue-800 shadow-sm transition hover:bg-blue-100"
            >
              Novo agente
            </button>
          </div>

          {formPanel ? (
            <div className="mt-4">
              {formPanel === "service" ? (
                <CreateServiceRequestForm
                  onSuccess={() => {
                    setFormPanel(null);
                    loadData();
                  }}
                />
              ) : (
                <CreateAttendantForm
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
          <StatCard label="Agentes ocupados" value={summary?.busyAttendants ?? "-"} />
          <StatCard label="Clientes na fila" value={summary?.waitingServiceRequests ?? "-"} />
          <StatCard label="Clientes em atendimento" value={summary?.inProgressServiceRequests ?? "-"} />
        </div>

        <div className="mt-6 grid gap-6 lg:grid-cols-2">
          <Section
            title="Em atendimento"
            description="Clientes já atribuídos a agentes."
            right={<span className="text-sm text-slate-500">{loading ? "carregando..." : `${activeRequests.length} ativos`}</span>}
          >
            <div className="overflow-x-auto rounded-lg border border-slate-200">
              <table className="w-full min-w-[560px] text-left text-sm">
                <thead className="bg-slate-50 text-slate-600">
                  <tr>
                    <th className="px-3 py-2">Cliente</th>
                    <th className="px-3 py-2">Agente</th>
                    <th className="px-3 py-2 text-center">Tempo</th>
                    <th className="px-3 py-2">Ação</th>
                  </tr>
                </thead>
                <tbody className="bg-white">
                  {activeRequests.map((request) => (
                    <tr key={request.id} className="border-t border-slate-100">
                      <td className="px-3 py-2 font-medium text-slate-900">{request.customerName}</td>
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
                      <td className="px-3 py-6 text-slate-500" colSpan={4}>Nenhum atendimento em andamento.</td>
                    </tr>
                  ) : null}
                </tbody>
              </table>
            </div>
          </Section>

          <Section
            title="Fila de espera"
            description="Clientes aguardando disponibilidade de agentes elegíveis."
            right={<span className="text-sm text-slate-500">{loading ? "carregando..." : `${queue.length} clientes`}</span>}
          >
            <div className="overflow-x-auto rounded-lg border border-slate-200">
              <table className="w-full min-w-[500px] text-left text-sm">
                <thead className="bg-slate-50 text-slate-600">
                  <tr>
                    <th className="px-3 py-2">Cliente</th>
                    <th className="px-3 py-2">Categoria</th>
                    <th className="px-3 py-2 text-center">Espera</th>
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
        </div>

        <div className="mt-6">
          <Section
            title="Agentes logados"
            description="Carga, tempo disponível ou em pausa, categorias e ações operacionais."
            right={<span className="text-sm text-slate-500">{loading ? "carregando..." : `${attendants.length} agentes`}</span>}
          >
            {attendantError ? <div className="mb-3 rounded bg-rose-50 px-3 py-2 text-sm text-rose-700">{attendantError}</div> : null}
            <div className="overflow-x-auto rounded-lg border border-slate-200">
              <table className="w-full min-w-[900px] text-left text-sm">
                <thead className="bg-slate-50 text-slate-600">
                  <tr>
                    <th className="px-3 py-2">Nome</th>
                    <th className="px-3 py-2">Status</th>
                    <th className="px-3 py-2 text-center">Atendimentos</th>
                    <th className="px-3 py-2 text-center">Tempo</th>
                    <th className="px-3 py-2">Categorias</th>
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
                              <button onClick={() => togglePause(attendant)} disabled={(attendant.status !== "AVAILABLE" && attendant.status !== "PAUSED") || pauseActionId === attendant.id} className="rounded bg-amber-500 px-2 py-1 text-xs text-white hover:bg-amber-600 disabled:bg-slate-300">
                                {pauseActionId === attendant.id ? "..." : attendant.status === "PAUSED" ? "Voltar" : "Pausa"}
                              </button>
                              <button onClick={() => { setAttendantError(null); setEditingId(null); setConfirmDeleteId(attendant.id); }} className="rounded bg-rose-600 px-2 py-1 text-xs text-white hover:bg-rose-700">Excluir</button>
                            </div>
                          )}
                        </td>
                      </tr>
                    );
                  })}
                  {attendants.length === 0 ? (
                    <tr>
                      <td className="px-3 py-6 text-slate-500" colSpan={6}>Nenhum agente cadastrado ainda.</td>
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
                    <th className="px-3 py-2">Agente</th>
                    <th className="px-3 py-2 text-center">Atendimentos</th>
                    <th className="px-3 py-2 text-center">Tempo médio</th>
                    <th className="px-3 py-2 text-center">Pausas</th>
                    <th className="px-3 py-2 text-center">Tempo em pausa</th>
                  </tr>
                </thead>
                <tbody className="bg-white">
                  {summary?.todayByAttendant.map((item) => (
                    <tr key={item.attendantId} className="border-t border-slate-100">
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
      </main>
    </div>
  );
}
