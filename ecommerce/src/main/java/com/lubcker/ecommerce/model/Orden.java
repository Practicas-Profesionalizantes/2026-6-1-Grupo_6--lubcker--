package com.lubcker.ecommerce.model;
import jakarta.persistence.*;
import lombok.*;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "orden")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Orden {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int idOrden;

	@ManyToOne
	@JoinColumn(name = "id_cliente")
	private Cliente cliente;

	private Date fecha;

	@OneToMany(mappedBy = "orden")
	private List<Detalle_Orden> detalles;

	@OneToMany(mappedBy = "orden")
	private List<Pago> pagos;

	@OneToOne(mappedBy = "orden")
	private Envio envio;
}