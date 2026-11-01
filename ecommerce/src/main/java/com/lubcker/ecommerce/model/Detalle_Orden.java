package com.lubcker.ecommerce.model;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "detalle_orden")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Detalle_Orden {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int idDetalle;

	@ManyToOne
	@JoinColumn(name = "id_orden")
	private Orden orden;

	@ManyToOne
	@JoinColumn(name = "id_producto")
	private Producto producto;

	// Cantidad de paquetes comprados de la medida "tamano" (ej: 2 paquetes de 5 kg)
	private int cantidad;

	// Medida de cada paquete, en la unidad del producto (ej: 5 = 5 kg). 1 para productos comunes.
	@ColumnDefault("1")
	@Column(nullable = false)
	private int tamano = 1;
	private int subtotal;
	// Precio de UN paquete (precio por unidad x tamano)
	private int precioUnitario;
}