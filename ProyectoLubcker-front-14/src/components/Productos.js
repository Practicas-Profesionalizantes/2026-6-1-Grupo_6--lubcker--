import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { resolverImagen } from '../imagenes';
import { useFavoritos } from '../FavoritosContext';
import { formatearPrecio, normalizarTexto } from '../formato';
import { useAviso } from '../useAviso';
import StockBadge from './StockBadge';
import { mapearMedidas, vendePorMedida, simboloUnidad } from '../medidas';
import './Productos.css';
import { API_URL as API_BASE } from '../config';

const API_URL = `${API_BASE}/productos`;

export default function Productos({ agregarAlCarrito, filtro, soloIntereses, busqueda = '' }) {
  const { esFavorito, alternarFavorito } = useFavoritos();
  const navigate = useNavigate();
  const [aviso, mostrarAviso] = useAviso();
  const [productos, setProductos] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    fetch(API_URL)
      .then((res) => {
        if (!res.ok) throw new Error('No se pudieron cargar los productos');
        return res.json();
      })
      .then((data) => {
        // Traducimos la forma del backend (idProducto, nombreCategoria...)
        // a la forma que usa este componente (id, categoria...)
        const mapeados = data.map((p) => ({
          id: p.idProducto,
          nombre: p.nombre,
          precio: p.precio,
          stock: p.stock,
          ...mapearMedidas(p),
          imagen: resolverImagen(p.imagen),
          categoria: (p.nombreCategoria || '').toLowerCase(),
        }));
        setProductos(mapeados);
        setCargando(false);
      })
      .catch((err) => {
        setError(err.message);
        setCargando(false);
      });
  }, []);

  const agregar = (prod) => {
    // Los productos por medida se agregan desde su página, donde se elige la medida
    if (vendePorMedida(prod)) {
      navigate(`/producto/${prod.id}`);
      return;
    }
    if (!agregarAlCarrito(prod)) {
      mostrarAviso(
        prod.stock > 0 ? `No hay más stock de ${prod.nombre}.` : `${prod.nombre} está sin stock.`,
        'error'
      );
    }
  };

  let productosMostrados = productos;

  if (soloIntereses) {
    productosMostrados = productos.filter((p) => esFavorito(p.id));
  } else {
    if (filtro) {
      productosMostrados = productosMostrados.filter((p) => p.categoria === filtro);
    }
    const texto = normalizarTexto(busqueda);
    if (texto) {
      productosMostrados = productosMostrados.filter((p) => normalizarTexto(p.nombre).includes(texto));
    }
  }

  if (cargando) {
    return <p className="mensaje-intereses-vacio">Cargando productos...</p>;
  }

  if (error) {
    return <p className="mensaje-intereses-vacio">No se pudieron cargar los productos. ¿Está corriendo el backend?</p>;
  }

  // Productos por medida: "$10.000 por kg"; el resto, el precio de siempre
  const precioTexto = (prod) =>
    vendePorMedida(prod) ? `${formatearPrecio(prod.precio)} por ${simboloUnidad(prod.unidad)}` : formatearPrecio(prod.precio);

  const botonFavorito = (prod) => (
    <button
      onClick={() => alternarFavorito(prod.id)}
      className="btn-like-catalogo"
      aria-label={esFavorito(prod.id) ? `Quitar ${prod.nombre} de favoritos` : `Agregar ${prod.nombre} a favoritos`}
      aria-pressed={esFavorito(prod.id)}
    >
      {esFavorito(prod.id) ? '❤️' : '🤍'}
    </button>
  );

  return (
    <div className={`productos-grid-contenedor ${soloIntereses ? 'vista-intereses' : 'vista-catalogo'}`}>
      {aviso}

      {productosMostrados.length === 0 && (
        <p className="mensaje-intereses-vacio">
          {soloIntereses
            ? 'Aún no tienes productos en tus intereses. Tocá 🤍 en un producto para guardarlo.'
            : busqueda
              ? `No encontramos productos para "${busqueda}".`
              : filtro
                ? 'No hay productos en esta categoría.'
                : 'Todavía no hay productos publicados.'}
        </p>
      )}

      {productosMostrados.map((prod) => {
        const agotado = prod.stock <= 0;
        return (
          <div key={prod.id} className={`producto-tarjeta-item ${soloIntereses ? 'tarjeta-horizontal' : 'tarjeta-vertical'}`}>

            <Link to={`/producto/${prod.id}`} className="enlace-tarjeta-completa" aria-label={`Ver ${prod.nombre}`} />

            {soloIntereses ? (
              <>
                <div className="bloque-izquierdo-horizontal">
                  <div className="contenedor-imagen-mini">
                    <img src={prod.imagen} alt={prod.nombre} className="imagen-producto-catalogo" />
                  </div>
                  <h3 className="titulo-producto-catalogo texto-recortado">
                    {prod.nombre}
                  </h3>
                </div>

                <div className="bloque-derecho-horizontal">
                  <span className="precio-producto-catalogo">{precioTexto(prod)}</span>
                  {botonFavorito(prod)}
                  <button onClick={() => agregar(prod)} className="btn-agregar-catalogo-horizontal" disabled={agotado}>
                    {agotado ? 'Sin stock' : vendePorMedida(prod) ? 'Elegir medida' : 'Agregar'}
                  </button>
                </div>
              </>
            ) : (
              <>
                <div className="contenedor-imagen-cuadrada">
                  <img src={prod.imagen} alt={prod.nombre} className="imagen-producto-catalogo" />
                </div>

                <h3 className="titulo-producto-catalogo titulo-centrado">
                  {prod.nombre}
                </h3>

                <span className="precio-producto-catalogo precio-centrado">
                  {precioTexto(prod)}
                </span>

                <div className="precio-centrado">
                  <StockBadge stock={prod.stock} unidad={prod.unidad} />
                </div>

                <div className="contenedor-botones-tarjeta">
                  {botonFavorito(prod)}
                  <button onClick={() => agregar(prod)} className="btn-agregar-catalogo-vertical" disabled={agotado}>
                    {agotado ? 'Sin stock' : vendePorMedida(prod) ? 'Elegir medida' : 'Agregar al carrito'}
                  </button>
                </div>
              </>
            )}

          </div>
        );
      })}
    </div>
  );
}
