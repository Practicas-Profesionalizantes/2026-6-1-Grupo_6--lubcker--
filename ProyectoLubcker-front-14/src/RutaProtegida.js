import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from './AuthContext';

// Páginas que requieren sesión (historial, panel, etc.).
// Si no hay usuario, manda a /login y recuerda a dónde quería ir.
export function SoloConSesion({ children }) {
  const { usuario } = useAuth();
  const location = useLocation();

  if (!usuario) {
    return <Navigate to="/login" replace state={{ desde: location.pathname }} />;
  }
  return children;
}

// Páginas solo para administradores (panel de productos, etc.).
// Sin sesión va a /login; con sesión pero sin rol ADMIN vuelve al inicio.
// OJO: esto solo oculta la pantalla. La protección real tiene que estar en el backend.
export function SoloAdmin({ children }) {
  const { usuario } = useAuth();
  const location = useLocation();

  if (!usuario) {
    return <Navigate to="/login" replace state={{ desde: location.pathname }} />;
  }
  if (usuario.rol !== 'ADMIN') {
    return <Navigate to="/" replace />;
  }
  return children;
}

// Páginas solo para quien NO inició sesión (login y registro).
// Si ya hay usuario, no tiene sentido mostrarlas: vuelve al inicio.
export function SoloSinSesion({ children }) {
  const { usuario } = useAuth();

  if (usuario) {
    return <Navigate to="/" replace />;
  }
  return children;
}