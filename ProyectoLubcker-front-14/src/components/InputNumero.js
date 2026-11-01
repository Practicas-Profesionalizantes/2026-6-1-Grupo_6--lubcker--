import React, { useRef, useLayoutEffect } from 'react';

// Campo numérico que va poniendo el punto de los miles mientras se escribe (2000 -> 2.000).
// El decimal se escribe con coma (2.500,50).
//
// El valor que recibe y entrega el padre es texto "crudo", sin separadores de miles y con punto
// decimal ("2500.5"), listo para Number()/parseInt(). onChange recibe un objeto con la forma
// { target: { name, value } }, igual que un input común, así que sirve para los mismos handlers.
//
// Props: value, onChange, name, decimales (cuántos decimales admite; 0 = solo enteros).
// El resto de las props (id, className, placeholder, required...) pasan al <input>.

// "2500.5" -> "2.500,5" ("2500." -> "2.500," para no perder la coma mientras se escribe)
export function formatearEntrada(crudo) {
  const texto = crudo === null || crudo === undefined ? '' : String(crudo);
  if (texto === '') return '';
  const [entero, decimal] = texto.split('.');
  const conMiles = entero.replace(/\B(?=(\d{3})+(?!\d))/g, '.');
  return decimal === undefined ? conMiles : `${conMiles},${decimal}`;
}

// Lo que escribió la persona -> valor crudo. Se descartan letras y símbolos; la coma es el decimal.
export function limpiarEntrada(texto, decimales) {
  let limpio = texto.replace(/[^\d,]/g, '');
  const posComa = limpio.indexOf(',');
  if (decimales <= 0 || posComa === -1) {
    limpio = limpio.replace(/,/g, '');
    return limpio.replace(/^0+(?=\d)/, '');
  }
  const entero = limpio.slice(0, posComa).replace(/,/g, '').replace(/^0+(?=\d)/, '');
  const decimal = limpio.slice(posComa + 1).replace(/,/g, '').slice(0, decimales);
  return `${entero === '' ? '0' : entero}.${decimal}`;
}

export default function InputNumero({ value, onChange, name, decimales = 0, ...resto }) {
  const ref = useRef(null);
  const digitosAntesDelCursor = useRef(null);

  const mostrado = formatearEntrada(value);

  // Después de reformatear, el cursor vuelve al mismo dígito donde estaba (si no, saltaría al final)
  useLayoutEffect(() => {
    const el = ref.current;
    if (!el || digitosAntesDelCursor.current === null || document.activeElement !== el) return;
    let contados = 0;
    let pos = 0;
    while (pos < mostrado.length && contados < digitosAntesDelCursor.current) {
      if (/[\d,]/.test(mostrado[pos])) contados++;
      pos++;
    }
    el.setSelectionRange(pos, pos);
    digitosAntesDelCursor.current = null;
  }, [mostrado]);

  const alCambiar = (e) => {
    const texto = e.target.value;
    const cursor = e.target.selectionStart ?? texto.length;
    digitosAntesDelCursor.current = (texto.slice(0, cursor).match(/[\d,]/g) || []).length;
    onChange({ target: { name, value: limpiarEntrada(texto, decimales) } });
  };

  return (
    <input
      {...resto}
      ref={ref}
      name={name}
      type="text"
      inputMode={decimales > 0 ? 'decimal' : 'numeric'}
      autoComplete="off"
      value={mostrado}
      onChange={alCambiar}
    />
  );
}
