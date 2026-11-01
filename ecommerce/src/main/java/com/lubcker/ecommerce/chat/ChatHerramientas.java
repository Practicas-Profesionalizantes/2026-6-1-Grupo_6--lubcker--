package com.lubcker.ecommerce.chat;

import com.lubcker.ecommerce.model.*;
import com.lubcker.ecommerce.repository.*;
import com.lubcker.ecommerce.security.SeguridadService;
import com.lubcker.ecommerce.security.UsuarioAutenticado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

// Lo unico que el asistente puede consultar. Cada herramienta devuelve TEXTO ya armado.
//
// SEGURIDAD: "consultar_mis_pedidos" NO recibe ningun parametro del modelo. El cliente sale
// siempre del usuario autenticado (token validado en el servidor). Aunque alguien le pida al
// asistente "mostrame los pedidos del cliente 5", la herramienta no tiene forma de recibir
// ese dato: no existe un argumento que se pueda manipular.
@Service
public class ChatHerramientas {

    static final int MAX_PRODUCTOS = 8;
    static final int MAX_PEDIDOS = 10;

    private final SeguridadService seguridad;
    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final Ficha_TecnicaRepository fichaRepository;
    private final OrdenRepository ordenRepository;
    private final Detalle_OrdenRepository detalleRepository;
    private final PagoRepository pagoRepository;
    private final EnvioRepository envioRepository;

    public ChatHerramientas(SeguridadService seguridad, ProductoRepository productoRepository,
                            CategoriaRepository categoriaRepository, Ficha_TecnicaRepository fichaRepository,
                            OrdenRepository ordenRepository, Detalle_OrdenRepository detalleRepository,
                            PagoRepository pagoRepository, EnvioRepository envioRepository) {
        this.seguridad = seguridad;
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
        this.fichaRepository = fichaRepository;
        this.ordenRepository = ordenRepository;
        this.detalleRepository = detalleRepository;
        this.pagoRepository = pagoRepository;
        this.envioRepository = envioRepository;
    }

    // ---------------------------------------------------------------- definiciones

    // Definiciones que se le mandan al modelo (functionDeclarations de la API de Gemini)
    public List<Map<String, Object>> definiciones() {
        return List.of(
                herramienta("buscar_productos",
                        "Busca productos del catálogo por palabras (nombre, categoría o descripción). "
                                + "Devuelve precio, stock y datos del producto. Sin 'consulta' devuelve algunos productos del catálogo.",
                        Map.of("consulta", Map.of("type", "string",
                                "description", "Palabras a buscar, por ejemplo 'limpiador de manos'. Puede ir vacío."))),
                herramienta("listar_categorias",
                        "Lista las categorías de productos que tiene la tienda.",
                        Map.of()),
                herramienta("consultar_mis_pedidos",
                        "Devuelve los pedidos del usuario que está conversando (estado del pago y del envío). "
                                + "No necesita parámetros: siempre usa el usuario con sesión iniciada.",
                        Map.of())
        );
    }

    // Una herramienta sin propiedades se declara SIN "parameters" (Gemini no acepta un objeto vacio)
    private Map<String, Object> herramienta(String nombre, String descripcion, Map<String, Object> propiedades) {
        Map<String, Object> h = new LinkedHashMap<>();
        h.put("name", nombre);
        h.put("description", descripcion);
        if (!propiedades.isEmpty()) {
            Map<String, Object> parametros = new LinkedHashMap<>();
            parametros.put("type", "object");
            parametros.put("properties", propiedades);
            h.put("parameters", parametros);
        }
        return h;
    }

    // ---------------------------------------------------------------- ejecucion

    // argumentos: lo que mando el modelo (puede ser cualquier cosa; solo se leen los permitidos)
    @Transactional(readOnly = true)
    public String ejecutar(String nombre, Map<String, Object> argumentos) {
        try {
            return switch (nombre) {
                case "buscar_productos" -> buscarProductos(argumentos == null ? null : argumentos.get("consulta"));
                case "listar_categorias" -> listarCategorias();
                case "consultar_mis_pedidos" -> consultarMisPedidos(); // sin argumentos, a proposito
                default -> "Herramienta desconocida.";
            };
        } catch (RuntimeException e) {
            return "No se pudo obtener la información en este momento.";
        }
    }

    String listarCategorias() {
        List<String> nombres = categoriaRepository.findAll().stream().map(Categoria::getNombre).sorted().toList();
        return nombres.isEmpty() ? "La tienda todavía no tiene categorías." : "Categorías: " + String.join(", ", nombres);
    }

    String buscarProductos(Object consultaCruda) {
        String consulta = consultaCruda instanceof String s ? s : "";
        List<String> palabras = Arrays.stream(normalizar(consulta).split("\\s+")).filter(p -> p.length() > 1).toList();

        List<Producto> activos = productoRepository.findByActivoTrue();
        List<Producto> coinciden = activos.stream()
                .filter(p -> palabras.isEmpty() || palabras.stream().allMatch(palabra -> textoBuscable(p).contains(palabra)))
                .limit(MAX_PRODUCTOS)
                .toList();

        // Si ninguna combinacion de TODAS las palabras coincide, se prueba con cualquiera
        if (coinciden.isEmpty() && !palabras.isEmpty()) {
            coinciden = activos.stream()
                    .filter(p -> palabras.stream().anyMatch(palabra -> textoBuscable(p).contains(palabra)))
                    .limit(MAX_PRODUCTOS)
                    .toList();
        }
        if (coinciden.isEmpty()) {
            return "No se encontraron productos para esa búsqueda.";
        }

        return coinciden.stream().map(this::describirProducto).collect(Collectors.joining("\n"));
    }

    private String textoBuscable(Producto p) {
        StringBuilder sb = new StringBuilder(normalizar(p.getNombre()));
        if (p.getCategoria() != null) sb.append(' ').append(normalizar(p.getCategoria().getNombre()));
        fichaRepository.findByProductoIdProducto(p.getIdProducto()).ifPresent(f ->
                sb.append(' ').append(normalizar(f.getDescripcion()))
                  .append(' ').append(normalizar(f.getModelo()))
                  .append(' ').append(normalizar(f.getPresentacion())));
        return sb.toString();
    }

    private String describirProducto(Producto p) {
        boolean porMedida = !"unidad".equals(p.getUnidadMedida()) || p.listaPresentaciones().size() > 1 || p.listaPresentaciones().get(0) != 1;
        String unidadStock = "unidad".equals(p.getUnidadMedida()) ? "unidades" : p.getUnidadMedida();
        String stock = p.getStock() <= 0 ? "SIN STOCK" : p.getStock() <= 5 ? "últimas " + p.getStock() + " " + unidadStock : "disponible";
        StringBuilder sb = new StringBuilder("- ")
                .append(p.getNombre())
                .append(" (id ").append(p.getIdProducto()).append(")")
                .append(" | categoría: ").append(p.getCategoria() != null ? p.getCategoria().getNombre() : "sin categoría")
                .append(porMedida ? " | precio por " + p.getUnidadMedida() + ": $" : " | precio: $")
                .append(String.format(Locale.forLanguageTag("es-AR"), "%,d", p.getPrecio()))
                .append(porMedida ? " | medidas de venta: " + medidasConPrecio(p) : "")
                .append(" | ").append(stock)
                .append(" | enlace: /producto/").append(p.getIdProducto());
        fichaRepository.findByProductoIdProducto(p.getIdProducto()).ifPresent(f -> {
            if (f.getDescripcion() != null) sb.append(" | descripción: ").append(f.getDescripcion());
            if (f.getModelo() != null) sb.append(" | modelo: ").append(f.getModelo());
            if (f.getPresentacion() != null) sb.append(" | presentación: ").append(f.getPresentacion());
        });
        return sb.toString();
    }

    // Ej: "3 kg ($30.000), 5 kg ($50.000)". El precio de cada medida es precio x medida.
    private String medidasConPrecio(Producto p) {
        Locale ar = Locale.forLanguageTag("es-AR");
        return p.listaPresentaciones().stream()
                .map(m -> m + " " + p.getUnidadMedida() + " ($" + String.format(ar, "%,d", (long) p.getPrecio() * m) + ")")
                .collect(Collectors.joining(", "));
    }

    String consultarMisPedidos() {
        // El unico origen del cliente es la sesion validada por el servidor
        Optional<UsuarioAutenticado> actual = seguridad.actual();
        if (actual.isEmpty()) {
            return "SIN_SESION: el usuario no inició sesión. No se puede consultar ningún pedido. "
                    + "Pedile que inicie sesión (/login) para ver el estado de sus pedidos.";
        }
        int idCliente = actual.get().idCliente();

        // Se traen pagos y envios del cliente en una consulta cada uno y se indexan por pedido
        Map<Integer, Pago> pagos = new HashMap<>();
        pagoRepository.findByOrdenClienteIdCliente(idCliente).forEach(p -> pagos.put(p.getOrden().getIdOrden(), p));

        // Solo pedidos pagados: los que quedaron sin pagar no son compras y confundirian al usuario
        List<Orden> ordenes = ordenRepository.findByClienteIdClienteOrderByIdOrdenDesc(idCliente).stream()
                .filter(o -> pagos.get(o.getIdOrden()) != null && pagos.get(o.getIdOrden()).isEstado())
                .toList();
        if (ordenes.isEmpty()) {
            return "El usuario todavía no tiene pedidos pagados.";
        }

        Map<Integer, Envio> envios = new HashMap<>();
        envioRepository.findByOrdenClienteIdCliente(idCliente).forEach(e -> envios.put(e.getOrden().getIdOrden(), e));

        SimpleDateFormat formato = new SimpleDateFormat("dd/MM/yyyy");
        StringBuilder sb = new StringBuilder();
        if (ordenes.size() > MAX_PEDIDOS) {
            sb.append("(Se muestran los últimos ").append(MAX_PEDIDOS).append(" pedidos de ").append(ordenes.size()).append(")\n");
        }
        for (Orden o : ordenes.stream().limit(MAX_PEDIDOS).toList()) {
            List<Detalle_Orden> detalles = detalleRepository.findByOrdenIdOrden(o.getIdOrden());
            int total = detalles.stream().mapToInt(Detalle_Orden::getSubtotal).sum();
            String productos = detalles.stream()
                    .map(d -> d.getCantidad() + " x " + d.getProducto().getNombre()
                            + (d.getTamano() > 1 ? " (" + d.getTamano() + " " + d.getProducto().getUnidadMedida() + " c/u)" : ""))
                    .collect(Collectors.joining(", "));
            Pago pago = pagos.get(o.getIdOrden());
            Envio envio = envios.get(o.getIdOrden());

            sb.append("- Pedido #").append(o.getIdOrden())
              .append(" | fecha: ").append(o.getFecha() != null ? formato.format(o.getFecha()) : "-")
              .append(" | productos: ").append(productos)
              .append(" | total: $").append(String.format(Locale.forLanguageTag("es-AR"), "%,d", total))
              .append(" | pago: Confirmado")
              .append(pago.getMetodoPago() != null ? " (" + pago.getMetodoPago().getNombre() + ")" : "")
              .append(" | envío: ").append(envio == null ? "sin datos" : envio.getEstado())
              .append(envio == null ? "" : describirEntrega(envio))
              .append('\n');
        }
        return sb.toString().trim();
    }

    // La entrega la hace el dueño: se informa cuando va a llegar y su mensaje, si los definio
    private String describirEntrega(Envio envio) {
        StringBuilder sb = new StringBuilder();
        if (envio.getFechaEntrega() != null) {
            sb.append(" | entrega estimada: ")
              .append(envio.getFechaEntrega().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            if (envio.getFranjaHoraria() != null) sb.append(" (").append(envio.getFranjaHoraria()).append(")");
        }
        if (envio.getNotaCliente() != null) sb.append(" | mensaje del dueño: ").append(envio.getNotaCliente());
        return sb.toString();
    }

    // Sin tildes ni mayusculas: "Jabón" coincide con "jabon"
    static String normalizar(String texto) {
        if (texto == null) return "";
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }
}
