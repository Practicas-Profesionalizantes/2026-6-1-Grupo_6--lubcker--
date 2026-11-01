package com.lubcker.ecommerce.model;
import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "categoria")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Categoria {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int idCategoria;

	private String nombre;

	@OneToMany(mappedBy = "categoria")
	private List<Producto> productos;
}