import React, { useState, useEffect } from 'react';
import { useSearchParams, useLocation } from 'react-router-dom';
import './CatalogoInteractivo.css';
import Productos from './Productos';
import { useFavoritos } from '../FavoritosContext';
import { API_URL } from '../config';

// La categoría y la búsqueda viven en la URL (/?categoria=limpieza, /?q=jabon):
// así el buscador del navbar y el menú lateral pueden llevar directo a un resultado,
// y el resultado se puede compartir o recargar.
export default function CatalogoInteractivo({ agregarAlCarrito, seccionFija, setSeccionFija }) {
  const [parametros, setParametros] = useSearchParams();
  const location = useLocation();
  const [categorias, setCategorias] = useState([]);
  const { favoritos } = useFavoritos();

  const filtroSeleccionado = parametros.get('categoria') || '';
  const busqueda = parametros.get('q') || '';

  // Las categorías del menú salen de la base de datos
  useEffect(() => {
    fetch(`${API_URL}/categorias`)
      .then((res) => (res.ok ? res.json() : []))
      .then(setCategorias)
      .catch(() => setCategorias([]));
  }, []);

  // Al llegar con una búsqueda, una categoría o desde "Productos" del menú, baja al catálogo
  useEffect(() => {
    if (busqueda || filtroSeleccionado || location.state?.verCatalogo) {
      setSeccionFija('todos');
      const timer = setTimeout(() => {
        document.getElementById('catalogo-ancla')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
      }, 50);
      return () => clearTimeout(timer);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [busqueda, filtroSeleccionado, location.state?.verCatalogo]);

  const cambiarParametros = (cambios) => {
    const nuevos = new URLSearchParams(parametros);
    Object.entries(cambios).forEach(([clave, valor]) => {
      if (valor) nuevos.set(clave, valor);
      else nuevos.delete(clave);
    });
    setParametros(nuevos, { replace: true });
  };

  const elegirCategoria = (valor) => {
    cambiarParametros({ categoria: valor });
    setSeccionFija('todos');
  };

  const verTodos = () => {
    setParametros({}, { replace: true });
    setSeccionFija('todos');
  };

  const verIntereses = () => {
    setParametros({}, { replace: true });
    setSeccionFija('intereses');
  };

  const enIntereses = seccionFija === 'intereses';
  const enTodos = seccionFija === 'todos' && !filtroSeleccionado && !busqueda;

  return (
    <div className="catalogo-contenedor">
      <div className="catalogo-cabecera">
        <h2>Catálogo de Productos</h2>
        <p>Encontrá todo para la higiene mecánica</p>
      </div>

      {/* Menú del catálogo: categorías a la izquierda, favoritos a la derecha */}
      <nav className="cat-menu" aria-label="Menú del catálogo">
        <div className="cat-menu-lista">
          <button
            type="button"
            className={`cat-menu-item ${enTodos ? 'activo' : ''}`}
            onClick={verTodos}
            aria-pressed={enTodos}
          >
            Todos
          </button>

          {categorias.map((c) => {
            const valor = c.nombre.toLowerCase();
            const activa = !enIntereses && filtroSeleccionado === valor;
            return (
              <button
                key={c.idCategoria}
                type="button"
                className={`cat-menu-item ${activa ? 'activo' : ''}`}
                onClick={() => elegirCategoria(valor)}
                aria-pressed={activa}
              >
                {c.nombre}
              </button>
            );
          })}
        </div>

        <button
          type="button"
          className={`cat-menu-favoritos ${enIntereses ? 'activo' : ''}`}
          onClick={verIntereses}
          aria-pressed={enIntereses}
          aria-label="Tus intereses"
        >
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" />
          </svg>
          <span className="cat-menu-favoritos-texto">Tus intereses</span>
          {favoritos.length > 0 && <span className="cat-menu-contador">{favoritos.length}</span>}
        </button>
      </nav>

      {busqueda && !enIntereses && (
        <div className="catalogo-busqueda-activa">
          Resultados para <strong>“{busqueda}”</strong>
          <button type="button" onClick={() => cambiarParametros({ q: '' })} aria-label="Quitar búsqueda">
            ✕ Quitar búsqueda
          </button>
        </div>
      )}

      <div className="catalogo-contenido">
        <Productos
          agregarAlCarrito={agregarAlCarrito}
          filtro={filtroSeleccionado}
          busqueda={busqueda}
          soloIntereses={enIntereses}
        />
      </div>
    </div>
  );
}