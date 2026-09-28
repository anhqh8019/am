import { clearSession, readToken } from './auth';

const API_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api';

export async function api<T>(path:string, init:RequestInit = {}):Promise<T> {
  const token = readToken();
  const response = await fetch(`${API_URL}${path}`, {
    ...init,
    headers: {
      ...(init.body ? {'Content-Type':'application/json'} : {}),
      ...(token ? {Authorization:`Bearer ${token}`} : {}),
      ...init.headers
    }
  });
  if (response.status === 401) {
    clearSession();
    throw new Error('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.');
  }
  if (!response.ok) {
    let message = 'Không thể thực hiện yêu cầu.';
    try { const body = await response.json(); message = body.detail ?? body.message ?? message; } catch { /* empty */ }
    throw new Error(message);
  }
  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}
