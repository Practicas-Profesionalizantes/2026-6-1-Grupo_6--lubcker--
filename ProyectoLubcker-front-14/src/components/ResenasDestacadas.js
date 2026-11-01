import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { resolverImagen } from '../imagenes';
import Estrellas from './Estrellas';
import './ResenasDestacadas.css';
import { API_URL } from '../config';


// Página principal: últimas opiniones de 4 y 5 estrellas. Si no hay, no se muestra nada.
export default function ResenasDestacadas() {
  const [resenas, setResenas] = useState([]);

  useEffect(() => {
    fetch(`${API_URL}/resenas/destacadas`)
      .then((res) => (res.ok ? res.json() : []))
      .then(setResenas)
      .catch(() => setResenas([]));
  }, []);

  if (resenas.length === 0) return null;

  return (
    <section className="destacadas-seccion">
      <h2 className="titulo-seccion">Lo que dicen nuestros clientes</h2>
      <div className="destacadas-grid">
        {resenas.map((r) => (
          <Link key={r.idResenas} to={`/producto/${r.idProducto}`} className="destacadas-tarjeta">
            <div className="destacadas-producto">
              <img src={resolverImagen(r.imagenProducto)} alt="" />
              <span>{r.nombreProducto}</span>
            </div>
            <Estrellas valor={r.valoracion} tamano={16} />
            {r.descripcion && <p className="destacadas-texto">“{r.descripcion}”</p>}
            <span className="destacadas-autor">{r.nombreCliente} · ✓ Compra verificada</span>
          </Link>
        ))}
      </div>
    </section>
  );
}
