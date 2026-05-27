import { useEffect, useRef, useState } from "react";
import type { DashboardSummaryResponse } from "../types/api";
import { api } from "../services/api";
import { getApiBaseUrl } from "../services/http";

/**
 * Hook para atualizar dashboard em tempo real via SSE.
 *
 * Como explicar em entrevista:
 * - "Abro um EventSource apontando pro endpoint /stream"
 * - "Quando chega um evento, atualizo o state local do React"
 * - "Se SSE falhar, eu ainda tenho o fallback de carregar o summary via HTTP"
 */
export function useDashboardSse() {
  const [summary, setSummary] = useState<DashboardSummaryResponse | null>(null);
  const [status, setStatus] = useState<"connecting" | "connected" | "disconnected">("connecting");
  const sourceRef = useRef<EventSource | null>(null);

  useEffect(() => {
    let cancelled = false;

    // Snapshot inicial (HTTP): garante que a tela carrega mesmo antes do primeiro evento SSE.
    api.dashboard
      .summary()
      .then((data) => {
        if (!cancelled) setSummary(data);
      })
      .catch(() => {
        // Se falhar, SSE pode ainda funcionar; deixamos a UI mostrar erro/estado.
      });

    const url = `${getApiBaseUrl()}/api/dashboard/stream`;
    const source = new EventSource(url);
    sourceRef.current = source;

    source.onopen = () => {
      if (!cancelled) setStatus("connected");
    };

    source.onerror = () => {
      if (!cancelled) setStatus("disconnected");
      // Comentario: EventSource tenta reconectar automaticamente por padrao.
    };

    source.addEventListener("dashboard-update", (event) => {
      try {
        const data = JSON.parse((event as MessageEvent).data) as DashboardSummaryResponse;
        if (!cancelled) setSummary(data);
      } catch {
        // Ignora payload invalido.
      }
    });

    return () => {
      cancelled = true;
      source.close();
      sourceRef.current = null;
    };
  }, []);

  return { summary, status };
}

