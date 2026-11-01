// Entregas a cargo del dueño: textos de fecha/franja/mensaje y aviso por WhatsApp.

// "2026-11-04" -> "martes 4 de noviembre" (se arma con la fecha local: new Date("2026-11-04") la correría un día)
export function formatearFechaEntrega(iso) {
  if (!iso) return '';
  const [anio, mes, dia] = iso.split('-').map(Number);
  if (!anio || !mes || !dia) return '';
  return new Date(anio, mes - 1, dia).toLocaleDateString('es-AR', { weekday: 'long', day: 'numeric', month: 'long' });
}

// Texto corto para el cliente: "Martes 4 de noviembre, de 14 a 16 hs"
export function textoEntrega(envio) {
  if (!envio?.fechaEntrega) return '';
  const fecha = formatearFechaEntrega(envio.fechaEntrega);
  const texto = fecha.charAt(0).toUpperCase() + fecha.slice(1);
  return envio.franjaHoraria ? `${texto}, ${envio.franjaHoraria}` : texto;
}

// Teléfono como lo escribió la persona -> número para wa.me (Argentina: 549 + código de área + número).
// Acepta "11 4430-6194", "011 15 4430-6194", "+54 9 11 4430-6194", etc. Devuelve '' si no hay dígitos.
export function numeroWhatsApp(telefono) {
  let d = String(telefono || '').replace(/\D/g, '').replace(/^00/, '');
  if (!d) return '';
  if (d.startsWith('54')) {
    return d.startsWith('549') ? d : `549${d.slice(2)}`;
  }
  d = d.replace(/^0/, '');
  // Se saca el "15" de los celulares (código de área + 15 + número = 12 dígitos)
  if (d.length === 12) {
    for (const i of [2, 3, 4]) {
      if (d.slice(i, i + 2) === '15') {
        d = d.slice(0, i) + d.slice(i + 2);
        break;
      }
    }
  }
  return `549${d}`;
}

// Mensaje prearmado para avisarle al cliente cuándo llega su pedido
export function mensajeEntrega(orden, envio) {
  const nombre = (orden.nombreCliente || '').split(' ')[0] || '';
  const saludo = `Hola${nombre ? ` ${nombre}` : ''}, te escribimos de Lubcker por tu pedido #${orden.idOrden}.`;
  if (!envio?.fechaEntrega) {
    return `${saludo} Estamos preparándolo; en cuanto tengamos el día de entrega te avisamos.`;
  }
  const franja = envio.franjaHoraria ? `, ${envio.franjaHoraria}` : '';
  const nota = envio.notaCliente ? ` ${envio.notaCliente}` : '';
  return `${saludo} Te lo vamos a entregar el ${formatearFechaEntrega(envio.fechaEntrega)}${franja}.${nota}`;
}

export function linkWhatsApp(telefono, mensaje) {
  const numero = numeroWhatsApp(telefono);
  return numero ? `https://wa.me/${numero}?text=${encodeURIComponent(mensaje)}` : '';
}
