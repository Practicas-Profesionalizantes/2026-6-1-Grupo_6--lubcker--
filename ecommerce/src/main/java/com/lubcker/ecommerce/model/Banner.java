package com.lubcker.ecommerce.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

// Imagen del carrusel de la pagina principal. La carga el admin desde "Gestionar carrusel".
@Entity
@Table(name = "banner")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Banner {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int idBanner;

	// Ruta de la imagen subida (ej: "uploads/abc.jpg")
	@Column(nullable = false)
	private String imagen;

	// Texto que se muestra sobre la imagen (opcional)
	private String titulo;

	// Ruta interna a la que lleva al hacer clic, ej: "/producto/5" (opcional)
	private String enlace;

	// Posicion en el carrusel (de menor a mayor)
	@Column(name = "orden")
	private int orden;

	// Un banner inactivo se conserva pero no se muestra
	@ColumnDefault("1")
	@Column(nullable = false)
	private boolean activo = true;
}
