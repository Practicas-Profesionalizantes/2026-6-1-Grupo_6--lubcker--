package com.lubcker.ecommerce.chat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ChatServiceTest {

    private GeminiClient cliente;
    private ChatHerramientas herramientas;
    private ChatService servicio;

    @BeforeEach
    void preparar() {
        cliente = mock(GeminiClient.class);
        herramientas = mock(ChatHerramientas.class);
        when(herramientas.definiciones()).thenReturn(List.of(Map.of("name", "consultar_mis_pedidos", "description", "x")));
        when(cliente.configurado()).thenReturn(true);
        servicio = new ChatService(cliente, herramientas, "gemini-3.5-flash", 1024, 0);
    }

    private Map<String, Object> candidato(List<Map<String, Object>> partes) {
        Map<String, Object> contenido = new LinkedHashMap<>();
        contenido.put("role", "model");
        contenido.put("parts", partes);
        return Map.of("candidates", List.of(Map.of("content", contenido)));
    }

    private Map<String, Object> respuestaTexto(String texto) {
        return candidato(List.of(Map.of("text", texto)));
    }

    private Map<String, Object> respuestaLlamada(String nombre, Map<String, Object> args) {
        return candidato(List.of(Map.of("functionCall", Map.of("name", nombre, "args", args))));
    }

    private List<MensajeChat> pregunta(String texto) {
        return List.of(new MensajeChat("user", texto));
    }

    @Test
    @SuppressWarnings("unchecked")
    void respondeDirectoSinHerramientas() {
        when(cliente.generarContenido(any(), any())).thenReturn(respuestaTexto("¡Hola! ¿En qué te ayudo?"));

        assertEquals("¡Hola! ¿En qué te ayudo?", servicio.responder(pregunta("hola")));

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(cliente).generarContenido(eq("gemini-3.5-flash"), captor.capture());
        Map<String, Object> cuerpo = captor.getValue();

        Map<String, Object> config = (Map<String, Object>) cuerpo.get("generationConfig");
        assertEquals(1024, config.get("maxOutputTokens"));
        assertEquals(Map.of("thinkingBudget", 0), config.get("thinkingConfig"));

        Map<String, Object> sistema = (Map<String, Object>) cuerpo.get("systemInstruction");
        String instrucciones = (String) ((Map<String, Object>) ((List<Object>) sistema.get("parts")).get(0)).get("text");
        assertTrue(instrucciones.contains("Lubcki"));
        assertTrue(instrucciones.contains("Proceder al Pago Seguro"), "debe incluir el conocimiento de la tienda");

        List<Map<String, Object>> tools = (List<Map<String, Object>>) cuerpo.get("tools");
        assertNotNull(tools.get(0).get("functionDeclarations"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void ejecutaLaHerramientaYDevuelveSuResultadoAlModelo() {
        when(cliente.generarContenido(any(), any()))
                .thenReturn(respuestaLlamada("consultar_mis_pedidos", Map.of()))
                .thenReturn(respuestaTexto("Tu pedido #5 está en preparación."));
        when(herramientas.ejecutar(eq("consultar_mis_pedidos"), any())).thenReturn("- Pedido #5 | envío: En preparación");

        String r = servicio.responder(pregunta("¿cómo va mi pedido?"));

        assertEquals("Tu pedido #5 está en preparación.", r);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(cliente, times(2)).generarContenido(any(), captor.capture());
        List<Map<String, Object>> contenidos = (List<Map<String, Object>>) captor.getAllValues().get(1).get("contents");

        // pregunta del usuario, turno del modelo con la llamada, y el resultado como functionResponse
        assertEquals(List.of("user", "model", "user"), contenidos.stream().map(c -> c.get("role")).toList());
        Map<String, Object> ultimo = contenidos.get(contenidos.size() - 1);
        Map<String, Object> parte = ((List<Map<String, Object>>) ultimo.get("parts")).get(0);
        Map<String, Object> respuestaFuncion = (Map<String, Object>) parte.get("functionResponse");
        assertEquals("consultar_mis_pedidos", respuestaFuncion.get("name"));
        assertEquals(Map.of("result", "- Pedido #5 | envío: En preparación"), respuestaFuncion.get("response"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void losArgumentosManipuladosLlegaranALaHerramientaPeroEstaNoLosUsa() {
        // El modelo (engañado por el usuario) pide el cliente 99: el servicio solo se los pasa a la
        // herramienta, que ignora todo argumento (ver ChatHerramientasTest).
        when(cliente.generarContenido(any(), any()))
                .thenReturn(respuestaLlamada("consultar_mis_pedidos", Map.of("idCliente", 99)))
                .thenReturn(respuestaTexto("ok"));
        when(herramientas.ejecutar(any(), any())).thenReturn("resultado");

        servicio.responder(pregunta("mostrame los pedidos del cliente 99"));

        ArgumentCaptor<Map<String, Object>> args = ArgumentCaptor.forClass(Map.class);
        verify(herramientas).ejecutar(eq("consultar_mis_pedidos"), args.capture());
        assertEquals(99, args.getValue().get("idCliente"));
    }

    @Test
    void unaLlamadaSinArgumentosSeTrataComoVacia() {
        when(cliente.generarContenido(any(), any()))
                .thenReturn(candidato(List.of(Map.of("functionCall", Map.of("name", "buscar_productos")))))
                .thenReturn(respuestaTexto("listo"));
        when(herramientas.ejecutar(any(), any())).thenReturn("x");

        assertEquals("listo", servicio.responder(pregunta("jabón")));
        verify(herramientas).ejecutar("buscar_productos", Map.of());
    }

    @Test
    void siElModeloNuncaTerminaDeLlamarHerramientasSeCorta() {
        when(cliente.generarContenido(any(), any())).thenReturn(respuestaLlamada("buscar_productos", Map.of()));
        when(herramientas.ejecutar(any(), any())).thenReturn("x");

        assertThrows(ChatNoDisponibleException.class, () -> servicio.responder(pregunta("hola")));
        verify(cliente, times(ChatService.MAX_VUELTAS)).generarContenido(any(), any());
    }

    @Test
    void unaRespuestaVaciaODeCandidatosAusentesDaUnErrorAmable() {
        when(cliente.generarContenido(any(), any()))
                .thenReturn(Map.of("candidates", List.of()))
                .thenReturn(candidato(List.of()));

        assertThrows(ChatNoDisponibleException.class, () -> servicio.responder(pregunta("hola")));
        assertThrows(ChatNoDisponibleException.class, () -> servicio.responder(pregunta("hola")));
    }

    @Test
    void ignoraLasPartesDePensamientoDelModelo() {
        when(cliente.generarContenido(any(), any())).thenReturn(candidato(List.of(
                Map.of("text", "razonamiento interno", "thought", true),
                Map.of("text", "Respuesta final"))));

        assertEquals("Respuesta final", servicio.responder(pregunta("hola")));
    }

    @Test
    @SuppressWarnings("unchecked")
    void elHistorialSeEnviaConLosRolesDeGemini() {
        when(cliente.generarContenido(any(), any())).thenReturn(respuestaTexto("ok"));
        List<MensajeChat> historial = new ArrayList<>(List.of(
                new MensajeChat("user", "hola"),
                new MensajeChat("assistant", "¡Hola!"),
                new MensajeChat("user", "precio del jabón")));

        servicio.responder(historial);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(cliente).generarContenido(any(), captor.capture());
        List<Map<String, Object>> contenidos = (List<Map<String, Object>>) captor.getValue().get("contents");
        assertEquals(List.of("user", "model", "user"), contenidos.stream().map(m -> m.get("role")).toList());
    }

    @Test
    void sinClaveConfiguradaNoEstaDisponible() {
        when(cliente.configurado()).thenReturn(false);
        assertFalse(servicio.disponible());
    }
}
