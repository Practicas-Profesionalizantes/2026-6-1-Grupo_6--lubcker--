import React, { useState, useEffect } from 'react';
import { ESTADOS_ENVIO, claseEstadoEnvio, claseEstadoPago, textoEstadoPago } from '../estados';
import { useAviso } from '../useAviso';
import { formatearPrecio } from '../formato';
import './PedidosAdmin.css';
import { etiquetaMedida } from '../medidas';
import { textoEntrega, mensajeEntrega, linkWhatsApp } from '../entrega';
import { API_URL } from '../config';


export default function PedidosAdmin() {
  const [ordenes, setOrdenes] = useState([]);
  const [pagos, setPagos] = useState({});   // idOrden -> pago
  const [envios, setEnvios] = useState({}); // idOrden -> envío
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState(null);
  const [busqueda, setBusqueda] = useState('');
  const [aviso, mostrarAviso] = useAviso();
  const [programando, setProgramando] = useState(null); // { orden, envio, fecha, franja, nota } mientras se edita la entrega
  const [guardandoEntrega, setGuardandoEntrega] = useState(false);
  const [recarga, setRecarga] = useState(0); // al cambiar, se vuelven a pedir los pedidos

  useEffect(() => {
    const pedir = (ruta) =>
      fetch(`${API_URL}/${ruta}`).then((res) => {
        if (!res.ok) throw new Error('error');
        return res.json();
      });

    Promise.all([pedir('ordenes'), pedir('pagos'), pedir('envios')])
      .then(([listaOrdenes, listaPagos, listaEnvios]) => {
        // Solo se muestran los pedidos pagados: los que quedaron sin pagar no son ventas
        const pagados = new Set(listaPagos.filter((p) => p.estado).map((p) => p.idOrden));
        setOrdenes(listaOrdenes.filter((o) => pagados.has(o.idOrden)).sort((a, b) => b.idOrden - a.idOrden));
        setPagos(Object.fromEntries(listaPagos.map((p) => [p.idOrden, p])));
        setEnvios(Object.fromEntries(listaEnvios.map((e) => [e.idOrden, e])));
        setCargando(false);

        // Pedidos sin pagar: se le pregunta a Mercado Pago si ya se pagaron (por si no llego el aviso, ej: localhost)
        const pendientes = listaOrdenes.filter((o) => {
          const envio = listaEnvios.find((e) => e.idOrden === o.idOrden);
          return !pagados.has(o.idOrden) && (envio?.estado || '').toLowerCase() !== 'cancelado';
        });
        Promise.all(pendientes.map((o) =>
          fetch(`${API_URL}/mercadopago/orden/${o.idOrden}/sincronizar`, { method: 'POST' })
            .then((res) => (res.ok ? res.json() : null))
            .catch(() => null)
        )).then((resultados) => {
          if (resultados.some((r) => r?.confirmado)) setRecarga((n) => n + 1);
        });
      })
      .catch(() => {
        setError('No se pudieron cargar los pedidos. ¿Está corriendo el backend?');
        setCargando(false);
      });
  }, [recarga]);

  const eliminarPedido = (orden) => {
    const estado = (envios[orden.idOrden]?.estado || '').toLowerCase();
    const devuelveStock = estado === 'pendiente' || estado === 'en preparación';
    const aviso = devuelveStock
      ? 'Las unidades vuelven al stock.'
      : 'El stock no se modifica.';
    if (!window.confirm(`¿Eliminar el pedido #${orden.idOrden} de forma definitiva? Se borran también su pago y su envío. ${aviso}`)) {
      return;
    }
    fetch(`${API_URL}/ordenes/${orden.idOrden}`, { method: 'DELETE' })
      .then((res) => {
        if (!res.ok) throw new Error();
        setOrdenes((prev) => prev.filter((o) => o.idOrden !== orden.idOrden));
        mostrarAviso(`Pedido #${orden.idOrden} eliminado`);
      })
      .catch(() => mostrarAviso('No se pudo eliminar el pedido.', 'error'));
  };

  const abrirEntrega = (orden, envio) =>
    setProgramando({
      orden,
      envio,
      fecha: envio.fechaEntrega || '',
      franja: envio.franjaHoraria || '',
      nota: envio.notaCliente || '',
    });

  const guardarEntrega = (e) => {
    e.preventDefault();
    setGuardandoEntrega(true);
    fetch(`${API_URL}/envios/${programando.envio.idEnvio}/entrega`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        fechaEntrega: programando.fecha,
        franjaHoraria: programando.franja.trim(),
        notaCliente: programando.nota.trim(),
      }),
    })
      .then(async (res) => {
        const data = await res.json().catch(() => null);
        if (!res.ok) throw new Error(data?.mensaje || 'No se pudo guardar la entrega.');
        setEnvios((prev) => ({ ...prev, [data.idOrden]: data }));
        mostrarAviso(`Entrega del pedido #${data.idOrden} guardada`);
        setProgramando(null);
      })
      .catch((err) => mostrarAviso(err instanceof TypeError ? 'No se pudo conectar con el servidor.' : err.message, 'error'))
      .finally(() => setGuardandoEntrega(false));
  };

  const cambiarEstadoEnvio = (envio, estado) => {
    if (estado === 'Cancelado' &&
        !window.confirm(`¿Cancelar el pedido #${envio.idOrden}? Las unidades vuelven al stock y el pedido no se podrá reactivar.`)) {
      return;
    }
    // El backend recibe el estado como texto plano (no JSON)
    fetch(`${API_URL}/envios/${envio.idEnvio}/estado`, {
      method: 'PUT',
      headers: { 'Content-Type': 'text/plain' },
      body: estado,
    })
      .then(async (res) => {
        const data = await res.json().catch(() => null);
        if (!res.ok) throw new Error(data?.mensaje || 'No se pudo cambiar el estado del envío.');
        return data;
      })
      .then((actualizado) => {
        setEnvios((prev) => ({ ...prev, [actualizado.idOrden]: actualizado }));
        mostrarAviso(`Envío del pedido #${actualizado.idOrden}: ${actualizado.estado}`);
      })
      .catch((err) => mostrarAviso(err.message, 'error'));
  };

  const total = (orden) => (orden.detalles || []).reduce((suma, d) => suma + d.subtotal, 0);

  const filtradas = ordenes.filter((o) => {
    const texto = busqueda.toLowerCase();
    return (
      String(o.idOrden).includes(texto) ||
      (o.nombreCliente || '').toLowerCase().includes(texto) ||
      (o.detalles || []).some((d) => d.nombreProducto.toLowerCase().includes(texto))
    );
  });

  if (cargando) return <p className="pedidos-vacio">Cargando pedidos...</p>;
  if (error) return <p className="pedidos-vacio">{error}</p>;

  return (
    <div className="pedidos-contenedor">
      {aviso}
      <div className="pedidos-tarjeta">
        <h1 className="pedidos-titulo">Gestión de Pedidos</h1>
        <p className="pedidos-subtitulo">Pedidos pagados: actualizá el estado de cada envío.</p>

        <div className="pedidos-barra">
          <input
            type="text"
            placeholder="Buscar por pedido, cliente o producto..."
            value={busqueda}
            onChange={(e) => setBusqueda(e.target.value)}
            className="pedidos-busqueda"
          />
          <span className="pedidos-contador">
            {filtradas.length} {filtradas.length === 1 ? 'pedido' : 'pedidos'}
          </span>
        </div>

        <div className="pedidos-tabla-scroll">
          <table className="pedidos-tabla">
            <thead>
              <tr>
                <th>Pedido</th>
                <th>Fecha</th>
                <th>Cliente</th>
                <th>Productos</th>
                <th>Total</th>
                <th>Pago</th>
                <th>Envío</th>
                <th>Entrega</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {filtradas.length === 0 ? (
                <tr>
                  <td colSpan="9" className="pedidos-vacio">
                    {ordenes.length === 0 ? 'Todavía no hay pedidos pagados.' : 'Ningún pedido coincide con la búsqueda.'}
                  </td>
                </tr>
              ) : (
                filtradas.map((orden) => {
                  const pago = pagos[orden.idOrden];
                  const envio = envios[orden.idOrden];
                  return (
                    <tr key={orden.idOrden}>
                      <td className="pedidos-id">#{orden.idOrden}</td>
                      <td>{orden.fecha ? new Date(orden.fecha).toLocaleDateString('es-AR') : '-'}</td>
                      <td>{orden.nombreCliente || '-'}</td>
                      <td>
                        {(orden.detalles || []).map((d) => (
                          <div key={d.idDetalle}>{d.cantidad} × {d.nombreProducto}{d.tamano > 1 ? ` (${etiquetaMedida(d.tamano, d.unidadMedida)})` : ''}</div>
                        ))}
                      </td>
                      <td className="pedidos-total">{formatearPrecio(total(orden))}</td>
                      <td>
                        {pago ? (
                          <>
                            <span className={`estado-badge ${claseEstadoPago(pago.estado)}`}>
                              {textoEstadoPago(pago.estado)}
                            </span>
                            <div className="pedidos-detalle-chico">{pago.nombreMetodoPago}</div>
                          </>
                        ) : '-'}
                      </td>
                      <td>
                        {envio ? (
                          <>
                            <select
                              className={`pedidos-select estado-badge ${claseEstadoEnvio(envio.estado)}`}
                              value={envio.estado}
                              onChange={(e) => cambiarEstadoEnvio(envio, e.target.value)}
                              disabled={(envio.estado || '').toLowerCase() === 'cancelado'}
                              title={(envio.estado || '').toLowerCase() === 'cancelado' ? 'Un pedido cancelado no se puede reactivar' : undefined}
                            >
                              {/* Si el estado actual no está en la lista, se conserva como opción */}
                              {!ESTADOS_ENVIO.includes(envio.estado) && (
                                <option value={envio.estado}>{envio.estado}</option>
                              )}
                              {ESTADOS_ENVIO.map((e) => (
                                <option key={e} value={e}>{e}</option>
                              ))}
                            </select>
                          </>
                        ) : '-'}
                      </td>
                      <td>
                        {envio && (envio.estado || '').toLowerCase() !== 'cancelado' ? (
                          <>
                            {envio.fechaEntrega ? (
                              <div className="pedidos-entrega-fecha">{textoEntrega(envio)}</div>
                            ) : (
                              <div className="pedidos-detalle-chico">Sin fecha</div>
                            )}
                            {envio.notaCliente && <div className="pedidos-detalle-chico">{envio.notaCliente}</div>}
                            <button className="pedidos-btn" onClick={() => abrirEntrega(orden, envio)}>
                              {envio.fechaEntrega ? 'Editar entrega' : 'Programar entrega'}
                            </button>
                            {orden.telefonoCliente ? (
                              <a className="pedidos-btn pedidos-btn-whatsapp" target="_blank" rel="noopener noreferrer"
                                href={linkWhatsApp(orden.telefonoCliente, mensajeEntrega(orden, envio))}>
                                Avisar por WhatsApp
                              </a>
                            ) : (
                              <div className="pedidos-detalle-chico">El cliente no dejó teléfono</div>
                            )}
                          </>
                        ) : '-'}
                      </td>
                      <td>
                        <button className="pedidos-btn pedidos-btn-peligro" onClick={() => eliminarPedido(orden)}>
                          Eliminar
                        </button>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>

      {programando && (
        <div className="pedidos-modal-fondo" onClick={() => setProgramando(null)}>
          <div className="pedidos-modal" onClick={(e) => e.stopPropagation()}>
            <h2>Entrega del pedido #{programando.orden.idOrden}</h2>
            <p className="pedidos-modal-cliente">
              {programando.orden.nombreCliente}
              {programando.orden.telefonoCliente ? ` · ${programando.orden.telefonoCliente}` : ''}
            </p>
            <form onSubmit={guardarEntrega}>
              <label htmlFor="entrega-fecha">Fecha estimada de entrega</label>
              <input id="entrega-fecha" type="date" className="pedidos-modal-input" value={programando.fecha}
                min={new Date().toISOString().slice(0, 10)}
                onChange={(e) => setProgramando({ ...programando, fecha: e.target.value })} />

              <label htmlFor="entrega-franja">Horario (opcional)</label>
              <input id="entrega-franja" type="text" className="pedidos-modal-input" maxLength={50}
                placeholder="Ej: de 14 a 16 hs" value={programando.franja}
                onChange={(e) => setProgramando({ ...programando, franja: e.target.value })} />

              <label htmlFor="entrega-nota">Mensaje para el cliente (opcional)</label>
              <textarea id="entrega-nota" className="pedidos-modal-input" rows={3} maxLength={255}
                placeholder="Ej: Te llamo cuando esté llegando" value={programando.nota}
                onChange={(e) => setProgramando({ ...programando, nota: e.target.value })} />

              <div className="pedidos-modal-acciones">
                <button type="button" className="pedidos-btn pedidos-btn-secundario" onClick={() => setProgramando(null)}>Cancelar</button>
                <button type="submit" className="pedidos-btn" disabled={guardandoEntrega}>
                  {guardandoEntrega ? 'Guardando...' : 'Guardar'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
