import React, { useState, useEffect } from 'react';
import { resolverImagen, subirImagen } from '../imagenes';
import { useAviso } from '../useAviso';
import './BannersAdmin.css';
import { API_URL } from '../config';


const ANCHO_RECOMENDADO = 1800;
const ALTO_RECOMENDADO = 600;

async function leerRespuesta(res, mensajePorDefecto) {
  const data = await res.json().catch(() => null);
  if (!res.ok) throw new Error(data?.mensaje || mensajePorDefecto);
  return data;
}

// Lee el tamaño real de la imagen elegida, para avisar si no es 3:1
function leerDimensiones(archivo) {
  return new Promise((resolve) => {
    const url = URL.createObjectURL(archivo);
    const img = new Image();
    img.onload = () => {
      resolve({ ancho: img.naturalWidth, alto: img.naturalHeight });
      URL.revokeObjectURL(url);
    };
    img.onerror = () => {
      resolve(null);
      URL.revokeObjectURL(url);
    };
    img.src = url;
  });
}

const FORMULARIO_VACIO = { imagen: '', titulo: '', enlace: '' };

export default function BannersAdmin() {
  const [aviso, mostrarAviso] = useAviso();
  const [banners, setBanners] = useState([]);
  const [productos, setProductos] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState(null);

  const [formulario, setFormulario] = useState(FORMULARIO_VACIO);
  const [editandoId, setEditandoId] = useState(null);
  const [subiendo, setSubiendo] = useState(false);
  const [guardando, setGuardando] = useState(false);
  const [advertenciaMedida, setAdvertenciaMedida] = useState('');

  useEffect(() => {
    Promise.all([
      fetch(`${API_URL}/admin/banners`).then((res) => leerRespuesta(res, 'error')),
      fetch(`${API_URL}/productos`).then((res) => leerRespuesta(res, 'error')),
    ])
      .then(([listaBanners, listaProductos]) => {
        setBanners(listaBanners);
        setProductos(listaProductos);
        setCargando(false);
      })
      .catch(() => {
        setError('No se pudieron cargar los banners. ¿Está corriendo el backend?');
        setCargando(false);
      });
  }, []);

  // ---------- Formulario ----------
  const elegirArchivo = async (e) => {
    const archivo = e.target.files[0];
    e.target.value = '';
    if (!archivo) return;

    setAdvertenciaMedida('');
    const medida = await leerDimensiones(archivo);
    if (medida) {
      const proporcion = medida.ancho / medida.alto;
      if (Math.abs(proporcion - 3) > 0.15) {
        setAdvertenciaMedida(
          `La imagen mide ${medida.ancho} × ${medida.alto} px (proporción ${proporcion.toFixed(2)}:1). ` +
          `Para verse completa tiene que ser ${ANCHO_RECOMENDADO} × ${ALTO_RECOMENDADO} px (3:1); si no, se recortan los bordes.`
        );
      } else if (medida.ancho < 1200) {
        setAdvertenciaMedida(`La imagen es chica (${medida.ancho} px de ancho): puede verse borrosa. Lo ideal son ${ANCHO_RECOMENDADO} × ${ALTO_RECOMENDADO} px.`);
      }
    }

    setSubiendo(true);
    try {
      const ruta = await subirImagen(archivo);
      setFormulario((prev) => ({ ...prev, imagen: ruta }));
    } catch (err) {
      mostrarAviso(err.message, 'error');
    } finally {
      setSubiendo(false);
    }
  };

  const cancelar = () => {
    setEditandoId(null);
    setFormulario(FORMULARIO_VACIO);
    setAdvertenciaMedida('');
  };

  const editar = (banner) => {
    setEditandoId(banner.idBanner);
    setFormulario({ imagen: banner.imagen, titulo: banner.titulo || '', enlace: banner.enlace || '' });
    setAdvertenciaMedida('');
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const guardar = (e) => {
    e.preventDefault();
    if (!formulario.imagen) {
      mostrarAviso('Primero subí una imagen.', 'error');
      return;
    }
    setGuardando(true);
    const esEdicion = editandoId !== null;
    fetch(esEdicion ? `${API_URL}/admin/banners/${editandoId}` : `${API_URL}/admin/banners`, {
      method: esEdicion ? 'PUT' : 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        imagen: formulario.imagen,
        titulo: formulario.titulo.trim(),
        enlace: formulario.enlace,
      }),
    })
      .then((res) => leerRespuesta(res, 'No se pudo guardar el banner.'))
      .then((guardado) => {
        setBanners((prev) =>
          esEdicion ? prev.map((b) => (b.idBanner === guardado.idBanner ? guardado : b)) : [...prev, guardado]
        );
        mostrarAviso(esEdicion ? 'Banner actualizado' : 'Banner agregado al carrusel');
        cancelar();
      })
      .catch((err) => mostrarAviso(err.message, 'error'))
      .finally(() => setGuardando(false));
  };

  // ---------- Acciones sobre la lista ----------
  const alternarActivo = (banner) => {
    fetch(`${API_URL}/admin/banners/${banner.idBanner}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        imagen: banner.imagen,
        titulo: banner.titulo || '',
        enlace: banner.enlace || '',
        activo: !banner.activo,
      }),
    })
      .then((res) => leerRespuesta(res, 'No se pudo cambiar el estado.'))
      .then((guardado) => {
        setBanners((prev) => prev.map((b) => (b.idBanner === guardado.idBanner ? guardado : b)));
        mostrarAviso(guardado.activo ? 'Banner visible en el carrusel' : 'Banner oculto');
      })
      .catch((err) => mostrarAviso(err.message, 'error'));
  };

  const mover = (indice, direccion) => {
    const destino = indice + direccion;
    if (destino < 0 || destino >= banners.length) return;
    const nuevos = [...banners];
    [nuevos[indice], nuevos[destino]] = [nuevos[destino], nuevos[indice]];
    setBanners(nuevos); // se actualiza al instante

    fetch(`${API_URL}/admin/banners/orden`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(nuevos.map((b) => b.idBanner)),
    })
      .then((res) => leerRespuesta(res, 'No se pudo cambiar el orden.'))
      .then(setBanners)
      .catch((err) => {
        setBanners(banners); // si falla, se vuelve al orden anterior
        mostrarAviso(err.message, 'error');
      });
  };

  const eliminar = (banner) => {
    if (!window.confirm('¿Eliminar este banner del carrusel?')) return;
    fetch(`${API_URL}/admin/banners/${banner.idBanner}`, { method: 'DELETE' })
      .then(async (res) => {
        if (!res.ok) await leerRespuesta(res, 'No se pudo eliminar el banner.');
        setBanners((prev) => prev.filter((b) => b.idBanner !== banner.idBanner));
        if (editandoId === banner.idBanner) cancelar();
        mostrarAviso('Banner eliminado');
      })
      .catch((err) => mostrarAviso(err.message, 'error'));
  };

  const nombreEnlace = (enlace) => {
    if (!enlace) return null;
    const m = enlace.match(/^\/producto\/(\d+)$/);
    if (m) {
      const p = productos.find((x) => x.idProducto === Number(m[1]));
      return p ? `Producto: ${p.nombre}` : enlace;
    }
    return enlace;
  };

  if (cargando) return <p className="banners-vacio">Cargando banners...</p>;
  if (error) return <p className="banners-vacio">{error}</p>;

  return (
    <div className="banners-contenedor">
      {aviso}
      <div className="banners-tarjeta">
        <h1 className="banners-titulo">Gestionar carrusel</h1>
        <div className="banners-guia">
          <strong>Medida recomendada: {ANCHO_RECOMENDADO} × {ALTO_RECOMENDADO} px</strong>
        </div>

        {/* ---------- Formulario ---------- */}
        <form onSubmit={guardar} className="banners-form">
          <h2>{editandoId !== null ? 'Editar banner' : 'Agregar banner'}</h2>

          <div className="banners-form-imagen">
            {formulario.imagen ? (
              <img src={resolverImagen(formulario.imagen)} alt="Vista previa" className="banners-previa" />
            ) : (
              <div className="banners-previa banners-previa-vacia">Sin imagen</div>
            )}
          </div>

          <label className="banners-campo">
            Imagen
            <input
              type="file"
              accept="image/png,image/jpeg,image/webp,image/gif"
              onChange={elegirArchivo}
              disabled={subiendo}
            />
            {subiendo && <small>Subiendo imagen...</small>}
          </label>

          {advertenciaMedida && <div className="banners-advertencia">{advertenciaMedida}</div>}

          <label className="banners-campo">
            Texto sobre la imagen (opcional)
            <input
              type="text"
              value={formulario.titulo}
              onChange={(e) => setFormulario({ ...formulario, titulo: e.target.value })}
              maxLength={100}
            />
          </label>

          <label className="banners-campo">
            Al hacer clic lleva a (opcional)
            <select value={formulario.enlace} onChange={(e) => setFormulario({ ...formulario, enlace: e.target.value })}>
              <option value="">Ningún enlace</option>
              {/* Si el enlace guardado no es de un producto de la lista, se conserva */}
              {formulario.enlace && !productos.some((p) => `/producto/${p.idProducto}` === formulario.enlace) && (
                <option value={formulario.enlace}>{formulario.enlace}</option>
              )}
              {productos.map((p) => (
                <option key={p.idProducto} value={`/producto/${p.idProducto}`}>Producto: {p.nombre}</option>
              ))}
            </select>
          </label>

          <div className="banners-form-botones">
            {editandoId !== null && (
              <button type="button" className="banners-btn banners-btn-secundario" onClick={cancelar}>Cancelar</button>
            )}
            <button type="submit" className="banners-btn" disabled={guardando || subiendo || !formulario.imagen}>
              {guardando ? 'Guardando...' : editandoId !== null ? 'Guardar cambios' : 'Agregar al carrusel'}
            </button>
          </div>
        </form>

        {/* ---------- Lista ---------- */}
        <h2 className="banners-lista-titulo">En el carrusel ({banners.length})</h2>
        {banners.length === 0 ? (
          <p className="banners-vacio">Todavía no cargaste ningún banner. Mientras no haya, el carrusel no se muestra en la página principal.</p>
        ) : (
          <ul className="banners-lista">
            {banners.map((b, i) => (
              <li key={b.idBanner} className={`banners-item ${b.activo ? '' : 'banners-item-oculto'}`}>
                <img src={resolverImagen(b.imagen)} alt="" className="banners-miniatura" />
                <div className="banners-item-datos">
                  <strong>{b.titulo || 'Sin texto'}</strong>
                  <span>{nombreEnlace(b.enlace) || 'Sin enlace'}</span>
                  {!b.activo && <span className="banners-oculto-etiqueta">Oculto</span>}
                </div>
                <div className="banners-item-acciones">
                  <button className="banners-icono" onClick={() => mover(i, -1)} disabled={i === 0} aria-label="Subir" title="Subir">▲</button>
                  <button className="banners-icono" onClick={() => mover(i, 1)} disabled={i === banners.length - 1} aria-label="Bajar" title="Bajar">▼</button>
                  <button className="banners-btn banners-btn-secundario" onClick={() => alternarActivo(b)}>
                    {b.activo ? 'Ocultar' : 'Mostrar'}
                  </button>
                  <button className="banners-btn banners-btn-secundario" onClick={() => editar(b)}>Editar</button>
                  <button className="banners-btn banners-btn-peligro" onClick={() => eliminar(b)}>Eliminar</button>
                </div>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}
