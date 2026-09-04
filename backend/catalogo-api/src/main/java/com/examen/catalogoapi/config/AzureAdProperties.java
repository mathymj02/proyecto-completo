package com.examen.catalogoapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Igual que en auth-api: mapea la seccion "azure:" de application.yml a un
 * objeto Java tipado. Ver el comentario detallado en
 * auth-api/config/AzureAdProperties.java para la explicacion completa de
 * @ConfigurationProperties y el "relaxed binding" kebab-case <-> camelCase.
 *
 * Nota: cada microservicio tiene SU PROPIA copia de esta clase (no la
 * compartimos como una libreria comun) para mantener los 3 servicios
 * totalmente independientes entre si, sin dependencias cruzadas — asi
 * podrias desplegar, escalar o incluso reescribir uno sin tocar los otros.
 * En un proyecto mas grande, esto se resolveria con una libreria compartida
 * ("commons" o "shared-security") publicada en un repositorio Maven interno.
 */
@ConfigurationProperties(prefix = "azure")
public class AzureAdProperties {

    private String tenantId;
    private String clientId;
    private String expectedAudience;
    private String issuerV1;
    private String issuerV2;

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getExpectedAudience() { return expectedAudience; }
    public void setExpectedAudience(String expectedAudience) { this.expectedAudience = expectedAudience; }

    public String getIssuerV1() { return issuerV1; }
    public void setIssuerV1(String issuerV1) { this.issuerV1 = issuerV1; }

    public String getIssuerV2() { return issuerV2; }
    public void setIssuerV2(String issuerV2) { this.issuerV2 = issuerV2; }
}
