import { textoStock } from '../medidas';

// Muestra "Sin stock" o "Últimas N unidades". Con stock suficiente no muestra nada.
export const POCAS_UNIDADES = 5;

export default function StockBadge({ stock, unidad = 'unidad' }) {
  if (typeof stock !== 'number') return null;
  if (stock <= 0) return <span className="stock-badge stock-agotado">Sin stock</span>;
  if (stock <= POCAS_UNIDADES) {
    // Productos por medida: "Quedan 3 kg"
    if (unidad !== 'unidad') {
      return <span className="stock-badge stock-pocas">Quedan {textoStock(stock, unidad)}</span>;
    }
    return (
      <span className="stock-badge stock-pocas">
        {stock === 1 ? '¡Última unidad!' : `Últimas ${stock} unidades`}
      </span>
    );
  }
  return null;
}
