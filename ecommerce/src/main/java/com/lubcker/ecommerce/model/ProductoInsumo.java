package com.lubcker.ecommerce.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

// Una linea de la "receta" de un producto: cuanto de un insumo se usa para fabricar UNA unidad
// del producto (1 kg, 1 litro, 1 unidad... segun la unidad de medida del producto).
@Entity
@Table(name = "producto_insumo",
		uniqueConstraints = @UniqueConstraint(columnNames = {"id_producto", "id_insumo"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductoInsumo {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int idProductoInsumo;

	@ManyToOne(optional = false)
	@JoinColumn(name = "id_producto")
	private Producto producto;

	@ManyToOne(optional = false)
	@JoinColumn(name = "id_insumo")
	private Insumo insumo;

	// En la unidad del insumo, por cada unidad del producto. Admite decimales (ej: 0,25 kg).
	@Column(nullable = false, precision = 14, scale = 4)
	private BigDecimal cantidad;
}
