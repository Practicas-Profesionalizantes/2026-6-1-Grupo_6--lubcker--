package com.lubcker.ecommerce.model;
import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "empresa_logistica")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Empresa_Logistica {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int idEmpresa;

	private String nombre;
	private String sucursal;

	@OneToMany(mappedBy = "empresaLogistica")
	private List<Envio> envios;
}