import React from 'react';
import { useNavigate } from 'react-router-dom'; // <-- IMPORTACIÓN PARA LA NAVEGACIÓN
import { formatearPrecio } from '../formato';
import { vendePorMedida, etiquetaMedida, simboloUnidad } from '../medidas';
import './Carrito.css';

// 1. Recibimos las dos nuevas propiedades aquí arriba
const Carrito = ({ carrito, eliminarDelCarrito, agregarAlCarrito, restarDelCarrito }) => {
  
  const navigate = useNavigate(); // <-- INICIALIZACIÓN DEL HOOK
  // Stock usado por todas las líneas de un mismo producto (en unidades de medida)
  const enUso = (idProducto) =>
    (carrito || []).filter((i) => i.id === idProducto).reduce((suma, i) => suma + i.tamano * i.cantidad, 0);
  const sinStockParaOtro = (item) => typeof item.stock === 'number' && enUso(item.id) + item.tamano > item.stock;

  const valorTotalGeneral = carrito ? carrito.reduce((total, item) => total + (item.precio * item.cantidad), 0) : 0;

  return (
    <div className="carrito-pagina-container">
      <div className="carrito-tarjeta-principal">
        <h2 className="carrito-titulo-grande">Tu Carrito de Compras</h2>
        <p className="carrito-subtitulo-tienda">Gestioná los productos de higiene mecánica antes de finalizar el pedido.</p>
        
        <div className="carrito-tabla-cabecera">
          <div>Producto</div>
          <div>Detalle</div>
          <div>Cantidad</div>
          <div>Subtotal</div>
          <div>Acción</div>
        </div>

        <div className="carrito-listado-cuerpo">
          {!carrito || carrito.length === 0 ? (
            <div className="carrito-seccion-vacia">
              <p className="carrito-icono-vacio">🛒</p>
              <h3>Tu carrito está vacío</h3>
              {/* 2. Le damos vida al botón cuando está vacío */}
              <button onClick={() => navigate('/')} className="btn-seguir-comprando" style={{ marginTop: '15px' }}>
                Ir a ver productos
              </button>
            </div>
          ) : (
            <>
              {carrito.map((item) => (
                <div key={item.clave} className="carrito-fila-item">
                  
                  <div className="carrito-col-foto">
                    <img 
                      src={item.imagen || "https://picsum.photos/300"} 
                      alt={item.nombre} 
                    />
                  </div>

                  <div className="carrito-col-detalle">
                    <span className="carrito-item-nombre-txt">
                      {item.nombre}{vendePorMedida(item) ? ` · ${etiquetaMedida(item.tamano, item.unidad)}` : ''}
                    </span>
                    <span className="carrito-item-precio-unitario">
                      {vendePorMedida(item)
                        ? `${formatearPrecio(item.precio)} c/u (${formatearPrecio(item.precioBase)} por ${simboloUnidad(item.unidad)})`
                        : `Precio unitario: ${formatearPrecio(item.precio)}`}
                    </span>
                  </div>

                    {/* 3. COLUMNA CANTIDAD: Botones de control de unidades + y - */}
                    <div className="carrito-col-cantidad" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px' }}>
                      {/* Botón para restar cantidad */}
                      <button 
                        onClick={() => restarDelCarrito(item.clave)}
                        style={{ 
                          background: '#e2e8f0', color: '#1e293b', border: 'none', 
                          borderRadius: '50%', width: '28px', height: '28px', 
                          fontSize: '18px', cursor: 'pointer', fontWeight: 'bold',
                          display: 'flex', alignItems: 'center', justifyContent: 'center'
                        }}
                        title="Restar una unidad"
                      >
                        −
                      </button>

                      <span className="carrito-badge-multiplicador">x{item.cantidad}</span>

                      {/* Botón para sumar cantidad con el símbolo "+" adentro */}
                      <button 
                        onClick={() => agregarAlCarrito(item)}
                        disabled={sinStockParaOtro(item)}
                        style={{ 
                          background: '#e2e8f0', color: '#1e293b', border: 'none', 
                          borderRadius: '50%', width: '28px', height: '28px', 
                          fontSize: '18px', cursor: 'pointer', fontWeight: 'bold',
                          display: 'flex', alignItems: 'center', justifyContent: 'center'
                        }}
                        title={sinStockParaOtro(item) ? 'No hay más stock' : 'Agregar una unidad más'}
                      >
                        +
                      </button>
                    </div>


                  <div className="carrito-col-subtotal">
                    {formatearPrecio(item.precio * item.cantidad)}
                  </div>

                  <div className="carrito-col-eliminar">
                    <button 
                      onClick={() => eliminarDelCarrito(item.clave)}
                      className="btn-eliminar"
                      title="Eliminar producto"
                    >
                      ✕
                    </button>
                  </div>

                </div>
              ))}

              <div className="carrito-resumen-gastos">
                <div className="carrito-fila-total">
                  <span>Valor Total General:</span>
                  <span className="carrito-monto-total">{formatearPrecio(valorTotalGeneral)}</span>
                </div>
                <div className="carrito-contenedor-botones">
                  {/* 4. Le damos vida al botón de volver con onClick */}
                  <button onClick={() => navigate('/')} className="btn-seguir-comprando">
                    Volver al catálogo
                  </button>
                  
                  {/* ACCIÓN CONECTADA PARA REDIRIGIR A LA VISTA DE PAGO */}
                  <button 
                    onClick={() => navigate('/pago')} 
                    className="btn-proceder-pago"
                  >
                    Proceder al Pago Seguro
                  </button>
                </div>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}

export default Carrito;