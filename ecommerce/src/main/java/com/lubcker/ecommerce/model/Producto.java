package com.lubcker.ecommerce.model;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "producto")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Producto {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int idProducto;

	@ManyToOne
	@JoinColumn(name = "id_categoria")
	private Categoria categoria;

	@OneToMany(mappedBy = "producto")
	private List<Resenas> resenas;

	@OneToOne(mappedBy = "producto")
	private Ficha_Tecnica fichaTecnica;

	@OneToMany(mappedBy = "producto")
	private List<Detalle_Orden> detalles;

	private String nombre;
	// Precio de UNA unidad de medida (ej: de 1 kg). El precio final es precio x medida elegida.
	private int precio;
	private String imagen;

	// Unidad en la que se mide el producto: unidad, kg, litro o metro. El stock se cuenta en esta unidad.
	@ColumnDefault("'unidad'")
	@Column(nullable = false, length = 20)
	private String unidadMedida = "unidad";

	// Medidas en las que se vende, separadas por coma. Ej: "3,5,10" con unidad kg = se vende de 3, 5 o 10 kg.
	@ColumnDefault("'1'")
	@Column(nullable = false)
	private String presentaciones = "1";

	// Stock disponible (en la unidad de medida). Se descuenta al comprar y se devuelve si se cancela el envio.
	@ColumnDefault("0")
	@Column(nullable = false)
	private int stock;

	// Un producto inactivo (descontinuado) no se muestra ni se vende, pero se conserva
	// porque puede estar en pedidos viejos.
	@ColumnDefault("1")
	@Column(nullable = false)
	private boolean activo = true;

	// Las medidas de venta como lista de numeros (si no hay ninguna valida, se vende de a 1)
	public List<Integer> listaPresentaciones() {
		List<Integer> lista = new ArrayList<>();
		if (presentaciones != null) {
			for (String p : presentaciones.split(",")) {
				try {
					int valor = Integer.parseInt(p.trim());
					if (valor > 0) lista.add(valor);
				} catch (NumberFormatException ignorada) {
					// se saltea
				}
			}
		}
		if (lista.isEmpty()) lista.add(1);
		return lista;
	}
}