package com.lubcker.ecommerce.model;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "pago")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Pago {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int idPago;

	// ManyToOne: si en tu negocio una orden se paga siempre de una sola vez,
	// podes cambiar esto (y el JoinColumn) a @OneToOne.
	@ManyToOne
	@JoinColumn(name = "id_orden")
	private Orden orden;

	@ManyToOne
	@JoinColumn(name = "id_metodo_pago")
	private MetodoPago metodoPago;

	private boolean estado;
	private int monto;

	// Id del pago en Mercado Pago (se completa cuando el pago se aprueba)
	private String mpPaymentId;
}