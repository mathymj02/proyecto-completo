package com.examen.authapi.config;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Un JWT (JSON Web Token) trae un claim "aud" (audience) que dice PARA QUIEN
 * fue emitido ese token. Por ejemplo, un usuario puede loguearse una vez con
 * Azure AD y recibir tokens distintos segun a que API vaya a llamar
 * (uno para Microsoft Graph, otro para nuestra propia API, etc).
 *
 * Spring Security, por defecto, SOLO valida:
 *   - que la firma del token sea autentica (que de verdad la genero Azure AD)
 *   - que no este vencido (exp) ni sea usado antes de tiempo (nbf)
 *   - que el "issuer" (quien lo emitio) sea el esperado
 *
 * Pero NO valida el "audience" por defecto. Si no lo validaramos, un token
 * valido pero emitido para OTRA API (ej. Graph) tambien pasaria como valido
 * aca, lo cual es un hueco de seguridad. Por eso escribimos este validador
 * manualmente e implementamos la interfaz OAuth2TokenValidator<Jwt>.
 *
 * Este objeto se "engancha" al JwtDecoder en SecurityConfig, combinado con
 * los validadores por defecto (timestamp, issuer), usando
 * DelegatingOAuth2TokenValidator.
 */
public class AudienceValidator implements OAuth2TokenValidator<Jwt> {

    /** El audience que ESTA api considera valido (viene de application.yml). */
    private final String expectedAudience;

    public AudienceValidator(String expectedAudience) {
        this.expectedAudience = expectedAudience;
    }

    /**
     * Spring Security llama este metodo automaticamente por cada request,
     * DESPUES de verificar la firma del token pero ANTES de dejar pasar la
     * peticion al controller.
     *
     * jwt.getAudience() devuelve una lista (un token puede tener varios
     * audiences), por eso usamos .contains() en vez de comparar con ==.
     *
     * @return success() si el audience esperado esta en la lista del token,
     *         o failure() con un error especifico si no (Spring Security
     *         traduce eso automaticamente en una respuesta 401).
     */
    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        if (jwt.getAudience() != null && jwt.getAudience().contains(expectedAudience)) {
            return OAuth2TokenValidatorResult.success();
        }

        OAuth2Error error = new OAuth2Error(
                "invalid_token",
                "El token no tiene la audiencia esperada: " + expectedAudience,
                null
        );
        return OAuth2TokenValidatorResult.failure(error);
    }
}
