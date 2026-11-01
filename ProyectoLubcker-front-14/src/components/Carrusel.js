import React, { useState, useEffect, useRef } from 'react';
import { Link } from 'react-router-dom';
import { resolverImagen } from '../imagenes';
import './Carrusel.css';
import { API_URL } from '../config';


// Carrusel de la página principal. Las imágenes las carga el admin desde "Gestionar carrusel"
// (medida recomendada: 1800 × 600 px, proporción 3:1). Si no hay ninguna, no se muestra nada.
export default function Carrusel({ titulo }) {
  const [slides, setSlides] = useState([]);
  const [currentSlide, setCurrentSlide] = useState(0);
  const [isPaused, setIsPaused] = useState(false);
  const intervalRef = useRef(null);
  const timeoutRef = useRef(null);

  const nextSlide = () => {
    resetPauseTimer();
    setCurrentSlide((prev) => (prev + 1) % slides.length);
  };

  const prevSlide = () => {
    resetPauseTimer();
    setCurrentSlide((prev) => (prev - 1 + slides.length) % slides.length);
  };

  // Al interactuar, el avance automático se pausa 8 segundos
  const resetPauseTimer = () => {
    setIsPaused(true);
    if (timeoutRef.current) clearTimeout(timeoutRef.current);
    timeoutRef.current = setTimeout(() => setIsPaused(false), 8000);
  };

  useEffect(() => {
    fetch(`${API_URL}/banners`)
      .then((res) => (res.ok ? res.json() : []))
      .then(setSlides)
      .catch(() => setSlides([]));
  }, []);

  useEffect(() => {
    setCurrentSlide(0);
  }, [slides]);

  useEffect(() => {
    if (!isPaused && slides.length > 1) {
      intervalRef.current = setInterval(() => {
        setCurrentSlide((prev) => (prev + 1) % slides.length);
      }, 4000);
    }
    return () => {
      if (intervalRef.current) clearInterval(intervalRef.current);
      if (timeoutRef.current) clearTimeout(timeoutRef.current);
    };
  }, [isPaused, slides.length]);

  // Sin banners cargados no hay nada que mostrar (ni siquiera el título)
  if (slides.length === 0) return null;

  const hayVarios = slides.length > 1;

  return (
    <>
      {titulo && <h2 className="titulo-seccion">{titulo}</h2>}
      <div className="carrusel-contenedor" onClick={resetPauseTimer}>
        <div className="carrusel-slides" style={{ transform: `translateX(-${currentSlide * 100}%)` }}>
          {slides.map((slide) => {
            const contenido = (
              <>
                <img src={resolverImagen(slide.imagen)} alt={slide.titulo || 'Banner'} className="carrusel-image" />
                {slide.titulo && (
                  <div className="carrusel-overlay">
                    <p className="carrusel-name">{slide.titulo}</p>
                  </div>
                )}
              </>
            );
            return (
              <div className="carrusel-slide" key={slide.idBanner}>
                {slide.enlace ? (
                  <Link to={slide.enlace} className="carrusel-enlace">{contenido}</Link>
                ) : (
                  contenido
                )}
              </div>
            );
          })}
        </div>

        {hayVarios && (
          <>
            <button className="carrusel-btn carrusel-prev" aria-label="Anterior" onClick={(e) => { e.stopPropagation(); prevSlide(); }}>
              <span>&#10094;</span>
            </button>
            <button className="carrusel-btn carrusel-next" aria-label="Siguiente" onClick={(e) => { e.stopPropagation(); nextSlide(); }}>
              <span>&#10095;</span>
            </button>
          </>
        )}
      </div>
    </>
  );
}
