import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../AuthContext';
import { PROVINCIAS } from '../provincias';
import './Registro.css';
import { API_URL } from '../config';


// Registro en dos pasos: 1) datos personales  2) datos del negocio.
// Se envía todo junto al final, así no quedan cuentas a medio crear.
const Registro = () => {
  const [paso, setPaso] = useState(1);
  const [formData, setFormData] = useState({
    nombre: '',
    apellido: '',
    telefono: '',
    email: '',
    password: '',
    confirmPassword: ''
  });
  const [negocio, setNegocio] = useState({
    nombre: '',
    rubro: '',
    provincia: '',
    ciudad: '',
    calle: '',
    altura: ''
  });
  const [rubros, setRubros] = useState([]);
  const [error, setError] = useState('');
  const [cargando, setCargando] = useState(false);
  const navigate = useNavigate();
  const { iniciarSesion } = useAuth();

  // Los rubros salen del backend (una sola lista para toda la app)
  useEffect(() => {
    fetch(`${API_URL}/negocios/rubros`)
      .then((res) => (res.ok ? res.json() : []))
      .then(setRubros)
      .catch(() => setRubros([]));
  }, []);

  const manejarCambio = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
    if (error) setError('');
  };

  const manejarCambioNegocio = (e) => {
    setNegocio({ ...negocio, [e.target.name]: e.target.value });
    if (error) setError('');
  };

  const tieneMinimoCaracteres = formData.password.length >= 8;
  const tieneNumero = /\d/.test(formData.password);
  const contraseñasCoinciden = formData.password === formData.confirmPassword && formData.confirmPassword !== '';

  // Paso 1 -> paso 2
  const continuar = (e) => {
    e.preventDefault();

    if (!tieneMinimoCaracteres) {
      setError('La contraseña debe tener al menos 8 caracteres.');
      return;
    }
    if (!tieneNumero) {
      setError('La contraseña debe incluir al menos un número.');
      return;
    }
    if (!contraseñasCoinciden) {
      setError('Las contraseñas no coinciden.');
      return;
    }
    setError('');
    setPaso(2);
  };

  // Paso 2: crea la cuenta con los datos personales y del negocio
  const manejarEnvio = (e) => {
    e.preventDefault();
    setCargando(true);

    fetch(`${API_URL}/auth/registro`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        nombre: formData.nombre.trim(),
        apellido: formData.apellido.trim(),
        telefono: formData.telefono.trim(),
        email: formData.email.trim(),
        contrasena: formData.password,
        negocio: {
          nombre: negocio.nombre.trim(),
          rubro: negocio.rubro,
          provincia: negocio.provincia,
          ciudad: negocio.ciudad.trim(),
          calle: negocio.calle.trim(),
          altura: parseInt(negocio.altura, 10) || 0,
        },
      }),
    })
      .then(async (res) => {
        const data = await res.json().catch(() => null);
        if (!res.ok) {
          const err = new Error(data?.mensaje || 'No se pudo crear la cuenta. Revisá los datos.');
          err.estado = res.status;
          throw err;
        }
        return data;
      })
      .then(({ token, usuario }) => {
        iniciarSesion({ ...usuario, token });
        navigate('/');
      })
      .catch((err) => {
        // Si el problema es del correo o la contraseña, vuelve al primer paso para corregirlo
        if (err.estado === 409 || /correo|contraseña|nombre es|apellido|teléfono/i.test(err.message)) {
          setPaso(1);
        }
        setError(
          err instanceof TypeError
            ? 'No se pudo conectar con el servidor. ¿Está corriendo el backend?'
            : err.message
        );
        setCargando(false);
      });
  };

  return (
    <div className="registro-contenedor">
      <div className="registro-tarjeta">
        <h2>Crear Cuenta</h2>
        <p className="registro-subtitulo">
          {paso === 1 ? 'Paso 1 de 2: tus datos personales' : 'Paso 2 de 2: los datos de tu negocio'}
        </p>

        <div className="registro-pasos" aria-hidden="true">
          <span className={paso >= 1 ? 'activo' : ''} />
          <span className={paso >= 2 ? 'activo' : ''} />
        </div>

        {error && <div className="registro-alerta-error">{error}</div>}

        {paso === 1 ? (
          <form onSubmit={continuar} className="registro-formulario">
            <div className="grupo-input">
              <input
                type="text"
                name="nombre"
                placeholder="Nombre"
                value={formData.nombre}
                onChange={manejarCambio}
                maxLength={100}
                required
              />
            </div>

            <div className="grupo-input">
              <input
                type="text"
                name="apellido"
                placeholder="Apellido"
                value={formData.apellido}
                onChange={manejarCambio}
                maxLength={100}
                required
              />
            </div>

            <div className="grupo-input">
              <input
                type="tel"
                name="telefono"
                placeholder="Teléfono con código de área (ej: 11 4430-6194)"
                value={formData.telefono}
                onChange={manejarCambio}
                maxLength={20}
                required
              />
            </div>

            <div className="grupo-input">
              <input
                type="email"
                name="email"
                placeholder="Correo electrónico"
                value={formData.email}
                onChange={manejarCambio}
                maxLength={100}
                required
              />
            </div>

            <div className="grupo-input">
              <input
                type="password"
                name="password"
                placeholder="Contraseña"
                value={formData.password}
                onChange={manejarCambio}
                autoComplete="new-password"
                required
              />
            </div>

            <div className="grupo-input">
              <input
                type="password"
                name="confirmPassword"
                placeholder="Confirmar contraseña"
                value={formData.confirmPassword}
                onChange={manejarCambio}
                autoComplete="new-password"
                required
              />
            </div>

            <div className="registro-requisitos">
              <p className={tieneMinimoCaracteres ? "cumplido" : "pendiente"}>
                {tieneMinimoCaracteres ? "✓" : "○"} Mínimo 8 caracteres
              </p>
              <p className={tieneNumero ? "cumplido" : "pendiente"}>
                {tieneNumero ? "✓" : "○"} Incluye al menos un número
              </p>
              {formData.confirmPassword && (
                <p className={contraseñasCoinciden ? "cumplido" : "error-coincidencia"}>
                  {contraseñasCoinciden ? "✓ Las contraseñas coinciden" : "✕ Las contraseñas no coinciden"}
                </p>
              )}
            </div>

            <button
              type="submit"
              className="btn-registro-primario"
              disabled={!tieneMinimoCaracteres || !tieneNumero || !contraseñasCoinciden}
            >
              Continuar
            </button>
          </form>
        ) : (
          <form onSubmit={manejarEnvio} className="registro-formulario">
            <div className="grupo-input">
              <input
                type="text"
                name="nombre"
                placeholder="Nombre del negocio"
                value={negocio.nombre}
                onChange={manejarCambioNegocio}
                maxLength={100}
                required
                autoFocus
              />
            </div>

            <div className="grupo-input">
              <select name="rubro" value={negocio.rubro} onChange={manejarCambioNegocio} required aria-label="Rubro">
                <option value="" disabled>Rubro del negocio</option>
                {rubros.map((r) => <option key={r} value={r}>{r}</option>)}
              </select>
            </div>

            <p className="registro-seccion">Dirección del negocio</p>

            <div className="grupo-input">
              <select name="provincia" value={negocio.provincia} onChange={manejarCambioNegocio} required aria-label="Provincia">
                <option value="" disabled>Provincia</option>
                {PROVINCIAS.map((p) => <option key={p} value={p}>{p}</option>)}
              </select>
            </div>

            <div className="grupo-input">
              <input
                type="text"
                name="ciudad"
                placeholder="Ciudad / Localidad"
                value={negocio.ciudad}
                onChange={manejarCambioNegocio}
                maxLength={100}
                required
              />
            </div>

            <div className="registro-fila">
              <div className="grupo-input">
                <input
                  type="text"
                  name="calle"
                  placeholder="Calle"
                  value={negocio.calle}
                  onChange={manejarCambioNegocio}
                  maxLength={100}
                  required
                />
              </div>
              <div className="grupo-input registro-altura">
                <input
                  type="number"
                  name="altura"
                  placeholder="Altura"
                  min="1"
                  value={negocio.altura}
                  onChange={manejarCambioNegocio}
                  required
                />
              </div>
            </div>

            <div className="registro-botones">
              <button type="button" className="btn-registro-secundario" onClick={() => { setError(''); setPaso(1); }}>
                Volver
              </button>
              <button type="submit" className="btn-registro-primario" disabled={cargando}>
                {cargando ? 'Creando cuenta...' : 'Crear cuenta'}
              </button>
            </div>
          </form>
        )}

        <p className="registro-pie">
          ¿Ya tienes cuenta? <Link to="/login" className="link-lubcker">Inicia Sesión</Link>
        </p>
      </div>
    </div>
  );
};

export default Registro;
