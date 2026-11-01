import React from 'react';
import './Ayuda.css';

// Datos de contacto de Lubcker. Para cambiarlos, se editan solo acá.
const WHATSAPP_TEXTO = '+54 9 11 4430-6194';
const WHATSAPP_LINK = 'https://wa.me/5491144306194';
const EMAIL = 'info@lubcker.com';

export default function Ayuda() {
  return (
    <div className="ayuda-contenedor">
      <div className="ayuda-tarjeta">
        <h1 className="ayuda-titulo">¿Necesitas ayuda con tu caso?</h1>
        <p className="ayuda-texto">Contactate al</p>

        <ul className="ayuda-contactos">
          <li>
            <span className="ayuda-etiqueta">Whatsapp:</span>
            <a href={WHATSAPP_LINK} target="_blank" rel="noopener noreferrer">{WHATSAPP_TEXTO}</a>
          </li>
          <li>
            <span className="ayuda-etiqueta">Email:</span>
            <a href={`mailto:${EMAIL}`}>{EMAIL}</a>
          </li>
        </ul>
      </div>
    </div>
  );
}
