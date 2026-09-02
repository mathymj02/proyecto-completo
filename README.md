# Proyecto completo — Backend (Spring Boot) + Frontend (Azure AD) + Postman

Este paquete junta todo lo necesario para correr el examen de punta a punta
en tu máquina local, sin depender de AWS todavía.

```
proyecto-completo/
├── backend/          → las 3 APIs Spring Boot (ver backend/README.md para detalle)
│   ├── auth-api/
│   ├── catalogo-api/
│   └── carrito-api/
├── frontend/         → el repo del profe (boton-inicio-azure), ya configurado
│                        para apuntar a localhost en vez de AWS
└── postman/          → colección + environment listos para importar
```

## Cambios que ya apliqué en `frontend/` respecto al repo original
1. `src/auth/AuthConfig.js`: `redirectUri` y `postLogoutRedirectUri` ahora
   apuntan a `http://localhost:5173/` (antes apuntaban a la URL de API
   Gateway en AWS).
2. Se agregó `.env` con `VITE_API_URL=http://localhost:8083/api/v1/auth/me`
   — el botón "Obtener Token y Enviar al Backend" del frontend prueba
   contra `auth-api` local. Cámbialo a `catalogo-api` (8081) o
   `carrito-api` (8082) si quieres probar esos con el mismo botón.

## ⚠️ Paso manual en Azure
Ve a [portal.azure.com](https://portal.azure.com) → Microsoft Entra ID →
Registros de aplicaciones → tu app → **Autenticación** → agrega
`http://localhost:5173/` como Redirect URI tipo **SPA** (sin borrar la que
ya está de AWS). Sin esto, el login local falla con
`redirect_uri_mismatch`. Es un cambio en la consola de Azure, ningún
archivo de código lo puede reemplazar.

## Cómo correr todo

### 1. Backend (3 terminales o 3 "Run" en IntelliJ)
```bash
cd backend/auth-api      && mvn spring-boot:run   # puerto 8083
cd backend/catalogo-api  && mvn spring-boot:run   # puerto 8081
cd backend/carrito-api   && mvn spring-boot:run   # puerto 8082
```
(o usa el botón Run sobre cada `XxxApplication.java` en IntelliJ, como ya
veníamos haciendo).

### 2. Frontend
```bash
cd frontend
npm install
npm run dev
```
Abre `http://localhost:5173`.

### 3. Login y captura del token
Haz clic en iniciar sesión con Microsoft, luego en "Obtener Token y Enviar
al Backend". Copia el JWT que aparece en la caja "Token JWT Capturado".

### 4. Postman
1. Abre Postman → Import → arrastra los 2 archivos de la carpeta `postman/`
   (`examen-apis.postman_collection.json` y `examen-local.postman_environment.json`).
2. Arriba a la derecha, selecciona el environment **examen-local**.
3. Abre el environment (ícono de ojo) y pega tu JWT en la variable `token`.
4. Ya puedes correr cualquier request de la colección: están agrupadas en
   3 carpetas (Auth API, Catalogo API, Carrito API) con GET/POST/PUT/DELETE
   de ejemplo, apuntando a `localhost:8081/8082/8083` mediante las
   variables `{{auth_url}}`, `{{catalogo_url}}`, `{{carrito_url}}`.

> El token de Azure expira típicamente en 1 hora — si empiezas a ver 401
> después de un rato, vuelve al frontend, clic en el botón de nuevo, y
> pega el token fresco en el environment de Postman.

## Antes de subir esto a producción / AWS
Recuerda revertir `frontend/src/auth/AuthConfig.js` a la URL de tu API
Gateway (y agregar esa URL en Azure) antes de hacer el deploy final — los
cambios de este paquete son **solo para desarrollo local**. El
`backend/README.md` tiene todo el detalle de despliegue en EC2 + API
Gateway que vimos en el chat.

---

## 🎓 El flujo MSAL

Todo el código del frontend (`main.jsx`, `AuthConfig.js`, `LoginButton.jsx`,
`TokenSender.jsx`) tiene comentarios explicando cada pieza — acá va el
mapa mental de cómo se conectan entre sí:

```
1. main.jsx crea UNA instancia de PublicClientApplication (MSAL) con la
   config de AuthConfig.js, y la comparte a toda la app via <MsalProvider>.

2. LoginButton.jsx llama a instance.loginRedirect(loginRequest):
   el navegador SALE de tu app y va a login.microsoftonline.com.
   El usuario inicia sesión ahí (no en tu código — nunca ves su contraseña).

3. Azure redirige de vuelta a tu redirectUri (localhost:5173) con el
   resultado codificado en la URL. main.jsx detecta esto con
   handleRedirectPromise() y termina de armar la sesión.

4. Con sesión activa, TokenSender.jsx pide un SEGUNDO token, esta vez
   con el scope de TU API (apiRequest, no loginRequest). Este es el
   JWT que tu Spring Boot puede validar — el de loginRequest (User.Read)
   es para Microsoft Graph, no serviría contra tu backend.

5. Ese JWT se manda en el header "Authorization: Bearer <token>" al
   backend. A partir de ahí, es exactamente el flujo que ya conoces del
   lado de Spring Security (SecurityConfig valida la firma, issuer y
   audience, y el Controller responde).
```

### Conceptos de MSAL/OIDC que vas a encontrar comentados en el código

| Concepto | Dónde se explica en detalle |
|---|---|
| `PublicClientApplication`, `MsalProvider`, React Context | `main.jsx` |
| `handleRedirectPromise()` — por qué existe y cuándo se usa | `main.jsx` |
| `loginRedirect()` vs `acquireTokenSilent()` vs `acquireTokenRedirect()` | `LoginButton.jsx`, `TokenSender.jsx` |
| Diferencia entre el scope `User.Read` (Graph) y `api://.../write-read` (tu API) | `AuthConfig.js` |
| `useMsal()` vs `useIsAuthenticated()` | `LoginButton.jsx`, `App.jsx` |
| Por qué `fetch()` no lanza excepción en un 401/500 (hay que chequear `res.ok`) | `TokenSender.jsx` |
| `import.meta.env.VITE_*` — cómo Vite expone variables de `.env` al navegador | `TokenSender.jsx` |
