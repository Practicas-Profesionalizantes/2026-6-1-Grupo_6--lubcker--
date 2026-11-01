// Agrega el token de sesión a todas las llamadas al backend, en un solo lugar.
// Se instala una vez en index.js: después de eso cualquier fetch() hacia la API
// lleva "Authorization: Bearer <token>" si hay una sesión iniciada.

import { API_URL } from './config';

const CLAVE_STORAGE = 'lubcker_usuario'; // misma clave que usa AuthContext
export const EVENTO_SESION_EXPIRADA = 'lubcker:sesion-expirada';


function leerToken() {
  try {
    const guardado = localStorage.getItem(CLAVE_STORAGE);
    return guardado ? JSON.parse(guardado).token || null : null;
  } catch {
    return null;
  }
}

export function instalarFetchConToken() {
  const fetchOriginal = window.fetch.bind(window);

  window.fetch = async (entrada, opciones = {}) => {
    const url = typeof entrada === 'string' ? entrada : entrada.url;
    const esApi = url.startsWith(API_URL);
    const token = esApi ? leerToken() : null;

    if (token) {
      const headers = new Headers(opciones.headers || {});
      headers.set('Authorization', `Bearer ${token}`);
      opciones = { ...opciones, headers };
    }

    const respuesta = await fetchOriginal(entrada, opciones);

    // 401 con token enviado = la sesión venció (o el usuario ya no existe): se cierra sesión
    if (token && respuesta.status === 401) {
      window.dispatchEvent(new Event(EVENTO_SESION_EXPIRADA));
    }
    return respuesta;
  };
}
