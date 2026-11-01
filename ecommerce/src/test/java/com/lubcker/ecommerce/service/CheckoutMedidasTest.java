package com.lubcker.ecommerce.service;

import com.lubcker.ecommerce.dto.CheckoutRequestDTO;
import com.lubcker.ecommerce.dto.CheckoutResponseDTO;
import com.lubcker.ecommerce.dto.ItemCheckoutDTO;
import com.lubcker.ecommerce.mercadopago.MercadoPagoService;
import com.lubcker.ecommerce.model.*;
import com.lubcker.ecommerce.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Productos que se venden por medida (kg, litro...): el precio final es precio x medida
class CheckoutMedidasTest {

    @Mock private ClienteRepository clienteRepository;
    @Mock private ProductoRepository productoRepository;
    @Mock private OrdenRepository ordenRepository;
    @Mock private Detalle_OrdenRepository detalleOrdenRepository;
    @Mock private DireccionRepository direccionRepository;
    @Mock private MetodoPagoRepository metodoPagoRepository;
    @Mock private PagoRepository pagoRepository;
    @Mock private EnvioRepository envioRepository;
    @Mock private Empresa_LogisticaRepository empresaLogisticaRepository;
    @Mock private MercadoPagoService mercadoPagoService;
    @InjectMocks private CheckoutService servicio;

    private Producto grasa;

    @BeforeEach
    void preparar() {
        MockitoAnnotations.openMocks(this);
        Cliente cliente = new Cliente();
        cliente.setIdCliente(7);
        when(clienteRepository.findById(7)).thenReturn(Optional.of(cliente));
        MetodoPago mp = new MetodoPago();
        mp.setNombre("Mercado Pago");
        when(metodoPagoRepository.findAll()).thenReturn(List.of(mp));
        Empresa_Logistica empresa = new Empresa_Logistica();
        empresa.setNombre("Correo");
        when(empresaLogisticaRepository.findAll()).thenReturn(List.of(empresa));
        when(mercadoPagoService.disponible()).thenReturn(true);
        when(mercadoPagoService.crearLinkDePago(any(), any())).thenReturn("https://mp.test/pagar");
        when(ordenRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(detalleOrdenRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(direccionRepository.findAll()).thenReturn(List.of());
        when(direccionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(pagoRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(envioRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        grasa = new Producto();
        grasa.setIdProducto(26);
        grasa.setNombre("GRASA");
        grasa.setPrecio(10000);          // por kg
        grasa.setUnidadMedida("kg");
        grasa.setPresentaciones("3,5,7,24");
        grasa.setStock(100);             // kg
        grasa.setActivo(true);
        when(productoRepository.findByIdParaActualizar(26)).thenReturn(Optional.of(grasa));
    }

    private CheckoutRequestDTO pedido(ItemCheckoutDTO... items) {
        return new CheckoutRequestDTO(7, 0, "BA", "LP", "Calle", 1, "11 4430-6194", List.of(items));
    }

    @Test
    void sinTelefonoDeContactoNoSePuedeComprar() {
        // El cliente del test no tiene telefono guardado y el pedido tampoco lo trae
        CheckoutRequestDTO sinTelefono = new CheckoutRequestDTO(7, 0, "BA", "LP", "Calle", 1, "", List.of(new ItemCheckoutDTO(26, 1, 5)));
        CheckoutException e = assertThrows(CheckoutException.class, () -> servicio.procesar(sinTelefono));
        assertTrue(e.getMessage().contains("teléfono"));
    }

    @Test
    void unTelefonoInvalidoSeRechaza() {
        CheckoutRequestDTO malo = new CheckoutRequestDTO(7, 0, "BA", "LP", "Calle", 1, "abc", List.of(new ItemCheckoutDTO(26, 1, 5)));
        assertThrows(CheckoutException.class, () -> servicio.procesar(malo));
    }

    @Test
    void elPrecioEsElPrecioPorKiloPorLaMedidaElegida() {
        CheckoutResponseDTO r = servicio.procesar(pedido(new ItemCheckoutDTO(26, 1, 5)));

        assertEquals(50000, r.getTotal());
        assertEquals(1, r.getDetalles().size());
        assertEquals(5, r.getDetalles().get(0).getTamano());
        assertEquals(50000, r.getDetalles().get(0).getPrecioUnitario());
        assertEquals("kg", r.getDetalles().get(0).getUnidadMedida());
    }

    @Test
    void siete_kilos_son_setenta_mil() {
        assertEquals(70000, servicio.procesar(pedido(new ItemCheckoutDTO(26, 1, 7))).getTotal());
    }

    @Test
    void elStockSeDescuentaEnKilos() {
        servicio.procesar(pedido(new ItemCheckoutDTO(26, 2, 5))); // 2 bolsas de 5 kg = 10 kg

        assertEquals(90, grasa.getStock());
    }

    @Test
    void variasMedidasDelMismoProductoSumanPrecioYStock() {
        CheckoutResponseDTO r = servicio.procesar(pedido(new ItemCheckoutDTO(26, 1, 3), new ItemCheckoutDTO(26, 2, 24)));

        assertEquals(3 * 10000 + 2 * 24 * 10000, r.getTotal());
        assertEquals(2, r.getDetalles().size());
        assertEquals(100 - 3 - 48, grasa.getStock());
    }

    @Test
    void elMismoItemRepetidoSeUnifica() {
        CheckoutResponseDTO r = servicio.procesar(pedido(new ItemCheckoutDTO(26, 1, 5), new ItemCheckoutDTO(26, 2, 5)));

        assertEquals(1, r.getDetalles().size());
        assertEquals(3, r.getDetalles().get(0).getCantidad());
        assertEquals(150000, r.getTotal());
    }

    @Test
    void unaMedidaQueElProductoNoVendeSeRechaza() {
        CheckoutException e = assertThrows(CheckoutException.class,
                () -> servicio.procesar(pedido(new ItemCheckoutDTO(26, 1, 4)))); // vende 3, 5, 7 y 24

        assertTrue(e.getMessage().contains("no se vende en esa medida"));
        assertEquals(100, grasa.getStock());
    }

    @Test
    void sinStockSuficienteEnKilosSeRechaza() {
        grasa.setStock(8);

        CheckoutException e = assertThrows(CheckoutException.class,
                () -> servicio.procesar(pedido(new ItemCheckoutDTO(26, 1, 5), new ItemCheckoutDTO(26, 1, 7)))); // 12 kg

        assertTrue(e.getMessage().contains("quedan 8 kg"), e.getMessage());
        assertEquals(8, grasa.getStock());
    }

    @Test
    void unProductoComunSigueFuncionandoComoAntes() {
        Producto comun = new Producto();
        comun.setIdProducto(25);
        comun.setNombre("1DEDO");
        comun.setPrecio(20000);
        comun.setStock(10);
        comun.setActivo(true);
        when(productoRepository.findByIdParaActualizar(25)).thenReturn(Optional.of(comun));

        // Sin "tamano" (como lo mandaba el frontend viejo) = 1
        CheckoutResponseDTO r = servicio.procesar(pedido(new ItemCheckoutDTO(25, 3, 0)));

        assertEquals(60000, r.getTotal());
        assertEquals(7, comun.getStock());
        assertEquals(1, r.getDetalles().get(0).getTamano());
    }

    @Test
    void unMontoQueDesbordaSeRechazaEnVezDeDarUnTotalNegativo() {
        grasa.setPrecio(100_000_000);
        grasa.setPresentaciones("24");
        grasa.setStock(1_000_000);

        assertThrows(CheckoutException.class, () -> servicio.procesar(pedido(new ItemCheckoutDTO(26, 5, 24))));
    }
}
