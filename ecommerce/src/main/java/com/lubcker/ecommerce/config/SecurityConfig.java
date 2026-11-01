package com.lubcker.ecommerce.config;

import com.lubcker.ecommerce.security.JwtAuthFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

// Reglas de acceso. Se evalúan en orden y gana la primera que coincide.
// Lo que NO esta listado explicitamente queda como SOLO ADMIN (ultima regla): asi un
// endpoint nuevo nunca queda publico por olvido.
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // El login lo resuelve AuthController con tokens. Este bean vacio evita que Spring cree
    // un usuario "user" con contraseña aleatoria por defecto (que no se usa).
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            throw new UsernameNotFoundException("No se usa: el login es por token");
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // API sin cookies de sesion: el token va en el header
            .cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req, res, ex) -> escribirJson(res, 401, "Tenés que iniciar sesión."))
                .accessDeniedHandler((req, res, ex) -> escribirJson(res, 403, "No tenés permiso para hacer esto.")))
            .authorizeHttpRequests(a -> a
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // ---- Con sesion, aunque esten bajo rutas de lectura publica ----
                .requestMatchers(HttpMethod.GET, "/api/resenas/mias", "/api/resenas/estado/*", "/api/resenas-generales/mia").authenticated()

                // ---- Publico ----
                .requestMatchers("/uploads/**").permitAll()
                // Documentacion de la API (se apaga con SWAGGER_HABILITADO=false)
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/registro", "/api/chat").permitAll()
                // Aviso de Mercado Pago: no lleva sesion; el servicio verifica el pago contra la API de MP
                .requestMatchers(HttpMethod.POST, "/api/mercadopago/webhook").permitAll()
                .requestMatchers(HttpMethod.GET,
                        "/api/productos/**", "/api/categorias/**", "/api/fichas-tecnicas/**",
                        "/api/resenas/**", "/api/resenas-generales", "/api/metodos-pago/**", "/api/negocios/rubros", "/api/banners").permitAll()

                // ---- Solo admin (antes de las reglas generales de /api/direcciones) ----
                .requestMatchers(HttpMethod.GET, "/api/direcciones").hasRole("ADMIN")

                // ---- Usuario con sesion (los controllers verifican que el dato sea suyo) ----
                .requestMatchers(HttpMethod.POST, "/api/checkout", "/api/archivos", "/api/resenas").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/resenas-generales").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/mercadopago/confirmar", "/api/mercadopago/orden/*/link",
                        "/api/mercadopago/orden/*/sincronizar").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/resenas/*", "/api/resenas-generales/*").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/resenas/*", "/api/resenas-generales/*").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/ordenes/cliente/*", "/api/pagos/cliente/*",
                        "/api/envios/cliente/*").authenticated()
                .requestMatchers("/api/direcciones/**").authenticated()
                .requestMatchers("/api/favoritos", "/api/favoritos/**").authenticated()
                .requestMatchers("/api/negocios/mio").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/usuarios/*", "/api/clientes/*").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/usuarios/*", "/api/clientes/*").authenticated()

                // ---- Todo lo demas: solo admin ----
                .anyRequest().hasRole("ADMIN"))
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // Origenes permitidos (frontend). En produccion: APP_CORS_ORIGINS=https://tu-app.vercel.app
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.origins:http://localhost:3000}") String origenes) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(origenes.split(",")).map(String::trim).toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/api/**", config);
        return fuente;
    }

    private void escribirJson(jakarta.servlet.http.HttpServletResponse res, int estado, String mensaje)
            throws java.io.IOException {
        res.setStatus(estado);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        res.getWriter().write("{\"mensaje\":\"" + mensaje + "\"}");
    }
}
