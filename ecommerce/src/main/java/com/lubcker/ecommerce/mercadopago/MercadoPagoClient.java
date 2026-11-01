package com.lubcker.ecommerce.mercadopago;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

// Llamadas HTTP a la API de Mercado Pago. El access token se lee de la configuracion
// (secrets.properties) y nunca se loguea.
@Component
public class MercadoPagoClient {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoClient.class);

    private final String accessToken;
    private final RestClient http;

    public MercadoPagoClient(@Value("${app.mercadopago.access-token:}") String accessToken,
                             @Value("${app.mercadopago.base-url:https://api.mercadopago.com}") String baseUrl) {
        this.accessToken = accessToken == null ? "" : accessToken.trim();

        HttpClient cliente = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(cliente);
        fabrica.setReadTimeout(Duration.ofSeconds(15));

        this.http = RestClient.builder().baseUrl(baseUrl).requestFactory(fabrica).build();
    }

    public boolean configurado() {
        return !accessToken.isEmpty();
    }

    // POST /checkout/preferences -> devuelve la preferencia (con init_point, el link de pago)
    public Map<String, Object> crearPreferencia(Map<String, Object> cuerpo) {
        exigirConfigurado();
        try {
            return http.post()
                    .uri("/checkout/preferences")
                    .header("Authorization", "Bearer " + accessToken)
                    .body(cuerpo)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        } catch (RestClientResponseException e) {
            // Solo el codigo: el cuerpo del error puede traer datos del pedido
            log.error("Mercado Pago rechazo la preferencia: HTTP {}", e.getStatusCode().value());
            throw new MercadoPagoException("No se pudo iniciar el pago con Mercado Pago. Probá de nuevo en un momento.");
        } catch (RestClientException e) {
            log.error("No se pudo conectar con Mercado Pago: {}", e.getClass().getSimpleName());
            throw new MercadoPagoException("No se pudo conectar con Mercado Pago. Probá de nuevo en un momento.");
        }
    }

    // GET /v1/payments/{id} -> el pago real segun Mercado Pago, o null si no existe.
    // El id debe ser numerico: asi nunca se arma una ruta con texto de un tercero.
    public Map<String, Object> obtenerPago(String idPago) {
        exigirConfigurado();
        if (idPago == null || !idPago.matches("[0-9]{1,20}")) {
            return null;
        }
        try {
            return http.get()
                    .uri("/v1/payments/{id}", idPago)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                return null;
            }
            log.error("Mercado Pago respondio con error al consultar un pago: HTTP {}", e.getStatusCode().value());
            throw new MercadoPagoException("No se pudo consultar el pago en Mercado Pago. Probá de nuevo en un momento.");
        } catch (RestClientException e) {
            log.error("No se pudo conectar con Mercado Pago: {}", e.getClass().getSimpleName());
            throw new MercadoPagoException("No se pudo conectar con Mercado Pago. Probá de nuevo en un momento.");
        }
    }

    // GET /v1/payments/search?external_reference={idOrden} -> los pagos que Mercado Pago tiene de ese pedido
    // (el mas nuevo primero). Sirve para enterarse de un pago aunque no haya llegado el aviso (webhook).
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> buscarPagosPorReferencia(int idOrden) {
        exigirConfigurado();
        try {
            Map<String, Object> respuesta = http.get()
                    .uri("/v1/payments/search?external_reference={ref}&sort=date_created&criteria=desc&limit=10", idOrden)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
            Object resultados = respuesta == null ? null : respuesta.get("results");
            return resultados instanceof List<?> l ? (List<Map<String, Object>>) l : List.of();
        } catch (RestClientResponseException e) {
            log.error("Mercado Pago respondio con error al buscar pagos: HTTP {}", e.getStatusCode().value());
            throw new MercadoPagoException("No se pudo consultar el pago en Mercado Pago. Probá de nuevo en un momento.");
        } catch (RestClientException e) {
            log.error("No se pudo conectar con Mercado Pago: {}", e.getClass().getSimpleName());
            throw new MercadoPagoException("No se pudo conectar con Mercado Pago. Probá de nuevo en un momento.");
        }
    }

    private void exigirConfigurado() {
        if (!configurado()) {
            throw new MercadoPagoException("Los pagos online todavía no están configurados.");
        }
    }
}
