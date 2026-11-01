import React, { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import './Pago.css';
import { API_URL } from '../config';


// A esta página vuelve el cliente desde Mercado Pago (?payment_id=...&status=...&external_reference=...).
// El resultado NO se toma de la URL: se le pide al backend que verifique el pago con Mercado Pago.
const PagoResultado = () => {
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const idPago = params.get('payment_id') || params.get('collection_id');
  const idOrdenUrl = params.get('external_reference');

  const [estado, setEstado] = useState(idPago ? 'verificando' : 'sin-pago');
  const [idOrden, setIdOrden] = useState(idOrdenUrl);

  useEffect(() => {
    if (!idPago) return;
    fetch(`${API_URL}/mercadopago/confirmar?payment_id=${encodeURIComponent(idPago)}`, { method: 'POST' })
      .then(async (res) => {
        const data = await res.json().catch(() => null);
        if (!res.ok) throw new Error('no-verificado');
        setIdOrden(String(data.idOrden));
        if (data.confirmado) setEstado('aprobado');
        else if (data.estado === 'pending' || data.estado === 'in_process') setEstado('pendiente');
        else setEstado('rechazado');
      })
      .catch(() => setEstado('error'));
  }, [idPago]);

  const mensajes = {
    verificando: { titulo: 'Verificando tu pago...', texto: 'Un momento, estamos confirmando el pago con Mercado Pago.' },
    aprobado: { titulo: '¡Pago aprobado!', texto: 'Recibimos tu pago. Ya estamos preparando tu pedido.' },
    pendiente: { titulo: 'Tu pago está pendiente', texto: 'Mercado Pago todavía no confirmó el pago (por ejemplo, si elegiste pagar en efectivo). Cuando se acredite, tu pedido se actualiza solo.' },
    rechazado: { titulo: 'No se pudo completar el pago', texto: 'El pago fue rechazado o cancelado. Tu pedido sigue guardado: podés intentar pagarlo de nuevo desde Mis compras.' },
    error: { titulo: 'No pudimos verificar el pago', texto: 'Si pagaste, el pedido se actualiza en unos minutos. Revisalo en Mis compras.' },
    'sin-pago': { titulo: 'El pago no se completó', texto: 'Tu pedido quedó guardado y pendiente de pago. Podés pagarlo desde Mis compras.' },
  };
  const { titulo, texto } = mensajes[estado];

  return (
    <div className="pago-pagina-container">
      <div className="pago-confirmacion">
        <h2 className="pago-titulo">{titulo}</h2>
        {idOrden && <p>Pedido <strong>#{idOrden}</strong></p>}
        <p>{texto}</p>
        <div className="pago-botones-confirmacion">
          <button onClick={() => navigate('/historial')} className="btn-confirmar-pago">Ver mis compras</button>
          <button onClick={() => navigate('/')} className="btn-volver">Seguir comprando</button>
        </div>
      </div>
    </div>
  );
};

export default PagoResultado;
