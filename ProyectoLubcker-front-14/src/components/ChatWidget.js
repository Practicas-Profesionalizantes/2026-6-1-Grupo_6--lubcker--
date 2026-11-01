import React, { useState, useEffect, useRef } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../AuthContext';
import './ChatWidget.css';
import { API_URL } from '../config';

const CLAVE_CHAT = 'lubcker_chat';
const CLAVE_GLOBO = 'lubcker_chat_globo';
const MAX_CARACTERES = 500;
const MAX_HISTORIAL = 10; // mensajes que se reenvían como contexto

const MENSAJE_INICIAL = {
  rol: 'assistant',
  inicial: true,
  texto:
    '¡Hola! Soy Lubcki, el asistente de Lubcker. Puedo ayudarte con preguntas frecuentes, cómo usar la página, ' +
    'asesorarte sobre productos y contarte cómo va tu pedido.',
};

const SUGERENCIAS = [
  '¿Cómo hago una compra?',
  '¿Qué métodos de pago hay?',
  '¿Cómo va mi pedido?',
  'Recomendame un producto',
];

// Texto plano con enlaces internos: [nombre](/producto/5) -> <Link>. Todo lo demás se muestra
// como texto (React lo escapa), así que la respuesta del asistente nunca puede inyectar HTML.
function renderizarTexto(texto, alNavegar) {
  const partes = [];
  const patron = /\[([^\]]+)\]\((\/[^)\s]*)\)/g;
  let ultimo = 0;
  let m;
  while ((m = patron.exec(texto)) !== null) {
    if (m.index > ultimo) partes.push(texto.slice(ultimo, m.index));
    // Solo rutas internas (empiezan con una sola "/")
    if (m[2].startsWith('//')) {
      partes.push(m[0]);
    } else {
      partes.push(
        <Link key={m.index} to={m[2]} onClick={alNavegar} className="chat-enlace">
          {m[1]}
        </Link>
      );
    }
    ultimo = m.index + m[0].length;
  }
  if (ultimo < texto.length) partes.push(texto.slice(ultimo));
  return partes;
}

function leerGuardado(idUsuario) {
  try {
    const crudo = sessionStorage.getItem(CLAVE_CHAT);
    const datos = crudo ? JSON.parse(crudo) : null;
    // Solo se reutiliza la conversación si es del mismo usuario (o de visitante)
    if (datos && datos.usuario === idUsuario && Array.isArray(datos.mensajes) && datos.mensajes.length > 0) {
      return datos.mensajes;
    }
  } catch {}
  return [MENSAJE_INICIAL];
}

export default function ChatWidget() {
  const { usuario } = useAuth();
  const idUsuario = usuario?.idUsuario ?? null;

  const [abierto, setAbierto] = useState(false);
  const [mensajes, setMensajes] = useState(() => leerGuardado(idUsuario));
  const [texto, setTexto] = useState('');
  const [cargando, setCargando] = useState(false);
  const [mostrarGlobo, setMostrarGlobo] = useState(false);

  const finRef = useRef(null);
  const inputRef = useRef(null);
  const botonRef = useRef(null);
  const idUsuarioPrevio = useRef(idUsuario);

  // Al iniciar o cerrar sesión se reinicia la conversación: puede contener datos de pedidos
  // del usuario anterior y no tiene que verlos otra persona en el mismo navegador.
  useEffect(() => {
    if (idUsuarioPrevio.current !== idUsuario) {
      idUsuarioPrevio.current = idUsuario;
      setMensajes([MENSAJE_INICIAL]);
      setCargando(false);
      setTexto('');
    }
  }, [idUsuario]);

  useEffect(() => {
    try {
      sessionStorage.setItem(CLAVE_CHAT, JSON.stringify({ usuario: idUsuario, mensajes }));
    } catch {}
  }, [mensajes, idUsuario]);

  // El globo "¿Tenés preguntas?" aparece una vez por visita, a los pocos segundos
  useEffect(() => {
    let yaVisto = false;
    try {
      yaVisto = sessionStorage.getItem(CLAVE_GLOBO) === '1';
    } catch {}
    if (yaVisto) return undefined;
    const timer = setTimeout(() => setMostrarGlobo(true), 1500);
    return () => clearTimeout(timer);
  }, []);

  const cerrarGlobo = () => {
    setMostrarGlobo(false);
    try {
      sessionStorage.setItem(CLAVE_GLOBO, '1');
    } catch {}
  };

  const abrir = () => {
    cerrarGlobo();
    setAbierto(true);
  };

  const cerrar = () => {
    setAbierto(false);
    botonRef.current?.focus();
  };

  useEffect(() => {
    if (abierto) inputRef.current?.focus();
  }, [abierto]);

  useEffect(() => {
    finRef.current?.scrollIntoView({ block: 'end' });
  }, [mensajes, cargando, abierto]);

  // Esc cierra la ventana
  useEffect(() => {
    if (!abierto) return undefined;
    const alTeclear = (e) => {
      if (e.key === 'Escape') cerrar();
    };
    document.addEventListener('keydown', alTeclear);
    return () => document.removeEventListener('keydown', alTeclear);
  }, [abierto]);

  const enviar = (preguntaDirecta) => {
    const pregunta = (preguntaDirecta ?? texto).trim();
    if (!pregunta || cargando) return;

    const nuevos = [...mensajes, { rol: 'user', texto: pregunta }];
    setMensajes(nuevos);
    setTexto('');
    setCargando(true);

    // Contexto: últimos mensajes reales (sin el saludo ni los errores locales)
    const historial = nuevos
      .filter((m) => !m.inicial && !m.error)
      .slice(-MAX_HISTORIAL)
      .map((m) => ({ rol: m.rol, texto: m.texto }));

    fetch(`${API_URL}/chat`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ mensajes: historial }),
    })
      .then(async (res) => {
        const data = await res.json().catch(() => null);
        if (!res.ok) {
          throw new Error(data?.mensaje || 'No pude responder ahora. Probá de nuevo en un momento.');
        }
        return data.respuesta;
      })
      .then((respuesta) => setMensajes((prev) => [...prev, { rol: 'assistant', texto: respuesta }]))
      .catch((err) =>
        setMensajes((prev) => [
          ...prev,
          {
            rol: 'assistant',
            error: true,
            texto:
              err instanceof TypeError
                ? 'No pude conectarme con el servidor. Revisá tu conexión e intentá de nuevo.'
                : err.message,
          },
        ])
      )
      .finally(() => setCargando(false));
  };

  const nuevaConversacion = () => {
    setMensajes([MENSAJE_INICIAL]);
    setTexto('');
    inputRef.current?.focus();
  };

  const soloSaludo = mensajes.length === 1 && mensajes[0].inicial;

  return (
    <div className="chat-contenedor">
      {abierto && (
        <section className="chat-ventana" role="dialog" aria-label="Chat con Lubcki">
          <header className="chat-cabecera">
            <div className="chat-cabecera-titulo">
              <span className="chat-avatar" aria-hidden="true">L</span>
              <div>
                <strong>Lubcki</strong>
                <small>Asistente de Lubcker</small>
              </div>
            </div>
            <div className="chat-cabecera-acciones">
              <button type="button" onClick={nuevaConversacion} title="Nueva conversación" aria-label="Nueva conversación">↺</button>
              <button type="button" onClick={cerrar} title="Cerrar" aria-label="Cerrar el chat">✕</button>
            </div>
          </header>

          <div className="chat-mensajes" aria-live="polite">
            {mensajes.map((m, i) => (
              <div key={i} className={`chat-mensaje chat-${m.rol} ${m.error ? 'chat-error' : ''}`}>
                {m.rol === 'assistant' ? renderizarTexto(m.texto, undefined) : m.texto}
              </div>
            ))}

            {soloSaludo && !cargando && (
              <div className="chat-sugerencias">
                {SUGERENCIAS.map((s) => (
                  <button key={s} type="button" onClick={() => enviar(s)}>{s}</button>
                ))}
              </div>
            )}

            {cargando && (
              <div className="chat-mensaje chat-assistant chat-escribiendo" aria-label="Lubcki está escribiendo">
                <span /><span /><span />
              </div>
            )}
            <div ref={finRef} />
          </div>

          <form
            className="chat-formulario"
            onSubmit={(e) => {
              e.preventDefault();
              enviar();
            }}
          >
            <input
              ref={inputRef}
              type="text"
              value={texto}
              onChange={(e) => setTexto(e.target.value)}
              maxLength={MAX_CARACTERES}
              placeholder="Escribí tu pregunta..."
              aria-label="Escribí tu pregunta"
              disabled={cargando}
              autoComplete="off"
            />
            <button type="submit" disabled={cargando || !texto.trim()} aria-label="Enviar">➤</button>
          </form>
          <p className="chat-aviso">Lubcki es una IA y puede equivocarse. No compartas contraseñas ni datos de tarjeta.</p>
        </section>
      )}

      {!abierto && mostrarGlobo && (
        <div className="chat-globo" role="status">
          <button type="button" className="chat-globo-texto" onClick={abrir}>
            ¿Tenés preguntas? ¡Preguntale a Lubcki acá!
          </button>
          <button type="button" className="chat-globo-cerrar" onClick={cerrarGlobo} aria-label="Cerrar aviso">✕</button>
        </div>
      )}

      <button
        ref={botonRef}
        type="button"
        className={`chat-boton ${abierto ? 'chat-boton-abierto' : ''}`}
        onClick={() => (abierto ? cerrar() : abrir())}
        aria-label={abierto ? 'Cerrar el chat' : 'Abrir el chat con Lubcki'}
        aria-expanded={abierto}
      >
        {abierto ? (
          <span aria-hidden="true">✕</span>
        ) : (
          <svg viewBox="0 0 24 24" width="28" height="28" aria-hidden="true">
            <path
              fill="currentColor"
              d="M4 3h16a2 2 0 0 1 2 2v11a2 2 0 0 1-2 2H9l-5 4v-4H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z"
            />
          </svg>
        )}
      </button>
    </div>
  );
}
