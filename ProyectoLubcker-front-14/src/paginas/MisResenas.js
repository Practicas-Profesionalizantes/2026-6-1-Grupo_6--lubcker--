import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../AuthContext';
import { useAviso } from '../useAviso';
import Estrellas from '../components/Estrellas';
import '../components/Resenas.css';
import './MisResenas.css';
import { API_URL } from '../config';


// Reseñas del cliente: su opinión general sobre Lubcker y los productos que puede reseñar.
// Las de producto se cargan en la página de cada producto (solo si lo compró; lo valida el backend).
export default function MisResenas() {
  const { usuario } = useAuth();
  const [aviso, mostrarAviso] = useAviso();
  const idCliente = usuario?.idCliente;

  // Opinión general
  const [generales, setGenerales] = useState([]);
  const [mia, setMia] = useState(null);
  const [valoracion, setValoracion] = useState(0);
  const [descripcion, setDescripcion] = useState('');
  const [editando, setEditando] = useState(false);
  const [enviando, setEnviando] = useState(false);

  // Productos comprados (sin repetir y sin pedidos cancelados) y los que ya reseñó
  const [productos, setProductos] = useState(null);
  const [resenados, setResenados] = useState({}); // idProducto -> reseña

  const cargarGenerales = () => {
    fetch(`${API_URL}/resenas-generales`)
      .then((res) => (res.ok ? res.json() : []))
      .then(setGenerales)
      .catch(() => setGenerales([]));
    fetch(`${API_URL}/resenas-generales/mia`)
      .then((res) => (res.ok ? res.json() : null))
      .then(setMia)
      .catch(() => setMia(null));
  };

  useEffect(() => {
    cargarGenerales();

    fetch(`${API_URL}/resenas/mias`)
      .then((res) => (res.ok ? res.json() : []))
      .then((lista) => setResenados(Object.fromEntries(lista.map((r) => [r.idProducto, r]))))
      .catch(() => setResenados({}));

    if (!idCliente) {
      setProductos([]);
      return;
    }
    const pedir = (ruta) =>
      fetch(`${API_URL}/${ruta}/cliente/${idCliente}`).then((res) => {
        if (!res.ok) throw new Error('error');
        return res.json();
      });
    Promise.all([pedir('ordenes'), pedir('envios'), pedir('pagos')])
      .then(([ordenes, envios, pagos]) => {
        const pagados = new Set(pagos.filter((p) => p.estado).map((p) => p.idOrden));
        const cancelados = new Set(
          envios.filter((e) => (e.estado || '').toLowerCase() === 'cancelado').map((e) => e.idOrden)
        );
        const vistos = new Map();
        ordenes
          .filter((o) => pagados.has(o.idOrden) && !cancelados.has(o.idOrden))
          .forEach((o) => (o.detalles || []).forEach((d) => {
            if (!vistos.has(d.idProducto)) vistos.set(d.idProducto, d.nombreProducto);
          }));
        setProductos([...vistos].map(([idProducto, nombre]) => ({ idProducto, nombre })));
      })
      .catch(() => setProductos(null));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [idCliente]);

  const mostrarFormulario = !mia || editando;

  const empezarEdicion = () => {
    setValoracion(mia.valoracion);
    setDescripcion(mia.descripcion || '');
    setEditando(true);
  };

  const enviar = (e) => {
    e.preventDefault();
    if (valoracion < 1) {
      mostrarAviso('Elegí de 1 a 5 estrellas.', 'error');
      return;
    }
    setEnviando(true);
    const esEdicion = Boolean(mia);
    fetch(esEdicion ? `${API_URL}/resenas-generales/${mia.idResenaGeneral}` : `${API_URL}/resenas-generales`, {
      method: esEdicion ? 'PUT' : 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ valoracion, descripcion: descripcion.trim() }),
    })
      .then(async (res) => {
        const data = await res.json().catch(() => null);
        if (!res.ok) throw new Error(data?.mensaje || 'No se pudo guardar tu opinión.');
        mostrarAviso(esEdicion ? 'Opinión actualizada' : '¡Gracias por tu opinión!');
        setEditando(false);
        setValoracion(0);
        setDescripcion('');
        cargarGenerales();
      })
      .catch((err) => mostrarAviso(err instanceof TypeError ? 'No se pudo conectar con el servidor.' : err.message, 'error'))
      .finally(() => setEnviando(false));
  };

  const eliminar = () => {
    if (!window.confirm('¿Eliminar tu opinión sobre Lubcker?')) return;
    fetch(`${API_URL}/resenas-generales/${mia.idResenaGeneral}`, { method: 'DELETE' })
      .then((res) => {
        if (!res.ok) throw new Error();
        mostrarAviso('Opinión eliminada');
        setEditando(false);
        cargarGenerales();
      })
      .catch(() => mostrarAviso('No se pudo eliminar tu opinión.', 'error'));
  };

  const esAdmin = usuario?.rol === 'ADMIN';

  return (
    <div className="mis-resenas-contenedor">
      {aviso}
      <div className="mis-resenas-tarjeta">
        <h1 className="mis-resenas-titulo">Reseñas</h1>
        <p className="mis-resenas-subtitulo">Contanos qué te parece Lubcker y los productos que compraste.</p>

        {/* ---------- Opinión sobre Lubcker ---------- */}
        <section className="mis-resenas-bloque">
          <h2>Tu opinión sobre Lubcker</h2>

          {esAdmin && <p className="resenas-nota">Las opiniones las dejan los clientes con su cuenta.</p>}

          {!esAdmin && mia && !editando && (
            <div className="resenas-mia">
              <strong>Tu opinión</strong>
              <Estrellas valor={mia.valoracion} />
              {mia.descripcion && <p>{mia.descripcion}</p>}
              <div className="resenas-mia-acciones">
                <button type="button" className="resenas-link" onClick={empezarEdicion}>Editar</button>
                <button type="button" className="resenas-link resenas-link-peligro" onClick={eliminar}>Eliminar</button>
              </div>
            </div>
          )}

          {!esAdmin && mostrarFormulario && (
            <form className="resenas-formulario" onSubmit={enviar}>
              <strong>{mia ? 'Editar tu opinión' : 'Dejá tu opinión sobre la tienda'}</strong>
              <Estrellas valor={valoracion} onChange={setValoracion} tamano={28} />
              <textarea
                value={descripcion}
                onChange={(e) => setDescripcion(e.target.value)}
                maxLength={255}
                rows={3}
                placeholder="¿Cómo fue tu experiencia con Lubcker? (opcional)"
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
                    {enviando ? 'Enviando...' : mia ? 'Guardar' : 'Publicar opinión'}
                  </button>
                </div>
              </div>
            </form>
          )}

          {generales.length > 0 && (
            <ul className="resenas-lista">
              {generales.map((r) => (
                <li key={r.idResenaGeneral} className="resenas-item">
                  <div className="resenas-item-cabecera">
                    <strong>{r.nombreCliente}</strong>
                    <Estrellas valor={r.valoracion} tamano={15} />
                  </div>
                  {r.descripcion && <p>{r.descripcion}</p>}
                </li>
              ))}
            </ul>
          )}
        </section>

        {/* ---------- Productos comprados ---------- */}
        <section className="mis-resenas-bloque">
          <h2>Reseñas de productos</h2>
          <p className="resenas-nota">Solo podés reseñar los productos que compraste.</p>

          {productos === null && <p className="resenas-vacio">No se pudieron cargar tus compras.</p>}

          {productos && productos.length === 0 && (
            <p className="resenas-vacio">
              Todavía no compraste ningún producto. <Link to="/">Ir al catálogo</Link>
            </p>
          )}

          {productos && productos.length > 0 && (
            <ul className="mis-resenas-productos">
              {productos.map((p) => {
                const r = resenados[p.idProducto];
                return (
                  <li key={p.idProducto} className="mis-resenas-producto">
                    <div>
                      <strong>{p.nombre}</strong>
                      {r && <Estrellas valor={r.valoracion} tamano={15} />}
                    </div>
                    <Link to={`/producto/${p.idProducto}?resenar=1`} className="historial-link-resena">
                      {r ? 'Editar reseña' : '★ Dejar reseña'}
                    </Link>
                  </li>
                );
              })}
            </ul>
          )}
        </section>
      </div>
    </div>
  );
}
