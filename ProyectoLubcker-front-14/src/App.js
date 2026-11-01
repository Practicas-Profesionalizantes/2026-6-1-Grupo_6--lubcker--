import { useState, useEffect } from 'react';
import './App.css';
import { BrowserRouter as Router, Routes, Route, useLocation } from 'react-router-dom';
import Navbar from './components/Navbar';
import CatalogoInteractivo from './components/CatalogoInteractivo';
import Carrusel from './components/Carrusel';
import Historial from './paginas/Historial';
import MisResenas from './paginas/MisResenas';
import Ayuda from './paginas/Ayuda';
import Carrito from './paginas/Carrito';
import PaginaProducto from "./paginas/PaginaProducto";
import Login from './paginas/Login';
import Registro from './paginas/Registro';
import Perfil from './paginas/Perfil';
import PedidosAdmin from './paginas/PedidosAdmin';
import UsuariosAdmin from './paginas/UsuariosAdmin';
import BannersAdmin from './paginas/BannersAdmin';
import InsumosAdmin from './paginas/InsumosAdmin';
import MateriaPrimaAdmin from './paginas/MateriaPrimaAdmin';
import ChatWidget from './components/ChatWidget';
import Pago from './paginas/Pago';
import PagoResultado from './paginas/PagoResultado';
import AdminProductos from './paginas/AdminProductos';
import { AuthProvider } from './AuthContext';
import { FavoritosProvider } from './FavoritosContext';
import ResenasDestacadas from './components/ResenasDestacadas';
import { SoloConSesion, SoloSinSesion, SoloAdmin } from './RutaProtegida';

// COMPONENTE AUXILIAR PARA FORZAR EL SCROLL AL TOPE EN CADA CAMBIO DE RUTA
function ScrollToTop() {
  const { pathname } = useLocation();

  useEffect(() => {
    window.scrollTo(0, 0);
  }, [pathname]);

  return null;
}

// Lee el carrito guardado en el navegador (si no hay o está dañado, arranca vacío)
function leerCarritoGuardado() {
  try {
    const guardado = localStorage.getItem('carrito');
    const lista = guardado ? JSON.parse(guardado) : [];
    // Carritos guardados antes de las medidas: se completan como productos de a 1 unidad
    return lista.map((i) => ({
      tamano: 1, unidad: 'unidad', precioBase: i.precio, ...i, clave: `${i.id}-${i.tamano || 1}`,
    }));
  } catch {
    return [];
  }
}

function App() {
  const [carrito, setCarrito] = useState(leerCarritoGuardado);

  // Cada vez que cambia el carrito, se guarda para que sobreviva a recargas de la página
  useEffect(() => {
    try {
      localStorage.setItem('carrito', JSON.stringify(carrito));
    } catch {
      // Si el navegador no deja guardar, la app sigue funcionando sin persistencia
    }
  }, [carrito]);
  const [seccionCatalogo, setSeccionCatalogo] = useState('todos');

  // Cada línea del carrito es un producto en UNA medida (ej: GRASA x 5 kg), identificada por "clave".
  // "precioBase" es el precio de una unidad de medida y "precio" el de un paquete (precioBase x tamano).
  // "stock" está en unidades de medida: entre todas las líneas del mismo producto no pueden pasarlo.
  // Agrega un paquete. Devuelve false si no hay stock para sumarlo
  // (el backend igual vuelve a validar el stock y los precios al confirmar la compra).
  const agregarAlCarrito = (producto, tamanoElegido) => {
    const tamano = tamanoElegido || producto.tamano || 1;
    const precioBase = producto.precioBase ?? producto.precio;
    const clave = `${producto.id}-${tamano}`;

    const delProducto = carrito.filter(item => item.id === producto.id);
    const stock = typeof producto.stock === 'number' ? producto.stock : delProducto[0]?.stock;
    const enUso = delProducto.reduce((suma, item) => suma + item.tamano * item.cantidad, 0);

    if (typeof stock === 'number' && enUso + tamano > stock) return false;

    setCarrito((prevCarrito) => {
      const existe = prevCarrito.find(item => item.clave === clave);
      if (existe) {
        return prevCarrito.map(item =>
          item.clave === clave ? { ...item, cantidad: item.cantidad + 1, stock } : item
        );
      }
      return [...prevCarrito, { ...producto, clave, tamano, precioBase, precio: precioBase * tamano, cantidad: 1, stock }];
    });
    return true;
  };

  // Resta un paquete; si queda en 0, saca la línea del carrito
  const restarDelCarrito = (clave) => {
    setCarrito((prevCarrito) =>
      prevCarrito
        .map(item => item.clave === clave ? { ...item, cantidad: item.cantidad - 1 } : item)
        .filter(item => item.cantidad > 0)
    );
  };

  const eliminarDelCarrito = (clave) => {
    setCarrito((prevCarrito) => prevCarrito.filter(item => item.clave !== clave));
  };

  // Función para vaciar el carrito cuando el pago sea exitoso
  const vaciarCarrito = () => {
    setCarrito([]);
  };

  return (
    <AuthProvider>
      <FavoritosProvider>
      <Router>
        {/* SE INYECTA AQUÍ PARA QUE CONTROLE TODAS LAS RUTAS DE LA APP */}
        <ScrollToTop />

        <div className="app-container">

          <header>
            <Navbar
              carrito={carrito}
              eliminarDelCarrito={eliminarDelCarrito}
              setSeccionCatalogo={setSeccionCatalogo}
            />
          </header>

          <main className="main-content">
            <Routes>
              <Route
                path="/"
                element={
                  <>
                    <section className="bienvenida">
                      <h1 className="bienvenida-titulo">¡Bienvenido a Lubcker!</h1>
                      <p className="bienvenida-subtitulo">Tu aliado para la higiene y seguridad de tus manos en el taller.</p>
                    </section>

                    <section className="seccion-carrusel">
                      <Carrusel />
                    </section>

                    <section className="seccion-catalogo" id="catalogo-ancla">
                      <CatalogoInteractivo
                        agregarAlCarrito={agregarAlCarrito}
                        carrito={carrito}
                        eliminarDelCarrito={eliminarDelCarrito}
                        seccionFija={seccionCatalogo}
                        setSeccionFija={setSeccionCatalogo}
                      />
                    </section>

                    <ResenasDestacadas />
                  </>
                }
              />

              <Route path="/historial" element={<SoloConSesion><Historial /></SoloConSesion>} />
              <Route path="/ayuda" element={<Ayuda />} />
              <Route path="/resenas" element={<SoloConSesion><MisResenas /></SoloConSesion>} />
              <Route path="/login" element={<SoloSinSesion><Login /></SoloSinSesion>} />
              <Route path="/admin/productos" element={<SoloAdmin><AdminProductos /></SoloAdmin>} />
              <Route path="/admin/banners" element={<SoloAdmin><BannersAdmin /></SoloAdmin>} />
              <Route path="/admin/insumos" element={<SoloAdmin><InsumosAdmin /></SoloAdmin>} />
              <Route path="/admin/materia-prima" element={<SoloAdmin><MateriaPrimaAdmin /></SoloAdmin>} />
              <Route path="/admin/usuarios" element={<SoloAdmin><UsuariosAdmin /></SoloAdmin>} />
              <Route path="/admin/pedidos" element={<SoloAdmin><PedidosAdmin /></SoloAdmin>} />
              <Route path="/perfil" element={<SoloConSesion><Perfil /></SoloConSesion>} />
              <Route path="/registro" element={<SoloSinSesion><Registro /></SoloSinSesion>} />

              <Route path="/carrito" element={
                <Carrito
                  carrito={carrito}
                  eliminarDelCarrito={eliminarDelCarrito}
                  agregarAlCarrito={agregarAlCarrito}
                  restarDelCarrito={restarDelCarrito}
                />
              } />

              <Route path="/pago" element={
                <SoloConSesion><Pago carrito={carrito} vaciarCarrito={vaciarCarrito} /></SoloConSesion>
              } />

              <Route path="/pago/resultado" element={<SoloConSesion><PagoResultado /></SoloConSesion>} />

              <Route
                path="/producto/:id"
                element={
                  <PaginaProducto agregarAlCarrito={agregarAlCarrito} />
                }
              />
            </Routes>
          </main>

          {/* Asistente de chat: botón flotante en todas las páginas */}
          <ChatWidget />

          <footer>
            <div className="footer-contenido">
              <p>&copy; 2026 Lubcker - Tienda de Higiene Mecánica.</p>
              <p>Todos los derechos reservados.</p>
            </div>
          </footer>

        </div>
      </Router>
      </FavoritosProvider>
    </AuthProvider>
  );
}

export default App;