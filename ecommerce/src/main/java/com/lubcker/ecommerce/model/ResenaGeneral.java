package com.lubcker.ecommerce.model;
import jakarta.persistence.*;
import lombok.*;

// Opinion de un cliente sobre Lubcker en general (no de un producto). Una por cliente.
@Entity
@Table(name = "resena_general")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResenaGeneral {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int idResenaGeneral;

	@OneToOne
	@JoinColumn(name = "id_cliente", nullable = false, unique = true)
	private Cliente cliente;

	private int valoracion;
	private String descripcion;
}
