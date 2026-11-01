import React from 'react';
import ReactDOM from 'react-dom/client';
import './index.css';
import App from './App';
import './responsive.css'; // ajustes para celular: va después de App para ganar a los CSS de cada pantalla
import { instalarFetchConToken } from './api';

// Antes de renderizar: así todas las llamadas a la API llevan el token de sesión
instalarFetchConToken();

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(
    <App />
);