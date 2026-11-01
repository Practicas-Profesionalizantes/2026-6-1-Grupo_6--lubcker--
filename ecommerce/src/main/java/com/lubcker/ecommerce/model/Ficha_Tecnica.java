package com.lubcker.ecommerce.model;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ficha_tecnica")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Ficha_Tecnica {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int idFicha;

	@OneToOne
	@JoinColumn(name = "id_producto")
	private Producto producto;

	private String modelo;
	private String presentacion;
	private String descripcion;
}