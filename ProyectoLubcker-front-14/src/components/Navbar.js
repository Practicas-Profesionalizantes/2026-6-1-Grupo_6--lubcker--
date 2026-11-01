import { useState, useEffect, useRef } from 'react'; 
import './Navbar.css';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../AuthContext';
import { urlFotoPerfil, resolverImagen } from '../imagenes';
import { formatearPrecio, normalizarTexto } from '../formato';

import usuarioImg from '../assets/iconos/usuario.jpg';
import carritoImg from '../assets/iconos/carrito.png';
import { etiquetaMedida } from '../medidas'; 
import logolubckerImg from '../assets/iconos/logolubcker.png';
import { API_URL } from '../config';

const Navbar = ({ carrito = [], eliminarDelCarrito }) => { 
  const [menuActivo, setMenuActivo] = useState(null);
  const navigate = useNavigate();
  const contenedorUsuarioRef = useRef(null);
  const { usuario, cerrarSesion } = useAuth();
  const [textoBusqueda, setTextoBusqueda] = useState('');
  const [categorias, setCategorias] = useState(null); // se cargan al abrir el submenú
  const [verCategorias, setVerCategorias] = useState(false);
  const [productosBusqueda, setProductosBusqueda] = useState(null); // se cargan al empezar a escribir
  const [sugerenciasAbiertas, setSugerenciasAbiertas] = useState(false);
  const buscadorRef = useRef(null);

  const esAdmin = usuario?.rol === 'ADMIN';
  const fotoUrl = urlFotoPerfil(usuario?.fotoPerfil);
  const inicialUsuario = (usuario?.nombreUsuario || '?').charAt(0).toUpperCase();

  const alternarMenu = (menu) => {
    setMenuActivo(menuActivo === menu ? null : menu);
  };

  const irAlLogin = () => {
    setMenuActivo(null); 
    navigate('/login');  
  };

  const irAlRegistro = (e) => {
    e.preventDefault(); 
    e.stopPropagation(); 
    setMenuActivo(null); 
    navigate('/registro');
  };

  const salir = () => {
    setMenuActivo(null);
    cerrarSesion();
    navigate('/');
  };

  // Sugerencias del buscador: productos cuyo nombre contiene lo escrito (primero los que empiezan igual)
  const textoNormalizado = normalizarTexto(textoBusqueda.trim());
  const sugerencias = textoNormalizado && productosBusqueda
    ? productosBusqueda
        .filter((p) => p.activo !== false && normalizarTexto(p.nombre).includes(textoNormalizado))
        .sort((a, b) =>
          Number(normalizarTexto(b.nombre).startsWith(textoNormalizado)) -
          Number(normalizarTexto(a.nombre).startsWith(textoNormalizado)))
        .slice(0, 6)
    : [];

  const cambiarBusqueda = (e) => {
    setTextoBusqueda(e.target.value);
    setSugerenciasAbiertas(true);
    if (productosBusqueda === null) {
      fetch(`${API_URL}/productos`)
        .then((res) => (res.ok ? res.json() : []))
        .then(setProductosBusqueda)
        .catch(() => setProductosBusqueda([]));
    }
  };

  const irAlProducto = (idProducto) => {
    setSugerenciasAbiertas(false);
    setMenuActivo(null);
    navigate(`/producto/${idProducto}`);
  };

  // Buscador: lleva al catálogo filtrado (/?q=texto)
  const buscar = (e) => {
    e.preventDefault();
    const texto = textoBusqueda.trim();
    setSugerenciasAbiertas(false);
    setMenuActivo(null);
    navigate(texto ? `/?q=${encodeURIComponent(texto)}` : '/');
  };

  const irAlCatalogo = () => {
    setMenuActivo(null);
    navigate('/', { state: { verCatalogo: Date.now() } });
  };

  const alternarCategorias = () => {
    setVerCategorias(!verCategorias);
    if (categorias === null) {
      fetch(`${API_URL}/categorias`)
        .then((res) => (res.ok ? res.json() : []))
        .then(setCategorias)
        .catch(() => setCategorias([]));
    }
  };

  const irACategoria = (nombre) => {
    setMenuActivo(null);
    navigate(`/?categoria=${encodeURIComponent(nombre.toLowerCase())}`);
  };

  // Lleva a la vista completa del carrito (/carrito)
  const irAlCarrito = () => {
    setMenuActivo(null);
    navigate('/carrito');
  };

  useEffect(() => {
    const manejarClicFuera = (evento) => {
      if (
        menuActivo === 'usuario' && 
        contenedorUsuarioRef.current && 
        !contenedorUsuarioRef.current.contains(evento.target)
      ) {
        setMenuActivo(null);
      }
    };

    document.addEventListener('mousedown', manejarClicFuera);
    return () => {
      document.removeEventListener('mousedown', manejarClicFuera);
    };
  }, [menuActivo]);

  // Las sugerencias se cierran al hacer clic fuera del buscador
  useEffect(() => {
    const cerrarSugerencias = (evento) => {
      if (buscadorRef.current && !buscadorRef.current.contains(evento.target)) {
        setSugerenciasAbiertas(false);
      }
    };
    document.addEventListener('mousedown', cerrarSugerencias);
    return () => document.removeEventListener('mousedown', cerrarSugerencias);
  }, []);

  // CONTROL SEGURO DE TOTALES
  let totalArticulos = 0;
  let totalPrecio = 0;

  if (carrito && Array.isArray(carrito)) {
    totalArticulos = carrito.reduce((acumulador, item) => acumulador + item.cantidad, 0);
    totalPrecio = carrito.reduce((acumulador, item) => acumulador + (item.precio * item.cantidad), 0);
  }

  return (
    <>
      <nav className="navbar">
        <div className="navbar-left">
          <div className="hamburger" onClick={() => alternarMenu('lateral')}>
            <span className="line"></span>
            <span className="line"></span>
            <span className="line"></span>
          </div>
          <Link to="/" className="logo-lubcker" onClick={() => setMenuActivo(null)}>
            <img src={logolubckerImg} alt="Logo" className="logo-lubcker-img" />
          </Link>
        </div>

        <div className="navbar-center" ref={buscadorRef}>
          <form onSubmit={buscar} role="search" style={{ width: '100%', display: 'flex', justifyContent: 'center' }}>
            <input
              type="search"
              placeholder="Buscar productos..."
              className="search-bar"
              value={textoBusqueda}
              onChange={cambiarBusqueda}
              onFocus={() => setSugerenciasAbiertas(true)}
              onKeyDown={(e) => e.key === 'Escape' && setSugerenciasAbiertas(false)}
              aria-label="Buscar productos"
              autoComplete="off"
            />
          </form>

          {sugerenciasAbiertas && textoNormalizado && productosBusqueda && (
            <ul className="sugerencias-busqueda" role="listbox">
              {sugerencias.length === 0 ? (
                <li className="sugerencia-vacia">No encontramos productos para "{textoBusqueda.trim()}"</li>
              ) : (
                sugerencias.map((p) => (
                  <li
                    key={p.idProducto}
                    className="sugerencia-item"
                    role="option" aria-selected="false"
                    onMouseDown={(e) => { e.preventDefault(); irAlProducto(p.idProducto); }}
                  >
                    <img src={resolverImagen(p.imagen)} alt="" />
                    <span className="sugerencia-nombre">{p.nombre}</span>
                    <span className="sugerencia-precio">{formatearPrecio(p.precio)}</span>
                  </li>
                ))
              )}
            </ul>
          )}
        </div>

        <div className="navbar-right">
          
          <div className="icon-box" ref={contenedorUsuarioRef}>
            {!usuario ? (
              <img
                src={usuarioImg}
                alt="Usuario"
                className="nav-icon-login"
                onClick={() => alternarMenu('usuario')}
              />
            ) : fotoUrl ? (
              <img
                src={fotoUrl}
                alt={usuario.nombreUsuario}
                className="nav-avatar-img"
                onClick={() => alternarMenu('usuario')}
              />
            ) : (
              <div
                className="nav-avatar"
                onClick={() => alternarMenu('usuario')}
                title={usuario.nombreUsuario}
              >
                {inicialUsuario}
              </div>
            )}

            <div className={`ventanita-flotante ${menuActivo === 'usuario' ? 'abierto' : ''}`}>
              {!usuario ? (
                <>
                  <button className="btn-primario" onClick={irAlLogin} style={{ width: '100%', cursor: 'pointer' }}>
                    Iniciar Sesión
                  </button>

                  <button
                    className="btn-secundario"
                    onClick={irAlRegistro}
                    style={{ width: '100%', marginTop: '8px', cursor: 'pointer', zIndex: 9999 }}
                  >
                    Registrarse
                  </button>
                </>
              ) : (
                <>
                  <div className="usuario-menu-cabecera">
                    <span className="usuario-menu-nombre">Hola, {usuario.nombreUsuario}</span>
                    <span className="usuario-menu-email">{usuario.email}</span>
                  </div>

                  <Link to="/perfil" onClick={() => setMenuActivo(null)} style={{ textDecoration: 'none', width: '100%' }}>
                    <button className="btn-secundario" style={{ width: '100%' }}>Mi Perfil</button>
                  </Link>

                  {/* El admin no compra: no ve historial. Sus vistas de gestión están en el menú lateral */}
                  {!esAdmin && (
                    <Link to="/historial" onClick={() => setMenuActivo(null)} style={{ textDecoration: 'none', width: '100%' }}>
                      <button className="btn-secundario" style={{ marginTop: '8px', width: '100%' }}>Mi Historial</button>
                    </Link>
                  )}

                  {esAdmin && (
                    <button
                      className="btn-secundario"
                      style={{ marginTop: '8px', width: '100%' }}
                      onClick={() => setMenuActivo('lateral')}
                    >
                      Panel de administración
                    </button>
                  )}

                  <button className="btn-cerrar-sesion" onClick={salir} style={{ marginTop: '8px', width: '100%' }}>
                    Cerrar Sesión
                  </button>
                </>
              )}
            </div>
          </div>

          <div className="icon-box">
            <img src={carritoImg} alt="Carrito" className="nav-icon-carrito" onClick={() => alternarMenu('carrito')} />
            <span className="numero-carrito">{totalArticulos}</span>
            
            <div className={`ventanita-flotante ${menuActivo === 'carrito' ? 'abierto' : ''}`} style={{ width: '320px', padding: '15px' }}>
              <h4 style={{ marginBottom: '15px', borderBottom: '1px solid #ddd', paddingBottom: '10px' }}>Tu Carrito</h4>
              {(!carrito || carrito.length === 0) ? (
                <p className="carrito-vacio" style={{ textAlign: 'center', color: '#666', padding: '20px 0' }}>No hay productos todavía.</p>
              ) : (
                <>
                  <div className="carrito-lista-desplegable" style={{ maxHeight: '250px', overflowY: 'auto', marginBottom: '15px' }}>
                    {carrito.map((item) => (
                      <div key={item.clave || item.id} style={{ display: 'flex', alignItems: 'center', gap: '10px', padding: '10px 0', borderBottom: '1px solid #eee', fontSize: '13px' }}>
                        <img src={item.imagen || "https://picsum.photos/300"} alt={item.nombre} style={{ width: '45px', height: '45px', objectFit: 'cover', borderRadius: '5px' }} />
                        <div style={{ flex: 1, textAlign: 'left' }}>
                          <div style={{ fontWeight: 'bold', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis', maxWidth: '120px' }}>{item.nombre}{item.tamano > 1 || (item.unidad && item.unidad !== 'unidad') ? ` · ${etiquetaMedida(item.tamano, item.unidad)}` : ''}</div>
                          <div style={{ color: '#666', fontSize: '12px' }}>x{item.cantidad} - {formatearPrecio(item.precio * item.cantidad)}</div>
                        </div>
                        <button onClick={() => eliminarDelCarrito && eliminarDelCarrito(item.clave)} style={{ background: 'none', border: 'none', color: '#dc2626', cursor: 'pointer', fontSize: '16px' }}>✕</button>
                      </div>
                    ))}
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontWeight: 'bold', fontSize: '16px', marginBottom: '15px' }}>
                    <span>Total:</span>
                    <span style={{ color: '#27ae60' }}>{formatearPrecio(totalPrecio)}</span>
                  </div>
                </>
              )}

              {/* Se muestra SIEMPRE, haya o no productos en el carrito */}
              <button onClick={irAlCarrito} className="btn-primario" style={{ width: '100%', padding: '10px', fontWeight: 'bold', marginTop: '5px', cursor: 'pointer' }}>
                Ver Carrito Completo
              </button>
            </div>
          </div>
        </div>
      </nav>

      <div className={`menu-lateral ${menuActivo === 'lateral' ? 'abierto' : ''}`}>
        <button className="btn-cerrar" onClick={() => setMenuActivo(null)}>X</button>
        <ul>
          <li><Link to="/" onClick={() => setMenuActivo(null)} style={{ textDecoration: 'none', color: 'inherit', display: 'block' }}>Inicio</Link></li>
          <li onClick={irAlCatalogo} style={{ cursor: 'pointer' }}>Productos</li>
          <li onClick={alternarCategorias} style={{ cursor: 'pointer' }}>
            Categorías {verCategorias ? '▴' : '▾'}
          </li>
          {verCategorias && (
            <li style={{ paddingTop: 0 }}>
              <ul className="menu-lateral-subcategorias">
                {categorias === null && <li>Cargando...</li>}
                {categorias?.length === 0 && <li>No hay categorías</li>}
                {categorias?.map((c) => (
                  <li key={c.idCategoria} onClick={() => irACategoria(c.nombre)} style={{ cursor: 'pointer' }}>
                    {c.nombre}
                  </li>
                ))}
              </ul>
            </li>
          )}
          {/* Sin sesión, la ruta protegida manda al inicio de sesión y después vuelve acá */}
          {!esAdmin && (
            <>
              <li><Link to="/historial" onClick={() => setMenuActivo(null)} style={{ textDecoration: 'none', color: 'inherit', display: 'block' }}>Historial de Compras</Link></li>
              <li><Link to="/resenas" onClick={() => setMenuActivo(null)} style={{ textDecoration: 'none', color: 'inherit', display: 'block' }}>Reseñas</Link></li>
            </>
          )}

          {!esAdmin && (
            <li><Link to="/ayuda" onClick={() => setMenuActivo(null)} style={{ textDecoration: 'none', color: 'inherit', display: 'block' }}>Ayuda</Link></li>
          )}

          {esAdmin && (
            <>
              <li className="menu-lateral-titulo">Administración</li>
              <li><Link to="/admin/productos" onClick={() => setMenuActivo(null)} style={{ textDecoration: 'none', color: 'inherit', display: 'block' }}>Gestionar productos</Link></li>
              <li><Link to="/admin/insumos" onClick={() => setMenuActivo(null)} style={{ textDecoration: 'none', color: 'inherit', display: 'block' }}>Insumos</Link></li>
              <li><Link to="/admin/materia-prima" onClick={() => setMenuActivo(null)} style={{ textDecoration: 'none', color: 'inherit', display: 'block' }}>Materia prima de productos</Link></li>
              <li><Link to="/admin/pedidos" onClick={() => setMenuActivo(null)} style={{ textDecoration: 'none', color: 'inherit', display: 'block' }}>Gestionar pedidos</Link></li>
              <li><Link to="/admin/usuarios" onClick={() => setMenuActivo(null)} style={{ textDecoration: 'none', color: 'inherit', display: 'block' }}>Gestionar usuarios</Link></li>
              <li><Link to="/admin/banners" onClick={() => setMenuActivo(null)} style={{ textDecoration: 'none', color: 'inherit', display: 'block' }}>Gestionar carrusel</Link></li>
            </>
          )}
        </ul>
      </div>
    </>
  );
};

export default Navbar;