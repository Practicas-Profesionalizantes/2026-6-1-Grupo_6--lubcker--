package com.lubcker.ecommerce.service;

import com.lubcker.ecommerce.dto.RecetaDTO;
import com.lubcker.ecommerce.dto.RecetaItemRequestDTO;
import com.lubcker.ecommerce.model.Insumo;
import com.lubcker.ecommerce.model.Producto;
import com.lubcker.ecommerce.model.ProductoInsumo;
import com.lubcker.ecommerce.repository.InsumoRepository;
import com.lubcker.ecommerce.repository.ProductoInsumoRepository;
import com.lubcker.ecommerce.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RecetaServiceTest {

    private ProductoRepository productos;
    private InsumoRepository insumos;
    private ProductoInsumoRepository recetas;
    private RecetaService servicio;
    private Producto grasa;

    @BeforeEach
    void preparar() {
        productos = mock(ProductoRepository.class);
        insumos = mock(InsumoRepository.class);
        recetas = mock(ProductoInsumoRepository.class);
        servicio = new RecetaService(productos, insumos, recetas);

        grasa = new Producto();
        grasa.setIdProducto(26);
        grasa.setNombre("GRASA");
        grasa.setPrecio(10000); // por kg
        grasa.setUnidadMedida("kg");
        when(productos.findById(26)).thenReturn(Optional.of(grasa));
    }

    private Insumo insumo(int id, String nombre, String unidad, String costo) {
        Insumo i = new Insumo();
        i.setIdInsumo(id);
        i.setNombre(nombre);
        i.setUnidadMedida(unidad);
        i.setCostoUnitario(new BigDecimal(costo));
        return i;
    }

    private ProductoInsumo linea(Insumo insumo, String cantidad) {
        ProductoInsumo l = new ProductoInsumo();
        l.setProducto(grasa);
        l.setInsumo(insumo);
        l.setCantidad(new BigDecimal(cantidad));
        return l;
    }

    @Test
    void costoEsLaSumaDeCantidadPorCostoDeCadaInsumo() {
        // 1 kg de producto: 0,4 kg de arena a $2.000 + 0,25 litros de glicerina a $4.000 + 0,01 kg de colorante a $30.000
        RecetaDTO r = servicio.armar(grasa, List.of(
                linea(insumo(1, "Arena", "kg", "2000"), "0.4"),
                linea(insumo(2, "Glicerina", "litro", "4000"), "0.25"),
                linea(insumo(3, "Colorante", "kg", "30000"), "0.01")));

        assertEquals(0, new BigDecimal("2100.00").compareTo(r.getCostoFabricacion())); // 800 + 1000 + 300
        assertEquals(3, r.getItems().size());
        assertEquals(0, new BigDecimal("800.00").compareTo(r.getItems().get(0).getCosto()));
    }

    @Test
    void gananciaYMargenSalenDelPrecioPublicado() {
        RecetaDTO r = servicio.armar(grasa, List.of(linea(insumo(1, "Arena", "kg", "2500"), "1")));

        assertEquals(10000, r.getPrecioVenta());
        assertEquals(0, new BigDecimal("7500.00").compareTo(r.getGanancia()));
        assertEquals(0, new BigDecimal("75.0").compareTo(r.getMargenPorcentaje()));
    }

    @Test
    void siCuestaMasQueElPrecioLaGananciaEsNegativa() {
        RecetaDTO r = servicio.armar(grasa, List.of(linea(insumo(1, "Caro", "kg", "12000"), "1")));

        assertEquals(-1, r.getGanancia().signum());
        assertEquals(-1, r.getMargenPorcentaje().signum());
    }

    @Test
    void sinInsumosElCostoEsCeroYLaGananciaEsElPrecio() {
        RecetaDTO r = servicio.armar(grasa, List.of());

        assertTrue(r.getItems().isEmpty());
        assertEquals(0, BigDecimal.ZERO.compareTo(r.getCostoFabricacion()));
        assertEquals(0, new BigDecimal("10000").compareTo(r.getGanancia()));
    }

    @Test
    void conPrecioCeroElMargenNoSeCalcula() {
        grasa.setPrecio(0);
        assertNull(servicio.armar(grasa, List.of(linea(insumo(1, "Arena", "kg", "10"), "1"))).getMargenPorcentaje());
    }

    @Test
    void siCambiaElPrecioDelInsumoCambiaElCostoPeroNoAntes() {
        Insumo arena = insumo(1, "Arena", "kg", "2000");
        List<ProductoInsumo> lineas = List.of(linea(arena, "1"));
        assertEquals(0, new BigDecimal("2000.00").compareTo(servicio.armar(grasa, lineas).getCostoFabricacion()));

        arena.setCostoUnitario(new BigDecimal("3000"));
        assertEquals(0, new BigDecimal("3000.00").compareTo(servicio.armar(grasa, lineas).getCostoFabricacion()));
    }

    @Test
    void guardarReemplazaLaRecetaEntera() {
        Insumo arena = insumo(1, "Arena", "kg", "2000");
        when(insumos.findById(1)).thenReturn(Optional.of(arena));

        String error = servicio.guardar(26, List.of(new RecetaItemRequestDTO(1, new BigDecimal("0.5"))));

        assertNull(error);
        verify(recetas).borrarPorProducto(26);
        verify(recetas).saveAll(any());
    }

    @Test
    void guardarRechazaInsumosRepetidosOInexistentesYNoToca() {
        when(insumos.findById(1)).thenReturn(Optional.of(insumo(1, "Arena", "kg", "2000")));

        assertNotNull(servicio.guardar(26, List.of(
                new RecetaItemRequestDTO(1, BigDecimal.ONE), new RecetaItemRequestDTO(1, BigDecimal.TEN))));
        assertNotNull(servicio.guardar(26, List.of(new RecetaItemRequestDTO(99, BigDecimal.ONE))));
        assertNotNull(servicio.guardar(999, List.of()));
        verify(recetas, never()).borrarPorProducto(anyInt());
        verify(recetas, never()).saveAll(any());
    }

    @Test
    void guardarListaVaciaDejaElProductoSinMateriaPrima() {
        assertNull(servicio.guardar(26, List.of()));

        verify(recetas).borrarPorProducto(26);
    }
}
