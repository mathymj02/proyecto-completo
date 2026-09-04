# Pedidos360

Sistema de pedidos con autenticación Azure AD (OIDC), backend en microservicios
Spring Boot y frontend en Angular con MSAL.

```
proyecto-final/
├── backend/     → 3 microservicios Spring Boot (auth-api, catalogo-api, carrito-api)
│                  Ver backend/README.md para arquitectura, endpoints y guía de aprendizaje.
├── frontend/    → Angular + MSAL (login, guards, interceptor)
│                  Ver frontend/README.md para arquitectura y errores comunes.
└── postman/     → Colección + environment para probar los 3 APIs
```

## Inicio rápido

```bash
# Backend (3 terminales / 3 "Run" en IntelliJ)
cd backend/auth-api && mvn spring-boot:run       # puerto 8083
cd backend/catalogo-api && mvn spring-boot:run   # puerto 8081
cd backend/carrito-api && mvn spring-boot:run    # puerto 8082

# Frontend
cd frontend
npm install
npm start   # http://localhost:4200
```

Antes de correr el frontend, reemplaza `clientId` y `apiScopes` en
`frontend/src/environments/environment.ts` con los de tu propia App
Registration en Azure (ver `frontend/README.md`, sección de configuración).

## Documentación detallada
- [`backend/README.md`](./backend/README.md) — endpoints, arquitectura de
  3 capas, guía de aprendizaje de Spring Security + JWT.
- [`frontend/README.md`](./frontend/README.md) — arquitectura de MSAL en
  Angular, comparación con React, tabla de errores comunes.
