import axios from 'axios';
import { API_BASE_URL } from '../config/api';
import { limparSessao, obterSessao } from '../utils/gerenciadorSessao';

const clienteAxios = axios.create({
  baseURL: API_BASE_URL,
  headers: { 'Content-Type': 'application/json' },
});

clienteAxios.interceptors.request.use((config) => {
  const sessao = obterSessao();
  if (sessao?.token) {
    config.headers.Authorization = `Bearer ${sessao.token}`;
  }
  return config;
});

clienteAxios.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status;

    if (status === 401) {
      limparSessao();
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }

    if (status === 403) {
      if (window.location.pathname !== '/acesso-negado') {
        window.location.href = '/acesso-negado';
      }
    }

    return Promise.reject(error);
  },
);

export default clienteAxios;
