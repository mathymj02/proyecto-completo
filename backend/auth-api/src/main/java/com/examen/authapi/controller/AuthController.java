package com.examen.authapi.controller;

import com.examen.authapi.dto.TokenInfoResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * @RestController = @Controller + @ResponseBody. Le dice a Spring:
 *   1. Esta clase maneja peticiones HTTP (@Controller).
 *   2. Lo que devuelvan sus metodos se escribe DIRECTO en el body de la
 *      respuesta como JSON (@ResponseBody), en vez de interpretarlo como el
 *      nombre de una vista HTML (que es el comportamiento de @Controller
 *      "puro", pensado para apps web tradicionales con Thymeleaf/JSP).
 *
 * @RequestMapping("/api/v1/auth") define el prefijo comun de TODAS las rutas
 * de esta clase: cada @GetMapping/@PostMapping de aca abajo se concatena a
 * este prefijo.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    /**
     * Endpoint publico (ver SecurityConfig: "/api/v1/auth/public/**" esta en
     * permitAll()). Sirve para comprobar rapido que el servicio esta arriba
     * y respondiendo, sin necesitar ningun token. Un "health check" clasico.
     */
    @GetMapping("/public/ping")
    public ResponseEntity<Map<String, String>> ping() {
        return ResponseEntity.ok(Map.of("status", "auth-api up"));
    }

    /**
     * Este es EL endpoint clave para el examen: "una API que me devuelva el
     * token de auth". Aca no generamos un token nuevo (eso lo hace Azure AD,
     * no nosotros) — lo que hacemos es CONFIRMAR y DEVOLVER la informacion
     * que contiene el token que ya nos mando el frontend.
     *
     * Flujo completo de la peticion:
     *   1. El frontend (React + MSAL) hace GET a este endpoint con el header
     *      "Authorization: Bearer <jwt>".
     *   2. ANTES de que este metodo se ejecute, Spring Security intercepta
     *      la peticion, extrae el token, lo valida (firma, expiracion,
     *      issuer, audience — ver SecurityConfig y AudienceValidator) y, si
     *      es valido, lo convierte en un objeto Jwt.
     *   3. Si el token NO es valido, Spring corta la cadena ahi mismo y
     *      responde 401 Unauthorized. Este metodo ni siquiera se llega a
     *      ejecutar — por eso no necesitamos validar nada "a mano" aca dentro.
     *   4. Si es valido, Spring nos "inyecta" ese objeto Jwt directamente
     *      como parametro gracias a @AuthenticationPrincipal.
     *
     * @param jwt El token ya decodificado y validado. AuthenticationPrincipal
     *            le dice a Spring "dame el objeto que representa al usuario
     *            autenticado actual", que en un resource server JWT es
     *            justamente una instancia de Jwt.
     */
    @GetMapping("/me")
    public ResponseEntity<TokenInfoResponse> me(@AuthenticationPrincipal Jwt jwt) {
        TokenInfoResponse response = new TokenInfoResponse(
                true,
                jwt.getSubject(),                                  // claim "sub"
                jwt.getClaimAsString("name"),                      // claim "name"
                jwt.getClaimAsString("preferred_username"),        // claim "preferred_username"
                jwt.getIssuer() != null ? jwt.getIssuer().toString() : null, // claim "iss"
                jwt.getIssuedAt(),                                 // claim "iat"
                jwt.getExpiresAt(),                                // claim "exp"
                jwt.getClaims()                                    // TODOS los claims, como Map
        );
        return ResponseEntity.ok(response);
    }
}
