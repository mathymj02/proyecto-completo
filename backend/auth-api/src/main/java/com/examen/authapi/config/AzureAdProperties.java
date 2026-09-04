package com.examen.authapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Esta clase es un "espejo" en Java de la seccion "azure:" del archivo
 * application.yml. Spring la llena automaticamente al arrancar gracias a
 * @ConfigurationProperties(prefix = "azure").
 *
 * ¿Por que no usar @Value directamente en SecurityConfig, como hicimos con
 * "cors.allowed-origins"? Porque cuando tienes VARIAS propiedades relacionadas
 * (tenant-id, client-id, audience, issuer...) es mas prolijo agruparlas en
 * una sola clase, en vez de tener 5 campos @Value sueltos repartidos por el
 * codigo. Ademas @ConfigurationProperties valida tipos y hace "type-safe
 * binding": si en el yml escribes mal un nombre, Spring te avisa al arrancar.
 *
 * Relacion nombre-yml <-> nombre-java (esto se llama "relaxed binding"):
 *   azure.tenant-id          ->  tenantId
 *   azure.client-id          ->  clientId
 *   azure.expected-audience  ->  expectedAudience
 *   azure.issuer-v1          ->  issuerV1
 *   azure.issuer-v2          ->  issuerV2
 * Spring convierte automaticamente "kebab-case" (con guiones, como se
 * acostumbra en YAML) a "camelCase" (como se acostumbra en Java).
 *
 * Nota: esta clase necesita ser "activada" en algun lado con
 * @EnableConfigurationProperties(AzureAdProperties.class) — eso lo hicimos
 * en SecurityConfig.
 */
@ConfigurationProperties(prefix = "azure")
public class AzureAdProperties {

    /** Identificador de tu tenant (organizacion) en Azure Active Directory / Entra ID. */
    private String tenantId;

    /** Identificador (Application ID) de tu App Registration en Azure. */
    private String clientId;

    /**
     * El "audience" (aud) que debe traer el token para ser valido en ESTA api.
     * Sirve para que un token pensado para otra API (ej. Microsoft Graph) no
     * pueda usarse aqui por error/abuso.
     */
    private String expectedAudience;

    /** Issuer esperado si tu app registrada emite tokens formato v1 (opcional). */
    private String issuerV1;

    /** Issuer esperado si tu app registrada emite tokens formato v2 (el mas comun hoy). */
    private String issuerV2;

    // --- Getters y setters ---
    // Spring los necesita para poder "inyectar" (setear) cada valor leido
    // del yml. Sin estos metodos, @ConfigurationProperties no puede escribir
    // en los campos privados.

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
