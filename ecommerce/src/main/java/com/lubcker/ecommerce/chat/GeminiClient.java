package com.lubcker.ecommerce.chat;

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
import java.util.Map;

// Llamada HTTP a la API de Gemini (POST {base-url}/models/{modelo}:generateContent).
// La clave se lee de la configuracion (nunca se escribe en el codigo) y no se loguea.
@Component
public class GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);

    private final String apiKey;
    private final RestClient http;

    public GeminiClient(@Value("${app.chat.api-key:}") String apiKey,
                        @Value("${app.chat.base-url:https://generativelanguage.googleapis.com/v1beta}") String baseUrl) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();

        HttpClient cliente = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(cliente);
        fabrica.setReadTimeout(Duration.ofSeconds(45));

        this.http = RestClient.builder().baseUrl(baseUrl).requestFactory(fabrica).build();
    }

    private static void pausar(long milisegundos) {
        try {
            Thread.sleep(milisegundos);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public boolean configurado() {
        return !apiKey.isEmpty();
    }

    // Cuantas veces se intenta si Gemini responde 503 (pico de demanda pasajero)
    private static final int INTENTOS = 3;

    public Map<String, Object> generarContenido(String modelo, Map<String, Object> cuerpo) {
        if (!configurado()) {
            throw new ChatNoDisponibleException("El asistente todavía no está configurado. Probá más tarde.");
        }
        try {
            for (int intento = 1; ; intento++) {
                try {
                    return http.post()
                            .uri("/models/{modelo}:generateContent", modelo)
                            // La clave va en un header (no en la URL) para que no quede en logs ni historiales
                            .header("x-goog-api-key", apiKey)
                            .body(cuerpo)
                            .retrieve()
                            .body(new ParameterizedTypeReference<Map<String, Object>>() {});
                } catch (RestClientResponseException e) {
                    if (e.getStatusCode().value() != 503 || intento >= INTENTOS) throw e;
                    log.warn("Gemini respondio 503 (intento {} de {}), se reintenta", intento, INTENTOS);
                    pausar(1000L * intento);
                }
            }
        } catch (RestClientResponseException e) {
            // Solo el codigo: el cuerpo del error puede incluir fragmentos de la peticion
            int codigo = e.getStatusCode().value();
            log.error("Gemini respondio con error HTTP {}", codigo);
            // Gemini responde 400 cuando la clave no es valida y 403 cuando no tiene permiso
            if (codigo == 400 || codigo == 401 || codigo == 403) {
                throw new ChatNoDisponibleException("El asistente no está disponible por un problema de configuración.");
            }
            if (codigo == 429) {
                throw new ChatNoDisponibleException("El asistente está con mucha demanda. Probá de nuevo en un momento.");
            }
            throw new ChatNoDisponibleException("El asistente no pudo responder. Probá de nuevo en un momento.");
        } catch (RestClientException e) {
            log.error("No se pudo conectar con Gemini: {}", e.getClass().getSimpleName());
            throw new ChatNoDisponibleException("No pude conectarme con el asistente. Probá de nuevo en un momento.");
        }
    }
}
