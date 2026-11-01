package com.lubcker.ecommerce.mercadopago;

import com.lubcker.ecommerce.model.Detalle_Orden;
import com.lubcker.ecommerce.model.Envio;
import com.lubcker.ecommerce.model.Orden;
import com.lubcker.ecommerce.model.Pago;
import com.lubcker.ecommerce.repository.EnvioRepository;
import com.lubcker.ecommerce.repository.PagoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

// Cobro con Checkout Pro: se crea una preferencia (link de pago) y el pedido se marca como
// pagado SOLO despues de consultar el pago real a Mercado Pago con nuestro access token.
// Nada de lo que mande el navegador o un tercero alcanza para aprobar un pago.
@Service
public class MercadoPagoService {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoService.class);

    private final MercadoPagoClient cliente;
    private final PagoRepository pagoRepository;
    private final EnvioRepository envioRepository;
    private final OrdenCancelacionService cancelacion;
    private final String frontendUrl;
    private final String webhookUrl;
    private final int horasParaPagar;

    public MercadoPagoService(MercadoPagoClient cliente, PagoRepository pagoRepository, EnvioRepository envioRepository,
                              OrdenCancelacionService cancelacion,
                              @Value("${app.mercadopago.frontend-url:http://localhost:3000}") String frontendUrl,
                              @Value("${app.mercadopago.webhook-url:}") String webhookUrl,
                              @Value("${app.mercadopago.horas-para-pagar:24}") int horasParaPagar) {
        this.cliente = cliente;
        this.pagoRepository = pagoRepository;
        this.envioRepository = envioRepository;
        this.cancelacion = cancelacion;
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");
        this.webhookUrl = webhookUrl == null ? "" : webhookUrl.trim();
        this.horasParaPagar = horasParaPagar;
    }

    public boolean disponible() {
        return cliente.configurado();
    }

    // Crea la preferencia de la orden y devuelve el link de pago. Los precios salen de los
    // detalles guardados en la base (los mismos que calculo el checkout).
    public String crearLinkDePago(Orden orden, List<Detalle_Orden> detalles) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (Detalle_Orden d : detalles) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", String.valueOf(d.getProducto().getIdProducto()));
            item.put("title", d.getTamano() > 1
                    ? d.getProducto().getNombre() + " (" + d.getTamano() + " " + d.getProducto().getUnidadMedida() + ")"
                    : d.getProducto().getNombre());
            item.put("quantity", d.getCantidad());
            item.put("unit_price", d.getPrecioUnitario());
            item.put("currency_id", "ARS");
            items.add(item);
        }

        String retorno = frontendUrl + "/pago/resultado";
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("items", items);
        cuerpo.put("external_reference", String.valueOf(orden.getIdOrden()));
        cuerpo.put("back_urls", Map.of("success", retorno, "pending", retorno, "failure", retorno));
        // auto_return solo funciona con URLs https
        if (retorno.startsWith("https://")) cuerpo.put("auto_return", "approved");
        if (!webhookUrl.isEmpty()) cuerpo.put("notification_url", webhookUrl);
        cuerpo.put("statement_descriptor", "LUBCKER");

        Map<String, Object> preferencia = cliente.crearPreferencia(cuerpo);
        Object link = preferencia == null ? null : preferencia.get("init_point");
        if (!(link instanceof String url) || url.isBlank()) {
            throw new MercadoPagoException("No se pudo iniciar el pago con Mercado Pago. Probá de nuevo en un momento.");
        }
        return url;
    }

    // Consulta el pago a Mercado Pago y, si esta aprobado y coincide con el pedido, lo confirma.
    // idClienteRestringido: si viene (llamada del usuario), el pedido tiene que ser de ese cliente.
    // Devuelve null si el pago no existe o no corresponde a un pedido de esta tienda.
    @Transactional
    public ResultadoPago sincronizar(String idPagoMp, Integer idClienteRestringido) {
        Map<String, Object> mp = cliente.obtenerPago(idPagoMp);
        if (mp == null) return null;

        int idOrden;
        try {
            idOrden = Integer.parseInt(String.valueOf(mp.get("external_reference")));
        } catch (NumberFormatException e) {
            return null;
        }

        List<Pago> pagos = pagoRepository.findByOrdenIdOrden(idOrden);
        if (pagos.isEmpty()) return null;
        Pago pago = pagos.get(0);

        if (idClienteRestringido != null && (pago.getOrden().getCliente() == null
                || pago.getOrden().getCliente().getIdCliente() != idClienteRestringido)) {
            throw new AccessDeniedException("El pago no es de este usuario");
        }

        String estado = String.valueOf(mp.get("status"));
        if (!"approved".equals(estado)) {
            return new ResultadoPago(idOrden, estado, pago.isEstado());
        }

        // Monto y moneda tienen que coincidir con el pedido
        BigDecimal pagado = mp.get("transaction_amount") instanceof Number n ? new BigDecimal(n.toString()) : null;
        boolean montoOk = pagado != null && pagado.compareTo(BigDecimal.valueOf(pago.getMonto())) == 0;
        boolean monedaOk = "ARS".equals(mp.get("currency_id"));
        if (!montoOk || !monedaOk) {
            log.warn("Pago {} aprobado pero no coincide con el pedido #{}: se ignora", idPagoMp, idOrden);
            return new ResultadoPago(idOrden, estado, pago.isEstado());
        }

        if (!pago.isEstado()) {
            Envio envio = envioRepository.findByOrdenIdOrden(idOrden).orElse(null);
            if (envio != null && "cancelado".equalsIgnoreCase(envio.getEstado())) {
                // Pago que llego despues de cancelar el pedido: hay que resolverlo a mano (devolucion)
                log.warn("Pago {} aprobado para el pedido #{} que ya estaba cancelado: revisar", idPagoMp, idOrden);
                return new ResultadoPago(idOrden, estado, false);
            }
            pago.setEstado(true);
            pago.setMpPaymentId(idPagoMp);
            pagoRepository.save(pago);
        }
        return new ResultadoPago(idOrden, estado, true);
    }

    // Busca en Mercado Pago los pagos del pedido y confirma el aprobado. Sirve cuando no llego el aviso
    // (webhook) ni el cliente volvio desde Mercado Pago (ej: desarrollo en localhost, o cerro la pestana).
    // Devuelve null si el pedido no existe.
    @Transactional
    public ResultadoPago sincronizarPedido(int idOrden, Integer idClienteRestringido) {
        List<Pago> pagos = pagoRepository.findByOrdenIdOrden(idOrden);
        if (pagos.isEmpty()) return null;
        Pago pago = pagos.get(0);

        if (idClienteRestringido != null && (pago.getOrden().getCliente() == null
                || pago.getOrden().getCliente().getIdCliente() != idClienteRestringido)) {
            throw new AccessDeniedException("El pedido no es de este usuario");
        }
        if (pago.isEstado()) return new ResultadoPago(idOrden, "approved", true);

        for (Map<String, Object> mp : cliente.buscarPagosPorReferencia(idOrden)) {
            if ("approved".equals(mp.get("status")) && mp.get("id") != null) {
                ResultadoPago r = sincronizar(String.valueOf(mp.get("id")), idClienteRestringido);
                if (r != null && r.confirmado()) return r;
            }
        }
        return new ResultadoPago(idOrden, "pending", false);
    }

    // Pedidos que nunca se pagaron: pasadas las horas permitidas se cancelan y vuelve el stock.
    @Scheduled(fixedDelay = 3_600_000, initialDelay = 120_000)
    @Transactional
    public void cancelarSinPagar() {
        Date limite = new Date(System.currentTimeMillis() - horasParaPagar * 3_600_000L);
        for (Envio envio : envioRepository.findPendientesSinPagarAntesDe(limite)) {
            int idOrden = envio.getOrden().getIdOrden();
            cancelacion.devolverStock(idOrden);
            envio.setEstado("Cancelado");
            envioRepository.save(envio);
            log.info("Pedido #{} cancelado por falta de pago (stock devuelto)", idOrden);
        }
    }
}
