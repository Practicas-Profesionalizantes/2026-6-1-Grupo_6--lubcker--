import React, { useState, useEffect } from 'react';
import { useAviso } from '../useAviso';
import { formatearDinero, normalizarTexto } from '../formato';
import { UNIDADES_INSUMO, simboloUnidad } from '../medidas';
import InputNumero from '../components/InputNumero';
import './InsumosAdmin.css';
import { API_URL } from '../config';


const FORMULARIO_VACIO = { nombre: '', unidadMedida: 'kg', costoUnitario: '', proveedor: '' };

// Lee el mensaje de error que manda el backend ({"mensaje": "..."})
async function leerRespuesta(res, mensajePorDefecto) {
  const data = await res.json().catch(() => null);
  if (!res.ok) throw new Error(data?.mensaje || mensajePorDefecto);
  return data;
}

// Gestor de insumos: la materia prima que usa el negocio, con su costo por unidad (obligatorio).
export default function InsumosAdmin() {
  const [insumos, setInsumos] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState(null);
  const [busqueda, setBusqueda] = useState('');
  const [modalAbierto, setModalAbierto] = useState(false);
  const [editandoId, setEditandoId] = useState(null);
  const [formulario, setFormulario] = useState(FORMULARIO_VACIO);
  const [guardando, setGuardando] = useState(false);
  const [aviso, mostrarAviso] = useAviso();

  const cargar = () =>
    fetch(`${API_URL}/admin/insumos`)
      .then((res) => leerRespuesta(res, 'error'))
      .then((data) => {
        setInsumos(data);
        setCargando(false);
      })
      .catch(() => {
        setError('No se pudieron cargar los insumos. ¿Está corriendo el backend?');
        setCargando(false);
      });

  useEffect(() => {
    cargar();
  }, []);

  const abrirNuevo = () => {
    setEditandoId(null);
    setFormulario(FORMULARIO_VACIO);
    setModalAbierto(true);
  };

  const abrirEdicion = (insumo) => {
    setEditandoId(insumo.idInsumo);
    setFormulario({
      nombre: insumo.nombre,
      unidadMedida: insumo.unidadMedida,
      costoUnitario: String(insumo.costoUnitario),
      proveedor: insumo.proveedor || '',
    });
    setModalAbierto(true);
  };

  const cerrarModal = () => {
    setModalAbierto(false);
    setEditandoId(null);
    setFormulario(FORMULARIO_VACIO);
  };

  const manejarCambio = (e) => setFormulario((prev) => ({ ...prev, [e.target.name]: e.target.value }));

  const guardar = async (e) => {
    e.preventDefault();
    const costo = Number(String(formulario.costoUnitario).replace(',', '.'));
    if (!costo || costo <= 0) {
      mostrarAviso('El costo por unidad es obligatorio y tiene que ser mayor a 0.', 'error');
      return;
    }
    setGuardando(true);
    try {
      await fetch(editandoId ? `${API_URL}/admin/insumos/${editandoId}` : `${API_URL}/admin/insumos`, {
        method: editandoId ? 'PUT' : 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          nombre: formulario.nombre.trim(),
          unidadMedida: formulario.unidadMedida,
          costoUnitario: costo,
          proveedor: formulario.proveedor.trim(),
        }),
      }).then((res) => leerRespuesta(res, 'No se pudo guardar el insumo.'));
      mostrarAviso(editandoId ? 'Insumo actualizado' : 'Insumo agregado');
      cerrarModal();
      cargar();
    } catch (err) {
      mostrarAviso(err instanceof TypeError ? 'No se pudo conectar con el servidor.' : err.message, 'error');
    } finally {
      setGuardando(false);
    }
  };

  const eliminar = (insumo) => {
    if (!window.confirm(`¿Eliminar el insumo "${insumo.nombre}"?`)) return;
    fetch(`${API_URL}/admin/insumos/${insumo.idInsumo}`, { method: 'DELETE' })
      .then(async (res) => {
        if (!res.ok) await leerRespuesta(res, 'No se pudo eliminar el insumo.');
        mostrarAviso(`"${insumo.nombre}" eliminado`);
        cargar();
      })
      .catch((err) => mostrarAviso(err.message, 'error'));
  };

  const texto = normalizarTexto(busqueda);
  const filtrados = insumos.filter((i) => !texto || normalizarTexto(`${i.nombre} ${i.proveedor || ''}`).includes(texto));

  if (cargando) return <p className="ins-vacio">Cargando insumos...</p>;
  if (error) return <p className="ins-vacio">{error}</p>;

  return (
    <div className="ins-contenedor">
      {aviso}
      <div className="ins-tarjeta">
        <div className="ins-encabezado">
          <div>
            <h1 className="ins-titulo">Insumos</h1>
          </div>
          <button className="ins-btn ins-btn-principal" onClick={abrirNuevo}>+ Agregar insumo</button>
        </div>

        <div className="ins-barra">
          <input
            type="text"
            className="ins-input"
            placeholder="Buscar por nombre o proveedor..."
            value={busqueda}
            onChange={(e) => setBusqueda(e.target.value)}
          />
          <span className="ins-contador">{filtrados.length} {filtrados.length === 1 ? 'insumo' : 'insumos'}</span>
        </div>

        {filtrados.length === 0 ? (
          <div className="ins-vacio">
            <p>{insumos.length === 0 ? 'Todavía no cargaste ningún insumo.' : 'No se encontraron insumos con esa búsqueda.'}</p>
            {insumos.length === 0 && <button className="ins-btn ins-btn-principal" onClick={abrirNuevo}>+ Agregar tu primer insumo</button>}
          </div>
        ) : (
          <div className="ins-tabla-scroll">
            <table className="ins-tabla">
              <thead>
                <tr>
                  <th>Insumo</th>
                  <th>Costo por unidad</th>
                  <th>Proveedor</th>
                  <th>Usado en</th>
                  <th>Acciones</th>
                </tr>
              </thead>
              <tbody>
                {filtrados.map((i) => (
                  <tr key={i.idInsumo}>
                    <td className="ins-nombre">{i.nombre}</td>
                    <td>{formatearDinero(i.costoUnitario)} <span className="ins-unidad">por {simboloUnidad(i.unidadMedida)}</span></td>
                    <td>{i.proveedor || <span className="ins-unidad">-</span>}</td>
                    <td>{i.productosQueLoUsan === 0 ? <span className="ins-unidad">Ningún producto</span> : `${i.productosQueLoUsan} ${i.productosQueLoUsan === 1 ? 'producto' : 'productos'}`}</td>
                    <td className="ins-acciones">
                      <button className="ins-btn" onClick={() => abrirEdicion(i)}>Editar</button>
                      <button className="ins-btn ins-btn-peligro" onClick={() => eliminar(i)}>Eliminar</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {modalAbierto && (
        <div className="ins-modal-fondo" onClick={cerrarModal}>
          <div className="ins-modal" onClick={(e) => e.stopPropagation()}>
            <h2>{editandoId ? 'Editar insumo' : 'Agregar insumo'}</h2>
            <form onSubmit={guardar}>
              <label htmlFor="ins-nombre">Nombre</label>
              <input id="ins-nombre" name="nombre" className="ins-input" value={formulario.nombre} onChange={manejarCambio}
                required maxLength={100} />

              <div className="ins-fila">
                <div>
                  <label htmlFor="ins-unidad">Unidad de medida</label>
                  <select id="ins-unidad" name="unidadMedida" className="ins-input" value={formulario.unidadMedida} onChange={manejarCambio}>
                    {UNIDADES_INSUMO.map((u) => <option key={u.valor} value={u.valor}>{u.nombre}</option>)}
                  </select>
                </div>
                <div>
                  <label htmlFor="ins-costo">Costo por {simboloUnidad(formulario.unidadMedida)} ($) *</label>
                  <InputNumero id="ins-costo" name="costoUnitario" className="ins-input" value={formulario.costoUnitario} onChange={manejarCambio}
                    required decimales={2} />
                </div>
              </div>

              <label htmlFor="ins-proveedor">Proveedor (opcional)</label>
              <input id="ins-proveedor" name="proveedor" className="ins-input" value={formulario.proveedor} onChange={manejarCambio}
                maxLength={100} />

              <div className="ins-modal-acciones">
                <button type="button" className="ins-btn" onClick={cerrarModal}>Cancelar</button>
                <button type="submit" className="ins-btn ins-btn-principal" disabled={guardando}>
                  {guardando ? 'Guardando...' : 'Guardar'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
