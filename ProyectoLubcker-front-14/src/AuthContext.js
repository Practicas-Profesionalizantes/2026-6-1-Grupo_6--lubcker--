import { createContext, useContext, useState, useEffect } from 'react';
import { EVENTO_SESION_EXPIRADA } from './api';

const AuthContext = createContext(null);
const CLAVE_STORAGE = 'lubcker_usuario';

export function AuthProvider({ children }) {
  // Se lee de localStorage al arrancar para que la sesión sobreviva a recargar la página.
  const [usuario, setUsuario] = useState(() => {
    try {
      const guardado = localStorage.getItem(CLAVE_STORAGE);
      const datos = guardado ? JSON.parse(guardado) : null;
      // Sesiones guardadas antes de que existieran los tokens: no sirven, hay que iniciar sesión de nuevo
      return datos && datos.token ? datos : null;
    } catch {
      return null;
    }
  });

  const iniciarSesion = (datosUsuario) => {
    setUsuario(datosUsuario);
    try {
      localStorage.setItem(CLAVE_STORAGE, JSON.stringify(datosUsuario));
    } catch {}
  };

  // Actualiza datos del usuario en sesión (ej: después de editar el perfil)
  const actualizarUsuario = (cambios) => {
    setUsuario((actual) => {
      const nuevo = { ...actual, ...cambios };
      try {
        localStorage.setItem(CLAVE_STORAGE, JSON.stringify(nuevo));
      } catch {}
      return nuevo;
    });
  };

  const cerrarSesion = () => {
    setUsuario(null);
    try {
      localStorage.removeItem(CLAVE_STORAGE);
    } catch {}
  };

  // Si el backend rechaza el token (venció), se cierra la sesión y las rutas protegidas mandan al login
  useEffect(() => {
    window.addEventListener(EVENTO_SESION_EXPIRADA, cerrarSesion);
    return () => window.removeEventListener(EVENTO_SESION_EXPIRADA, cerrarSesion);
  }, []);

  return (
    <AuthContext.Provider value={{ usuario, iniciarSesion, cerrarSesion, actualizarUsuario }}>
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => useContext(AuthContext);