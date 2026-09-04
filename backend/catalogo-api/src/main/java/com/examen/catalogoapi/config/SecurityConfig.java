package com.examen.catalogoapi.config;

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
 * Misma logica que auth-api/config/SecurityConfig.java (revisa ese archivo
 * para la explicacion linea por linea de cada pieza: JwtDecoder, validadores,
 * CORS, sessionManagement, etc). La UNICA diferencia real es que aca
 * permitimos sin token el acceso a "/h2-console/**", la consola web de la
 * base de datos H2 en memoria que usamos solo para desarrollo (para poder
 * inspeccionar visualmente las tablas mientras programas). En un ambiente
 * real de produccion, esa consola se deshabilita por completo.
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(AzureAdProperties.class)
public class SecurityConfig {

    @Value("#{'${cors.allowed-origins}'.split(',')}")
    private List<String> allowedOrigins;

    private final AzureAdProperties azureAdProperties;

    public SecurityConfig(AzureAdProperties azureAdProperties) {
        this.azureAdProperties = azureAdProperties;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // La consola H2 se muestra dentro de un <frame> HTML; por defecto
            // Spring Security bloquea que CUALQUIER pagina se muestre en un
            // frame (proteccion contra "clickjacking"). sameOrigin() permite
            // que se enmarque solo si el frame viene del mismo dominio
            // (localhost en este caso), que es justo nuestro caso de uso.
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/h2-console/**", "/actuator/health").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(jwtDecoder())));

        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        String jwkSetUri = "https://login.microsoftonline.com/" + azureAdProperties.getTenantId()
                + "/discovery/v2.0/keys";

        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();

        OAuth2TokenValidator<Jwt> withTimestamp = new JwtTimestampValidator();
        OAuth2TokenValidator<Jwt> withIssuer = new DelegatingOAuth2TokenValidator<>(
                new JwtIssuerValidator(azureAdProperties.getIssuerV2())
        );
        OAuth2TokenValidator<Jwt> withAudience = new AudienceValidator(azureAdProperties.getExpectedAudience());

        OAuth2TokenValidator<Jwt> combined = new DelegatingOAuth2TokenValidator<>(
                withTimestamp, withIssuer, withAudience
        );

        decoder.setJwtValidator(combined);
        return decoder;
    }

    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
