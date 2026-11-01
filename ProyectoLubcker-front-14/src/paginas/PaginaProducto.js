import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { resolverImagen } from '../imagenes';
import { useAviso } from '../useAviso';
import { formatearPrecio } from '../formato';
import { useFavoritos } from '../FavoritosContext';
import StockBadge from '../components/StockBadge';
import { mapearMedidas, vendePorMedida, etiquetaMedida, simboloUnidad, textoStock } from '../medidas';
import Resenas from '../components/Resenas';
import './PaginaProducto.css';
import { API_URL } from '../config';


function mapearProducto(p, ficha) {
  return {
    id: p.idProducto,
    nombre: p.nombre,
    precio: p.precio,
    stock: p.stock,
    ...mapearMedidas(p),
    activo: p.activo,
    imagen: resolverImagen(p.imagen),
    categoria: (p.nombreCategoria || '').toLowerCase(),
    idCategoria: p.idCategoria,
    descripcion: ficha?.descripcion || 'Sin descripción disponible para este producto.',
    modelo: ficha?.modelo || null,
    presentacion: ficha?.presentacion || null,
  };
}

function PaginaProducto({ agregarAlCarrito }) {
  const { id } = useParams();
  const [aviso, mostrarAviso] = useAviso();
  const navigate = useNavigate();
  const { esFavorito, alternarFavorito } = useFavoritos();

  const [producto, setProducto] = useState(null);
  const [productosRelacionados, setProductosRelacionados] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [noExiste, setNoExiste] = useState(false);
  const [medidaElegida, setMedidaElegida] = useState(null);

  useEffect(() => {
    setCargando(true);
    setNoExiste(false);

    fetch(`${API_URL}/productos/${id}`)
      .then((res) => {
        if (!res.ok) throw new Error('no existe');
        return res.json();
      })
      .then((dataProducto) => {
        // La descripción vive en ficha_tecnica, no en producto -> pedido aparte.
        // No todos los productos tienen ficha cargada, por eso el .catch en vez
        // de dejar que rompa toda la página si da 404.
        fetch(`${API_URL}/fichas-tecnicas/producto/${id}`)
          .then((res) => (res.ok ? res.json() : null))
          .catch(() => null)
          .then((ficha) => {
            const productoMapeado = mapearProducto(dataProducto, ficha);
            setProducto(productoMapeado);
            setMedidaElegida(
              productoMapeado.presentaciones.find((m) => m <= productoMapeado.stock) || productoMapeado.presentaciones[0]
            );

            fetch(`${API_URL}/productos/categoria/${dataProducto.idCategoria}`)
              .then((res) => (res.ok ? res.json() : []))
              .then((lista) => {
                const relacionados = lista
                  .filter((p) => p.idProducto !== dataProducto.idProducto)
                  .map((p) => mapearProducto(p, null));
                setProductosRelacionados(relacionados);
                setCargando(false);
              })
              .catch(() => setCargando(false));
          });
      })
      .catch(() => {
        setNoExiste(true);
        setCargando(false);
      });
  }, [id]);

  const agregar = (prod, medida = 1) => {
    if (agregarAlCarrito(prod, medida)) {
      mostrarAviso(`${prod.nombre}${vendePorMedida(prod) ? ` (${etiquetaMedida(medida, prod.unidad)})` : ''} añadido al carrito`);
    } else {
      mostrarAviso(prod.stock > 0 ? `No hay más stock de ${prod.nombre}.` : `${prod.nombre} está sin stock.`, 'error');
    }
  };

  if (cargando) {
    return <p className="mensaje-intereses-vacio">Cargando producto...</p>;
  }

  if (noExiste || !producto) {
    return (
      <div className="error-producto-no-existe">
        <h2>El producto solicitado no está disponible.</h2>
        <button onClick={() => navigate('/')} className="btn-comprar-carrito btn-error-regresar">
          Volver al catálogo
        </button>
      </div>
    );
  }

  return (
    <div className="pagina-producto-contenedor">
      {aviso}
      <button onClick={() => navigate('/')} className="btn-volver-catalogo">
        &larr; Volver al Catálogo
      </button>

      <div className="producto-detalle-grid">
        <div className="producto-wrapper-imagen">
          <img src={producto.imagen} alt={producto.nombre} className="producto-imagen-principal" />
        </div>

        <div className="producto-bloque-info">
          <span className="producto-categoria-etiqueta">
            Categoría: {producto.categoria}
          </span>
          <h1 className="producto-nombre-titulo">{producto.nombre}</h1>
          {vendePorMedida(producto) ? (
            <>
              <p className="producto-precio-destacado">
                {formatearPrecio(producto.precio * (medidaElegida || 1))}
                <span className="producto-precio-por-medida"> · {etiquetaMedida(medidaElegida || 1, producto.unidad)}</span>
              </p>
              <p className="producto-precio-base">{formatearPrecio(producto.precio)} por {simboloUnidad(producto.unidad)}</p>
            </>
          ) : (
            <p className="producto-precio-destacado">{formatearPrecio(producto.precio)}</p>
          )}

          {!producto.activo ? (
            <p className="producto-descontinuado">Este producto fue descontinuado y ya no está a la venta.</p>
          ) : (
            <StockBadge stock={producto.stock} unidad={producto.unidad} />
          )}

          {producto.activo && vendePorMedida(producto) && (
            <div className="producto-medidas" role="radiogroup" aria-label="Elegí la medida">
              <span className="producto-medidas-titulo">Elegí la medida:</span>
              <div className="producto-medidas-opciones">
                {producto.presentaciones.map((medida) => {
                  const sinStock = medida > producto.stock;
                  return (
                    <button
                      key={medida}
                      type="button"
                      role="radio"
                      aria-checked={medidaElegida === medida}
                      disabled={sinStock}
                      title={sinStock ? `Solo quedan ${textoStock(producto.stock, producto.unidad)}` : undefined}
                      className={`producto-medida-btn ${medidaElegida === medida ? 'activa' : ''}`}
                      onClick={() => setMedidaElegida(medida)}
                    >
                      <strong>{etiquetaMedida(medida, producto.unidad)}</strong>
                      <span>{formatearPrecio(producto.precio * medida)}</span>
                    </button>
                  );
                })}
              </div>
            </div>
          )}

          <p className="producto-texto-descripcion">
            {producto.descripcion}
          </p>

          {(producto.modelo || producto.presentacion) && (
            <dl className="producto-ficha">
              {producto.modelo && (<><dt>Modelo</dt><dd>{producto.modelo}</dd></>)}
              {producto.presentacion && (<><dt>Presentación</dt><dd>{producto.presentacion}</dd></>)}
            </dl>
          )}

          <div className="producto-acciones">
            <button
              onClick={() => agregar(producto, medidaElegida || 1)}
              className="btn-comprar-carrito"
              disabled={!producto.activo || producto.stock <= 0 || (vendePorMedida(producto) && (!medidaElegida || medidaElegida > producto.stock))}
            >
              {!producto.activo ? 'No disponible' : producto.stock <= 0 ? 'Sin stock' : 'Añadir al Carrito'}
            </button>
            <button
              type="button"
              onClick={() => alternarFavorito(producto.id)}
              className="btn-favorito-detalle"
              aria-pressed={esFavorito(producto.id)}
              aria-label={esFavorito(producto.id) ? 'Quitar de favoritos' : 'Agregar a favoritos'}
              title={esFavorito(producto.id) ? 'Quitar de favoritos' : 'Agregar a favoritos'}
            >
              {esFavorito(producto.id) ? '❤️' : '🤍'}
            </button>
          </div>
        </div>
      </div>

      {productosRelacionados.length > 0 && (
        <div className="seccion-relacionados">
          <h2 className="titulo-relacionados">Productos Relacionados</h2>
          <div className="productos-relacionados-grid">
            {productosRelacionados.map((prod) => (
              <div key={prod.id} className="producto-tarjeta-item tarjeta-vertical">

                <Link to={`/producto/${prod.id}`} className="enlace-tarjeta-completa" aria-label={`Ver ${prod.nombre}`} />

                <div className="contenedor-imagen-cuadrada">
                  <img src={prod.imagen} alt={prod.nombre} className="imagen-producto-catalogo" />
                </div>

                <h3 className="titulo-producto-catalogo titulo-centrado">
                  {prod.nombre}
                </h3>

                <span className="precio-producto-catalogo precio-centrado">
                  {vendePorMedida(prod) ? `${formatearPrecio(prod.precio)} por ${simboloUnidad(prod.unidad)}` : formatearPrecio(prod.precio)}
                </span>

                <div className="contenedor-botones-tarjeta">
                  <button
                    onClick={() => (vendePorMedida(prod) ? navigate(`/producto/${prod.id}`) : agregar(prod))}
                    className="btn-agregar-catalogo-vertical"
                    disabled={prod.stock <= 0}
                  >
                    {prod.stock <= 0 ? 'Sin stock' : vendePorMedida(prod) ? 'Elegir medida' : 'Agregar al carrito'}
                  </button>
                </div>

              </div>
            ))}
          </div>
        </div>
      )}

      <Resenas idProducto={producto.id} />
    </div>
  );
}

export default PaginaProducto;