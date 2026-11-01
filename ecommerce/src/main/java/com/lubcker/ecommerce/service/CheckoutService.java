package com.lubcker.ecommerce.service;

import com.lubcker.ecommerce.dto.CheckoutRequestDTO;
import com.lubcker.ecommerce.dto.CheckoutResponseDTO;
import com.lubcker.ecommerce.dto.DetalleOrdenDTO;
import com.lubcker.ecommerce.dto.ItemCheckoutDTO;
import com.lubcker.ecommerce.mercadopago.MercadoPagoException;
import com.lubcker.ecommerce.mercadopago.MercadoPagoService;
import com.lubcker.ecommerce.model.Cliente;
import com.lubcker.ecommerce.model.Detalle_Orden;
import com.lubcker.ecommerce.model.Direccion;
import com.lubcker.ecommerce.model.Empresa_Logistica;
import com.lubcker.ecommerce.model.Envio;
import com.lubcker.ecommerce.model.MetodoPago;
import com.lubcker.ecommerce.model.Orden;
import com.lubcker.ecommerce.model.Pago;
import com.lubcker.ecommerce.model.Producto;
import com.lubcker.ecommerce.repository.ClienteRepository;
import com.lubcker.ecommerce.repository.Detalle_OrdenRepository;
import com.lubcker.ecommerce.repository.DireccionRepository;
import com.lubcker.ecommerce.repository.Empresa_LogisticaRepository;
import com.lubcker.ecommerce.repository.EnvioRepository;
import com.lubcker.ecommerce.repository.MetodoPagoRepository;
import com.lubcker.ecommerce.repository.OrdenRepository;
import com.lubcker.ecommerce.repository.PagoRepository;
import com.lubcker.ecommerce.repository.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CheckoutService {

    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private ProductoRepository productoRepository;
    @Autowired
    private OrdenRepository ordenRepository;
    @Autowired
    private Detalle_OrdenRepository detalleOrdenRepository;
    @Autowired
    private DireccionRepository direccionRepository;
    @Autowired
    private MetodoPagoRepository metodoPagoRepository;
    @Autowired
    private PagoRepository pagoRepository;
    @Autowired
    private EnvioRepository envioRepository;
    @Autowired
    private Empresa_LogisticaRepository empresaLogisticaRepository;
    @Autowired
    private MercadoPagoService mercadoPagoService;

    // Todo el metodo corre en UNA transaccion: si algo falla, no queda nada guardado a medias.
    @Transactional
    public CheckoutResponseDTO procesar(CheckoutRequestDTO datos) {

        // 1. Validaciones
        if (!mercadoPagoService.disponible()) {
            throw new CheckoutException("Los pagos online no están disponibles por el momento. Probá más tarde.");
        }
        if (datos.getItems() == null || datos.getItems().isEmpty()) {
            throw new CheckoutException("El carrito está vacío.");
        }
        if (estaVacio(datos.getProvincia()) || estaVacio(datos.getCiudad())
                || estaVacio(datos.getCalle()) || datos.getAltura() <= 0) {
            throw new CheckoutException("Completá provincia, ciudad, calle y altura de la dirección de envío.");
        }

        Cliente cliente = clienteRepository.findById(datos.getIdCliente())
                .orElseThrow(() -> new CheckoutException("Cliente no encontrado."));

        // El dueño entrega los pedidos y avisa por WhatsApp: hace falta un telefono de contacto
        String telefono = datos.getTelefono() == null ? "" : datos.getTelefono().trim();
        if (!telefono.isEmpty()) {
            if (!telefono.matches("^[+0-9][0-9 ()-]{6,19}$")) {
                throw new CheckoutException("El teléfono no es válido. Ingresalo con el código de área, por ejemplo 11 4430-6194.");
            }
            cliente.setTelefono(telefono);
            clienteRepository.save(cliente);
        } else if (cliente.getTelefono() == null || cliente.getTelefono().isBlank()) {
            throw new CheckoutException("Ingresá un teléfono de contacto para coordinar la entrega.");
        }
        // El unico medio de pago es Mercado Pago (el idMetodoPago que mande el navegador se ignora)
        MetodoPago metodo = metodoPagoRepository.findAll().stream()
                .filter(m -> "Mercado Pago".equalsIgnoreCase(m.getNombre()))
                .findFirst()
                .orElseThrow(() -> new CheckoutException("El método de pago no está configurado."));
        Empresa_Logistica empresa = empresaLogisticaRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new CheckoutException("No hay una empresa de logística configurada."));

        // 2. Unificar repetidos y validar cantidades: producto -> (medida -> cantidad de paquetes)
        Map<Integer, Map<Integer, Integer>> pedido = new LinkedHashMap<>();
        for (ItemCheckoutDTO item : datos.getItems()) {
            if (item.getCantidad() <= 0) {
                throw new CheckoutException("La cantidad de cada producto debe ser mayor a 0.");
            }
            int tamano = item.getTamano() <= 0 ? 1 : item.getTamano();
            pedido.computeIfAbsent(item.getIdProducto(), k -> new LinkedHashMap<>())
                    .merge(tamano, item.getCantidad(), Integer::sum);
        }

        // 3. Orden
        Orden nuevaOrden = new Orden();
        nuevaOrden.setCliente(cliente);
        nuevaOrden.setFecha(new Date());
        final Orden orden = ordenRepository.save(nuevaOrden);

        // 4. Detalles: el precio sale de la base de datos, nunca del frontend.
        // Precio de un paquete = precio por unidad de medida x medida del paquete.
        List<DetalleOrdenDTO> detallesDTO = new ArrayList<>();
        int total = 0;
        for (Map.Entry<Integer, Map<Integer, Integer>> entrada : pedido.entrySet()) {
            final int idProducto = entrada.getKey();

            // Se bloquea la fila del producto hasta terminar: dos compras simultaneas
            // no pueden llevarse la misma unidad
            Producto producto = productoRepository.findByIdParaActualizar(idProducto)
                    .orElseThrow(() -> new CheckoutException("Producto no encontrado: " + idProducto));

            if (!producto.isActivo()) {
                throw new CheckoutException("\"" + producto.getNombre() + "\" ya no está disponible. Quitalo del carrito para continuar.");
            }

            // Cada medida tiene que ser una de las que el producto vende; el stock se descuenta en la unidad del producto
            long aDescontar = 0;
            for (Map.Entry<Integer, Integer> paquete : entrada.getValue().entrySet()) {
                if (!producto.listaPresentaciones().contains(paquete.getKey())) {
                    throw new CheckoutException("\"" + producto.getNombre() + "\" ya no se vende en esa medida. Quitalo del carrito y volvé a agregarlo.");
                }
                aDescontar += (long) paquete.getKey() * paquete.getValue();
            }
            String unidad = "unidad".equals(producto.getUnidadMedida()) ? "unidades" : producto.getUnidadMedida();
            if (producto.getStock() < aDescontar) {
                throw new CheckoutException(producto.getStock() == 0
                        ? "\"" + producto.getNombre() + "\" se quedó sin stock. Quitalo del carrito para continuar."
                        : "No hay stock suficiente de \"" + producto.getNombre() + "\": quedan "
                                + producto.getStock() + " " + unidad + ".");
            }
            producto.setStock(producto.getStock() - (int) aDescontar);
            productoRepository.save(producto);

            for (Map.Entry<Integer, Integer> paquete : entrada.getValue().entrySet()) {
                final int tamano = paquete.getKey();
                final int cantidad = paquete.getValue();
                int precioUnitario;
                int subtotal;
                try {
                    precioUnitario = Math.multiplyExact(producto.getPrecio(), tamano);
                    subtotal = Math.multiplyExact(precioUnitario, cantidad);
                    total = Math.addExact(total, subtotal);
                } catch (ArithmeticException e) {
                    throw new CheckoutException("El monto del pedido es demasiado grande. Reducí las cantidades.");
                }

                Detalle_Orden detalle = new Detalle_Orden();
                detalle.setOrden(orden);
                detalle.setProducto(producto);
                detalle.setCantidad(cantidad);
                detalle.setTamano(tamano);
                detalle.setPrecioUnitario(precioUnitario);
                detalle.setSubtotal(subtotal);
                Detalle_Orden guardado = detalleOrdenRepository.save(detalle);

                detallesDTO.add(new DetalleOrdenDTO(
                        guardado.getIdDetalle(),
                        orden.getIdOrden(),
                        producto.getIdProducto(),
                        producto.getNombre(),
                        cantidad,
                        precioUnitario,
                        subtotal,
                        tamano,
                        producto.getUnidadMedida()
                ));
            }
        }

        // 5. Direccion: se reutiliza si el cliente ya tiene una igual
        Direccion direccion = direccionRepository.findAll().stream()
                .filter(d -> d.getCliente() != null
                        && d.getCliente().getIdCliente() == cliente.getIdCliente()
                        && coincide(d.getProvincia(), datos.getProvincia())
                        && coincide(d.getCiudad(), datos.getCiudad())
                        && coincide(d.getCalle(), datos.getCalle())
                        && d.getAltura() == datos.getAltura())
                .findFirst()
                .orElseGet(() -> {
                    Direccion nueva = new Direccion();
                    nueva.setCliente(cliente);
                    nueva.setProvincia(datos.getProvincia().trim());
                    nueva.setCiudad(datos.getCiudad().trim());
                    nueva.setCalle(datos.getCalle().trim());
                    nueva.setAltura(datos.getAltura());
                    return direccionRepository.save(nueva);
                });

        // 6. Pago: queda PENDIENTE (estado=false) hasta que se confirme
        Pago pago = new Pago();
        pago.setOrden(orden);
        pago.setMetodoPago(metodo);
        pago.setEstado(false);
        pago.setMonto(total);
        Pago pagoGuardado = pagoRepository.save(pago);

        // 7. Envio
        Envio envio = new Envio();
        envio.setOrden(orden);
        envio.setDireccion(direccion);
        envio.setEmpresaLogistica(empresa);
        envio.setEstado("Pendiente");
        Envio envioGuardado = envioRepository.save(envio);

        // 8. Link de pago de Mercado Pago. Si falla, la excepcion deshace TODO (stock incluido)
        String urlPago;
        try {
            urlPago = mercadoPagoService.crearLinkDePago(orden, detalleOrdenRepository.findByOrdenIdOrden(orden.getIdOrden()));
        } catch (MercadoPagoException e) {
            throw new CheckoutException(e.getMessage());
        }

        String textoDireccion = direccion.getCalle() + " " + direccion.getAltura()
                + ", " + direccion.getCiudad() + ", " + direccion.getProvincia();

        return new CheckoutResponseDTO(
                orden.getIdOrden(),
                orden.getFecha(),
                total,
                pagoGuardado.getIdPago(),
                metodo.getNombre(),
                pagoGuardado.isEstado(),
                envioGuardado.getIdEnvio(),
                envioGuardado.getEstado(),
                empresa.getNombre(),
                textoDireccion,
                detallesDTO,
                urlPago
        );
    }

    private boolean estaVacio(String texto) {
        return texto == null || texto.trim().isEmpty();
    }

    private boolean coincide(String a, String b) {
        return a != null && b != null && a.trim().equalsIgnoreCase(b.trim());
    }
}