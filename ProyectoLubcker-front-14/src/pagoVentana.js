// Mercado Pago se abre en una pestaña nueva para no sacar al cliente de la tienda.
//
// El navegador solo deja abrir pestañas nuevas dentro del clic del usuario, y el link de pago llega
// despues de pedirlo al servidor. Por eso la pestaña se abre en blanco al hacer clic
// (abrirVentanaDePago) y se le carga el link cuando llega (irAlPago).

export function abrirVentanaDePago() {
  const ventana = window.open('', '_blank');
  if (ventana) {
    try {
      ventana.document.title = 'Mercado Pago';
      ventana.document.body.style.cssText = 'font-family: Arial, sans-serif; padding: 32px; color: #334155;';
      ventana.document.body.textContent = 'Abriendo Mercado Pago...';
    } catch {
      // la pestaña igual se navega despues; el texto de espera es solo un detalle
    }
  }
  return ventana;
}

// Carga el link en la pestaña abierta. Si el navegador bloqueó la pestaña nueva, se usa la actual.
export function irAlPago(ventana, url) {
  if (ventana && !ventana.closed) {
    ventana.opener = null; // la pagina de Mercado Pago no tiene por que controlar la tienda
    ventana.location.href = url;
    return true;
  }
  window.location.assign(url);
  return false;
}

// Si algo falla antes de tener el link, se cierra la pestaña en blanco
export function cerrarVentanaDePago(ventana) {
  if (ventana && !ventana.closed) ventana.close();
}
