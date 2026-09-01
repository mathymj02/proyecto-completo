package com.examen.carritoapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

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
