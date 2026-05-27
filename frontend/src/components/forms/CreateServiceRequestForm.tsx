import { useState } from "react";
import { api } from "../../services/api";
import type { ServiceCategory, ServiceRequestResponse } from "../../types/api";
import { serviceCategoryLabels } from "../../utils/labels";

interface CreateServiceRequestFormProps {
  onSuccess?: () => void;
}

export function CreateServiceRequestForm({ onSuccess }: CreateServiceRequestFormProps) {
  const [customerName, setCustomerName] = useState("");
  const [category, setCategory] = useState<ServiceCategory>("CARD_ISSUES");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<ServiceRequestResponse | null>(null);

  const categories: ServiceCategory[] = ["CARD_ISSUES", "LOAN_CONTRACTING", "OTHER_SUBJECTS"];

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSuccess(null);

    if (!customerName.trim()) {
      setError("Nome do cliente é obrigatório");
      return;
    }

    setLoading(true);
    try {
      const result = await api.serviceRequests.create({
        customerName: customerName.trim(),
        category,
      });
      setSuccess(result);
      setCustomerName("");
      setTimeout(() => setSuccess(null), 4000);
      onSuccess?.();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Erro ao criar atendimento");
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4 rounded-lg border border-slate-200 bg-white p-4">
      <h3 className="text-sm font-semibold text-slate-900">Criar novo atendimento</h3>

      <div>
        <label className="block text-sm font-medium text-slate-700">Nome do cliente</label>
        <input
          type="text"
          value={customerName}
          onChange={(e) => setCustomerName(e.target.value)}
          placeholder="Ex: Maria Santos"
          disabled={loading}
          className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm placeholder-slate-400 focus:border-blue-500 focus:outline-none disabled:bg-slate-50"
        />
      </div>

      <div>
        <label className="block text-sm font-medium text-slate-700">Categoria</label>
        <select
          value={category}
          onChange={(e) => setCategory(e.target.value as ServiceCategory)}
          disabled={loading}
          className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none disabled:bg-slate-50"
        >
          {categories.map((cat) => (
            <option key={cat} value={cat}>
              {serviceCategoryLabels[cat]}
            </option>
          ))}
        </select>
      </div>

      {error && <div className="rounded bg-rose-50 px-3 py-2 text-sm text-rose-700">{error}</div>}
      {success && (
        <div className="rounded bg-emerald-50 px-3 py-2 text-sm text-emerald-700">
          {success.attendantId
            ? `Atribuído a ${success.attendantName}.`
            : "Adicionado à fila: não há atendente disponível."}
        </div>
      )}

      <button
        type="submit"
        disabled={loading}
        className="w-full rounded bg-green-600 px-4 py-2 text-sm font-medium text-white hover:bg-green-700 disabled:bg-slate-400"
      >
        {loading ? "Criando..." : "Criar atendimento"}
      </button>
    </form>
  );
}
