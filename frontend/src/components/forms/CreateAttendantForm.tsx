import { useState } from "react";
import { api } from "../../services/api";
import type { ServiceCategory } from "../../types/api";
import { serviceCategoryLabels } from "../../utils/labels";

interface CreateAttendantFormProps {
  onSuccess?: () => void;
}

export function CreateAttendantForm({ onSuccess }: CreateAttendantFormProps) {
  const [name, setName] = useState("");
  const [selectedCategories, setSelectedCategories] = useState<ServiceCategory[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);

  const categories: ServiceCategory[] = ["CARD_ISSUES", "LOAN_CONTRACTING", "OTHER_SUBJECTS"];

  const toggleCategory = (category: ServiceCategory) => {
    setSelectedCategories((prev) =>
      prev.includes(category) ? prev.filter((c) => c !== category) : [...prev, category]
    );
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSuccess(false);

    if (!name.trim()) {
      setError("Nome do atendente é obrigatório");
      return;
    }

    if (selectedCategories.length === 0) {
      setError("Selecione pelo menos uma categoria");
      return;
    }

    setLoading(true);
    try {
      await api.attendants.create({ name: name.trim(), categories: selectedCategories });
      setSuccess(true);
      setName("");
      setSelectedCategories([]);
      setTimeout(() => setSuccess(false), 3000);
      onSuccess?.();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Erro ao criar atendente");
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4 rounded-lg border border-slate-200 bg-white p-4">
      <h3 className="text-sm font-semibold text-slate-900">Criar novo atendente</h3>

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
          Atendente criado com sucesso.
        </div>
      )}

      <button
        type="submit"
        disabled={loading}
        className="w-full rounded bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:bg-slate-400"
      >
        {loading ? "Criando..." : "Criar atendente"}
      </button>
    </form>
  );
}
