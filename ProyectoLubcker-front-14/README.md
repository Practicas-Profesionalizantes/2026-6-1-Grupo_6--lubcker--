# Lubcker - Frontend

Tienda online de productos de higiene mecánica. Frontend en React (Create React App).

## Cómo correrlo

Requiere Node 18 o superior y el backend corriendo en `http://localhost:8080` (ver la carpeta `ecommerce`).

```bash
npm install
npm start
```

Se abre en http://localhost:3000.

## Scripts

- `npm start`: servidor de desarrollo.
- `npm run build`: build de producción en `build/`.

## Estructura

- `src/paginas/`: pantallas (catálogo, carrito, pago, login, registro, perfil, historial, panel de admin).
- `src/components/`: Navbar, carrusel, catálogo.
- `src/AuthContext.js` y `src/RutaProtegida.js`: sesión y protección de rutas.
- `src/imagenes.js`: resolución y subida de imágenes.

## Administrador

El rol de administrador se asigna directamente en la base de datos:

```sql
UPDATE usuario SET rol = 'ADMIN' WHERE email = 'correo@ejemplo.com';
```

Después hay que cerrar sesión y volver a iniciarla para que el cambio se refleje.
