# Pedidos360 — Frontend (Angular + MSAL)

Frontend Angular del sistema **Pedidos360**: autenticación con **Azure AD
(Microsoft Entra ID)** vía OAuth 2.0 / OpenID Connect usando
`@azure/msal-angular`, y consumo de un backend de microservicios en
Spring Boot (`auth-api`, `catalogo-api`, `carrito-api`).

## Stack

- **Angular 18** (componentes standalone, sin NgModules)
- **@azure/msal-angular** + **@azure/msal-browser** — autenticación OIDC
- **RxJS** — manejo de eventos asíncronos de MSAL
- **HttpClient** con interceptor automático de token

## Arquitectura de carpetas

```
src/
├── environments/
│   ├── environment.ts        → config de DESARROLLO (Azure + URLs backend)
│   └── environment.prod.ts   → config de PRODUCCIÓN (mismo shape, otros valores)
├── app/
│   ├── config/
│   │   └── auth-config.ts    → las 3 factory functions de MSAL (instancia, guard, interceptor)
│   ├── guards/
│   │   └── role.guard.ts     → guard propio, valida permisos además de sesión
│   ├── services/
│   │   ├── jwt.util.ts       → decodifica el JWT para leer claims/roles
│   │   ├── auth-api.service.ts
│   │   ├── catalogo.service.ts
│   │   └── carrito.service.ts
│   ├── components/
│   │   ├── login/            → pantalla de login
│   │   └── dashboard/        → pantalla protegida, consume las 3 APIs
│   ├── app.component.ts      → inicializa MSAL (equivalente a "main.jsx" de una SPA en React)
│   └── app.routes.ts         → rutas + guards
```

## Antes de correrlo — reemplaza estos valores

Abre `src/environments/environment.ts` y reemplaza:

```ts
clientId: 'TU-CLIENT-ID-AQUI',        // Application (client) ID de TU App Registration
apiScopes: ['api://TU-CLIENT-ID-AQUI/write-read'],
```

por los valores reales de tu propia App Registration en Azure (la que
configuraste con soporte multi-tenant + personal accounts). **No uses la
del profesor** — ver la sección de errores comunes más abajo, el error
`AADSTS50020` sale justo por esto.

## Cómo correrlo

```bash
npm install
npm start          # equivale a "ng serve", levanta en http://localhost:4200
```

Para compilar para producción:
```bash
npm run build       # usa automáticamente environment.prod.ts
```

## Requisito en Azure: Redirect URI

En tu App Registration → Authentication → agrega `http://localhost:4200/`
como Redirect URI tipo **SPA**. Angular corre en el puerto 4200 por
defecto (React/Vite corría en 5173 — si vienes de esa versión, no olvides
este cambio de puerto).

---

## 🎓 Guía de aprendizaje: MSAL en Angular vs MSAL en React

Si ya viste la versión React de este mismo frontend, esta tabla te ayuda a
mapear los conceptos — son la MISMA idea, expresada con el estilo de cada
framework:

| Concepto | React (`@azure/msal-react`) | Angular (`@azure/msal-angular`) |
|---|---|---|
| Compartir la instancia de MSAL a toda la app | `<MsalProvider instance={msalInstance}>` (Context) | Array de `providers` en `app.config.ts` (Dependency Injection) |
| Configuración de MSAL | Un objeto `msalConfig` | Una función factory `MSALInstanceFactory()` |
| Proteger una ruta/pantalla | `if (accounts.length === 0) return null` a mano | `canActivate: [MsalGuard]` en `app.routes.ts` — declarativo |
| Adjuntar el token a una petición | `fetch(url, { headers: { Authorization: ... } })` a mano en cada llamada | `MsalInterceptor` lo hace solo, según `protectedResourceMap` |
| Leer si hay sesión activa | Hook `useMsal()` / `useIsAuthenticated()` | Inyectar `MsalService` en el constructor |
| Reaccionar a eventos de MSAL | `instance.addEventCallback(...)` | `MsalBroadcastService.msalSubject$` (Observable RxJS) |

### Por dónde empezar a leer el código (orden sugerido)
1. **`auth-config.ts`** — las 3 piezas de configuración de MSAL, con
   comentarios explicando cada una.
2. **`app.config.ts`** — cómo esas 3 piezas se conectan al resto de la app.
3. **`app.routes.ts`** + **`role.guard.ts`** — cómo se protege una ruta en
   2 niveles (sesión + permiso específico).
4. **`login.component.ts`** — el flujo de login/logout y por qué se
   necesita `MsalBroadcastService` (no solo leer `getAllAccounts()` una vez).
5. **`dashboard.component.ts`** — cómo los 3 servicios (`AuthApiService`,
   `CatalogoService`, `CarritoService`) consumen el backend sin manejar el
   token a mano.

---

## ⚠️ Errores comunes y cómo se resuelven

| Error | Causa probable | Solución |
|---|---|---|
| `AADSTS50020: User account ... does not exist in tenant` | Estás usando el `clientId`/tenant de otra persona (ej. el del profesor) con tu propia cuenta Microsoft | Crea tu propia App Registration con soporte multi-tenant, o usa la cuenta específica de ese tenant |
| `AADSTS50011: redirect_uri_mismatch` | La Redirect URI de `environment.ts` no coincide exacto con la registrada en Azure (incluyendo el `/` final) | Verifica que sean idénticas en ambos lados |
| Pantalla en blanco después del login, sin error visible | `app.component.ts` no llamó `handleRedirectObservable()`, o se llamó antes de `initialize()` | Revisa que `ngOnInit()` siga el orden: `initialize().then(() => handleRedirectObservable()...)` |
| `TS2353: Object literal may only specify known properties, 'storeAuthStateInCookie'...` | Versión de `@azure/msal-browser` más nueva que quitó esa opción (era un parche viejo para IE11) | Simplemente elimina esa línea de `cache` en `auth-config.ts` — ya no hace falta (ya viene corregido en este proyecto) |
| CORS error en la consola del navegador | El backend no tiene `http://localhost:4200` en su `cors.allowed-origins` (los `application.yml` del backend tenían `5173`, el puerto de Vite/React) | Actualiza `cors.allowed-origins` en los 3 `application.yml` del backend a `4200` (o agrega ambos, separados por coma) |
| 401 al llamar cualquiera de las 3 APIs | El scope en `environment.ts` no coincide exacto con el que expone tu App Registration, o el consentimiento de administrador no fue otorgado | Revisa "Expose an API" y "Permisos de API" → "Conceder consentimiento de administrador" en Azure |
| `NullInjectorError: No provider for MSAL_INSTANCE` | Falta alguno de los `providers` en `app.config.ts`, o hay un typo en el import | Compara contra la lista completa de `providers` en este README (sección de arquitectura) |
| El botón de login no hace nada / no redirige | Bloqueador de popups o extensión del navegador interfiriendo (aunque se usa `redirect`, no `popup`) | Prueba en una ventana de incógnito sin extensiones |

## Antes de subir esto a producción / AWS
Actualiza `environment.prod.ts` con la URL real de tu API Gateway y el
dominio donde publiques el build de Angular, y agrega esa URL como
Redirect URI adicional en Azure (sin borrar la de `localhost:4200`, que
sigue sirviendo para seguir desarrollando en local).
