import React, { useState, useEffect, useRef } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { useAuth } from '../AuthContext';
import { useAviso } from '../useAviso';
import Estrellas from './Estrellas';
import './Resenas.css';
import { API_URL } from '../config';


// Reseñas de un producto: listado, promedio y formulario.
// Solo puede reseñar quien compró el producto (lo valida el backend; acá se adapta la pantalla).
export default function Resenas({ idProducto }) {
  const { usuario } = useAuth();
  const location = useLocation();
  const [aviso, mostrarAviso] = useAviso();
  const formularioRef = useRef(null);

  const [resenas, setResenas] = useState([]);
  const [estado, setEstado] = useState(null); // { comprado, resena } del usuario con sesión
  const [valoracion, setValoracion] = useState(0);
  const [descripcion, setDescripcion] = useState('');
  const [editando, setEditando] = useState(false);
  const [enviando, setEnviando] = useState(false);

  const cargar = () => {
    fetch(`${API_URL}/resenas/producto/${idProducto}`)
      .then((res) => (res.ok ? res.json() : []))
      .then(setResenas)
      .catch(() => setResenas([]));

    if (usuario) {
      fetch(`${API_URL}/resenas/estado/${idProducto}`)
        .then((res) => (res.ok ? res.json() : null))
        .then(setEstado)
        .catch(() => setEstado(null));
    } else {
      setEstado(null);
    }
  };

  useEffect(() => {
    setEditando(false);
    cargar();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [idProducto, usuario?.idUsuario]);

  // Si viene desde el historial ("Dejar reseña"), baja hasta el formulario
  const pideResenar = new URLSearchParams(location.search).has('resenar');
  useEffect(() => {
    if (pideResenar && estado && formularioRef.current) {
      formularioRef.current.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }
  }, [pideResenar, estado]);

  const miResena = estado?.resena || null;
  const mostrarFormulario = estado?.comprado && (!miResena || editando);

  const empezarEdicion = () => {
    setValoracion(miResena.valoracion);
    setDescripcion(miResena.descripcion || '');
    setEditando(true);
  };

  const enviar = (e) => {
    e.preventDefault();
    if (valoracion < 1) {
      mostrarAviso('Elegí de 1 a 5 estrellas.', 'error');
      return;
    }
    setEnviando(true);
    const esEdicion = Boolean(miResena);
    fetch(esEdicion ? `${API_URL}/resenas/${miResena.idResenas}` : `${API_URL}/resenas`, {
      method: esEdicion ? 'PUT' : 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ idProducto, valoracion, descripcion: descripcion.trim() }),
    })
      .then(async (res) => {
        const data = await res.json().catch(() => null);
        if (!res.ok) throw new Error(data?.mensaje || 'No se pudo guardar la reseña.');
        mostrarAviso(esEdicion ? 'Reseña actualizada' : '¡Gracias por tu reseña!');
        setEditando(false);
        setValoracion(0);
        setDescripcion('');
        cargar();
      })
      .catch((err) => mostrarAviso(err.message, 'error'))
      .finally(() => setEnviando(false));
  };

  const eliminar = () => {
    if (!window.confirm('¿Eliminar tu reseña?')) return;
    fetch(`${API_URL}/resenas/${miResena.idResenas}`, { method: 'DELETE' })
      .then((res) => {
        if (!res.ok) throw new Error();
        mostrarAviso('Reseña eliminada');
        setEditando(false);
        cargar();
      })
      .catch(() => mostrarAviso('No se pudo eliminar la reseña.', 'error'));
  };

  const promedio = resenas.length
    ? resenas.reduce((suma, r) => suma + r.valoracion, 0) / resenas.length
    : 0;

  return (
    <section className="resenas-seccion" id="resenas">
      {aviso}
      <div className="resenas-cabecera">
        <h2>Opiniones de clientes</h2>
        {resenas.length > 0 && (
          <div className="resenas-promedio">
            <Estrellas valor={promedio} />
            <span>
              {promedio.toLocaleString('es-AR', { maximumFractionDigits: 1 })} · {resenas.length}{' '}
              {resenas.length === 1 ? 'opinión' : 'opiniones'}
            </span>
          </div>
        )}
      </div>

      {/* Qué puede hacer el usuario */}
      <div ref={formularioRef}>
        {!usuario && (
          <p className="resenas-nota">
            ¿Compraste este producto? <Link to="/login" state={{ desde: location.pathname }}>Iniciá sesión</Link> para dejar tu opinión.
          </p>
        )}

        {usuario && estado && !estado.comprado && (
          <p className="resenas-nota">Solo quienes compraron este producto pueden dejar una reseña.</p>
        )}

        {miResena && !editando && (
          <div className="resenas-mia">
            <strong>Tu reseña</strong>
            <Estrellas valor={miResena.valoracion} />
            {miResena.descripcion && <p>{miResena.descripcion}</p>}
            <div className="resenas-mia-acciones">
              <button type="button" className="resenas-link" onClick={empezarEdicion}>Editar</button>
              <button type="button" className="resenas-link resenas-link-peligro" onClick={eliminar}>Eliminar</button>
            </div>
          </div>
        )}

        {mostrarFormulario && (
          <form className="resenas-formulario" onSubmit={enviar}>
            <strong>{miResena ? 'Editar tu reseña' : 'Dejá tu reseña'}</strong>
            <Estrellas valor={valoracion} onChange={setValoracion} tamano={28} />
            <textarea
              value={descripcion}
              onChange={(e) => setDescripcion(e.target.value)}
              maxLength={255}
              rows={3}
              placeholder="Contanos qué te pareció (opcional)"
            />
            <div className="resenas-formulario-pie">
              <small>{descripcion.length}/255</small>
              <div>
                {editando && (
                  <button type="button" className="resenas-btn resenas-btn-secundario" onClick={() => setEditando(false)}>
                    Cancelar
                  </button>
                )}
                <button type="submit" className="resenas-btn" disabled={enviando}>
                  {enviando ? 'Enviando...' : miResena ? 'Guardar' : 'Publicar reseña'}
                </button>
              </div>
            </div>
          </form>
        )}
      </div>

      {/* Listado */}
      {resenas.length === 0 ? (
        <p className="resenas-vacio">Todavía no hay opiniones de este producto.</p>
      ) : (
        <ul className="resenas-lista">
          {resenas.map((r) => (
            <li key={r.idResenas} className="resenas-item">
              <div className="resenas-item-cabecera">
                <strong>{r.nombreCliente}</strong>
                <Estrellas valor={r.valoracion} tamano={15} />
              </div>
              {r.descripcion && <p>{r.descripcion}</p>}
              <span className="resenas-verificada">✓ Compra verificada</span>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
