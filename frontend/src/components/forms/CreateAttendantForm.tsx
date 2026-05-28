import { useEffect, useState } from "react";
import { api } from "../../services/api";
import type { ServiceCategory } from "../../types/api";
import { serviceCategoryLabels } from "../../utils/labels";

interface CreateAttendantFormProps {
  onSuccess?: () => void;
  onCancel?: () => void;
}

const namePattern = /^[\p{L} ]+$/u;

function normalizeName(value: string) {
  return value.trim().replace(/\s+/g, " ");
}

function isValidPersonName(value: string) {
  const normalized = normalizeName(value);
  return normalized.length >= 2 && namePattern.test(normalized);
}

export function CreateAttendantForm({ onSuccess, onCancel }: CreateAttendantFormProps) {
  const [name, setName] = useState("");
  const [badge, setBadge] = useState("");
  const [selectedCategories, setSelectedCategories] = useState<ServiceCategory[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);

  const categories: ServiceCategory[] = ["CARD_ISSUES", "LOAN_CONTRACTING", "OTHER_SUBJECTS"];
  const badgePattern = /^[A-Za-z0-9-]{2,20}$/;

  useEffect(() => {
    if (!error) return;
    const timeoutId = window.setTimeout(() => setError(null), 5000);
    return () => window.clearTimeout(timeoutId);
  }, [error]);

  const toggleCategory = (category: ServiceCategory) => {
    setSelectedCategories((prev) =>
      prev.includes(category) ? prev.filter((c) => c !== category) : [...prev, category]
    );
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSuccess(false);

    if (!isValidPersonName(name)) {
      setError("Use apenas letras e espaços no nome do agente.");
      return;
    }

    if (!badgePattern.test(badge.trim())) {
      setError("Informe um Badge com 2 a 20 letras, números ou hífen.");
      return;
    }

    if (selectedCategories.length === 0) {
      setError("Selecione pelo menos uma categoria.");
      return;
    }

    setLoading(true);
    try {
      await api.attendants.create({
        name: normalizeName(name),
        badge: badge.trim().toUpperCase(),
        categories: selectedCategories,
      });
      setSuccess(true);
      setName("");
      setBadge("");
      setSelectedCategories([]);
      setTimeout(() => setSuccess(false), 3000);
      onSuccess?.();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Erro ao criar agente.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4 rounded-lg border border-slate-200 bg-white p-4">
      <h3 className="text-lg font-semibold text-slate-900">Logar novo agente</h3>

      <div>
        <label className="block text-sm font-medium text-slate-700">Nome</label>
        <input
          type="text"
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder="Ex: João Silva"
          disabled={loading}
          className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm placeholder-slate-400 focus:border-blue-500 focus:outline-none disabled:bg-slate-50"
        />
      </div>

      <div>
        <label className="block text-sm font-medium text-slate-700">Badge</label>
        <input
          type="text"
          value={badge}
          onChange={(e) => setBadge(e.target.value)}
          placeholder="Ex: AG001"
          disabled={loading}
          className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm uppercase placeholder:normal-case placeholder-slate-400 focus:border-blue-500 focus:outline-none disabled:bg-slate-50"
        />
      </div>

      <div>
        <label className="block text-sm font-medium text-slate-700 mb-2">Categorias atendidas</label>
        <div className="space-y-2">
          {categories.map((cat) => (
            <label key={cat} className="flex items-center gap-2">
              <input
                type="checkbox"
                checked={selectedCategories.includes(cat)}
                onChange={() => toggleCategory(cat)}
                disabled={loading}
                className="rounded border-slate-300"
              />
              <span className="text-sm text-slate-700">{serviceCategoryLabels[cat]}</span>
            </label>
          ))}
        </div>
      </div>

      {error && <div className="rounded bg-rose-50 px-3 py-2 text-sm text-rose-700">{error}</div>}
      {success && (
        <div className="rounded bg-emerald-50 px-3 py-2 text-sm text-emerald-700">
          Agente criado com sucesso.
        </div>
      )}

      <div className="grid gap-2 sm:grid-cols-2">
        <button
          type="submit"
          disabled={loading}
          className="rounded bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:bg-slate-400"
        >
          {loading ? "Criando..." : "Criar agente"}
        </button>
        <button
          type="button"
          onClick={onCancel}
          disabled={loading}
          className="rounded bg-slate-200 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-300 disabled:bg-slate-100"
        >
          Cancelar
        </button>
      </div>
    </form>
  );
}
