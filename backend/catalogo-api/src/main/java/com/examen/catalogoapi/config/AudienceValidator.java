package com.examen.catalogoapi.config;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Identico en logica al de auth-api: verifica que el "aud" del JWT sea el
 * de ESTA api. Ver auth-api/config/AudienceValidator.java para la
 * explicacion linea por linea de por que hace falta este validador custom.
 */
public class AudienceValidator implements OAuth2TokenValidator<Jwt> {

    private final String expectedAudience;

    public AudienceValidator(String expectedAudience) {
        this.expectedAudience = expectedAudience;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        if (jwt.getAudience() != null && jwt.getAudience().contains(expectedAudience)) {
            return OAuth2TokenValidatorResult.success();
        }
        OAuth2Error error = new OAuth2Error(
                "invalid_token",
                "El token trae audience " + jwt.getAudience() + " pero se esperaba: " + expectedAudience,
                null
        );
        return OAuth2TokenValidatorResult.failure(error);
    }
}
