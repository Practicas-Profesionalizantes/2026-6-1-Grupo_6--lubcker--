package com.lubcker.ecommerce.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Datos generales de la documentacion de la API (Swagger UI: /swagger-ui.html).
// Para probar las rutas protegidas: hacer POST /api/auth/login, copiar el "token" de la respuesta
// y pegarlo en el boton "Authorize" (sin escribir "Bearer").
@Configuration
public class OpenApiConfig {

    private static final String ESQUEMA_JWT = "tokenJwt";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API de Lubcker")
                        .version("1.0")
                        .description("""
                                API REST de la tienda online Lubcker (productos de higiene y seguridad para talleres).

                                **Autenticación:** las rutas marcadas como *Sesión* o *Admin* piden el token JWT que \
                                devuelve `POST /api/auth/login`. Se envía en el encabezado `Authorization: Bearer <token>`. \
                                El token vence a las 8 horas.

                                **Acceso:** cada operación indica en su descripción si es *Público*, requiere *Sesión* \
                                (cliente o admin) o es solo para *Admin*. Lo que no está listado explícitamente en la \
                                configuración de seguridad queda como solo Admin.

                                **Errores:** los mensajes de error vienen como `{"mensaje": "..."}`.""")
                        .contact(new Contact().name("Equipo Lubcker")))
                .components(new Components().addSecuritySchemes(ESQUEMA_JWT, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Token que devuelve POST /api/auth/login")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_JWT));
    }
}
