// Estrellas de valoración. Solo lectura, o seleccionables si se pasa onChange.
export default function Estrellas({ valor = 0, onChange, tamano = 18 }) {
  const editable = typeof onChange === 'function';

  return (
    <span
      className={`estrellas ${editable ? 'estrellas-editables' : ''}`}
      style={{ fontSize: `${tamano}px` }}
      role={editable ? 'radiogroup' : 'img'}
      aria-label={editable ? 'Elegí una valoración' : `${valor} de 5 estrellas`}
    >
      {[1, 2, 3, 4, 5].map((n) => {
        const llena = n <= Math.round(valor);
        return editable ? (
          <button
            key={n}
            type="button"
            className={`estrella ${llena ? 'llena' : ''}`}
            onClick={() => onChange(n)}
            role="radio"
            aria-checked={n === valor}
            aria-label={`${n} ${n === 1 ? 'estrella' : 'estrellas'}`}
          >
            ★
          </button>
        ) : (
          <span key={n} className={`estrella ${llena ? 'llena' : ''}`} aria-hidden="true">
            ★
          </span>
        );
      })}
    </span>
  );
}
