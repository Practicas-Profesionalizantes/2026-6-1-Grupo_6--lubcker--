package com.lubcker.ecommerce.model;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int idUsuario;

	@OneToOne
	@JoinColumn(name = "id_cliente")
	private Cliente cliente;

	private String nombreUsuario;
	private String email;
	private String contrasena;

	// "CLIENTE" o "ADMIN". Si viene null (usuarios viejos) se trata como CLIENTE.
	private String rol;

	// Ruta de la foto subida (ej: "uploads/abc.jpg"). Null si no tiene foto.
	private String fotoPerfil;

	// Un usuario desactivado por el admin no puede iniciar sesion (sus datos se conservan).
	@ColumnDefault("1")
	@Column(nullable = false)
	private boolean activo = true;
}