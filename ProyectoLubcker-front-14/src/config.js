// Dirección del backend (con "/api" al final), definida en un solo lugar.
// Local: http://localhost:8080/api  |  Producción: variable REACT_APP_API_URL (ver .env.example).
// Se le saca la barra final por si se escribe "https://mi-backend.com/api/".
export const API_URL = (process.env.REACT_APP_API_URL || 'http://localhost:8080/api').replace(/\/+$/, '');

// Servidor sin el "/api" final: de ahí salen las imágenes subidas (/uploads/...)
export const HOST = API_URL.replace(/\/api$/, '');
