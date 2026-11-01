import React, { useState, useEffect } from 'react';
import { useAuth } from '../AuthContext';
import { useAviso } from '../useAviso';
import { formatearPrecio, normalizarTexto } from '../formato';
import { urlFotoPerfil } from '../imagenes';
import './UsuariosAdmin.css';
import { API_URL } from '../config';


async function leerRespuesta(res, mensajePorDefecto) {
  const data = await res.json().catch(() => null);
  if (!res.ok) throw new Error(data?.mensaje || mensajePorDefecto);
  return data;
}

const direccionTexto = (d) => `${d.calle} ${d.altura}, ${d.ciudad}, ${d.provincia}`;

export default function UsuariosAdmin() {
  const { usuario: yo } = useAuth();
  const [aviso, mostrarAviso] = useAviso();

  const [usuarios, setUsuarios] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState(null);
  const [busqueda, setBusqueda] = useState('');
  const [filtro, setFiltro] = useState('todos');

  const [detalle, setDetalle] = useState(null);       // usuario abierto en el modal
  const [cambioClave, setCambioClave] = useState(null); // usuario al que se le cambia la clave
  const [claves, setClaves] = useState({ nueva: '', confirmar: '' });
  const [guardandoClave, setGuardandoClave] = useState(false);

  const cargar = () =>
    fetch(`${API_URL}/admin/usuarios`)
      .then((res) => leerRespuesta(res, 'error'))
      .then((lista) => {
        setUsuarios(lista);
        setCargando(false);
      })
      .catch(() => {
        setError('No se pudieron cargar los usuarios. ¿Está corriendo el backend?');
        setCargando(false);
      });

  useEffect(() => {
    cargar();
  }, []);

  const reemplazar = (actualizado) => {
    setUsuarios((prev) => prev.map((u) => (u.idUsuario === actualizado.idUsuario ? actualizado : u)));
    setDetalle((d) => (d && d.idUsuario === actualizado.idUsuario ? actualizado : d));
  };

  // ---------- Acciones ----------
  const alternarActivo = (u) => {
    const desactivar = u.activo;
    if (desactivar && !window.confirm(`¿Desactivar a ${u.nombre} ${u.apellido || ''}? No va a poder iniciar sesión hasta que lo reactives.`)) {
      return;
    }
    fetch(`${API_URL}/admin/usuarios/${u.idUsuario}/activo`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ activo: !u.activo }),
    })
      .then((res) => leerRespuesta(res, 'No se pudo cambiar el estado del usuario.'))
      .then((actualizado) => {
        reemplazar(actualizado);
        mostrarAviso(actualizado.activo ? 'Usuario reactivado' : 'Usuario desactivado');
      })
      .catch((err) => mostrarAviso(err.message, 'error'));
  };

  const eliminar = (u) => {
    if (!window.confirm(`¿Eliminar definitivamente a ${u.nombre} ${u.apellido || ''} (${u.email})? Esta acción no se puede deshacer.`)) {
      return;
    }
    fetch(`${API_URL}/admin/usuarios/${u.idUsuario}`, { method: 'DELETE' })
      .then(async (res) => {
        if (!res.ok) await leerRespuesta(res, 'No se pudo eliminar el usuario.');
        setUsuarios((prev) => prev.filter((x) => x.idUsuario !== u.idUsuario));
        setDetalle(null);
        mostrarAviso('Usuario eliminado');
      })
      .catch((err) => mostrarAviso(err.message, 'error'));
  };

  const abrirCambioClave = (u) => {
    setClaves({ nueva: '', confirmar: '' });
    setCambioClave(u);
  };

  const claveValida = claves.nueva.length >= 8 && /\d/.test(claves.nueva);
  const clavesCoinciden = claves.nueva === claves.confirmar && claves.confirmar !== '';

  const guardarClave = (e) => {
    e.preventDefault();
    if (!claveValida || !clavesCoinciden) return;
    setGuardandoClave(true);
    fetch(`${API_URL}/admin/usuarios/${cambioClave.idUsuario}/contrasena`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ nueva: claves.nueva }),
    })
      .then(async (res) => {
        if (!res.ok) await leerRespuesta(res, 'No se pudo cambiar la contraseña.');
        mostrarAviso(`Contraseña de ${cambioClave.nombre} actualizada`);
        setCambioClave(null);
      })
      .catch((err) => mostrarAviso(err.message, 'error'))
      .finally(() => setGuardandoClave(false));
  };

  // ---------- Filtros ----------
  const texto = normalizarTexto(busqueda);
  const filtrados = usuarios.filter((u) => {
    if (filtro === 'clientes' && u.rol === 'ADMIN') return false;
    if (filtro === 'admins' && u.rol !== 'ADMIN') return false;
    if (filtro === 'inactivos' && u.activo) return false;
    if (!texto) return true;
    return [u.nombre, u.apellido, u.email, u.negocio?.nombre, u.negocio?.rubro]
      .some((campo) => normalizarTexto(campo).includes(texto));
  });

  if (cargando) return <p className="usuarios-vacio">Cargando usuarios...</p>;
  if (error) return <p className="usuarios-vacio">{error}</p>;

  const esYo = (u) => u.idUsuario === yo?.idUsuario;

  const acciones = (u) => (
    <div className="usuarios-acciones">
      <button className="usuarios-btn" onClick={() => abrirCambioClave(u)}>Cambiar contraseña</button>
      {!esYo(u) && (
        <>
          <button className="usuarios-btn usuarios-btn-secundario" onClick={() => alternarActivo(u)}>
            {u.activo ? 'Desactivar' : 'Reactivar'}
          </button>
          <button className="usuarios-btn usuarios-btn-peligro" onClick={() => eliminar(u)}>Eliminar</button>
        </>
      )}
    </div>
  );

  return (
    <div className="usuarios-contenedor">
      {aviso}
      <div className="usuarios-tarjeta">
        <h1 className="usuarios-titulo">Gestionar usuarios</h1>
        <p className="usuarios-subtitulo">Datos de cada cuenta, su negocio y sus compras. Desde acá podés cambiar contraseñas, desactivar o eliminar usuarios.</p>

        <div className="usuarios-barra">
          <input
            type="text"
            placeholder="Buscar por nombre, correo o negocio..."
            value={busqueda}
            onChange={(e) => setBusqueda(e.target.value)}
            className="usuarios-busqueda"
          />
          <select value={filtro} onChange={(e) => setFiltro(e.target.value)} className="usuarios-filtro" aria-label="Filtrar usuarios">
            <option value="todos">Todos</option>
            <option value="clientes">Clientes</option>
            <option value="admins">Administradores</option>
            <option value="inactivos">Desactivados</option>
          </select>
          <span className="usuarios-contador">{filtrados.length} {filtrados.length === 1 ? 'usuario' : 'usuarios'}</span>
        </div>

        {filtrados.length === 0 ? (
          <p className="usuarios-vacio">No hay usuarios que coincidan.</p>
        ) : (
          <div className="usuarios-lista">
            {filtrados.map((u) => (
              <div key={u.idUsuario} className={`usuarios-item ${u.activo ? '' : 'usuarios-item-inactivo'}`}>
                <div className="usuarios-item-principal">
                  {urlFotoPerfil(u.fotoPerfil) ? (
                    <img src={urlFotoPerfil(u.fotoPerfil)} alt="" className="usuarios-avatar" />
                  ) : (
                    <div className="usuarios-avatar usuarios-avatar-inicial">{(u.nombre || '?').charAt(0).toUpperCase()}</div>
                  )}
                  <div className="usuarios-item-datos">
                    <div className="usuarios-item-nombre">
                      {u.nombre} {u.apellido}
                      {u.rol === 'ADMIN' && <span className="usuarios-badge usuarios-badge-admin">Admin</span>}
                      {!u.activo && <span className="usuarios-badge usuarios-badge-inactivo">Desactivado</span>}
                      {esYo(u) && <span className="usuarios-badge">Vos</span>}
                    </div>
                    <div className="usuarios-item-email">{u.email}</div>
                    <div className="usuarios-item-negocio">
                      {u.negocio ? `${u.negocio.nombre} · ${u.negocio.rubro}` : 'Sin datos de negocio'}
                    </div>
                  </div>
                  <div className="usuarios-item-compras">
                    <strong>{u.cantidadPedidos}</strong> {u.cantidadPedidos === 1 ? 'pedido' : 'pedidos'}
                    <span>{formatearPrecio(u.totalGastado)}</span>
                  </div>
                </div>
                <div className="usuarios-item-pie">
                  <button className="usuarios-link" onClick={() => setDetalle(u)}>Ver toda la información</button>
                  {acciones(u)}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* ---------- Detalle ---------- */}
      {detalle && (
        <div className="admin-modal-fondo" onClick={() => setDetalle(null)}>
          <div className="admin-modal-contenido usuarios-modal" onClick={(e) => e.stopPropagation()}>
            <h2 className="admin-modal-titulo">{detalle.nombre} {detalle.apellido}</h2>

            <section className="usuarios-detalle-seccion">
              <h3>Cuenta</h3>
              <dl>
                <dt>Correo</dt><dd>{detalle.email}</dd>
                <dt>Rol</dt><dd>{detalle.rol === 'ADMIN' ? 'Administrador' : 'Cliente'}</dd>
                <dt>Estado</dt><dd>{detalle.activo ? 'Activo' : 'Desactivado'}</dd>
                <dt>N° de usuario</dt><dd>{detalle.idUsuario}</dd>
              </dl>
            </section>

            <section className="usuarios-detalle-seccion">
              <h3>Negocio</h3>
              {detalle.negocio ? (
                <dl>
                  <dt>Nombre</dt><dd>{detalle.negocio.nombre}</dd>
                  <dt>Rubro</dt><dd>{detalle.negocio.rubro}</dd>
                  <dt>Dirección</dt><dd>{direccionTexto(detalle.negocio)}</dd>
                </dl>
              ) : (
                <p className="usuarios-nota">Todavía no cargó los datos de su negocio.</p>
              )}
            </section>

            <section className="usuarios-detalle-seccion">
              <h3>Direcciones de envío guardadas</h3>
              {detalle.direcciones.length === 0 ? (
                <p className="usuarios-nota">No tiene direcciones guardadas.</p>
              ) : (
                <ul>
                  {detalle.direcciones.map((d) => <li key={d.idDireccion}>{direccionTexto(d)}</li>)}
                </ul>
              )}
            </section>

            <section className="usuarios-detalle-seccion">
              <h3>Compras</h3>
              <dl>
                <dt>Pedidos</dt><dd>{detalle.cantidadPedidos}</dd>
                <dt>Total comprado</dt><dd>{formatearPrecio(detalle.totalGastado)} <small>(sin pedidos cancelados)</small></dd>
              </dl>
            </section>

            {acciones(detalle)}

            <div className="admin-modal-acciones">
              <button type="button" className="btn-admin-cancelar" onClick={() => setDetalle(null)}>Cerrar</button>
            </div>
          </div>
        </div>
      )}

      {/* ---------- Cambiar contraseña ---------- */}
      {cambioClave && (
        <div className="admin-modal-fondo" onClick={() => setCambioClave(null)}>
          <div className="admin-modal-contenido usuarios-modal" onClick={(e) => e.stopPropagation()}>
            <h2 className="admin-modal-titulo">Nueva contraseña</h2>
            <p className="usuarios-nota">
              Para <strong>{cambioClave.nombre} {cambioClave.apellido}</strong> ({cambioClave.email}).
              Comunicale la nueva contraseña por un medio seguro.
            </p>
            <form onSubmit={guardarClave} className="usuarios-form-clave">
              <input
                type="password"
                placeholder="Nueva contraseña"
                value={claves.nueva}
                onChange={(e) => setClaves({ ...claves, nueva: e.target.value })}
                className="admin-form-input"
                autoFocus
                autoComplete="new-password"
              />
              <input
                type="password"
                placeholder="Repetir contraseña"
                value={claves.confirmar}
                onChange={(e) => setClaves({ ...claves, confirmar: e.target.value })}
                className="admin-form-input"
                autoComplete="new-password"
              />
              <ul className="usuarios-requisitos">
                <li className={claves.nueva.length >= 8 ? 'ok' : ''}>Mínimo 8 caracteres</li>
                <li className={/\d/.test(claves.nueva) ? 'ok' : ''}>Al menos un número</li>
                <li className={clavesCoinciden ? 'ok' : ''}>Las dos coinciden</li>
              </ul>
              <div className="admin-modal-acciones">
                <button type="button" className="btn-admin-cancelar" onClick={() => setCambioClave(null)}>Cancelar</button>
                <button type="submit" className="btn-admin-guardar" disabled={!claveValida || !clavesCoinciden || guardandoClave}>
                  {guardandoClave ? 'Guardando...' : 'Guardar contraseña'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
