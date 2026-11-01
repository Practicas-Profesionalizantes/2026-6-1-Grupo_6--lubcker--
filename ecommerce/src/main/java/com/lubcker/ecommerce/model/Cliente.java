package com.lubcker.ecommerce.model;
import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "cliente")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int idCliente;

    private String nombre;
    private String apellido;
    // Para coordinar la entrega (el dueño le avisa por WhatsApp)
    private String telefono;

    @OneToOne(mappedBy = "cliente")
    private Usuario usuario;

    @OneToMany(mappedBy = "cliente")
    private List<Direccion> direcciones;

    @OneToMany(mappedBy = "cliente")
    private List<Orden> ordenes;

    @OneToMany(mappedBy = "cliente")
    private List<Resenas> resenas;
}