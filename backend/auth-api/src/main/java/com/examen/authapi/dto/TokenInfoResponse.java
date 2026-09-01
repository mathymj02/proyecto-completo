package com.examen.authapi.dto;

import java.time.Instant;
import java.util.Map;

/**
 * "DTO" = Data Transfer Object: una clase cuyo unico proposito es cargar
 * datos entre capas (aca, del controller hacia el JSON de la respuesta).
 * No tiene logica de negocio, solo datos.
 *
 * Usamos un "record" de Java (disponible desde Java 16+): es una forma
 * abreviada de declarar una clase inmutable con campos finales, constructor,
 * getters (sin el prefijo "get", ej. valido() en vez de getValido()),
 * equals(), hashCode() y toString() generados automaticamente. Perfecto para
 * DTOs, porque son objetos de "solo lectura" que arma el backend para
 * mandarlos al frontend.
 *
 * Cuando el controller devuelve un TokenInfoResponse, Spring (a traves de la
 * libreria Jackson, incluida automaticamente por spring-boot-starter-web) lo
 * convierte solo a JSON antes de mandarlo por HTTP. No hay que escribir
 * ningun codigo de serializacion a mano.
 */
public record TokenInfoResponse(
        boolean valido,
        String subject,            // el "sub" o identificador unico del usuario en el token
        String nombre,              // claim "name" de Azure AD
        String preferredUsername,   // claim "preferred_username" (suele ser el email/UPN)
        String issuer,               // quien emitio el token (deberia ser Azure AD)
        Instant emitidoEn,           // claim "iat" (issued at)
        Instant expiraEn,            // claim "exp" (expiration)
        Map<String, Object> claims   // TODOS los claims del token, para inspeccion libre
) {}
