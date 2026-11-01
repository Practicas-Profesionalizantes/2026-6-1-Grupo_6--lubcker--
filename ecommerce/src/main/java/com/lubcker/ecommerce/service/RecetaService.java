package com.lubcker.ecommerce.service;

import com.lubcker.ecommerce.dto.RecetaDTO;
import com.lubcker.ecommerce.dto.RecetaItemDTO;
import com.lubcker.ecommerce.dto.RecetaItemRequestDTO;
import com.lubcker.ecommerce.dto.ResumenCostoDTO;
import com.lubcker.ecommerce.model.Insumo;
import com.lubcker.ecommerce.model.Producto;
import com.lubcker.ecommerce.model.ProductoInsumo;
import com.lubcker.ecommerce.repository.InsumoRepository;
import com.lubcker.ecommerce.repository.ProductoInsumoRepository;
import com.lubcker.ecommerce.repository.ProductoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

// Materia prima de cada producto y calculo de costo de fabricacion / ganancia.
// Todo es POR UNIDAD del producto: si se vende por kilo, la receta es para 1 kg.
// El costo se calcula en el momento con el costo actual de cada insumo: si el admin cambia el precio
// de un insumo, el costo de fabricacion cambia solo; si no, es fijo.
@Service
public class RecetaService {

    private final ProductoRepository productoRepository;
    private final InsumoRepository insumoRepository;
    private final ProductoInsumoRepository recetaRepository;

    public RecetaService(ProductoRepository productoRepository, InsumoRepository insumoRepository,
                         ProductoInsumoRepository recetaRepository) {
        this.productoRepository = productoRepository;
        this.insumoRepository = insumoRepository;
        this.recetaRepository = recetaRepository;
    }

    public static final List<String> UNIDADES_INSUMO = List.of("kg", "g", "litro", "ml", "unidad", "metro");

    // Receta de un producto con sus costos (items vacios si todavia no tiene materia prima)
    @Transactional(readOnly = true)
    public Optional<RecetaDTO> obtener(int idProducto) {
        return productoRepository.findById(idProducto)
                .map(p -> armar(p, recetaRepository.findByProducto(idProducto)));
    }

    // Reemplaza toda la receta del producto. Devuelve el error (para mostrarle al admin) o null si salio bien.
    @Transactional
    public String guardar(int idProducto, List<RecetaItemRequestDTO> items) {
        Producto producto = productoRepository.findById(idProducto).orElse(null);
        if (producto == null) return "El producto no existe.";

        Set<Integer> vistos = new HashSet<>();
        List<ProductoInsumo> nuevos = new ArrayList<>();
        for (RecetaItemRequestDTO item : items) {
            if (!vistos.add(item.getIdInsumo())) return "Un insumo está repetido en la receta.";
            Insumo insumo = insumoRepository.findById(item.getIdInsumo()).orElse(null);
            if (insumo == null) return "Uno de los insumos elegidos ya no existe.";
            ProductoInsumo linea = new ProductoInsumo();
            linea.setProducto(producto);
            linea.setInsumo(insumo);
            linea.setCantidad(item.getCantidad().setScale(4, RoundingMode.HALF_UP));
            nuevos.add(linea);
        }

        recetaRepository.borrarPorProducto(idProducto);
        recetaRepository.saveAll(nuevos);
        return null;
    }

    // Costo y ganancia por unidad de TODOS los productos que tienen materia prima cargada
    @Transactional(readOnly = true)
    public List<ResumenCostoDTO> resumen() {
        Map<Integer, List<ProductoInsumo>> porProducto = new LinkedHashMap<>();
        for (ProductoInsumo linea : recetaRepository.findAllConDetalle()) {
            porProducto.computeIfAbsent(linea.getProducto().getIdProducto(), k -> new ArrayList<>()).add(linea);
        }
        List<ResumenCostoDTO> resultado = new ArrayList<>();
        for (List<ProductoInsumo> lineas : porProducto.values()) {
            RecetaDTO r = armar(lineas.get(0).getProducto(), lineas);
            resultado.add(new ResumenCostoDTO(r.getIdProducto(), r.getPrecioVenta(), r.getCostoFabricacion(),
                    r.getGanancia(), r.getMargenPorcentaje()));
        }
        return resultado;
    }

    // Costo = suma de (cantidad x costo del insumo). Ganancia = precio de venta - costo.
    // Margen = ganancia / precio de venta.
    public RecetaDTO armar(Producto producto, List<ProductoInsumo> lineas) {
        List<RecetaItemDTO> items = new ArrayList<>();
        BigDecimal costoTotal = BigDecimal.ZERO;
        for (ProductoInsumo l : lineas) {
            BigDecimal costoLinea = l.getCantidad().multiply(l.getInsumo().getCostoUnitario()).setScale(2, RoundingMode.HALF_UP);
            costoTotal = costoTotal.add(costoLinea);
            items.add(new RecetaItemDTO(l.getInsumo().getIdInsumo(), l.getInsumo().getNombre(),
                    l.getInsumo().getUnidadMedida(), l.getInsumo().getCostoUnitario(), l.getCantidad(), costoLinea));
        }

        BigDecimal precio = BigDecimal.valueOf(producto.getPrecio());
        BigDecimal ganancia = precio.subtract(costoTotal).setScale(2, RoundingMode.HALF_UP);
        BigDecimal margen = precio.signum() > 0
                ? ganancia.multiply(BigDecimal.valueOf(100)).divide(precio, 1, RoundingMode.HALF_UP)
                : null;

        return new RecetaDTO(producto.getIdProducto(), producto.getNombre(), producto.getUnidadMedida(),
                producto.getPrecio(), items, costoTotal.setScale(2, RoundingMode.HALF_UP), ganancia, margen);
    }
}
