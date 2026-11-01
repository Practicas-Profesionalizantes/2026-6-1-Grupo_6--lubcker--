package com.lubcker.ecommerce.model;

import jakarta.persistence.*;
import lombok.*;

// Datos del negocio del cliente (se piden al registrarse y se editan desde "Mi Perfil").
@Entity
@Table(name = "negocio")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Negocio {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int idNegocio;

	@OneToOne(optional = false)
	@JoinColumn(name = "id_cliente", unique = true)
	private Cliente cliente;

	private String nombre;
	private String rubro;

	// Direccion del local
	private String provincia;
	private String ciudad;
	private String calle;
	private int altura;
}
