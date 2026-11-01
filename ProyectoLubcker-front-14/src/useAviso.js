import { useState, useRef, useEffect, useCallback } from 'react';

// Reemplaza a window.alert(): muestra un aviso dentro de la página que se oculta solo.
// Uso:  const [aviso, mostrarAviso] = useAviso();
//       ...  {aviso}  (en el JSX)   y   mostrarAviso('Texto', 'error')
export function useAviso(duracionMs = 3000) {
  const [datos, setDatos] = useState(null);
  const temporizador = useRef(null);

  useEffect(() => () => clearTimeout(temporizador.current), []);

  const mostrar = useCallback((texto, tipo = 'ok') => {
    clearTimeout(temporizador.current);
    setDatos({ texto, tipo });
    temporizador.current = setTimeout(() => setDatos(null), duracionMs);
  }, [duracionMs]);

  const aviso = datos ? (
    <div className={`aviso-flotante aviso-${datos.tipo}`} role="status">
      {datos.texto}
    </div>
  ) : null;

  return [aviso, mostrar];
}
