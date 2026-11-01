package com.lubcker.ecommerce.mercadopago;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.lubcker.ecommerce.model.Envio;
import com.lubcker.ecommerce.model.Orden;
import com.lubcker.ecommerce.model.Pago;
import com.lubcker.ecommerce.repository.Detalle_OrdenRepository;
import com.lubcker.ecommerce.repository.EnvioRepository;
import com.lubcker.ecommerce.repository.PagoRepository;
import com.lubcker.ecommerce.security.SeguridadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

@Tag(name = "Mercado Pago", description = "Verificación de pagos, nuevos links de pago y aviso (webhook) de Mercado Pago.")
@RestController
@RequestMapping("/api/mercadopago")
public class MercadoPagoController {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoController.class);

    private final MercadoPagoService servicio;
    private final SeguridadService seguridad;
    private final PagoRepository pagoRepository;
    private final EnvioRepository envioRepository;
    private final Detalle_OrdenRepository detalleOrdenRepository;
    private final String webhookSecret;

    public MercadoPagoController(MercadoPagoService servicio, SeguridadService seguridad, PagoRepository pagoRepository,
                                 EnvioRepository envioRepository, Detalle_OrdenRepository detalleOrdenRepository,
                                 @Value("${app.mercadopago.webhook-secret:}") String webhookSecret) {
        this.servicio = servicio;
        this.seguridad = seguridad;
        this.pagoRepository = pagoRepository;
        this.envioRepository = envioRepository;
        this.detalleOrdenRepository = detalleOrdenRepository;
        this.webhookSecret = webhookSecret == null ? "" : webhookSecret.trim();
    }

    // POST /api/mercadopago/confirmar?payment_id=123 -> el cliente vuelve de Mercado Pago y se
    // verifica su pago. Sirve aunque el webhook no pueda llegar (ej: desarrollo en localhost).
    @Operation(summary = "Verificar un pago contra la API de Mercado Pago al volver de pagar", description = "Acceso: **Sesión** (cliente o admin). Un cliente solo puede ver y modificar sus propios datos; el admin accede a todo.")
    @PostMapping("/confirmar")
    public ResponseEntity<?> confirmar(@RequestParam("payment_id") String paymentId) {
        int idCliente = seguridad.actual().map(u -> u.idCliente()).orElse(-1);
        ResultadoPago r = servicio.sincronizar(paymentId, idCliente);
        if (r == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("mensaje", "No encontramos ese pago."));
        }
        return ResponseEntity.ok(Map.of("idOrden", r.idOrden(), "estado", r.estadoMp(), "confirmado", r.confirmado()));
    }

    // POST /api/mercadopago/orden/{id}/sincronizar -> consulta a Mercado Pago si el pedido ya se pago
    @Operation(summary = "Consultar a Mercado Pago si un pedido pendiente ya fue pagado", description = "Acceso: **Sesión** (cliente o admin). Un cliente solo puede ver y modificar sus propios datos; el admin accede a todo.")
    @PostMapping("/orden/{idOrden}/sincronizar")
    public ResponseEntity<?> sincronizar(@PathVariable int idOrden) {
        // El admin puede sincronizar cualquier pedido; un cliente, solo los suyos
        Integer idCliente = seguridad.esAdmin() ? null : seguridad.actual().map(u -> u.idCliente()).orElse(-1);
        ResultadoPago r = servicio.sincronizarPedido(idOrden, idCliente);
        if (r == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(Map.of("idOrden", r.idOrden(), "estado", r.estadoMp(), "confirmado", r.confirmado()));
    }

    // POST /api/mercadopago/orden/{id}/link -> nuevo link para pagar un pedido pendiente
    @Operation(summary = "Generar un link nuevo para pagar un pedido pendiente (\"Pagar ahora\")", description = "Acceso: **Sesión** (cliente o admin). Un cliente solo puede ver y modificar sus propios datos; el admin accede a todo.")
    @PostMapping("/orden/{idOrden}/link")
    public ResponseEntity<?> link(@PathVariable int idOrden) {
        List<Pago> pagos = pagoRepository.findByOrdenIdOrden(idOrden);
        if (pagos.isEmpty()) return ResponseEntity.notFound().build();
        Pago pago = pagos.get(0);
        Orden orden = pago.getOrden();
        if (orden.getCliente() == null || !seguridad.puedeAccederCliente(orden.getCliente().getIdCliente())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("mensaje", "No tenés permiso para hacer esto."));
        }
        Envio envio = envioRepository.findByOrdenIdOrden(idOrden).orElse(null);
        if (pago.isEstado()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("mensaje", "Este pedido ya está pagado."));
        }
        if (envio != null && "cancelado".equalsIgnoreCase(envio.getEstado())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("mensaje", "Este pedido está cancelado."));
        }
        String url = servicio.crearLinkDePago(orden, detalleOrdenRepository.findByOrdenIdOrden(idOrden));
        return ResponseEntity.ok(Map.of("urlPago", url));
    }

    // POST /api/mercadopago/webhook -> aviso de Mercado Pago (publico). No se confia en lo que
    // trae: se consulta el pago a Mercado Pago con nuestro token.
    @Operation(summary = "Aviso de Mercado Pago (verifica la firma y consulta el pago real)", description = "Acceso: **Público** (no requiere sesión).")
    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(@RequestParam Map<String, String> params,
                                        @RequestHeader(value = "x-signature", required = false) String firma,
                                        @RequestHeader(value = "x-request-id", required = false) String requestId,
                                        @RequestBody(required = false) Map<String, Object> cuerpo) {
        String tipo = params.getOrDefault("type", params.getOrDefault("topic", ""));
        if (cuerpo != null && cuerpo.get("type") instanceof String t && tipo.isEmpty()) tipo = t;
        String id = params.getOrDefault("data.id", params.getOrDefault("id", ""));
        if (id.isEmpty() && cuerpo != null && cuerpo.get("data") instanceof Map<?, ?> d && d.get("id") != null) {
            id = String.valueOf(d.get("id"));
        }

        if (!webhookSecret.isEmpty() && !firmaValida(firma, requestId, id)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (!"payment".equals(tipo) || id.isEmpty()) {
            return ResponseEntity.ok().build(); // otros tipos de aviso: se ignoran
        }
        try {
            servicio.sincronizar(id, null);
        } catch (MercadoPagoException e) {
            // 500 para que Mercado Pago reintente el aviso mas tarde
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        return ResponseEntity.ok().build();
    }

    // Firma: HMAC-SHA256 de "id:{data.id};request-id:{x-request-id};ts:{ts};" con el secreto del webhook
    private boolean firmaValida(String firma, String requestId, String idPago) {
        if (firma == null) return false;
        String ts = null, v1 = null;
        for (String parte : firma.split(",")) {
            String[] kv = parte.trim().split("=", 2);
            if (kv.length == 2 && kv[0].equals("ts")) ts = kv[1];
            if (kv.length == 2 && kv[0].equals("v1")) v1 = kv[1];
        }
        if (ts == null || v1 == null) return false;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String plantilla = "id:" + idPago.toLowerCase() + ";request-id:" + (requestId == null ? "" : requestId) + ";ts:" + ts + ";";
            String esperado = HexFormat.of().formatHex(mac.doFinal(plantilla.getBytes(StandardCharsets.UTF_8)));
            return MessageDigest.isEqual(esperado.getBytes(StandardCharsets.UTF_8), v1.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("No se pudo verificar la firma del webhook: {}", e.getClass().getSimpleName());
            return false;
        }
    }

    @ExceptionHandler(MercadoPagoException.class)
    public ResponseEntity<Map<String, String>> manejarError(MercadoPagoException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("mensaje", e.getMessage()));
    }
}
