import React, { useState, useEffect, useRef } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../AuthContext';
import { claseEstadoEnvio, claseEstadoPago, textoEstadoPago } from '../estados';
import { formatearPrecio } from '../formato';
import { etiquetaMedida } from '../medidas';
import { textoEntrega } from '../entrega';
import { abrirVentanaDePago, irAlPago, cerrarVentanaDePago } from '../pagoVentana';
import './Historial.css';
import { API_URL } from '../config';


export default function Historial() {
  const { usuario } = useAuth();
  const [ordenes, setOrdenes] = useState([]);
  const [pagos, setPagos] = useState({});   // idOrden -> pago
  const [envios, setEnvios] = useState({});  // idOrden -> envío
  const [resenados, setResenados] = useState([]); // ids de productos que ya reseñó
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState(null);
  const [busqueda, setBusqueda] = useState('');
  const [errorPago, setErrorPago] = useState(null);
  const [recarga, setRecarga] = useState(0);        // al cambiar, se vuelven a pedir los pedidos
  const hayPendientes = useRef(false);

  const idCliente = usuario?.idCliente;

  useEffect(() => {
    if (!idCliente) {
      setCargando(false);
      return;
    }
    if (recarga === 0) setCargando(true);
    setError(null);
    const pedir = (ruta) =>
      fetch(`${API_URL}/${ruta}/cliente/${idCliente}`).then((res) => {
        if (!res.ok) throw new Error('error');
        return res.json();
      });

    // Las reseñas propias no son críticas: si fallan, el historial se muestra igual
    fetch(`${API_URL}/resenas/mias`)
      .then((res) => (res.ok ? res.json() : []))
      .then((lista) => setResenados(lista.map((r) => r.idProducto)))
      .catch(() => setResenados([]));

    Promise.all([pedir('ordenes'), pedir('pagos'), pedir('envios')])
      .then(([data, listaPagos, listaEnvios]) => {
        // Se indexan por orden para unirlos con cada fila de la tabla
        setPagos(Object.fromEntries(listaPagos.map((p) => [p.idOrden, p])));
        setEnvios(Object.fromEntries(listaEnvios.map((e) => [e.idOrden, e])));
        // Solo se muestran los pedidos ya pagados: los que quedaron sin pagar (carrito abandonado, o se
        // fue a Mercado Pago y no pagó) no son compras y se cancelan solos pasadas unas horas
        const pagados = new Set(listaPagos.filter((p) => p.estado).map((p) => p.idOrden));
        setOrdenes(data.filter((o) => pagados.has(o.idOrden)).sort((a, b) => b.idOrden - a.idOrden));
        setCargando(false);

        // Pedidos con pago pendiente: se le pregunta a Mercado Pago si ya se pagaron. Asi el estado se
        // actualiza aunque no haya llegado el aviso (por ejemplo en localhost) o se haya cerrado la pestaña.
        const pendientes = data.filter((o) => {
          const pago = listaPagos.find((p) => p.idOrden === o.idOrden);
          const envio = listaEnvios.find((e) => e.idOrden === o.idOrden);
          return pago && !pago.estado && (envio?.estado || '').toLowerCase() !== 'cancelado';
        });
        hayPendientes.current = pendientes.length > 0;
        Promise.all(pendientes.map((o) =>
          fetch(`${API_URL}/mercadopago/orden/${o.idOrden}/sincronizar`, { method: 'POST' })
            .then((res) => (res.ok ? res.json() : null))
            .catch(() => null)
        )).then((resultados) => {
          if (resultados.some((r) => r?.confirmado)) setRecarga((n) => n + 1);
        });
      })
      .catch(() => {
        setError('No se pudo cargar tu historial. ¿Está corriendo el backend?');
        setCargando(false);
      });
  }, [idCliente, recarga]);

  // Al volver a esta pestaña (por ejemplo despues de pagar en la de Mercado Pago) se actualiza el estado
  useEffect(() => {
    const alVolver = () => {
      if (document.visibilityState === 'visible' && hayPendientes.current) setRecarga((n) => n + 1);
    };
    document.addEventListener('visibilitychange', alVolver);
    window.addEventListener('focus', alVolver);
    return () => {
      document.removeEventListener('visibilitychange', alVolver);
      window.removeEventListener('focus', alVolver);
    };
  }, []);

  if (!usuario) {
    return (
      <div className="historial-contenedor">
        <div className="historial-tarjeta-principal">
          <h1 className="historial-titulo">Historial de Compras</h1>
          <p className="historial-subtitulo">
            Necesitás iniciar sesión para ver tus compras. <Link to="/login">Iniciar sesión</Link>
          </p>
        </div>
      </div>
    );
  }

  // Pedido con pago pendiente: se pide un link nuevo de Mercado Pago y se va a pagar
  const pagarAhora = (idOrden) => {
    setErrorPago(null);
    // Mercado Pago se abre en otra pestaña: se abre ya (dentro del clic) y se le carga el link al recibirlo
    const ventanaPago = abrirVentanaDePago();
    fetch(`${API_URL}/mercadopago/orden/${idOrden}/link`, { method: 'POST' })
      .then(async (res) => {
        const data = await res.json().catch(() => null);
        if (!res.ok || !data?.urlPago) throw new Error(data?.mensaje || 'No se pudo iniciar el pago.');
        irAlPago(ventanaPago, data.urlPago);
      })
      .catch((err) => {
        cerrarVentanaDePago(ventanaPago);
        setErrorPago(err instanceof TypeError ? 'No se pudo conectar con el servidor.' : err.message);
      });
  };

  const totalOrden = (orden) =>
    (orden.detalles || []).reduce((suma, d) => suma + d.subtotal, 0);

  const ordenesFiltradas = ordenes.filter((orden) => {
    const texto = busqueda.toLowerCase();
    return (
      String(orden.idOrden).includes(texto) ||
      (orden.detalles || []).some((d) => d.nombreProducto.toLowerCase().includes(texto))
    );
  });

  return (
    <div className="historial-contenedor">
      <div className="historial-tarjeta-principal">

        <div>
          <h1 className="historial-titulo">Historial de Compras</h1>
          <p className="historial-subtitulo">Los pedidos pagados con tu cuenta, {usuario.nombreUsuario}.</p>
        </div>

        <div className="historial-barra-herramientas">
          <input
            type="text"
            placeholder="Buscar por producto o número de pedido..."
            className="historial-input-filtro"
            value={busqueda}
            onChange={(e) => setBusqueda(e.target.value)}
          />
        </div>

        {cargando && <p style={{ marginTop: '20px' }}>Cargando historial...</p>}
        {error && <p style={{ marginTop: '20px', color: '#dc2626' }}>{error}</p>}
        {errorPago && <p style={{ marginTop: '20px', color: '#dc2626' }}>{errorPago}</p>}

        {!cargando && !error && (
          <div style={{ overflowX: 'auto', marginTop: '20px' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontFamily: 'sans-serif' }}>
              <thead>
                <tr style={{ backgroundColor: '#f4f4f4', borderBottom: '2px solid #ddd' }}>
                  <th style={{ padding: '12px' }}>Pedido</th>
                  <th style={{ padding: '12px' }}>Fecha</th>
                  <th style={{ padding: '12px' }}>Productos</th>
                  <th style={{ padding: '12px' }}>Total</th>
                  <th style={{ padding: '12px' }}>Pago</th>
                  <th style={{ padding: '12px' }}>Envío</th>
                </tr>
              </thead>
              <tbody>
                {ordenesFiltradas.length === 0 ? (
                  <tr>
                    <td colSpan="6" style={{ padding: '20px', textAlign: 'center', color: '#888' }}>
                      {ordenes.length === 0
                        ? 'Todavía no realizaste ninguna compra.'
                        : 'No se encontraron pedidos que coincidan con la búsqueda.'}
                    </td>
                  </tr>
                ) : (
                  ordenesFiltradas.map((orden) => (
                    <tr key={orden.idOrden} style={{ borderBottom: '1px solid #eee' }}>
                      <td style={{ padding: '12px', fontWeight: 'bold', color: '#555' }}>#{orden.idOrden}</td>
                      <td style={{ padding: '12px' }}>
                        {orden.fecha ? new Date(orden.fecha).toLocaleDateString('es-AR') : '-'}
                      </td>
                      <td style={{ padding: '12px', color: '#666' }}>
                        {(orden.detalles || []).map((d) => {
                          const cancelado = (envios[orden.idOrden]?.estado || '').toLowerCase() === 'cancelado';
                          return (
                            <div key={d.idDetalle} className="historial-producto">
                              <span>{d.cantidad} × {d.nombreProducto}{d.tamano > 1 ? ` (${etiquetaMedida(d.tamano, d.unidadMedida)})` : ''}</span>
                              {!cancelado && (
                                <Link to={`/producto/${d.idProducto}?resenar=1`} className="historial-link-resena">
                                  {resenados.includes(d.idProducto) ? 'Editar reseña' : '★ Dejar reseña'}
                                </Link>
                              )}
                            </div>
                          );
                        })}
                      </td>
                      <td style={{ padding: '12px', fontWeight: 'bold' }}>{formatearPrecio(totalOrden(orden))}</td>
                      <td style={{ padding: '12px' }}>
                        {pagos[orden.idOrden] ? (
                          <>
                            <span className={`estado-badge ${claseEstadoPago(pagos[orden.idOrden].estado)}`}>
                              {textoEstadoPago(pagos[orden.idOrden].estado)}
                            </span>
                            <div style={{ fontSize: '12px', color: '#666', marginTop: '4px' }}>
                              {pagos[orden.idOrden].nombreMetodoPago}
                            </div>
                            {!pagos[orden.idOrden].estado &&
                              (envios[orden.idOrden]?.estado || '').toLowerCase() !== 'cancelado' && (
                                <button className="btn-confirmar-pago" style={{ marginTop: '8px', padding: '6px 12px', fontSize: '13px' }}
                                  onClick={() => pagarAhora(orden.idOrden)}>
                                  Pagar ahora
                                </button>
                              )}
                          </>
                        ) : '-'}
                      </td>
                      <td style={{ padding: '12px' }}>
                        {envios[orden.idOrden] ? (
                          <>
                            <span className={`estado-badge ${claseEstadoEnvio(envios[orden.idOrden].estado)}`}>
                              {envios[orden.idOrden].estado}
                            </span>
                            {(envios[orden.idOrden].estado || '').toLowerCase() !== 'cancelado' && (
                              envios[orden.idOrden].fechaEntrega ? (
                                <div className="historial-entrega">
                                  <strong>Entrega estimada</strong>
                                  <span>{textoEntrega(envios[orden.idOrden])}</span>
                                  {envios[orden.idOrden].notaCliente && <em>{envios[orden.idOrden].notaCliente}</em>}
                                </div>
                              ) : (
                                <div style={{ fontSize: '12px', color: '#666', marginTop: '4px' }}>
                                  Te vamos a avisar el día de entrega
                                </div>
                              )
                            )}
                          </>
                        ) : '-'}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        )}

      </div>
    </div>
  );
}