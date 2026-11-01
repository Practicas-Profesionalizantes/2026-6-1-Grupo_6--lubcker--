package com.lubcker.ecommerce.model;
import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "direccion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Direccion {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int idDireccion;

	@ManyToOne
	@JoinColumn(name = "id_cliente")
	private Cliente cliente;

	private String provincia;
	private String ciudad;
	private String calle;
	private int altura;

	@OneToMany(mappedBy = "direccion")
	private List<Envio> envios;
}