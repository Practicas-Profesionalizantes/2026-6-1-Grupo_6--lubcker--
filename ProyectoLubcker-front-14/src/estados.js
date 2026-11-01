// Estados de envío que puede asignar el administrador (el checkout crea el envío como "Pendiente").
export const ESTADOS_ENVIO = ['Pendiente', 'En preparación', 'En camino', 'Entregado', 'Cancelado'];

// Clase CSS del "badge" según el estado del envío (ver App.css)
export function claseEstadoEnvio(estado) {
  switch ((estado || '').toLowerCase()) {
    case 'entregado':
      return 'estado-ok';
    case 'enviado':
    case 'en camino':
    case 'en preparación':
    case 'en preparacion':
      return 'estado-info';
    case 'cancelado':
      return 'estado-error';
    default:
      return 'estado-pendiente';
  }
}

export const claseEstadoPago = (confirmado) => (confirmado ? 'estado-ok' : 'estado-pendiente');
export const textoEstadoPago = (confirmado) => (confirmado ? 'Confirmado' : 'Pendiente');
