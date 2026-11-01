import React, { useState, useEffect } from 'react';
import { useAuth } from '../AuthContext';
import { urlFotoPerfil, subirImagen } from '../imagenes';
import { useAviso } from '../useAviso';
import { PROVINCIAS } from '../provincias';
import './Perfil.css';
import { API_URL } from '../config';


const DIRECCION_VACIA = { provincia: '', ciudad: '', calle: '', altura: '' };
const NEGOCIO_VACIO = { nombre: '', rubro: '', provincia: '', ciudad: '', calle: '', altura: '' };

function actualizarUsuarioEnBackend(idUsuario, cuerpo) {
  return fetch(`${API_URL}/usuarios/${idUsuario}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(cuerpo),
  });
}

export default function Perfil() {
  const { usuario, actualizarUsuario } = useAuth();
  const [aviso, mostrarAviso] = useAviso();

  // --- Datos personales ---
  const [datos, setDatos] = useState({
    nombre: usuario.nombreUsuario || '',
    apellido: '',
    telefono: '',
    email: usuario.email || '',
  });
  const [guardandoDatos, setGuardandoDatos] = useState(false);

  // --- Negocio ---
  const [negocio, setNegocio] = useState({ ...NEGOCIO_VACIO });
  const [tieneNegocio, setTieneNegocio] = useState(false);
  const [rubros, setRubros] = useState([]);
  const [guardandoNegocio, setGuardandoNegocio] = useState(false);

  // --- Foto ---
  const [subiendoFoto, setSubiendoFoto] = useState(false);
  const fotoUrl = urlFotoPerfil(usuario.fotoPerfil);

  // --- Direcciones ---
  const [direcciones, setDirecciones] = useState([]);
  const [formDireccion, setFormDireccion] = useState(DIRECCION_VACIA);
  const [direccionEditandoId, setDireccionEditandoId] = useState(null);

  const idCliente = usuario.idCliente;

  const cargarDirecciones = () => {
    fetch(`${API_URL}/direcciones/cliente/${idCliente}`)
      .then((res) => (res.ok ? res.json() : []))
      .then(setDirecciones)
      .catch(() => mostrarAviso('No se pudieron cargar las direcciones.', 'error'));
  };

  useEffect(() => {
    // El apellido vive en el cliente, no en el usuario
    fetch(`${API_URL}/clientes/${idCliente}`)
      .then((res) => (res.ok ? res.json() : null))
      .then((cliente) => {
        if (cliente) setDatos((prev) => ({ ...prev, apellido: cliente.apellido || '', telefono: cliente.telefono || '' }));
      })
      .catch(() => {});
    cargarDirecciones();

    fetch(`${API_URL}/negocios/rubros`)
      .then((res) => (res.ok ? res.json() : []))
      .then(setRubros)
      .catch(() => setRubros([]));

    // 204 = todavía no cargó los datos del negocio (cuentas creadas antes de este paso)
    fetch(`${API_URL}/negocios/mio`)
      .then((res) => (res.status === 200 ? res.json() : null))
      .then((datos) => {
        if (datos) {
          setNegocio({ ...datos, altura: String(datos.altura) });
          setTieneNegocio(true);
        }
      })
      .catch(() => {});
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [idCliente]);

  // ---------- Datos personales ----------
  const guardarDatos = (e) => {
    e.preventDefault();
    setGuardandoDatos(true);
    actualizarUsuarioEnBackend(usuario.idUsuario, {
      nombreUsuario: datos.nombre.trim(),
      apellido: datos.apellido.trim(),
      telefono: datos.telefono.trim(),
      email: datos.email.trim(),
    })
      .then((res) => {
        if (res.status === 409) throw new Error('Ya existe una cuenta con ese correo electrónico.');
        if (res.status === 400) throw new Error('El teléfono no es válido. Ingresalo con el código de área, por ejemplo 11 4430-6194.');
        if (!res.ok) throw new Error('No se pudieron guardar los datos.');
        return res.json();
      })
      .then((actualizado) => {
        actualizarUsuario(actualizado);
        mostrarAviso('Datos actualizados');
      })
      .catch((err) => mostrarAviso(err.message, 'error'))
      .finally(() => setGuardandoDatos(false));
  };

  // ---------- Negocio ----------
  const cambioNegocio = (e) => setNegocio({ ...negocio, [e.target.name]: e.target.value });

  const guardarNegocio = (e) => {
    e.preventDefault();
    setGuardandoNegocio(true);
    fetch(`${API_URL}/negocios/mio`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        nombre: negocio.nombre.trim(),
        rubro: negocio.rubro,
        provincia: negocio.provincia,
        ciudad: negocio.ciudad.trim(),
        calle: negocio.calle.trim(),
        altura: parseInt(negocio.altura, 10) || 0,
      }),
    })
      .then(async (res) => {
        const data = await res.json().catch(() => null);
        if (!res.ok) throw new Error(data?.mensaje || 'No se pudieron guardar los datos del negocio.');
        setTieneNegocio(true);
        mostrarAviso('Datos del negocio guardados');
      })
      .catch((err) => mostrarAviso(err.message, 'error'))
      .finally(() => setGuardandoNegocio(false));
  };

  // ---------- Foto ----------
  const cambiarFoto = async (e) => {
    const archivo = e.target.files[0];
    e.target.value = '';
    if (!archivo) return;
    setSubiendoFoto(true);
    try {
      const ruta = await subirImagen(archivo);
      const res = await actualizarUsuarioEnBackend(usuario.idUsuario, { fotoPerfil: ruta });
      if (!res.ok) throw new Error('No se pudo guardar la foto.');
      actualizarUsuario(await res.json());
      mostrarAviso('Foto actualizada');
    } catch (err) {
      mostrarAviso(err.message, 'error');
    } finally {
      setSubiendoFoto(false);
    }
  };

  const quitarFoto = () => {
    actualizarUsuarioEnBackend(usuario.idUsuario, { fotoPerfil: '' })
      .then((res) => {
        if (!res.ok) throw new Error();
        return res.json();
      })
      .then((actualizado) => {
        actualizarUsuario(actualizado);
        mostrarAviso('Foto eliminada');
      })
      .catch(() => mostrarAviso('No se pudo quitar la foto.', 'error'));
  };

  // ---------- Direcciones ----------
  const cambioDireccion = (e) => setFormDireccion({ ...formDireccion, [e.target.name]: e.target.value });

  const guardarDireccion = (e) => {
    e.preventDefault();
    const esEdicion = direccionEditandoId !== null;
    fetch(esEdicion ? `${API_URL}/direcciones/${direccionEditandoId}` : `${API_URL}/direcciones`, {
      method: esEdicion ? 'PUT' : 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        idCliente,
        provincia: formDireccion.provincia.trim(),
        ciudad: formDireccion.ciudad.trim(),
        calle: formDireccion.calle.trim(),
        altura: parseInt(formDireccion.altura, 10) || 0,
      }),
    })
      .then((res) => {
        if (!res.ok) throw new Error();
        setFormDireccion(DIRECCION_VACIA);
        setDireccionEditandoId(null);
        cargarDirecciones();
        mostrarAviso(esEdicion ? 'Dirección actualizada' : 'Dirección agregada');
      })
      .catch(() => mostrarAviso('No se pudo guardar la dirección.', 'error'));
  };

  const editarDireccion = (d) => {
    setDireccionEditandoId(d.idDireccion);
    setFormDireccion({ provincia: d.provincia, ciudad: d.ciudad, calle: d.calle, altura: d.altura });
  };

  const cancelarEdicion = () => {
    setDireccionEditandoId(null);
    setFormDireccion(DIRECCION_VACIA);
  };

  const eliminarDireccion = (id) => {
    if (!window.confirm('¿Eliminar esta dirección?')) return;
    fetch(`${API_URL}/direcciones/${id}`, { method: 'DELETE' })
      .then((res) => {
        if (!res.ok) throw new Error();
        if (direccionEditandoId === id) cancelarEdicion();
        cargarDirecciones();
      })
      .catch(() =>
        mostrarAviso('No se pudo eliminar. Puede que esté asociada a un pedido.', 'error')
      );
  };

  const inicial = (usuario.nombreUsuario || '?').charAt(0).toUpperCase();

  return (
    <div className="perfil-contenedor">
      {aviso}
      <h1 className="perfil-titulo">Mi Perfil</h1>

      {/* FOTO */}
      <section className="perfil-tarjeta perfil-foto-seccion">
        {fotoUrl ? (
          <img src={fotoUrl} alt={usuario.nombreUsuario} className="perfil-foto" />
        ) : (
          <div className="perfil-foto perfil-foto-inicial">{inicial}</div>
        )}
        <div className="perfil-foto-acciones">
          <label className={`perfil-btn ${subiendoFoto ? 'perfil-btn-deshabilitado' : ''}`}>
            {subiendoFoto ? 'Subiendo...' : 'Cambiar foto'}
            <input
              type="file"
              accept="image/png,image/jpeg,image/webp,image/gif"
              onChange={cambiarFoto}
              disabled={subiendoFoto}
              hidden
            />
          </label>
          {fotoUrl && (
            <button type="button" className="perfil-btn perfil-btn-secundario" onClick={quitarFoto}>
              Quitar foto
            </button>
          )}
        </div>
      </section>

      {/* DATOS */}
      <section className="perfil-tarjeta">
        <h2>Datos personales</h2>
        <form onSubmit={guardarDatos} className="perfil-form">
          <label>
            Nombre
            <input
              type="text"
              value={datos.nombre}
              onChange={(e) => setDatos({ ...datos, nombre: e.target.value })}
              required
            />
          </label>
          <label>
            Apellido
            <input
              type="text"
              value={datos.apellido}
              onChange={(e) => setDatos({ ...datos, apellido: e.target.value })}
            />
          </label>
          <label>
            Teléfono (para coordinar la entrega)
            <input
              type="tel"
              value={datos.telefono}
              onChange={(e) => setDatos({ ...datos, telefono: e.target.value })}
              placeholder="Con código de área, ej: 11 4430-6194"
              maxLength={20}
            />
          </label>
          <label>
            Correo electrónico
            <input
              type="email"
              value={datos.email}
              onChange={(e) => setDatos({ ...datos, email: e.target.value })}
              required
            />
          </label>
          <button type="submit" className="perfil-btn" disabled={guardandoDatos}>
            {guardandoDatos ? 'Guardando...' : 'Guardar cambios'}
          </button>
        </form>
      </section>

      {/* NEGOCIO */}
      <section className="perfil-tarjeta">
        <h2>Datos del negocio</h2>
        {!tieneNegocio && (
          <p className="perfil-vacio">Todavía no cargaste los datos de tu negocio. Completalos acá.</p>
        )}
        <form onSubmit={guardarNegocio} className="perfil-form">
          <label>
            Nombre del negocio
            <input name="nombre" type="text" value={negocio.nombre} onChange={cambioNegocio} maxLength={100} required />
          </label>
          <label>
            Rubro
            <select name="rubro" value={negocio.rubro} onChange={cambioNegocio} required>
              <option value="" disabled>Elegí un rubro</option>
              {/* Si el rubro guardado ya no está en la lista, se sigue mostrando */}
              {negocio.rubro && !rubros.includes(negocio.rubro) && <option value={negocio.rubro}>{negocio.rubro}</option>}
              {rubros.map((r) => <option key={r} value={r}>{r}</option>)}
            </select>
          </label>
          <label>
            Provincia
            <select name="provincia" value={negocio.provincia} onChange={cambioNegocio} required>
              <option value="" disabled>Elegí una provincia</option>
              {negocio.provincia && !PROVINCIAS.includes(negocio.provincia) && <option value={negocio.provincia}>{negocio.provincia}</option>}
              {PROVINCIAS.map((p) => <option key={p} value={p}>{p}</option>)}
            </select>
          </label>
          <label>
            Ciudad / Localidad
            <input name="ciudad" type="text" value={negocio.ciudad} onChange={cambioNegocio} maxLength={100} required />
          </label>
          <div className="perfil-fila">
            <label>
              Calle
              <input name="calle" type="text" value={negocio.calle} onChange={cambioNegocio} maxLength={100} required />
            </label>
            <label>
              Altura
              <input name="altura" type="number" min="1" value={negocio.altura} onChange={cambioNegocio} required />
            </label>
          </div>
          <button type="submit" className="perfil-btn" disabled={guardandoNegocio}>
            {guardandoNegocio ? 'Guardando...' : 'Guardar datos del negocio'}
          </button>
        </form>
      </section>

      {/* DIRECCIONES */}
      <section className="perfil-tarjeta">
        <h2>Mis direcciones</h2>

        {direcciones.length === 0 ? (
          <p className="perfil-vacio">Todavía no guardaste ninguna dirección.</p>
        ) : (
          <ul className="perfil-direcciones">
            {direcciones.map((d) => (
              <li key={d.idDireccion}>
                <span>
                  {d.calle} {d.altura}, {d.ciudad}, {d.provincia}
                </span>
                <span className="perfil-direccion-acciones">
                  <button type="button" className="perfil-link" onClick={() => editarDireccion(d)}>
                    Editar
                  </button>
                  <button
                    type="button"
                    className="perfil-link perfil-link-peligro"
                    onClick={() => eliminarDireccion(d.idDireccion)}
                  >
                    Eliminar
                  </button>
                </span>
              </li>
            ))}
          </ul>
        )}

        <form onSubmit={guardarDireccion} className="perfil-form perfil-form-direccion">
          <h3>{direccionEditandoId !== null ? 'Editar dirección' : 'Agregar dirección'}</h3>
          <div className="perfil-fila">
            <input name="provincia" placeholder="Provincia" value={formDireccion.provincia} onChange={cambioDireccion} required />
            <input name="ciudad" placeholder="Ciudad" value={formDireccion.ciudad} onChange={cambioDireccion} required />
          </div>
          <div className="perfil-fila">
            <input name="calle" placeholder="Calle" value={formDireccion.calle} onChange={cambioDireccion} required />
            <input name="altura" type="number" min="0" placeholder="Altura" value={formDireccion.altura} onChange={cambioDireccion} required />
          </div>
          <div className="perfil-fila">
            <button type="submit" className="perfil-btn">
              {direccionEditandoId !== null ? 'Guardar dirección' : 'Agregar dirección'}
            </button>
            {direccionEditandoId !== null && (
              <button type="button" className="perfil-btn perfil-btn-secundario" onClick={cancelarEdicion}>
                Cancelar
              </button>
            )}
          </div>
        </form>
      </section>
    </div>
  );
}
