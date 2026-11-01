package com.lubcker.ecommerce.chat;

import com.lubcker.ecommerce.model.*;
import com.lubcker.ecommerce.repository.*;
import com.lubcker.ecommerce.security.SeguridadService;
import com.lubcker.ecommerce.security.UsuarioAutenticado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

class ChatHerramientasTest {

    private SeguridadService seguridad;
    private ProductoRepository productos;
    private OrdenRepository ordenes;
    private Detalle_OrdenRepository detalles;
    private PagoRepository pagos;
    private EnvioRepository envios;
    private Ficha_TecnicaRepository fichas;
    private ChatHerramientas herramientas;

    @BeforeEach
    void preparar() {
        seguridad = mock(SeguridadService.class);
        productos = mock(ProductoRepository.class);
        ordenes = mock(OrdenRepository.class);
        detalles = mock(Detalle_OrdenRepository.class);
        pagos = mock(PagoRepository.class);
        envios = mock(EnvioRepository.class);
        fichas = mock(Ficha_TecnicaRepository.class);
        when(fichas.findByProductoIdProducto(anyInt())).thenReturn(Optional.empty());
        herramientas = new ChatHerramientas(seguridad, productos, mock(CategoriaRepository.class), fichas,
                ordenes, detalles, pagos, envios);
    }

    private Producto producto(int id, String nombre, int precio, int stock, String categoria) {
        Producto p = new Producto();
        p.setIdProducto(id);
        p.setNombre(nombre);
        p.setPrecio(precio);
        p.setStock(stock);
        Categoria c = new Categoria();
        c.setNombre(categoria);
        p.setCategoria(c);
        return p;
    }

    // Un pedido del cliente 7 con su pago y envio
    private void pedidoDelCliente7() {
        Orden orden = new Orden();
        orden.setIdOrden(55);
        orden.setFecha(new Date());
        when(ordenes.findByClienteIdClienteOrderByIdOrdenDesc(7)).thenReturn(List.of(orden));

        Detalle_Orden d = new Detalle_Orden();
        d.setProducto(producto(1, "1DEDO", 20000, 10, "Limpieza"));
        d.setCantidad(2);
        d.setSubtotal(40000);
        when(detalles.findByOrdenIdOrden(55)).thenReturn(List.of(d));

        Pago pago = new Pago();
        pago.setOrden(orden);
        pago.setEstado(true);
        MetodoPago mp = new MetodoPago();
        mp.setNombre("Transferencia bancaria");
        pago.setMetodoPago(mp);
        when(pagos.findByOrdenClienteIdCliente(7)).thenReturn(List.of(pago));

        Envio envio = new Envio();
        envio.setOrden(orden);
        envio.setEstado("En preparación");
        when(envios.findByOrdenClienteIdCliente(7)).thenReturn(List.of(envio));
    }

    // ------------------------------------------------------------ SEGURIDAD DE LOS PEDIDOS

    @Test
    void sinSesionNoConsultaNingunPedido() {
        when(seguridad.actual()).thenReturn(Optional.empty());

        String resultado = herramientas.ejecutar("consultar_mis_pedidos", Map.of());

        assertTrue(resultado.startsWith("SIN_SESION"));
        verifyNoInteractions(ordenes, pagos, envios, detalles);
    }

    @Test
    void conSesionSoloSeConsultaElClienteDelToken() {
        when(seguridad.actual()).thenReturn(Optional.of(new UsuarioAutenticado(3, 7, "CLIENTE")));
        pedidoDelCliente7();

        String resultado = herramientas.ejecutar("consultar_mis_pedidos", Map.of());

        assertTrue(resultado.contains("Pedido #55"));
        assertTrue(resultado.contains("En preparación"));
        assertTrue(resultado.contains("Confirmado (Transferencia bancaria)"));
        verify(ordenes).findByClienteIdClienteOrderByIdOrdenDesc(7);
        verify(pagos).findByOrdenClienteIdCliente(7);
        verify(envios).findByOrdenClienteIdCliente(7);
    }

    @Test
    void unModeloManipuladoNoPuedeElegirOtroCliente() {
        // El usuario logueado es el cliente 7, pero el modelo (engañado) intenta pedir el cliente 99
        when(seguridad.actual()).thenReturn(Optional.of(new UsuarioAutenticado(3, 7, "CLIENTE")));
        pedidoDelCliente7();

        herramientas.ejecutar("consultar_mis_pedidos", Map.of(
                "idCliente", 99, "id_cliente", 99, "cliente", "99", "idUsuario", 4, "email", "otro@correo.com"));

        verify(ordenes).findByClienteIdClienteOrderByIdOrdenDesc(7);
        verify(ordenes, never()).findByClienteIdClienteOrderByIdOrdenDesc(99);
        verify(pagos, never()).findByOrdenClienteIdCliente(99);
        verify(envios, never()).findByOrdenClienteIdCliente(99);
        verifyNoMoreInteractions(ordenes);
    }

    @Test
    void laHerramientaDePedidosNoDeclaraNingunParametro() {
        // Si el modelo no tiene ningun argumento para completar, no hay nada que manipular
        Map<String, Object> def = herramientas.definiciones().stream()
                .filter(h -> "consultar_mis_pedidos".equals(h.get("name")))
                .findFirst().orElseThrow();

        assertNull(def.get("parameters"), "la herramienta no declara ningun parametro");
    }

    @Test
    void unPedidoDeOtroClienteNuncaApareceEnLaRespuesta() {
        when(seguridad.actual()).thenReturn(Optional.of(new UsuarioAutenticado(3, 7, "CLIENTE")));
        pedidoDelCliente7();
        // Datos de otro cliente que existen en la base pero no deben salir
        Orden ajena = new Orden();
        ajena.setIdOrden(999);
        lenient().when(ordenes.findByClienteIdClienteOrderByIdOrdenDesc(8)).thenReturn(List.of(ajena));

        String resultado = herramientas.ejecutar("consultar_mis_pedidos", Map.of("idCliente", 8));

        assertFalse(resultado.contains("999"));
        assertTrue(resultado.contains("#55"));
    }

    @Test
    void sinPedidosLoDiceSinInventarNada() {
        when(seguridad.actual()).thenReturn(Optional.of(new UsuarioAutenticado(3, 7, "CLIENTE")));
        when(ordenes.findByClienteIdClienteOrderByIdOrdenDesc(7)).thenReturn(List.of());

        assertTrue(herramientas.ejecutar("consultar_mis_pedidos", Map.of()).contains("todavía no tiene pedidos pagados"));
    }

    @Test
    void losPedidosSinPagarNoSeMencionan() {
        when(seguridad.actual()).thenReturn(Optional.of(new UsuarioAutenticado(3, 7, "CLIENTE")));
        Orden pagada = new Orden();
        pagada.setIdOrden(55);
        Orden sinPagar = new Orden();
        sinPagar.setIdOrden(56);
        when(ordenes.findByClienteIdClienteOrderByIdOrdenDesc(7)).thenReturn(List.of(sinPagar, pagada));
        Pago pagoOk = new Pago();
        pagoOk.setOrden(pagada);
        pagoOk.setEstado(true);
        Pago pagoPendiente = new Pago();
        pagoPendiente.setOrden(sinPagar);
        pagoPendiente.setEstado(false);
        when(pagos.findByOrdenClienteIdCliente(7)).thenReturn(List.of(pagoPendiente, pagoOk));

        String resultado = herramientas.ejecutar("consultar_mis_pedidos", Map.of());

        assertTrue(resultado.contains("#55"));
        assertFalse(resultado.contains("#56"));
        assertFalse(resultado.contains("Pendiente"));
    }

    @Test
    void siTodosLosPedidosEstanSinPagarDiceQueNoTienePedidosPagados() {
        when(seguridad.actual()).thenReturn(Optional.of(new UsuarioAutenticado(3, 7, "CLIENTE")));
        Orden sinPagar = new Orden();
        sinPagar.setIdOrden(56);
        when(ordenes.findByClienteIdClienteOrderByIdOrdenDesc(7)).thenReturn(List.of(sinPagar));
        Pago pago = new Pago();
        pago.setOrden(sinPagar);
        pago.setEstado(false);
        when(pagos.findByOrdenClienteIdCliente(7)).thenReturn(List.of(pago));

        String resultado = herramientas.ejecutar("consultar_mis_pedidos", Map.of());

        assertFalse(resultado.contains("#56"));
        assertTrue(resultado.contains("todavía no tiene pedidos pagados"));
    }

    @Test
    void laRespuestaDePedidosNoIncluyeDatosPersonales() {
        when(seguridad.actual()).thenReturn(Optional.of(new UsuarioAutenticado(3, 7, "CLIENTE")));
        pedidoDelCliente7();

        String resultado = herramientas.ejecutar("consultar_mis_pedidos", Map.of()).toLowerCase();

        // Solo estado, fecha, productos, total, metodo de pago y empresa: nada de correo, direccion ni contraseña
        assertFalse(resultado.contains("@"));
        assertFalse(resultado.contains("contrase"));
        assertFalse(resultado.contains("direcci"));
    }

    // ------------------------------------------------------------ PRODUCTOS

    @Test
    void buscaProductosIgnorandoTildesYMayusculas() {
        when(productos.findByActivoTrue()).thenReturn(List.of(
                producto(1, "Jabón líquido", 1500, 20, "Limpieza"),
                producto(2, "Guantes", 900, 0, "Seguridad")));

        String r = herramientas.ejecutar("buscar_productos", Map.of("consulta", "JABON"));

        assertTrue(r.contains("Jabón líquido"));
        assertTrue(r.contains("/producto/1"));
        assertFalse(r.contains("Guantes"));
    }

    @Test
    void indicaCuandoUnProductoNoTieneStock() {
        when(productos.findByActivoTrue()).thenReturn(List.of(producto(2, "Guantes", 900, 0, "Seguridad")));

        assertTrue(herramientas.ejecutar("buscar_productos", Map.of("consulta", "guantes")).contains("SIN STOCK"));
    }

    @Test
    void noMuestraMasDeLaCantidadMaximaDeProductos() {
        List<Producto> muchos = new java.util.ArrayList<>();
        for (int i = 1; i <= 30; i++) muchos.add(producto(i, "Producto " + i, 100, 5, "Limpieza"));
        when(productos.findByActivoTrue()).thenReturn(muchos);

        String r = herramientas.ejecutar("buscar_productos", Map.of("consulta", ""));

        assertEquals(ChatHerramientas.MAX_PRODUCTOS, r.lines().count());
    }

    @Test
    void unArgumentoRaroDelModeloNoRompeLaBusqueda() {
        when(productos.findByActivoTrue()).thenReturn(List.of(producto(1, "Jabón", 100, 5, "Limpieza")));

        assertDoesNotThrow(() -> herramientas.ejecutar("buscar_productos", Map.of("consulta", 12345)));
        assertDoesNotThrow(() -> herramientas.ejecutar("buscar_productos", null));
    }

    @Test
    void unaHerramientaDesconocidaNoHaceNada() {
        assertEquals("Herramienta desconocida.", herramientas.ejecutar("borrar_todo", Map.of()));
        verifyNoInteractions(ordenes, pagos, envios);
    }

    @Test
    void unErrorInternoNoFiltraDetalles() {
        when(productos.findByActivoTrue()).thenThrow(new RuntimeException("SELECT secreto FROM tabla_interna"));

        String r = herramientas.ejecutar("buscar_productos", Map.of("consulta", "x"));

        assertFalse(r.contains("SELECT"));
        assertTrue(r.contains("No se pudo"));
    }
}
