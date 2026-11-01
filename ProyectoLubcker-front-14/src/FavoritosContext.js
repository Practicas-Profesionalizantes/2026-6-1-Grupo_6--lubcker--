import { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { useAuth } from './AuthContext';
import { API_URL } from './config';

// Favoritos (❤️):
//  - Con sesión se guardan en la cuenta (backend), así se ven desde cualquier dispositivo.
//  - Sin sesión se guardan en el navegador, y al iniciar sesión se suman a la cuenta.

const CLAVE_LOCAL = 'lubcker_favoritos';

const FavoritosContext = createContext(null);

function leerLocales() {
  try {
    const guardado = localStorage.getItem(CLAVE_LOCAL);
    const lista = guardado ? JSON.parse(guardado) : [];
    return Array.isArray(lista) ? lista.filter(Number.isInteger) : [];
  } catch {
    return [];
  }
}

function guardarLocales(ids) {
  try {
    localStorage.setItem(CLAVE_LOCAL, JSON.stringify(ids));
  } catch {}
}

export function FavoritosProvider({ children }) {
  const { usuario } = useAuth();
  const conSesion = Boolean(usuario?.token);
  const [favoritos, setFavoritos] = useState(() => (conSesion ? [] : leerLocales()));

  // Al iniciar sesión: sube los favoritos del navegador a la cuenta y trae la lista completa
  useEffect(() => {
    if (!conSesion) {
      setFavoritos(leerLocales());
      return;
    }
    const locales = leerLocales();
    fetch(`${API_URL}/favoritos/sincronizar`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(locales),
    })
      .then((res) => (res.ok ? res.json() : Promise.reject()))
      .then((ids) => {
        setFavoritos(ids);
        guardarLocales([]); // ya quedaron en la cuenta
      })
      .catch(() => setFavoritos(locales));
  }, [conSesion, usuario?.idUsuario]);

  const esFavorito = useCallback((id) => favoritos.includes(id), [favoritos]);

  const alternarFavorito = useCallback(
    (id) => {
      const quitar = favoritos.includes(id);
      const nuevos = quitar ? favoritos.filter((f) => f !== id) : [...favoritos, id];
      setFavoritos(nuevos); // se actualiza la pantalla al instante

      if (!conSesion) {
        guardarLocales(nuevos);
        return;
      }
      fetch(`${API_URL}/favoritos/${id}`, { method: quitar ? 'DELETE' : 'PUT' })
        .then((res) => {
          if (!res.ok) throw new Error();
        })
        .catch(() => setFavoritos(favoritos)); // si falla, se vuelve atrás
    },
    [favoritos, conSesion]
  );

  return (
    <FavoritosContext.Provider value={{ favoritos, esFavorito, alternarFavorito }}>
      {children}
    </FavoritosContext.Provider>
  );
}

export const useFavoritos = () => useContext(FavoritosContext);
