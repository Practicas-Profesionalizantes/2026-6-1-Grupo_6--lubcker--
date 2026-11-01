package com.lubcker.ecommerce.model;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "resenas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Resenas {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int idResenas;

	@ManyToOne
	@JoinColumn(name = "id_cliente")
	private Cliente cliente;

	@ManyToOne
	@JoinColumn(name = "id_producto")
	private Producto producto;

	private int valoracion;
	private String descripcion;
}