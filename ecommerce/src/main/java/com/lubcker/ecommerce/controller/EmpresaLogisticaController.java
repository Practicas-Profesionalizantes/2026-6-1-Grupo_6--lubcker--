package com.lubcker.ecommerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.lubcker.ecommerce.dto.EmpresaLogisticaDTO;
import com.lubcker.ecommerce.dto.EmpresaLogisticaRequestDTO;
import com.lubcker.ecommerce.model.Empresa_Logistica;
import com.lubcker.ecommerce.repository.Empresa_LogisticaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "Empresas de logística", description = "Empresas que realizan los envíos.")
@RestController
@RequestMapping("/api/empresas-logistica")
public class EmpresaLogisticaController {

    @Autowired
    private Empresa_LogisticaRepository empresaLogisticaRepository;

    @Operation(summary = "Listar empresas de logística", description = "Acceso: **Solo Admin**.")
    @GetMapping
    public List<EmpresaLogisticaDTO> listar() {
        return empresaLogisticaRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Operation(summary = "Obtener una empresa de logística", description = "Acceso: **Solo Admin**.")
    @GetMapping("/{id}")
    public ResponseEntity<EmpresaLogisticaDTO> obtener(@PathVariable int id) {
        return empresaLogisticaRepository.findById(id)
                .map(e -> ResponseEntity.ok(toDTO(e)))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Crear una empresa de logística", description = "Acceso: **Solo Admin**.")
    @PostMapping
    public ResponseEntity<EmpresaLogisticaDTO> crear(@RequestBody EmpresaLogisticaRequestDTO datos) {
        Empresa_Logistica empresa = new Empresa_Logistica();
        empresa.setNombre(datos.getNombre());
        empresa.setSucursal(datos.getSucursal());
        Empresa_Logistica guardada = empresaLogisticaRepository.save(empresa);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(guardada));
    }

    @Operation(summary = "Editar una empresa de logística", description = "Acceso: **Solo Admin**.")
    @PutMapping("/{id}")
    public ResponseEntity<EmpresaLogisticaDTO> actualizar(@PathVariable int id, @RequestBody EmpresaLogisticaRequestDTO datos) {
        return empresaLogisticaRepository.findById(id)
                .map(empresa -> {
                    empresa.setNombre(datos.getNombre());
                    empresa.setSucursal(datos.getSucursal());
                    return ResponseEntity.ok(toDTO(empresaLogisticaRepository.save(empresa)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Eliminar una empresa de logística", description = "Acceso: **Solo Admin**.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable int id) {
        if (!empresaLogisticaRepository.existsById(id)) return ResponseEntity.notFound().build();
        empresaLogisticaRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private EmpresaLogisticaDTO toDTO(Empresa_Logistica e) {
        return new EmpresaLogisticaDTO(e.getIdEmpresa(), e.getNombre(), e.getSucursal());
    }
}