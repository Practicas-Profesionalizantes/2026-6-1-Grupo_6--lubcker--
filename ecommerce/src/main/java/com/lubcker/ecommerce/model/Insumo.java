package com.lubcker.ecommerce.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

// Materia prima / insumo que usa el negocio para fabricar sus productos. Solo lo ve el admin.
@Entity
@Table(name = "insumo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Insumo {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int idInsumo;

	@Column(nullable = false, length = 100)
	private String nombre;

	// kg, g, litro, ml, unidad o metro. Las cantidades de las recetas se expresan en esta unidad.
	@Column(nullable = false, length = 20)
	private String unidadMedida;

	// Costo de UNA unidad de medida (ej: $1.250,50 el kg). Obligatorio: con esto se calcula el costo de fabricacion.
	@Column(nullable = false, precision = 14, scale = 2)
	private BigDecimal costoUnitario;

	@Column(length = 100)
	private String proveedor;
}
