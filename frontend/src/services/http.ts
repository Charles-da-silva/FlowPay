import axios from "axios";
import type { AxiosError, AxiosInstance } from "axios";

// Cliente HTTP usando Axios com configuracao centralizada.
// Comenta-se o "por quê": um lugar so para baseUrl/headers evita repeticao
// e facilita interceptadores de autenticacao/logging/erros globalmente.

const DEFAULT_BASE_URL = "http://localhost:8080";

export function getApiBaseUrl(): string {
  return import.meta.env.VITE_API_BASE_URL ?? DEFAULT_BASE_URL;
}

// Instancia Axios configurada com timeout e base URL.
const axiosInstance: AxiosInstance = axios.create({
  baseURL: getApiBaseUrl(),
  timeout: 10000,
  headers: {
    "Content-Type": "application/json",
    Accept: "application/json",
  },
});

// Interceptador de erro: padroniza respostas de erro.
axiosInstance.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    const data = error.response?.data as { detail?: string; title?: string } | undefined;
    const message = data?.detail || data?.title || error.response?.statusText || error.message;
    return Promise.reject(new Error(message));
  }
);

export async function httpGet<T>(path: string): Promise<T> {
  const response = await axiosInstance.get<T>(path);
  return response.data;
}

export async function httpPost<TResponse, TBody>(path: string, body: TBody): Promise<TResponse> {
  const response = await axiosInstance.post<TResponse>(path, body);
  return response.data;
}

export async function httpPut<TResponse, TBody>(path: string, body: TBody): Promise<TResponse> {
  const response = await axiosInstance.put<TResponse>(path, body);
  return response.data;
}

export async function httpPatch<T>(path: string): Promise<T> {
  const response = await axiosInstance.patch<T>(path);
  return response.data;
}

export async function httpDelete(path: string): Promise<void> {
  await axiosInstance.delete(path);
}
