package com.lubcker.ecommerce.model;

import java.util.List;

// Rubros que se ofrecen al registrarse. Es la unica fuente: el frontend los pide a
// GET /api/negocios/rubros, asi que para sumar uno alcanza con agregarlo aca.
public final class Rubros {

    public static final List<String> TODOS = List.of(
            "Taller mecánico de autos",
            "Concesionario",
            "Taller de motos",
            "Lubricentro",
            "Gomería",
            "Lavadero de autos",
            "Taller de chapa y pintura",
            "Taller de electricidad del automotor",
            "Rectificadora de motores",
            "Taller de camiones y vehículos pesados",
            "Taller de maquinaria agrícola",
            "Taller náutico",
            "Estación de servicio",
            "Industria o taller metalúrgico",
            "Otro"
    );

    private Rubros() {
    }

    public static boolean esValido(String rubro) {
        return rubro != null && TODOS.contains(rubro);
    }
}
