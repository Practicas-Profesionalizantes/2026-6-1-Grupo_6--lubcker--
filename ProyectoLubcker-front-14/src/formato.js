// Formato único de precios en todo el sitio: $ 12.500 (miles con punto, sin decimales)
export function formatearPrecio(valor) {
  const numero = Number(valor) || 0;
  return '$' + numero.toLocaleString('es-AR', { maximumFractionDigits: 0 });
}

// Montos que pueden tener centavos (costos de insumos): $1.250,50 | $2.000 (sin ceros de más)
export function formatearDinero(valor) {
  const numero = Number(valor) || 0;
  return '$' + numero.toLocaleString('es-AR', { minimumFractionDigits: 0, maximumFractionDigits: 2 });
}

// Para buscar sin importar mayúsculas ni acentos: "Jabón" coincide con "jabon"
export function normalizarTexto(texto) {
  return (texto || '')
    .toString()
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .toLowerCase()
    .trim();
}
