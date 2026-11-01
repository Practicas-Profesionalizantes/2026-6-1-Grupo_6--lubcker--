// Productos que se venden por medida (kg, litro, metro...).
// El precio del producto es el de UNA unidad de medida (ej: $10.000 el kilo) y el cliente elige
// entre las medidas que cargó el admin (ej: 3, 5, 10 kg). Precio final = precio x medida.

export const UNIDADES = [
  { valor: 'unidad', nombre: 'Unidad' },
  { valor: 'kg', nombre: 'Kilo (kg)' },
  { valor: 'litro', nombre: 'Litro (L)' },
  { valor: 'metro', nombre: 'Metro (m)' },
];

// Unidades en las que se miden los insumos (materia prima)
export const UNIDADES_INSUMO = [
  { valor: 'kg', nombre: 'Kilo (kg)' },
  { valor: 'g', nombre: 'Gramo (g)' },
  { valor: 'litro', nombre: 'Litro (L)' },
  { valor: 'ml', nombre: 'Mililitro (ml)' },
  { valor: 'unidad', nombre: 'Unidad' },
  { valor: 'metro', nombre: 'Metro (m)' },
];

// Completa unidad y medidas de un producto que viene del backend (productos viejos: unidad, de a 1)
export function mapearMedidas(p) {
  const presentaciones = Array.isArray(p.presentaciones) && p.presentaciones.length > 0 ? p.presentaciones : [1];
  return { unidad: p.unidadMedida || 'unidad', presentaciones };
}

// ¿Hay que elegir medida? (false = producto común, se agrega de a una unidad)
export function vendePorMedida(producto) {
  const medidas = producto.presentaciones || [1];
  return (producto.unidad || 'unidad') !== 'unidad' || medidas.length > 1 || medidas[0] !== 1;
}

const SIMBOLOS = { kg: 'kg', litro: 'L', metro: 'm' };

// 5 + kg -> "5 kg" | 1 + unidad -> "1 unidad" | 6 + unidad -> "6 unidades"
export function etiquetaMedida(tamano, unidad = 'unidad') {
  if (unidad === 'unidad') return tamano === 1 ? '1 unidad' : `${tamano} unidades`;
  return `${tamano} ${SIMBOLOS[unidad] || unidad}`;
}

// Cantidad de stock con su unidad: "12 kg", "1 unidad", "5 unidades"
export const textoStock = (stock, unidad = 'unidad') => etiquetaMedida(stock, unidad);

// "kg" -> "kg", "litro" -> "L"... (para "$10.000 por kg")
export const simboloUnidad = (unidad) => SIMBOLOS[unidad] || unidad;

// Lee "3, 5, 10" -> { lista: [3, 5, 10] } o { error: '...' }. Vacío = sin medidas (se vende de a 1).
export function leerMedidas(texto) {
  const partes = (texto || '').split(/[\s,;]+/).filter(Boolean);
  const lista = [];
  for (const parte of partes) {
    if (!/^\d+$/.test(parte) || Number(parte) < 1 || Number(parte) > 100000) {
      return { lista: [], error: `"${parte}" no es una medida válida: usá números enteros separados por coma (ej: 3, 5, 10).` };
    }
    if (!lista.includes(Number(parte))) lista.push(Number(parte));
  }
  if (lista.length > 12) return { lista: [], error: 'Podés cargar hasta 12 medidas.' };
  return { lista: lista.sort((a, b) => a - b) };
}
