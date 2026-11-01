import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../AuthContext';
import { formatearPrecio as formatoPesos } from '../formato';
import { vendePorMedida, etiquetaMedida } from '../medidas';
import { abrirVentanaDePago, irAlPago, cerrarVentanaDePago } from '../pagoVentana';
import './Pago.css';
import { API_URL } from '../config';


// Pago.js antepone "$" a mano en sus textos: acá solo se formatea el número
const formatearPrecio = (numero) => formatoPesos(numero).slice(1);

const Pago = ({ carrito, vaciarCarrito }) => {
  const navigate = useNavigate();
  const { usuario } = useAuth();

  const [formulario, setFormulario] = useState({ provincia: '', ciudad: '', calle: '', altura: '', telefono: '' });
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState(null);

  const total = carrito.reduce((sum, item) => sum + (item.precio * item.cantidad), 0);

  // Precarga la dirección del negocio como dirección de envío (se puede cambiar)
  // Teléfono de contacto: se completa con el que el cliente ya tiene guardado
  useEffect(() => {
    if (!usuario?.idCliente) return;
    fetch(`${API_URL}/clientes/${usuario.idCliente}`)
      .then((res) => (res.ok ? res.json() : null))
      .then((cliente) => {
        if (cliente?.telefono) {
          setFormulario((actual) => (actual.telefono ? actual : { ...actual, telefono: cliente.telefono }));
        }
      })
      .catch(() => {});
  }, [usuario?.idCliente]);

  useEffect(() => {
    fetch(`${API_URL}/negocios/mio`)
      .then((res) => (res.status === 200 ? res.json() : null))
      .then((negocio) => {
        if (!negocio) return;
        setFormulario((actual) =>
          actual.provincia || actual.ciudad || actual.calle || actual.altura
            ? actual // si ya empezó a escribir, no se pisa
            : { ...actual, provincia: negocio.provincia, ciudad: negocio.ciudad, calle: negocio.calle, altura: String(negocio.altura) }
        );
      })
      .catch(() => {});
  }, []);

  const manejarCambio = (e) => {
    setFormulario({ ...formulario, [e.target.name]: e.target.value });
  };

  const procesarPago = (e) => {
    e.preventDefault();
    setError(null);
    setEnviando(true);
    // Mercado Pago se abre en otra pestaña: se abre ya (dentro del clic) y se le carga el link al recibirlo
    const ventanaPago = abrirVentanaDePago();

    // Se mandan solo ids y cantidades: los precios los calcula el backend.
    // El medio de pago es siempre Mercado Pago.
    const cuerpo = {
      idCliente: usuario.idCliente,
      provincia: formulario.provincia.trim(),
      ciudad: formulario.ciudad.trim(),
      calle: formulario.calle.trim(),
      altura: parseInt(formulario.altura, 10) || 0,
      telefono: formulario.telefono.trim(),
      items: carrito.map((item) => ({ idProducto: item.id, cantidad: item.cantidad, tamano: item.tamano || 1 })),
    };

    fetch(`${API_URL}/checkout`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(cuerpo),
    })
      .then(async (res) => {
        const data = await res.json().catch(() => null);
        if (!res.ok) {
          throw new Error(data?.mensaje || 'No se pudo registrar la compra. Intentá de nuevo.');
        }
        return data;
      })
      .then((data) => {
        if (!data.urlPago) throw new Error('No se pudo iniciar el pago. Intentá de nuevo.');
        if (vaciarCarrito) vaciarCarrito();
        // La tienda sigue en su pestaña y muestra el pedido (pendiente hasta que se acredite el pago)
        if (irAlPago(ventanaPago, data.urlPago)) navigate('/historial');
      })
      .catch((err) => {
        cerrarVentanaDePago(ventanaPago);
        setError(err instanceof TypeError ? 'No se pudo conectar con el servidor.' : err.message);
        setEnviando(false);
      });
  };

  if (carrito.length === 0 && !enviando) {
    return (
      <div className="pago-contenedor-vacio">
        <h2>No hay productos para pagar</h2>
        <button onClick={() => navigate('/')} className="btn-volver">Ir al catálogo</button>
      </div>
    );
  }

  return (
    <div className="pago-pagina-container">
      <div className="pago-layout">

        {/* Formulario de Checkout */}
        <div className="pago-formulario-tarjeta">
          <h2 className="pago-titulo">Finalizar Compra</h2>
          <form onSubmit={procesarPago}>

            <h3>Dirección de envío</h3>
            <input type="text" name="provincia" placeholder="Provincia" required value={formulario.provincia} onChange={manejarCambio} />
            <input type="text" name="ciudad" placeholder="Ciudad" required value={formulario.ciudad} onChange={manejarCambio} />
            <input type="text" name="calle" placeholder="Calle" required value={formulario.calle} onChange={manejarCambio} />
            <input type="text" name="altura" placeholder="Altura (número)" inputMode="numeric" pattern="[0-9]*" required value={formulario.altura} onChange={manejarCambio} />

            <h3>Teléfono de contacto</h3>
            <input type="tel" name="telefono" placeholder="Teléfono con código de área (ej: 11 4430-6194)" required maxLength={20}
              value={formulario.telefono} onChange={manejarCambio} />
            <p style={{ fontSize: '13px', color: '#64748b', margin: '4px 0 12px' }}>
              Te escribimos por WhatsApp para avisarte cuándo llega tu pedido.
            </p>

            <h3>Método de Pago</h3>
            <div className="pago-datos-bancarios" style={{ marginTop: 0 }}>
              <p><strong>Mercado Pago</strong></p>
              <p>Al confirmar te llevamos a Mercado Pago para pagar de forma segura con tarjeta, dinero en cuenta o efectivo.</p>
            </div>

            {error && <p className="pago-error">{error}</p>}

            <button type="submit" className="btn-confirmar-pago" disabled={enviando}>
              {enviando ? 'Redirigiendo a Mercado Pago...' : `Pagar con Mercado Pago · $${formatearPrecio(total)}`}
            </button>
          </form>
        </div>

        {/* Resumen del Pedido */}
        <div className="pago-resumen-tarjeta">
          <h3>Resumen del Pedido</h3>
          <div className="pago-productos-lista">
            {carrito.map((item) => (
              <div key={item.clave || item.id} className="pago-item-resumen">
                <span>{item.nombre}{vendePorMedida(item) ? ` · ${etiquetaMedida(item.tamano, item.unidad)}` : ''} (x{item.cantidad})</span>
                <span>${formatearPrecio(item.precio * item.cantidad)}</span>
              </div>
            ))}
          </div>
          <div className="pago-total-final">
            <span>Total:</span>
            <strong>${formatearPrecio(total)}</strong>
          </div>
        </div>

      </div>
    </div>
  );
};

export default Pago;
