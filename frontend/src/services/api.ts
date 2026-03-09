import axios, { AxiosError } from 'axios';
import { ApiClientError, ApiError } from '../types';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api',
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    console.error('API Error:', error.response?.data || error.message);
    return Promise.reject(error);
  }
);

export function toApiClientError(error: unknown): ApiClientError {
  const fallbackMessage = 'Nao foi possivel concluir a requisicao agora.';

  if (!axios.isAxiosError(error)) {
    return {
      message: fallbackMessage,
    };
  }

  const axiosError = error as AxiosError<ApiError>;
  const status = axiosError.response?.status;
  const details = axiosError.response?.data?.details;
  const backendMessage = axiosError.response?.data?.error;

  if (!axiosError.response) {
    return {
      code: axiosError.code,
      message: 'Nao foi possivel conectar ao backend do Lume.',
    };
  }

  if (status === 400 && details) {
    return {
      status,
      code: axiosError.code,
      details,
      message: 'Existem campos invalidos na requisicao enviada.',
    };
  }

  if (status === 404) {
    return {
      status,
      code: axiosError.code,
      message: backendMessage || 'O recurso solicitado nao foi encontrado.',
    };
  }

  if (status === 403) {
    return {
      status,
      code: axiosError.code,
      message: backendMessage || 'Voce nao possui permissao para acessar este recurso no workspace atual.',
    };
  }

  if (status === 401) {
    return {
      status,
      code: axiosError.code,
      message: backendMessage || 'Sua sessao nao esta autenticada.',
    };
  }

  if (status === 409) {
    return {
      status,
      code: axiosError.code,
      message: backendMessage || 'A aplicacao ainda precisa concluir o setup inicial.',
    };
  }

  if (status === 502 || status === 503 || status === 504) {
    return {
      status,
      code: axiosError.code,
      message: 'O backend do Lume esta indisponivel no momento.',
    };
  }

  if (status && status >= 500) {
    return {
      status,
      code: axiosError.code,
      message: 'O backend do Lume respondeu com erro interno.',
    };
  }

  return {
    status,
    code: axiosError.code,
    details,
    message: backendMessage || fallbackMessage,
  };
}

export default api;
