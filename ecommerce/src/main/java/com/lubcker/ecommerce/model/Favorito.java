package com.lubcker.ecommerce.model;

import jakarta.persistence.*;
import lombok.*;

// Producto marcado con ❤️ por un cliente. Un mismo producto una sola vez por cliente.
@Entity
@Table(name = "favorito",
       uniqueConstraints = @UniqueConstraint(columnNames = {"id_cliente", "id_producto"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Favorito {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int idFavorito;

	@ManyToOne(optional = false)
	@JoinColumn(name = "id_cliente")
	private Cliente cliente;

	@ManyToOne(optional = false)
	@JoinColumn(name = "id_producto")
	private Producto producto;
}
