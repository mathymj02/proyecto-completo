package com.examen.authapi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Esta es la clase mas importante de seguridad del servicio. Define:
 *   1. Que rutas requieren estar autenticado y cuales son publicas.
 *   2. Como se valida el JWT que llega en el header "Authorization: Bearer ...".
 *   3. Que origenes (dominios del frontend) pueden llamar a esta API (CORS).
 *
 * @Configuration le dice a Spring "esta clase define beans, procesala al
 * arrancar". @EnableWebSecurity activa el modulo de Spring Security (si no
 * la pones, Spring Security no se activa aunque este en el pom.xml).
 *
 * "Resource Server" es el nombre tecnico (del estandar OAuth2/OIDC) para un
 * backend que NO genera tokens, solo los RECIBE y VALIDA. Quien genera los
 * tokens en nuestro caso es Azure AD (el "Authorization Server"), no
 * nosotros. Por eso nunca vas a ver un endpoint "/login" aca: el login pasa
 * en el frontend via MSAL, contra Azure directamente.
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(AzureAdProperties.class) // activa la clase de config de la seccion "azure:"
public class SecurityConfig {

    /**
     * Lista de origenes (protocolo + dominio + puerto) desde donde el
     * NAVEGADOR puede llamar a esta API sin que CORS lo bloquee.
     * Viene de application.yml, propiedad "cors.allowed-origins", como un
     * String separado por comas. La expresion "#{'...'.split(',')}" es SpEL
     * (Spring Expression Language): toma el string ya resuelto y lo separa
     * en una List<String> usando el metodo split() de Java, como cualquier
     * String normal.
     */
    @Value("#{'${cors.allowed-origins}'.split(',')}")
    private List<String> allowedOrigins;

    /**
     * Spring detecta que este constructor pide un AzureAdProperties y busca
     * automaticamente ese bean (ya creado gracias a @EnableConfigurationProperties)
     * para "inyectarlo" aca. Esto se llama "inyeccion de dependencias por
     * constructor" y es la forma recomendada en Spring moderno (mejor que
     * @Autowired en el campo, porque hace el objeto inmutable con "final" y
     * es mas facil de testear).
     */
    private final AzureAdProperties azureAdProperties;

    public SecurityConfig(AzureAdProperties azureAdProperties) {
        this.azureAdProperties = azureAdProperties;
    }

    /**
     * Define la "cadena de filtros" de seguridad: por cada peticion HTTP que
     * llega, Spring Security la pasa por una serie de filtros en orden antes
     * de dejarla llegar al controller. Aca configuramos que hace cada filtro.
     *
     * @Bean le dice a Spring "el resultado de este metodo es un objeto que
     * quiero que administres tu (un singleton, por defecto), y que otros
     * puedan pedir por inyeccion de dependencias si lo necesitan".
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CSRF (Cross-Site Request Forgery) es una proteccion pensada para
            // apps que usan SESIONES de navegador con cookies. Como nosotros
            // usamos JWT sin sesion (stateless), no aplica y hay que desactivarla,
            // si no, Spring bloqueria los POST/PUT/DELETE.
            .csrf(csrf -> csrf.disable())

            // Habilita CORS usando la configuracion que definimos mas abajo
            // en corsConfigurationSource().
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // STATELESS = "no guardes sesion de este usuario en el servidor".
            // Cada peticion debe traer su propio JWT y probar quien es por su
            // cuenta; el servidor no recuerda nada entre una peticion y otra.
            // Esto es clave en APIs REST: las hace mas faciles de escalar
            // (puedes tener 10 instancias del mismo backend detras de un load
            // balancer sin que importe cual atiende cada request).
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Reglas de autorizacion, se evaluan en orden de arriba hacia abajo:
            .authorizeHttpRequests(auth -> auth
                // Estas rutas NO requieren token (permitAll = libre acceso).
                .requestMatchers("/api/v1/auth/public/**", "/actuator/health").permitAll()
                // Cualquier otra ruta ("anyRequest") exige estar autenticado
                // con un JWT valido.
                .anyRequest().authenticated()
            )

            // Le dice a Spring Security que la forma de autenticarse en esta
            // API es via JWT (no login con usuario/contraseña, no OAuth2
            // login con redireccion). Y que use NUESTRO jwtDecoder() (con
            // las validaciones custom de audience) en vez del que Spring
            // arma automaticamente por defecto.
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(jwtDecoder())));

        return http.build();
    }

    /**
     * El JwtDecoder es el objeto responsable de:
     *   1. Tomar el string crudo del token (lo que viene despues de "Bearer ").
     *   2. Verificar su firma digital contra las llaves publicas de Azure AD
     *      (que se descargan automaticamente desde el "JWK Set" — piensa en
     *      esto como el "candado publico" con el que Azure firmo el token;
     *      si la firma no calza, alguien altero el token o no lo emitio Azure).
     *   3. Parsear sus claims (subject, expiracion, audience, roles, etc) a
     *      un objeto Jwt que despues podemos leer en los controllers.
     *   4. Correr una cadena de validaciones adicionales (las que armamos
     *      abajo) y rechazar el token si alguna falla.
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        // Esta URL es publica y la expone Azure AD por cada tenant: contiene
        // las llaves publicas actuales para verificar firmas. Spring la
        // consulta (y cachea) automaticamente, no hay que hacer nada mas.
        String jwkSetUri = "https://login.microsoftonline.com/" + azureAdProperties.getTenantId()
                + "/discovery/v2.0/keys";

        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();

        // JwtTimestampValidator: rechaza el token si ya vencio (exp) o si
        // todavia no deberia poder usarse (nbf = "not before").
        OAuth2TokenValidator<Jwt> withTimestamp = new JwtTimestampValidator();

        // JwtIssuerValidator: rechaza el token si el campo "iss" no coincide
        // EXACTAMENTE con el issuer configurado. Este es el punto que mas
        // suele fallar al integrar con Azure AD (ver README, seccion de
        // troubleshooting sobre tokens v1 vs v2).
        OAuth2TokenValidator<Jwt> withIssuer = new DelegatingOAuth2TokenValidator<>(
                new JwtIssuerValidator(azureAdProperties.getIssuerV2())
        );

        // Nuestro validador custom (ver AudienceValidator.java) para el "aud".
        OAuth2TokenValidator<Jwt> withAudience = new AudienceValidator(azureAdProperties.getExpectedAudience());

        // DelegatingOAuth2TokenValidator ejecuta TODOS los validadores que le
        // pasas, en orden, y falla apenas alguno falla (comportamiento tipo
        // "AND" logico entre todas las reglas).
        OAuth2TokenValidator<Jwt> combined = new DelegatingOAuth2TokenValidator<>(
                withTimestamp, withIssuer, withAudience
        );

        decoder.setJwtValidator(combined);
        return decoder;
    }

    /**
     * Configura la politica CORS del servicio. CORS es una proteccion que
     * viven en el NAVEGADOR (no en el backend ni en Postman/curl): evita que
     * una pagina web de un dominio "engañe" al navegador para que llame,
     * usando las cookies/sesion del usuario, a una API de OTRO dominio sin
     * permiso. Como nuestro frontend (React) corre en un dominio distinto al
     * backend, hay que autorizarlo explicitamente aca.
     */
    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Solo estos dominios pueden llamar a la API desde el navegador.
        configuration.setAllowedOrigins(allowedOrigins);

        // Metodos HTTP permitidos. OPTIONS es necesario porque el navegador
        // manda una peticion "preflight" OPTIONS automatica antes del
        // POST/PUT/DELETE real, para preguntar "¿me dejas hacer esto?".
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // Headers que el frontend tiene permitido enviar. Authorization es
        // el que lleva el JWT; Content-Type es necesario para mandar JSON.
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        // Permite que el navegador incluya credenciales (cookies, headers de
        // auth) en las peticiones cross-origin.
        configuration.setAllowCredentials(true);

        // Aplica esta configuracion de CORS a TODAS las rutas ("/**") del
        // servicio.
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
