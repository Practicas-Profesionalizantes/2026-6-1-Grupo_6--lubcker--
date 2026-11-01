package com.lubcker.ecommerce.model;
import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "metodo_pago")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MetodoPago {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int idMetodo;

	private String nombre;

	@OneToMany(mappedBy = "metodoPago")
	private List<Pago> pagos;
}