// Utilidades para mostrar y subir imágenes (productos y fotos de perfil).

import { API_URL, HOST } from './config';

export { HOST };

const PLACEHOLDER = 'https://picsum.photos/300';

// El campo "imagen" de un producto puede ser:
//  - una URL completa (https://...)
//  - una ruta subida al backend ("uploads/abc.jpg")
//  - el nombre de un archivo de src/assets/productos ("1DEDO.jpg")
export function resolverImagen(valor) {
  if (!valor) return PLACEHOLDER;
  if (/^https?:\/\//.test(valor)) return valor;
  if (valor.startsWith('uploads/')) return `${HOST}/${valor}`;
  try {
    return require(`./assets/productos/${valor}`);
  } catch {
    return PLACEHOLDER;
  }
}

// Foto de perfil: devuelve la URL o null si el usuario no tiene foto.
export function urlFotoPerfil(valor) {
  if (!valor) return null;
  if (/^https?:\/\//.test(valor)) return valor;
  return `${HOST}/${valor}`;
}

// Sube un archivo de imagen al backend y devuelve su ruta ("uploads/xxxx.jpg").
export async function subirImagen(archivo) {
  const datos = new FormData();
  datos.append('archivo', archivo);

  const res = await fetch(`${API_URL}/archivos`, { method: 'POST', body: datos });
  if (!res.ok) {
    let mensaje = 'No se pudo subir la imagen.';
    try {
      const cuerpo = await res.json();
      if (cuerpo.error) mensaje = cuerpo.error;
    } catch {}
    throw new Error(mensaje);
  }
  const { ruta } = await res.json();
  return ruta;
}
