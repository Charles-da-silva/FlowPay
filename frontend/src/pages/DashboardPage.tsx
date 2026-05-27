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

export function DashboardPage() {
  const { summary, status } = useDashboardSse();
  const [attendants, setAttendants] = useState<AttendantResponse[]>([]);
  const [queue, setQueue] = useState<ServiceRequestResponse[]>([]);
  const [activeRequests, setActiveRequests] = useState<ServiceRequestResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [finishingId, setFinishingId] = useState<number | null>(null);
  const [confirmFinishId, setConfirmFinishId] = useState<number | null>(null);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editName, setEditName] = useState("");
  const [editCategories, setEditCategories] = useState<ServiceCategory[]>([]);
  const [savingAttendantId, setSavingAttendantId] = useState<number | null>(null);
  const [deletingAttendantId, setDeletingAttendantId] = useState<number | null>(null);
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
    if (summary) {
      loadData();
    }
  }, [summary]);

  const startEdit = (attendant: AttendantResponse) => {
    setAttendantError(null);
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

    if (!editName.trim()) {
      setAttendantError("Nome do atendente é obrigatório.");
      return;
    }

    if (editCategories.length === 0) {
      setAttendantError("Selecione pelo menos uma categoria.");
      return;
    }

    setSavingAttendantId(id);
    try {
      await api.attendants.update(id, { name: editName.trim(), categories: editCategories });
      setEditingId(null);
      await loadData();
    } catch (err) {
      setAttendantError(err instanceof Error ? err.message : "Erro ao editar atendente.");
    } finally {
      setSavingAttendantId(null);
    }
  };

  const deleteAttendant = async (attendant: AttendantResponse) => {
    setAttendantError(null);

    if (attendant.activeServiceRequests > 0) {
      setAttendantError("Finalize os atendimentos em andamento antes de excluir este atendente.");
      return;
    }

    const confirmed = window.confirm(`Excluir ${attendant.name}?`);
    if (!confirmed) {
      return;
    }

    setDeletingAttendantId(attendant.id);
    try {
      await api.attendants.delete(attendant.id);
      await loadData();
    } catch (err) {
      setAttendantError(err instanceof Error ? err.message : "Erro ao excluir atendente.");
    } finally {
      setDeletingAttendantId(null);
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
    <div className="min-h-screen bg-slate-50">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto max-w-6xl px-4 py-4">
          <div className="flex items-center justify-between gap-4">
            <div>
              <h1 className="text-xl font-semibold text-slate-900">Flowpay - Painel</h1>
              <p className="mt-1 text-sm text-slate-500">
                Atualiza em tempo real quando fila e atendimentos mudam.
              </p>
            </div>
            <div
              className={`rounded-full px-3 py-1 text-xs font-medium ${
                status === "connected"
                  ? "bg-emerald-50 text-emerald-700"
                  : status === "connecting"
                    ? "bg-amber-50 text-amber-700"
                    : "bg-rose-50 text-rose-700"
              }`}
              title="Status da conexão em tempo real"
            >
              Tempo real: {sseStatusLabels[status]}
            </div>
          </div>
        </div>
      </header>

      <main className="mx-auto max-w-6xl px-4 py-6">
        <div className="mb-6 grid gap-4 lg:grid-cols-2">
          <CreateAttendantForm onSuccess={loadData} />
          <CreateServiceRequestForm onSuccess={loadData} />
        </div>

        <div className="grid gap-4 md:grid-cols-4">
          <StatCard label="Atendentes" value={summary?.totalAttendants ?? "-"} />
          <StatCard label="Disponíveis" value={summary?.availableAttendants ?? "-"} />
          <StatCard label="Ocupados" value={summary?.busyAttendants ?? "-"} />
          <StatCard label="Na fila" value={summary?.waitingServiceRequests ?? "-"} />
        </div>

        <div className="mt-6 grid gap-6 xl:grid-cols-3">
          <Section
            title="Em atendimento"
            description="Atendimentos já atribuídos a atendentes."
            right={
              <span className="text-sm text-slate-500">
                {loading ? "carregando..." : `${activeRequests.length} ativos`}
              </span>
            }
          >
            <div className="overflow-hidden rounded-xl border border-slate-200">
              <table className="w-full text-left text-sm">
                <thead className="bg-slate-50 text-slate-600">
                  <tr>
                    <th className="px-3 py-2">Cliente</th>
                    <th className="px-3 py-2">Atendente</th>
                    <th className="px-3 py-2">Categoria</th>
                    <th className="px-3 py-2">Ação</th>
                  </tr>
                </thead>
                <tbody className="bg-white">
                  {activeRequests.map((request) => (
                    <tr key={request.id} className="border-t border-slate-100">
                      <td className="px-3 py-2 font-medium text-slate-900">{request.customerName}</td>
                      <td className="px-3 py-2 text-slate-600">{request.attendantName ?? "-"}</td>
                      <td className="px-3 py-2 text-slate-600">{serviceCategoryLabels[request.category]}</td>
                      <td className="px-3 py-2">
                        {confirmFinishId === request.id ? (
                          <div className="flex gap-2">
                            <button
                              onClick={() => handleFinishRequest(request.id)}
                              disabled={finishingId === request.id}
                              className="text-xs bg-green-600 text-white px-2 py-1 rounded hover:bg-green-700 disabled:bg-slate-400"
                            >
                              {finishingId === request.id ? "..." : "Confirmar"}
                            </button>
                            <button
                              onClick={() => setConfirmFinishId(null)}
                              disabled={finishingId === request.id}
                              className="text-xs bg-slate-300 text-slate-700 px-2 py-1 rounded hover:bg-slate-400 disabled:bg-slate-400"
                            >
                              Cancelar
                            </button>
                          </div>
                        ) : (
                          <button
                            onClick={() => setConfirmFinishId(request.id)}
                            className="text-xs bg-blue-600 text-white px-3 py-1 rounded hover:bg-blue-700"
                          >
                            Finalizar
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                  {activeRequests.length === 0 ? (
                    <tr>
                      <td className="px-3 py-6 text-slate-500" colSpan={4}>
                        Nenhum atendimento em andamento.
                      </td>
                    </tr>
                  ) : null}
                </tbody>
              </table>
            </div>
          </Section>

          <Section
            title="Fila de espera"
            description="Ordem FIFO por categoria. Quando um atendente fica livre, o backend redistribui automaticamente."
            right={
              <span className="text-sm text-slate-500">
                {loading ? "carregando..." : `${queue.length} itens`}
              </span>
            }
          >
            <div className="overflow-hidden rounded-xl border border-slate-200">
              <table className="w-full text-left text-sm">
                <thead className="bg-slate-50 text-slate-600">
                  <tr>
                    <th className="px-3 py-2">Cliente</th>
                    <th className="px-3 py-2">Categoria</th>
                    <th className="px-3 py-2">Criado</th>
                  </tr>
                </thead>
                <tbody className="bg-white">
                  {queue.map((sr) => (
                    <tr key={sr.id} className="border-t border-slate-100">
                      <td className="px-3 py-2 font-medium text-slate-900">{sr.customerName}</td>
                      <td className="px-3 py-2 text-slate-600">{serviceCategoryLabels[sr.category]}</td>
                      <td className="px-3 py-2 text-slate-500">
                        {new Date(sr.createdAt).toLocaleString()}
                      </td>
                    </tr>
                  ))}
                  {queue.length === 0 ? (
                    <tr>
                      <td className="px-3 py-6 text-slate-500" colSpan={3}>
                        Nenhum atendimento em fila.
                      </td>
                    </tr>
                  ) : null}
                </tbody>
              </table>
            </div>
          </Section>

          <Section
            title="Atendentes"
            description="Mostra status, carga atual e categorias atendidas."
            right={
              <span className="text-sm text-slate-500">
                {loading ? "carregando..." : `${attendants.length} atendentes`}
              </span>
            }
          >
            {attendantError ? (
              <div className="mb-3 rounded bg-rose-50 px-3 py-2 text-sm text-rose-700">
                {attendantError}
              </div>
            ) : null}
            <div className="overflow-hidden rounded-xl border border-slate-200">
              <table className="w-full text-left text-sm">
                <thead className="bg-slate-50 text-slate-600">
                  <tr>
                    <th className="px-3 py-2">Nome</th>
                    <th className="px-3 py-2">Status</th>
                    <th className="px-3 py-2">Atendimentos</th>
                    <th className="px-3 py-2">Categorias</th>
                    <th className="px-3 py-2">Ações</th>
                  </tr>
                </thead>
                <tbody className="bg-white">
                  {attendants.map((a) => {
                    const isEditing = editingId === a.id;

                    return (
                      <tr key={a.id} className="border-t border-slate-100 align-top">
                        <td className="px-3 py-2 font-medium text-slate-900">
                          {isEditing ? (
                            <input
                              value={editName}
                              onChange={(event) => setEditName(event.target.value)}
                              className="w-36 rounded border border-slate-300 px-2 py-1 text-sm"
                            />
                          ) : (
                            a.name
                          )}
                        </td>
                        <td className="px-3 py-2 text-slate-600">{attendantStatusLabels[a.status]}</td>
                        <td className="px-3 py-2 text-slate-600">
                          {a.activeServiceRequests}/{a.maxSimultaneousCustomers}
                        </td>
                        <td className="px-3 py-2 text-slate-500">
                          {isEditing ? (
                            <div className="space-y-1">
                              {categories.map((category) => (
                                <label key={category} className="flex items-center gap-2">
                                  <input
                                    type="checkbox"
                                    checked={editCategories.includes(category)}
                                    onChange={() => toggleEditCategory(category)}
                                  />
                                  <span>{serviceCategoryLabels[category]}</span>
                                </label>
                              ))}
                            </div>
                          ) : (
                            a.categories.map((category) => serviceCategoryLabels[category]).join(", ")
                          )}
                        </td>
                        <td className="px-3 py-2">
                          {isEditing ? (
                            <div className="flex flex-wrap gap-2">
                              <button
                                onClick={() => saveAttendant(a.id)}
                                disabled={savingAttendantId === a.id}
                                className="rounded bg-green-600 px-2 py-1 text-xs text-white hover:bg-green-700 disabled:bg-slate-400"
                              >
                                {savingAttendantId === a.id ? "Salvando..." : "Salvar"}
                              </button>
                              <button
                                onClick={() => setEditingId(null)}
                                disabled={savingAttendantId === a.id}
                                className="rounded bg-slate-300 px-2 py-1 text-xs text-slate-700 hover:bg-slate-400 disabled:bg-slate-400"
                              >
                                Cancelar
                              </button>
                            </div>
                          ) : (
                            <div className="flex flex-wrap gap-2">
                              <button
                                onClick={() => startEdit(a)}
                                className="rounded bg-blue-600 px-2 py-1 text-xs text-white hover:bg-blue-700"
                              >
                                Editar
                              </button>
                              <button
                                onClick={() => deleteAttendant(a)}
                                disabled={deletingAttendantId === a.id}
                                className="rounded bg-rose-600 px-2 py-1 text-xs text-white hover:bg-rose-700 disabled:bg-slate-400"
                              >
                                {deletingAttendantId === a.id ? "Excluindo..." : "Excluir"}
                              </button>
                            </div>
                          )}
                        </td>
                      </tr>
                    );
                  })}
                  {attendants.length === 0 ? (
                    <tr>
                      <td className="px-3 py-6 text-slate-500" colSpan={5}>
                        Nenhum atendente cadastrado ainda.
                      </td>
                    </tr>
                  ) : null}
                </tbody>
              </table>
            </div>
          </Section>
        </div>
      </main>
    </div>
  );
}
