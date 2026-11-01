package com.lubcker.ecommerce.config;

import com.lubcker.ecommerce.model.Categoria;
import com.lubcker.ecommerce.model.Empresa_Logistica;
import com.lubcker.ecommerce.model.MetodoPago;
import com.lubcker.ecommerce.repository.CategoriaRepository;
import com.lubcker.ecommerce.repository.Empresa_LogisticaRepository;
import com.lubcker.ecommerce.repository.MetodoPagoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

// Carga los datos que el checkout necesita si todavia no existen.
// Se ejecuta en cada arranque, pero no duplica nada.
@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private MetodoPagoRepository metodoPagoRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private Empresa_LogisticaRepository empresaLogisticaRepository;

    @Override
    public void run(String... args) {
        // Las mismas categorias que ofrece el filtro del catalogo en el frontend
        crearCategoriaSiNoExiste("Limpieza");
        crearCategoriaSiNoExiste("Seguridad");

        crearMetodoSiNoExiste("Mercado Pago");

        if (empresaLogisticaRepository.count() == 0) {
            Empresa_Logistica empresa = new Empresa_Logistica();
            empresa.setNombre("Correo Argentino");
            empresa.setSucursal("Envío a domicilio");
            empresaLogisticaRepository.save(empresa);
        }
    }

    private void crearCategoriaSiNoExiste(String nombre) {
        boolean existe = categoriaRepository.findAll().stream()
                .anyMatch(c -> nombre.equalsIgnoreCase(c.getNombre()));
        if (!existe) {
            Categoria categoria = new Categoria();
            categoria.setNombre(nombre);
            categoriaRepository.save(categoria);
        }
    }

    private void crearMetodoSiNoExiste(String nombre) {
        boolean existe = metodoPagoRepository.findAll().stream()
                .anyMatch(m -> nombre.equalsIgnoreCase(m.getNombre()));
        if (!existe) {
            MetodoPago metodo = new MetodoPago();
            metodo.setNombre(nombre);
            metodoPagoRepository.save(metodo);
        }
    }
}