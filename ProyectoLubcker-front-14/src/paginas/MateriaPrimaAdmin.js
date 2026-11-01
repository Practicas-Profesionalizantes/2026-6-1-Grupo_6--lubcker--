import React, { useState, useEffect } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { useAviso } from '../useAviso';
import { formatearDinero } from '../formato';
import { simboloUnidad, etiquetaMedida } from '../medidas';
import InputNumero from '../components/InputNumero';
import './MateriaPrimaAdmin.css';
import { API_URL } from '../config';


const redondear2 = (n) => Math.round(n * 100) / 100;
// Acepta coma o punto decimal ("0,25" o "0.25")
const leerNumero = (texto) => {
  const n = Number(String(texto).replace(',', '.'));
  return texto !== '' && Number.isFinite(n) ? n : NaN;
};
const claveVendidas = (id) => `lubcker_vendidas_${id}`;
const clavePrecio = (id) => `lubcker_precio_venta_${id}`;

async function leerRespuesta(res, mensajePorDefecto) {
  const data = await res.json().catch(() => null);
  if (!res.ok) throw new Error(data?.mensaje || mensajePorDefecto);
  return data;
}

// "Materia prima de productos": qué insumos lleva cada producto publicado (por UNA unidad del producto),
// su costo de fabricación, la ganancia contra el precio de venta y la ganancia según lo vendido.
export default function MateriaPrimaAdmin() {
  const [params, setParams] = useSearchParams();
  const idSeleccionado = Number(params.get('producto')) || null;

  const [productos, setProductos] = useState([]);
  const [insumos, setInsumos] = useState([]);
  const [resumen, setResumen] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState(null);

  const [receta, setReceta] = useState(null); // datos guardados del producto elegido
  const [lineas, setLineas] = useState([]);   // [{ idInsumo, cantidad: '0,5' }] (lo que se está editando)
  const [guardadas, setGuardadas] = useState('[]');
  const [cargandoReceta, setCargandoReceta] = useState(false);
  const [guardando, setGuardando] = useState(false);
  const [vendidas, setVendidas] = useState('');
  const [precioElegido, setPrecioElegido] = useState(''); // precio de venta que el admin prueba; vacío = el publicado
  const [aviso, mostrarAviso] = useAviso();

  const cargarResumen = () =>
    fetch(`${API_URL}/admin/materia-prima/resumen`).then((res) => leerRespuesta(res, 'error')).then(setResumen).catch(() => {});

  useEffect(() => {
    Promise.all([
      fetch(`${API_URL}/productos`).then((res) => leerRespuesta(res, 'error')),
      fetch(`${API_URL}/admin/insumos`).then((res) => leerRespuesta(res, 'error')),
      fetch(`${API_URL}/admin/materia-prima/resumen`).then((res) => leerRespuesta(res, 'error')),
    ])
      .then(([prods, ins, res]) => {
        setProductos(prods);
        setInsumos(ins);
        setResumen(res);
        setCargando(false);
      })
      .catch(() => {
        setError('No se pudieron cargar los datos. ¿Está corriendo el backend?');
        setCargando(false);
      });
  }, []);

  // Al elegir un producto se trae su materia prima guardada
  useEffect(() => {
    if (!idSeleccionado) {
      setReceta(null);
      return;
    }
    setCargandoReceta(true);
    fetch(`${API_URL}/admin/materia-prima/producto/${idSeleccionado}`)
      .then((res) => leerRespuesta(res, 'No se pudo cargar la materia prima del producto.'))
      .then((data) => {
        const ls = data.items.map((i) => ({ idInsumo: i.idInsumo, cantidad: String(Number(i.cantidad)) }));
        setReceta(data);
        setLineas(ls);
        setGuardadas(JSON.stringify(ls));
        try {
          setVendidas(localStorage.getItem(claveVendidas(idSeleccionado)) || '');
          setPrecioElegido(localStorage.getItem(clavePrecio(idSeleccionado)) || '');
        } catch {
          setVendidas('');
          setPrecioElegido('');
        }
      })
      .catch((err) => {
        mostrarAviso(err.message, 'error');
        setReceta(null);
      })
      .finally(() => setCargandoReceta(false));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [idSeleccionado]);

  const hayCambios = JSON.stringify(lineas) !== guardadas;

  const elegirProducto = (valor) => {
    if (hayCambios && !window.confirm('Tenés cambios sin guardar en este producto. ¿Descartarlos?')) return;
    setParams(valor ? { producto: valor } : {});
  };

  const agregarInsumo = (valor) => {
    if (!valor) return;
    setLineas((prev) => [...prev, { idInsumo: Number(valor), cantidad: '' }]);
  };
  const cambiarCantidad = (idInsumo, cantidad) =>
    setLineas((prev) => prev.map((l) => (l.idInsumo === idInsumo ? { ...l, cantidad } : l)));
  const quitarInsumo = (idInsumo) => setLineas((prev) => prev.filter((l) => l.idInsumo !== idInsumo));

  // El precio de venta se puede cambiar para ver la ganancia con otro precio. No modifica el precio
  // publicado en la tienda (eso se hace en "Gestionar productos"); queda guardado en este navegador.
  const cambiarPrecio = (valor) => {
    setPrecioElegido(valor);
    try {
      if (valor === '') localStorage.removeItem(clavePrecio(idSeleccionado));
      else localStorage.setItem(clavePrecio(idSeleccionado), valor);
    } catch {
      // sin persistencia, el cálculo funciona igual
    }
  };

  const cambiarVendidas = (valor) => {
    setVendidas(valor);
    try {
      localStorage.setItem(claveVendidas(idSeleccionado), valor);
    } catch {
      // sin persistencia, el cálculo funciona igual
    }
  };

  const guardar = async () => {
    for (const l of lineas) {
      const n = leerNumero(l.cantidad);
      if (!(n > 0)) {
        mostrarAviso('Completá una cantidad mayor a 0 en todos los insumos.', 'error');
        return;
      }
    }
    setGuardando(true);
    try {
      const data = await fetch(`${API_URL}/admin/materia-prima/producto/${idSeleccionado}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ items: lineas.map((l) => ({ idInsumo: l.idInsumo, cantidad: leerNumero(l.cantidad) })) }),
      }).then((res) => leerRespuesta(res, 'No se pudo guardar la materia prima.'));
      const ls = data.items.map((i) => ({ idInsumo: i.idInsumo, cantidad: String(Number(i.cantidad)) }));
      setReceta(data);
      setLineas(ls);
      setGuardadas(JSON.stringify(ls));
      cargarResumen();
      mostrarAviso('Materia prima guardada');
    } catch (err) {
      mostrarAviso(err instanceof TypeError ? 'No se pudo conectar con el servidor.' : err.message, 'error');
    } finally {
      setGuardando(false);
    }
  };

  if (cargando) return <p className="mp-vacio">Cargando...</p>;
  if (error) return <p className="mp-vacio">{error}</p>;

  const conReceta = new Set(resumen.map((r) => r.idProducto));
  const insumosLibres = insumos.filter((i) => !lineas.some((l) => l.idInsumo === i.idInsumo));

  // ---------- Cálculo en pantalla (se actualiza mientras se edita) ----------
  const unidad = receta?.unidadMedida || 'unidad';
  const precioPublicado = receta?.precioVenta || 0;
  const precioPropio = leerNumero(precioElegido);
  const usaPrecioPropio = precioPropio >= 0 && precioPropio !== precioPublicado;
  const precio = precioPropio >= 0 ? precioPropio : precioPublicado;
  const detalle = lineas.map((l) => {
    const insumo = insumos.find((i) => i.idInsumo === l.idInsumo);
    const cantidad = leerNumero(l.cantidad);
    return { ...l, insumo, costo: insumo && cantidad > 0 ? redondear2(cantidad * Number(insumo.costoUnitario)) : 0 };
  });
  const costoFabricacion = redondear2(detalle.reduce((suma, d) => suma + d.costo, 0));
  const ganancia = redondear2(precio - costoFabricacion);
  const margen = precio > 0 ? Math.round((ganancia / precio) * 1000) / 10 : null;
  const hayGanancia = ganancia >= 0;

  const cantidadVendida = leerNumero(vendidas);
  const hayVentas = cantidadVendida >= 0;
  const ingreso = hayVentas ? redondear2(precio * cantidadVendida) : 0;
  const costoTotal = hayVentas ? redondear2(costoFabricacion * cantidadVendida) : 0;
  const gananciaTotal = redondear2(ingreso - costoTotal);

  const porUnidad = unidad === 'unidad' ? '1 unidad' : `1 ${simboloUnidad(unidad)}`;

  return (
    <div className="mp-contenedor">
      {aviso}
      <div className="mp-tarjeta">
        <h1 className="mp-titulo">Materia prima de productos</h1>
        <p className="mp-subtitulo">
          Elegí un producto publicado y definí qué insumos lleva. Se calcula cuánto cuesta fabricarlo y cuánto ganás
          con el precio al que lo vendés.
        </p>

        <label className="mp-etiqueta" htmlFor="mp-producto">Producto</label>
        <select id="mp-producto" className="mp-input" value={idSeleccionado || ''} onChange={(e) => elegirProducto(e.target.value)}>
          <option value="">Elegí un producto...</option>
          {productos.map((p) => (
            <option key={p.idProducto} value={p.idProducto}>
              {p.nombre}{conReceta.has(p.idProducto) ? '' : ' (sin materia prima cargada)'}
            </option>
          ))}
        </select>

        {insumos.length === 0 && (
          <p className="mp-aviso">
            Todavía no cargaste insumos. Primero agregalos en <Link to="/admin/insumos">Insumos</Link>.
          </p>
        )}

        {idSeleccionado && cargandoReceta && <p className="mp-vacio">Cargando materia prima...</p>}

        {idSeleccionado && receta && !cargandoReceta && (
          <>
            {/* ---------- Materia prima ---------- */}
            <div className="mp-seccion">
              <div className="mp-seccion-cabecera">
                <h2>Materia prima que usa <span className="mp-por">para fabricar {porUnidad} de {receta.nombreProducto}</span></h2>
                {hayCambios && <span className="mp-sin-guardar">Cambios sin guardar</span>}
              </div>

              {detalle.length === 0 ? (
                <p className="mp-vacio-chico">Todavía no cargaste materia prima para este producto.</p>
              ) : (
                <div className="mp-lineas">
                  {detalle.map((d) => (
                    <div key={d.idInsumo} className="mp-linea">
                      <div className="mp-linea-nombre">
                        <strong>{d.insumo?.nombre || 'Insumo eliminado'}</strong>
                        {d.insumo && <span>{formatearDinero(d.insumo.costoUnitario)} por {simboloUnidad(d.insumo.unidadMedida)}</span>}
                      </div>
                      <div className="mp-linea-cantidad">
                        <InputNumero
                          className="mp-input"
                          decimales={3}
                          value={d.cantidad}
                          onChange={(e) => cambiarCantidad(d.idInsumo, e.target.value)}
                          placeholder="Cantidad"
                          aria-label={`Cantidad de ${d.insumo?.nombre}`}
                        />
                        <span>{d.insumo ? simboloUnidad(d.insumo.unidadMedida) : ''}</span>
                      </div>
                      <div className="mp-linea-costo">{formatearDinero(d.costo)}</div>
                      <button type="button" className="mp-quitar" onClick={() => quitarInsumo(d.idInsumo)} title="Quitar insumo" aria-label="Quitar insumo">✕</button>
                    </div>
                  ))}
                </div>
              )}

              <div className="mp-agregar">
                <select
                  className="mp-input"
                  value=""
                  onChange={(e) => agregarInsumo(e.target.value)}
                  disabled={insumosLibres.length === 0}
                  aria-label="Agregar insumo"
                >
                  <option value="">{insumosLibres.length === 0 ? 'No quedan insumos para agregar' : '+ Agregar insumo...'}</option>
                  {insumosLibres.map((i) => (
                    <option key={i.idInsumo} value={i.idInsumo}>
                      {i.nombre} ({formatearDinero(i.costoUnitario)} por {simboloUnidad(i.unidadMedida)})
                    </option>
                  ))}
                </select>
                <button type="button" className="mp-btn mp-btn-principal" onClick={guardar} disabled={!hayCambios || guardando}>
                  {guardando ? 'Guardando...' : 'Guardar materia prima'}
                </button>
              </div>
            </div>

            {/* ---------- Costo, precio y ganancia ---------- */}
            <div className="mp-seccion">
              <h2>Costo y ganancia <span className="mp-por">por {porUnidad}</span></h2>
              <div className="mp-cuadros">
                <div className="mp-cuadro">
                  <span>Costo de fabricación</span>
                  <strong>{formatearDinero(costoFabricacion)}</strong>
                </div>
                <div className="mp-cuadro">
                  <label htmlFor="mp-precio-venta">Precio de venta</label>
                  <div className="mp-precio-campo">
                    <span>$</span>
                    <InputNumero id="mp-precio-venta" className="mp-input mp-input-precio" decimales={2}
                      value={precioElegido === '' ? String(precioPublicado) : precioElegido}
                      onChange={(e) => cambiarPrecio(e.target.value)} />
                  </div>
                  {usaPrecioPropio ? (
                    <button type="button" className="mp-link" onClick={() => cambiarPrecio('')}>
                      Volver al publicado ({formatearDinero(precioPublicado)})
                    </button>
                  ) : (
                    <small>Podés cambiarlo para ver tu ganancia</small>
                  )}
                </div>
                <div className={`mp-cuadro mp-cuadro-resultado ${hayGanancia ? 'mp-ganancia' : 'mp-perdida'}`}>
                  <span>{hayGanancia ? 'Ganancia' : 'Pérdida'}</span>
                  <strong>{formatearDinero(ganancia)}</strong>
                  {margen !== null && <small>Margen: {margen.toLocaleString('es-AR')}%</small>}
                </div>
              </div>
              {lineas.length === 0 && <p className="mp-vacio-chico">Sin materia prima cargada, el costo de fabricación es $0.</p>}
            </div>

            {/* ---------- Ganancia según lo vendido ---------- */}
            <div className="mp-seccion">
              <h2>Ganancia según lo que vendiste</h2>
              <label className="mp-etiqueta" htmlFor="mp-vendidas">
                {unidad === 'unidad' ? '¿Cuántas unidades vendiste?' : `¿Cuántos ${simboloUnidad(unidad)} vendiste?`}
              </label>
              <InputNumero id="mp-vendidas" className="mp-input mp-input-corto" decimales={3} value={vendidas}
                onChange={(e) => cambiarVendidas(e.target.value)} />

              {hayVentas ? (
                <div className="mp-cuadros">
                  <div className="mp-cuadro">
                    <span>Ingreso por ventas</span>
                    <strong>{formatearDinero(ingreso)}</strong>
                    <small>{formatearDinero(precio)} × {etiquetaMedida(cantidadVendida, unidad)}</small>
                  </div>
                  <div className="mp-cuadro">
                    <span>Costo de fabricar lo vendido</span>
                    <strong>{formatearDinero(costoTotal)}</strong>
                    <small>{formatearDinero(costoFabricacion)} × {etiquetaMedida(cantidadVendida, unidad)}</small>
                  </div>
                  <div className={`mp-cuadro mp-cuadro-resultado ${gananciaTotal >= 0 ? 'mp-ganancia' : 'mp-perdida'}`}>
                    <span>{gananciaTotal >= 0 ? 'Ganancia total' : 'Pérdida total'}</span>
                    <strong>{formatearDinero(gananciaTotal)}</strong>
                  </div>
                </div>
              ) : (
                <p className="mp-vacio-chico">Ingresá lo que vendiste para ver la ganancia total.</p>
              )}
            </div>
          </>
        )}

        {/* ---------- Resumen de todos los productos ---------- */}
        {resumen.length > 0 && (
          <div className="mp-seccion">
            <h2>Todos los productos con materia prima <span className="mp-por">(por unidad)</span></h2>
            <div className="mp-tabla-scroll">
              <table className="mp-tabla">
                <thead>
                  <tr><th>Producto</th><th>Costo de fabricación</th><th>Precio de venta</th><th>Ganancia</th><th>Margen</th></tr>
                </thead>
                <tbody>
                  {resumen.map((r) => {
                    const prod = productos.find((p) => p.idProducto === r.idProducto);
                    return (
                      <tr key={r.idProducto} className={r.idProducto === idSeleccionado ? 'mp-fila-activa' : ''} onClick={() => elegirProducto(String(r.idProducto))}>
                        <td className="mp-nombre">{prod?.nombre || `Producto #${r.idProducto}`}</td>
                        <td>{formatearDinero(r.costoFabricacion)}</td>
                        <td>{formatearDinero(r.precioVenta)}</td>
                        <td className={Number(r.ganancia) >= 0 ? 'mp-texto-ganancia' : 'mp-texto-perdida'}>{formatearDinero(r.ganancia)}</td>
                        <td>{r.margenPorcentaje === null ? '-' : `${Number(r.margenPorcentaje).toLocaleString('es-AR')}%`}</td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
