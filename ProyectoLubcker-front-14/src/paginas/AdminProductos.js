import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { resolverImagen, subirImagen } from '../imagenes';
import { useAviso } from '../useAviso';
import { formatearPrecio, formatearDinero, normalizarTexto } from '../formato';
import { POCAS_UNIDADES } from '../components/StockBadge';
import { UNIDADES, mapearMedidas, vendePorMedida, etiquetaMedida, simboloUnidad, textoStock, leerMedidas } from '../medidas';
import InputNumero from '../components/InputNumero';
import './AdminProductos.css';
import { API_URL } from '../config';


function mapearProducto(p) {
  return {
    id: p.idProducto,
    nombre: p.nombre,
    precio: p.precio,
    stock: p.stock,
    ...mapearMedidas(p),
    activo: p.activo,
    imagenValor: p.imagen || '',
    imagen: resolverImagen(p.imagen),
    categoria: (p.nombreCategoria || '').toLowerCase(),
    idCategoria: p.idCategoria,
  };
}

const FORMULARIO_VACIO = {
  nombre: '', precio: '', stock: '0', idCategoria: '', imagen: '', activo: true,
  unidadMedida: 'unidad', medidas: '',
  modelo: '', presentacion: '', descripcion: '',
};

// Lee el mensaje de error que manda el backend ({"mensaje": "..."})
async function leerRespuesta(res, mensajePorDefecto) {
  const data = await res.json().catch(() => null);
  if (!res.ok) throw new Error(data?.mensaje || mensajePorDefecto);
  return data;
}

export default function AdminProductos() {
  const [productos, setProductos] = useState([]);
  const [categorias, setCategorias] = useState([]);
  const [costos, setCostos] = useState({}); // idProducto -> costo de fabricación y ganancia por unidad
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState(null);

  const [busqueda, setBusqueda] = useState('');
  const [filtroEstado, setFiltroEstado] = useState('todos');
  const [modalAbierto, setModalAbierto] = useState(false);
  const [productoEditandoId, setProductoEditandoId] = useState(null);
  const [formulario, setFormulario] = useState(FORMULARIO_VACIO);
  const [subiendo, setSubiendo] = useState(false);
  const [guardando, setGuardando] = useState(false);
  const [aviso, mostrarAviso] = useAviso();

  // Gestión de categorías
  const [modalCategorias, setModalCategorias] = useState(false);
  const [nuevaCategoria, setNuevaCategoria] = useState('');
  const [categoriaEditando, setCategoriaEditando] = useState(null); // { idCategoria, nombre }

  const cargarDatos = () => {
    Promise.all([
      // El panel ve también los productos desactivados
      fetch(`${API_URL}/productos?incluirInactivos=true`).then((res) => leerRespuesta(res, 'error')),
      fetch(`${API_URL}/categorias`).then((res) => leerRespuesta(res, 'error')),
      // Costos de fabricación (no es crítico: si falla, las tarjetas se ven igual)
      fetch(`${API_URL}/admin/materia-prima/resumen`).then((res) => leerRespuesta(res, 'error')).catch(() => []),
    ])
      .then(([dataProductos, dataCategorias, dataCostos]) => {
        setProductos(dataProductos.map(mapearProducto));
        setCategorias(dataCategorias);
        setCostos(Object.fromEntries(dataCostos.map((c) => [c.idProducto, c])));
        setCargando(false);
      })
      .catch(() => {
        setError('No se pudieron cargar los datos. ¿Está corriendo el backend?');
        setCargando(false);
      });
  };

  useEffect(() => {
    cargarDatos();
  }, []);

  const texto = normalizarTexto(busqueda);
  const productosFiltrados = productos.filter((p) => {
    if (texto && !normalizarTexto(p.nombre).includes(texto)) return false;
    if (filtroEstado === 'activos') return p.activo;
    if (filtroEstado === 'inactivos') return !p.activo;
    if (filtroEstado === 'sin-stock') return p.stock <= 0;
    if (filtroEstado === 'poco-stock') return p.stock > 0 && p.stock <= POCAS_UNIDADES;
    return true;
  });

  // ---------- Formulario de producto ----------
  const abrirFormularioNuevo = () => {
    setProductoEditandoId(null);
    setFormulario({
      ...FORMULARIO_VACIO,
      idCategoria: categorias[0]?.idCategoria || '',
    });
    setModalAbierto(true);
  };

  const abrirFormularioEdicion = (producto) => {
    setProductoEditandoId(producto.id);
    setFormulario({
      ...FORMULARIO_VACIO,
      nombre: producto.nombre,
      precio: producto.precio,
      stock: producto.stock,
      idCategoria: producto.idCategoria,
      imagen: producto.imagenValor,
      activo: producto.activo,
      unidadMedida: producto.unidad,
      medidas: vendePorMedida(producto) ? producto.presentaciones.join(', ') : '',
    });
    setModalAbierto(true);

    // La ficha técnica se trae aparte (no todos los productos tienen)
    fetch(`${API_URL}/fichas-tecnicas/producto/${producto.id}`)
      .then((res) => (res.ok ? res.json() : null))
      .then((ficha) => {
        if (ficha) {
          setFormulario((prev) => ({
            ...prev,
            modelo: ficha.modelo || '',
            presentacion: ficha.presentacion || '',
            descripcion: ficha.descripcion || '',
          }));
        }
      })
      .catch(() => {});
  };

  const cerrarModal = () => {
    setModalAbierto(false);
    setProductoEditandoId(null);
    setFormulario(FORMULARIO_VACIO);
  };

  const manejarCambioFormulario = (e) => {
    const { name, value, type, checked } = e.target;
    setFormulario(prev => ({ ...prev, [name]: type === 'checkbox' ? checked : value }));
  };

  // Sube la imagen elegida al backend y guarda su ruta en el formulario
  const manejarArchivo = async (e) => {
    const archivo = e.target.files[0];
    e.target.value = '';
    if (!archivo) return;
    setSubiendo(true);
    try {
      const ruta = await subirImagen(archivo);
      setFormulario(prev => ({ ...prev, imagen: ruta }));
    } catch (err) {
      mostrarAviso(err.message, 'error');
    } finally {
      setSubiendo(false);
    }
  };

  const guardarProducto = async (e) => {
    e.preventDefault();
    const { lista: medidas, error: errorMedidas } = leerMedidas(formulario.medidas);
    if (errorMedidas) {
      mostrarAviso(errorMedidas, 'error');
      return;
    }
    if (!(parseInt(formulario.precio, 10) >= 1)) {
      mostrarAviso('El precio tiene que ser mayor a 0.', 'error');
      return;
    }
    setGuardando(true);

    const cuerpo = {
      nombre: formulario.nombre.trim(),
      precio: parseInt(formulario.precio, 10) || 0,
      stock: parseInt(formulario.stock, 10) || 0,
      idCategoria: parseInt(formulario.idCategoria, 10),
      imagen: formulario.imagen.trim(),
      activo: formulario.activo,
      unidadMedida: formulario.unidadMedida,
      presentaciones: medidas.length > 0 ? medidas : [1],
    };

    const esEdicion = productoEditandoId !== null;
    try {
      const guardado = await fetch(
        esEdicion ? `${API_URL}/productos/${productoEditandoId}` : `${API_URL}/productos`,
        {
          method: esEdicion ? 'PUT' : 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(cuerpo),
        }
      ).then((res) => leerRespuesta(res, 'No se pudo guardar el producto.'));

      // Ficha técnica (descripción, modelo, presentación) del producto recién guardado
      await fetch(`${API_URL}/fichas-tecnicas/producto/${guardado.idProducto}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          idProducto: guardado.idProducto,
          modelo: formulario.modelo.trim(),
          presentacion: formulario.presentacion.trim(),
          descripcion: formulario.descripcion.trim(),
        }),
      }).then((res) => leerRespuesta(res, 'El producto se guardó, pero no la ficha técnica.'));

      mostrarAviso(esEdicion ? 'Producto actualizado' : 'Producto publicado');
      cargarDatos();
      cerrarModal();
    } catch (err) {
      mostrarAviso(err.message, 'error');
      cargarDatos();
    } finally {
      setGuardando(false);
    }
  };

  const alternarActivo = (producto) => {
    fetch(`${API_URL}/productos/${producto.id}/activo`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ activo: !producto.activo }),
    })
      .then((res) => leerRespuesta(res, 'No se pudo cambiar el estado del producto.'))
      .then((actualizado) => {
        setProductos(prev => prev.map(p => (p.id === producto.id ? mapearProducto(actualizado) : p)));
        mostrarAviso(actualizado.activo ? `"${producto.nombre}" vuelve a estar a la venta` : `"${producto.nombre}" se desactivó`);
      })
      .catch((err) => mostrarAviso(err.message, 'error'));
  };

  const eliminarProducto = (id, nombre) => {
    const confirmar = window.confirm(`¿Seguro que querés eliminar "${nombre}"? Esta acción no se puede deshacer.`);
    if (!confirmar) return;

    fetch(`${API_URL}/productos/${id}`, { method: 'DELETE' })
      .then(async (res) => {
        if (!res.ok) await leerRespuesta(res, 'No se pudo eliminar el producto.');
        setProductos(prev => prev.filter(p => p.id !== id));
        mostrarAviso(`"${nombre}" eliminado`);
      })
      .catch((err) => mostrarAviso(err.message, 'error'));
  };

  // ---------- Categorías ----------
  const recargarCategorias = () =>
    fetch(`${API_URL}/categorias`)
      .then((res) => leerRespuesta(res, 'error'))
      .then(setCategorias)
      .catch(() => {});

  const crearCategoria = (e) => {
    e.preventDefault();
    const nombre = nuevaCategoria.trim();
    if (!nombre) return;
    fetch(`${API_URL}/categorias`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ nombre }),
    })
      .then((res) => leerRespuesta(res, 'No se pudo crear la categoría.'))
      .then(() => {
        setNuevaCategoria('');
        recargarCategorias();
        mostrarAviso(`Categoría "${nombre}" creada`);
      })
      .catch((err) => mostrarAviso(err.message, 'error'));
  };

  const guardarCategoria = (e) => {
    e.preventDefault();
    const nombre = categoriaEditando.nombre.trim();
    fetch(`${API_URL}/categorias/${categoriaEditando.idCategoria}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ nombre }),
    })
      .then((res) => leerRespuesta(res, 'No se pudo renombrar la categoría.'))
      .then(() => {
        setCategoriaEditando(null);
        cargarDatos(); // los productos muestran el nombre de su categoría
        mostrarAviso('Categoría actualizada');
      })
      .catch((err) => mostrarAviso(err.message, 'error'));
  };

  const eliminarCategoria = (categoria) => {
    if (!window.confirm(`¿Eliminar la categoría "${categoria.nombre}"?`)) return;
    fetch(`${API_URL}/categorias/${categoria.idCategoria}`, { method: 'DELETE' })
      .then(async (res) => {
        if (!res.ok) await leerRespuesta(res, 'No se pudo eliminar la categoría.');
        recargarCategorias();
        mostrarAviso(`Categoría "${categoria.nombre}" eliminada`);
      })
      .catch((err) => mostrarAviso(err.message, 'error'));
  };

  if (cargando) {
    return <p className="admin-productos-vacio">Cargando productos...</p>;
  }

  if (error) {
    return <p className="admin-productos-vacio">{error}</p>;
  }

  return (
    <div className="admin-productos-contenedor">
      {aviso}
      <div className="admin-productos-tarjeta-principal">

        <div className="admin-productos-encabezado">
          <div>
            <h1 className="admin-productos-titulo">Mis Productos Publicados</h1>
          </div>
          <div className="admin-encabezado-botones">
            <button className="btn-admin-secundario" onClick={() => setModalCategorias(true)}>
              Categorías
            </button>
            <button className="btn-admin-agregar" onClick={abrirFormularioNuevo}>
              + Agregar Producto
            </button>
          </div>
        </div>

        <div className="admin-productos-barra-herramientas">
          <input
            type="text"
            placeholder="Buscar producto por nombre..."
            className="admin-productos-input-busqueda"
            value={busqueda}
            onChange={(e) => setBusqueda(e.target.value)}
          />
          <select
            className="admin-productos-filtro"
            value={filtroEstado}
            onChange={(e) => setFiltroEstado(e.target.value)}
            aria-label="Filtrar por estado"
          >
            <option value="todos">Todos</option>
            <option value="activos">A la venta</option>
            <option value="inactivos">Desactivados</option>
            <option value="sin-stock">Sin stock</option>
            <option value="poco-stock">Poco stock (≤ {POCAS_UNIDADES})</option>
          </select>
          <span className="admin-productos-contador">
            {productosFiltrados.length} {productosFiltrados.length === 1 ? 'producto' : 'productos'}
          </span>
        </div>

        {productosFiltrados.length === 0 ? (
          <div className="admin-productos-vacio">
            <p>{productos.length === 0 ? 'Todavía no tenés productos publicados.' : 'No se encontraron productos con ese filtro.'}</p>
            {productos.length === 0 && (
              <button className="btn-admin-agregar" onClick={abrirFormularioNuevo}>+ Agregar tu primer producto</button>
            )}
          </div>
        ) : (
          <div className="admin-productos-grid">
            {productosFiltrados.map((producto) => (
              <div key={producto.id} className={`admin-producto-tarjeta ${producto.activo ? '' : 'admin-producto-inactivo'}`}>
                <div className="admin-producto-imagen-contenedor">
                  <img src={producto.imagen} alt={producto.nombre} className="admin-producto-imagen" />
                </div>

                <div className="admin-producto-etiquetas">
                  <span className="admin-producto-categoria-badge">
                    {producto.categoria}
                  </span>
                  {!producto.activo && <span className="admin-badge-inactivo">Desactivado</span>}
                </div>

                <h3 className="admin-producto-nombre">{producto.nombre}</h3>
                <span className="admin-producto-precio">
                  {formatearPrecio(producto.precio)}{vendePorMedida(producto) ? ` por ${simboloUnidad(producto.unidad)}` : ''}
                </span>
                {vendePorMedida(producto) && (
                  <span className="admin-producto-medidas">
                    Se vende en: {producto.presentaciones.map((m) => etiquetaMedida(m, producto.unidad)).join(' · ')}
                  </span>
                )}
                <span className={`admin-producto-stock ${producto.stock <= 0 ? 'sin-stock' : producto.stock <= POCAS_UNIDADES ? 'poco-stock' : ''}`}>
                  Stock: {textoStock(producto.stock, producto.unidad)}
                </span>

                {costos[producto.id] ? (
                  <Link to={`/admin/materia-prima?producto=${producto.id}`} className="admin-producto-costos" title="Ver o editar la materia prima">
                    <small>Por {producto.unidad === 'unidad' ? '1 unidad' : `1 ${simboloUnidad(producto.unidad)}`}</small>
                    <div><span>Costo de fabricación</span><strong>{formatearDinero(costos[producto.id].costoFabricacion)}</strong></div>
                    <div><span>Precio de venta</span><strong>{formatearDinero(producto.precio)}</strong></div>
                    <div className={Number(costos[producto.id].ganancia) >= 0 ? 'costo-ganancia' : 'costo-perdida'}>
                      <span>{Number(costos[producto.id].ganancia) >= 0 ? 'Ganancia' : 'Pérdida'}</span>
                      <strong>
                        {formatearDinero(costos[producto.id].ganancia)}
                        {costos[producto.id].margenPorcentaje !== null && ` (${Number(costos[producto.id].margenPorcentaje).toLocaleString('es-AR')}%)`}
                      </strong>
                    </div>
                  </Link>
                ) : (
                  <Link to={`/admin/materia-prima?producto=${producto.id}`} className="admin-producto-costos-vacio">
                    Cargar materia prima y ver la ganancia
                  </Link>
                )}

                <div className="admin-producto-acciones">
                  <button className="btn-admin-editar" onClick={() => abrirFormularioEdicion(producto)}>
                    Editar
                  </button>
                  <button className="btn-admin-activar" onClick={() => alternarActivo(producto)}>
                    {producto.activo ? 'Desactivar' : 'Activar'}
                  </button>
                  <button className="btn-admin-eliminar" onClick={() => eliminarProducto(producto.id, producto.nombre)}>
                    Eliminar
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* ---------- Modal de producto ---------- */}
      {modalAbierto && (
        <div className="admin-modal-fondo" onClick={cerrarModal}>
          <div className="admin-modal-contenido" onClick={(e) => e.stopPropagation()}>
            <h2 className="admin-modal-titulo">
              {productoEditandoId ? 'Editar Producto' : 'Agregar Nuevo Producto'}
            </h2>

            <form onSubmit={guardarProducto} className="admin-modal-formulario">
              <div className="admin-form-grupo">
                <label htmlFor="nombre">Nombre del Producto</label>
                <input
                  id="nombre"
                  name="nombre"
                  type="text"
                  value={formulario.nombre}
                  onChange={manejarCambioFormulario}
                  required
                  maxLength={100}
                  className="admin-form-input"
                />
              </div>

              <div className="admin-form-fila">
                <div className="admin-form-grupo">
                  <label htmlFor="unidadMedida">Unidad de medida</label>
                  <select
                    id="unidadMedida"
                    name="unidadMedida"
                    value={formulario.unidadMedida}
                    onChange={manejarCambioFormulario}
                    className="admin-form-input"
                  >
                    {UNIDADES.map((u) => (
                      <option key={u.valor} value={u.valor}>{u.nombre}</option>
                    ))}
                  </select>
                </div>

                <div className="admin-form-grupo">
                  <label htmlFor="medidas">Medidas a la venta (opcional)</label>
                  <input
                    id="medidas"
                    name="medidas"
                    type="text"
                    value={formulario.medidas}
                    onChange={manejarCambioFormulario}
                    className="admin-form-input"
                    placeholder={formulario.unidadMedida === 'unidad' ? 'Ej: 1, 6, 12' : 'Ej: 3, 5, 10'}
                  />
                </div>
              </div>
              <small className="admin-form-ayuda">
                {formulario.unidadMedida === 'unidad' && !formulario.medidas.trim()
                  ? 'El producto se vende de a una unidad.'
                  : 'El cliente elige una de estas medidas y el precio final se calcula solo (precio por unidad de medida × medida).'}
              </small>

              <div className="admin-form-fila">
                <div className="admin-form-grupo">
                  <label htmlFor="precio">{formulario.unidadMedida === 'unidad' ? 'Precio ($)' : `Precio por ${simboloUnidad(formulario.unidadMedida)} ($)`}</label>
                  <InputNumero
                    id="precio"
                    name="precio"
                    value={formulario.precio}
                    onChange={manejarCambioFormulario}
                    required
                    className="admin-form-input"
                    placeholder="0"
                  />
                </div>

                <div className="admin-form-grupo">
                  <label htmlFor="stock">{formulario.unidadMedida === 'unidad' ? 'Stock (unidades)' : `Stock (en ${simboloUnidad(formulario.unidadMedida)})`}</label>
                  <InputNumero
                    id="stock"
                    name="stock"
                    value={formulario.stock}
                    onChange={manejarCambioFormulario}
                    required
                    className="admin-form-input"
                  />
                </div>
              </div>

              {leerMedidas(formulario.medidas).lista.length > 0 && Number(formulario.precio) > 0 && (
                <div className="admin-form-medidas-preview">
                  <strong>Precios que verá el cliente:</strong>
                  <ul>
                    {leerMedidas(formulario.medidas).lista.map((m) => (
                      <li key={m}>{etiquetaMedida(m, formulario.unidadMedida)} → {formatearPrecio(Number(formulario.precio) * m)}</li>
                    ))}
                  </ul>
                </div>
              )}

              <div className="admin-form-fila">
                <div className="admin-form-grupo">
                  <label htmlFor="idCategoria">Categoría</label>
                  <select
                    id="idCategoria"
                    name="idCategoria"
                    value={formulario.idCategoria}
                    onChange={manejarCambioFormulario}
                    required
                    className="admin-form-input"
                  >
                    {categorias.length === 0 && <option value="">No hay categorías cargadas</option>}
                    {categorias.map((c) => (
                      <option key={c.idCategoria} value={c.idCategoria}>{c.nombre}</option>
                    ))}
                  </select>
                </div>

                <div className="admin-form-grupo admin-form-check">
                  <label>
                    <input
                      type="checkbox"
                      name="activo"
                      checked={formulario.activo}
                      onChange={manejarCambioFormulario}
                    />
                    A la venta (visible en el catálogo)
                  </label>
                </div>
              </div>

              <div className="admin-form-grupo">
                <label htmlFor="archivo-imagen">Imagen del producto</label>
                <input
                  id="archivo-imagen"
                  type="file"
                  accept="image/png,image/jpeg,image/webp,image/gif"
                  onChange={manejarArchivo}
                  disabled={subiendo}
                  className="admin-form-input"
                />
                {subiendo && <small>Subiendo imagen...</small>}
              </div>

              {formulario.imagen && (
                <div className="admin-form-preview">
                  <img src={resolverImagen(formulario.imagen)} alt="Vista previa" />
                </div>
              )}

              <fieldset className="admin-form-ficha">
                <div className="admin-form-grupo">
                  <label htmlFor="descripcion">Descripción</label>
                  <textarea
                    id="descripcion"
                    name="descripcion"
                    value={formulario.descripcion}
                    onChange={manejarCambioFormulario}
                    maxLength={255}
                    rows={3}
                    className="admin-form-input"
                    placeholder="Para qué sirve, cómo se usa..."
                  />
                </div>
                <div className="admin-form-fila">
                  <div className="admin-form-grupo">
                    <label htmlFor="modelo">Modelo</label>
                    <input
                      id="modelo"
                      name="modelo"
                      type="text"
                      value={formulario.modelo}
                      onChange={manejarCambioFormulario}
                      maxLength={50}
                      className="admin-form-input"
                    />
                  </div>
                  <div className="admin-form-grupo">
                    <label htmlFor="presentacion">Presentación</label>
                    <input
                      id="presentacion"
                      name="presentacion"
                      type="text"
                      value={formulario.presentacion}
                      onChange={manejarCambioFormulario}
                      maxLength={100}
                      className="admin-form-input"
                      placeholder="Ej: Aerosol 400 ml"
                    />
                  </div>
                </div>
              </fieldset>

              <div className="admin-modal-acciones">
                <button type="button" className="btn-admin-cancelar" onClick={cerrarModal}>
                  Cancelar
                </button>
                <button type="submit" className="btn-admin-guardar" disabled={guardando || subiendo}>
                  {guardando ? 'Guardando...' : productoEditandoId ? 'Guardar Cambios' : 'Publicar Producto'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ---------- Modal de categorías ---------- */}
      {modalCategorias && (
        <div className="admin-modal-fondo" onClick={() => { setModalCategorias(false); setCategoriaEditando(null); }}>
          <div className="admin-modal-contenido" onClick={(e) => e.stopPropagation()}>
            <h2 className="admin-modal-titulo">Categorías</h2>

            <ul className="admin-categorias-lista">
              {categorias.length === 0 && <li className="admin-categorias-vacio">Todavía no hay categorías.</li>}
              {categorias.map((c) => (
                <li key={c.idCategoria}>
                  {categoriaEditando?.idCategoria === c.idCategoria ? (
                    <form onSubmit={guardarCategoria} className="admin-categoria-form">
                      <input
                        value={categoriaEditando.nombre}
                        onChange={(e) => setCategoriaEditando({ ...categoriaEditando, nombre: e.target.value })}
                        className="admin-form-input"
                        maxLength={100}
                        required
                        autoFocus
                        aria-label="Nuevo nombre de la categoría"
                      />
                      <button type="submit" className="btn-admin-editar">Guardar</button>
                      <button type="button" className="btn-admin-cancelar" onClick={() => setCategoriaEditando(null)}>Cancelar</button>
                    </form>
                  ) : (
                    <>
                      <span>{c.nombre}</span>
                      <span className="admin-categoria-acciones">
                        <button className="btn-admin-editar" onClick={() => setCategoriaEditando({ ...c })}>Renombrar</button>
                        <button className="btn-admin-eliminar" onClick={() => eliminarCategoria(c)}>Eliminar</button>
                      </span>
                    </>
                  )}
                </li>
              ))}
            </ul>

            <form onSubmit={crearCategoria} className="admin-categoria-form admin-categoria-nueva">
              <input
                value={nuevaCategoria}
                onChange={(e) => setNuevaCategoria(e.target.value)}
                className="admin-form-input"
                placeholder="Nueva categoría"
                maxLength={100}
                aria-label="Nombre de la nueva categoría"
              />
              <button type="submit" className="btn-admin-guardar" disabled={!nuevaCategoria.trim()}>Agregar</button>
            </form>

            <div className="admin-modal-acciones">
              <button type="button" className="btn-admin-cancelar" onClick={() => { setModalCategorias(false); setCategoriaEditando(null); }}>
                Cerrar
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
