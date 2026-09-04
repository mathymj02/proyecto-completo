# Examen — 3 APIs (Spring Boot) para el frontend `boton-inicio-azure`

Este proyecto contiene **3 microservicios independientes**, cada uno protegido como
*OAuth2 Resource Server* que valida el JWT emitido por Azure AD (mismo tenant/app
que ya configuró tu profe en el frontend `AuthConfig.js`).

| Servicio      | Puerto | Responsabilidad                                   |
|---------------|--------|----------------------------------------------------|
| `auth-api`    | 8083   | Valida el JWT y devuelve la identidad/claims del usuario autenticado |
| `catalogo-api`| 8081   | CRUD de productos                                  |
| `carrito-api` | 8082   | Carrito de compras, uno por usuario autenticado    |

## Requisitos
- Java 17
- Maven 3.9+ (o usa el wrapper de tu IDE)

> Nota: no pude descargar dependencias de Maven Central desde este entorno para
> compilar el proyecto (no tengo acceso a `repo.maven.apache.org`), así que el
> código no fue compilado/probado automáticamente. Cómpialo tú con
> `mvn clean package` en cada carpeta — si algo no compila, dime el error exacto
> y lo corrijo.

## Cómo correrlas
Desde cada carpeta (`auth-api`, `catalogo-api`, `carrito-api`):
```bash
mvn spring-boot:run
```
o generando el jar:
```bash
mvn clean package -DskipTests
java -jar target/*.jar
```

## ⚠️ Punto crítico: revisa el `issuer` real de tu token

En `application.yml` de cada servicio configuré `issuer-v2` apuntando a:
```
https://login.microsoftonline.com/e5372bf0-c5e3-4286-887c-79069f209c1f/v2.0
```
Esto es correcto **solo si** tu App Registration en Azure acepta tokens v2.0
(`accessTokenAcceptedVersion: 2` en el manifiesto). Si tu app quedó en v1
(que es lo más común cuando expones un API con "App ID URI" tipo
`api://<client-id>/scope`), el token real va a traer:
```
"iss": "https://sts.windows.net/e5372bf0-c5e3-4286-887c-79069f209c1f/"
```
y Spring va a rechazarlo con `invalid_token: issuer mismatch`.

**Cómo saberlo rápido:** pega el token que te muestra el frontend (React,
"Token JWT Capturado") en https://jwt.ms y mira el campo `iss`. Si es
`sts.windows.net`, cambia en cada `application.yml`:
```yaml
azure:
  issuer-v2: https://sts.windows.net/e5372bf0-c5e3-4286-887c-79069f209c1f/
```

También revisa el campo `aud` del token — debe calzar con
`expected-audience: api://57750927-6116-478c-a047-d06caa8fcd00`. Si tu token
trae otro formato de audience (a veces es solo el GUID sin `api://`), ajusta
esa propiedad para que el `AudienceValidator` no rechace el token.

## Endpoints

### auth-api (puerto 8083)
| Método | Ruta                     | Auth | Descripción |
|--------|--------------------------|------|-------------|
| GET    | `/api/v1/auth/public/ping` | No  | Health check |
| GET    | `/api/v1/auth/me`        | Sí   | Devuelve claims decodificados del JWT (subject, nombre, expiración, etc.) |

### catalogo-api (puerto 8081)
| Método | Ruta                              | Auth | Descripción |
|--------|-----------------------------------|------|-------------|
| GET    | `/api/v1/catalogo/productos`      | Sí   | Lista productos (filtro opcional `?categoria=`) |
| GET    | `/api/v1/catalogo/productos/{id}` | Sí   | Obtiene un producto |
| POST   | `/api/v1/catalogo/productos`      | Sí   | Crea producto |
| PUT    | `/api/v1/catalogo/productos/{id}` | Sí   | Actualiza producto |
| DELETE | `/api/v1/catalogo/productos/{id}` | Sí   | Elimina producto |

Body de ejemplo (POST/PUT):
```json
{
  "nombre": "Audífonos Bluetooth",
  "descripcion": "Cancelación de ruido activa",
  "precio": 39990,
  "stock": 12,
  "categoria": "Audio",
  "imagenUrl": "https://..."
}
```

### carrito-api (puerto 8082)
Todos los endpoints operan sobre **el carrito del usuario autenticado** (se
identifica por el claim `oid` del JWT, nunca por un id que mande el cliente).

| Método | Ruta                              | Descripción |
|--------|-----------------------------------|-------------|
| GET    | `/api/v1/carritos/mios`           | Obtiene (o crea) mi carrito con el total calculado |
| POST   | `/api/v1/carritos/items`          | Agrega un producto (suma cantidad si ya existe) |
| PUT    | `/api/v1/carritos/items/{itemId}` | Cambia la cantidad de un item |
| DELETE | `/api/v1/carritos/items/{itemId}` | Elimina un item |
| DELETE | `/api/v1/carritos/mios`           | Vacía el carrito |

Body de ejemplo (POST items):
```json
{
  "productoId": 1,
  "nombreProducto": "Teclado mecanico",
  "precioUnitario": 29990,
  "cantidad": 2
}
```

Body de ejemplo (PUT items/{id}):
```json
{ "cantidad": 5 }
```

## Cómo probar con el token real del frontend
1. Corre el frontend (`npm run dev`) e inicia sesión con el botón de Microsoft.
2. En la sección "Prueba de envío de Token", copia el JWT capturado.
3. Con Postman/curl, pega ese token en cada request:
```bash
curl -H "Authorization: Bearer <TOKEN>" http://localhost:8081/api/v1/catalogo/productos
curl -H "Authorization: Bearer <TOKEN>" http://localhost:8082/api/v1/carritos/mios
curl -H "Authorization: Bearer <TOKEN>" http://localhost:8083/api/v1/auth/me
```
Si te da 401, revisa el punto del `issuer`/`audience` de arriba antes que nada
— es el 90% de los problemas al integrar Spring con Azure AD.

## Nota sobre `VITE_API_URL` del frontend
El frontend por defecto apunta a una URL de API Gateway en AWS
(`https://0t16t89h07.execute-api.us-east-1.amazonaws.com/desarrallo/api/v1`).
Para probar contra tus APIs locales, crea un `.env` en el frontend:
```
VITE_API_URL=http://localhost:8081/api/v1
```
(o el puerto del servicio que quieras probar desde el botón de TokenSender).

## H2 Console (solo catalogo-api y carrito-api)
Disponible en `http://localhost:8081/h2-console` y `http://localhost:8082/h2-console`
(JDBC URL: `jdbc:h2:mem:catalogodb` / `jdbc:h2:mem:carritodb`, user `sa`, sin password)
para inspeccionar los datos mientras desarrollas.

---

## 🎓 Guía de aprendizaje: cómo está armado el código

Todo el código fuente (`.java`) tiene comentarios Javadoc explicando **qué
hace y por qué** cada clase, variable y método importante — no solo "qué
hace la línea", sino la decisión de diseño detrás. Esta sección es el mapa
para saber por dónde empezar a leerlo y en qué orden tiene sentido.

### La arquitectura en 3 capas (se repite en catalogo-api y carrito-api)

```
Controller  →  Service  →  Repository  →  Base de datos
(HTTP)         (lógica)     (SQL/JPA)
```

- **Controller** (`controller/`): traduce HTTP a Java y de vuelta. No debería
  tener lógica de negocio — solo lee la petición, llama al Service, y
  envuelve el resultado en un `ResponseEntity`.
- **Service** (`service/`): las reglas reales de la aplicación (qué pasa si
  no existe algo, cómo combinar datos, qué hacer si un producto ya está en
  el carrito, etc).
- **Repository** (`repository/`): una interfaz que extiende `JpaRepository`
  — Spring genera la implementación (el SQL) automáticamente, nosotros
  nunca escribimos una sola línea de SQL a mano.
- **Model / Entity** (`model/`): las clases que mapean 1 a 1 con las tablas
  de la base de datos (anotadas con `@Entity`).
- **DTO** (`dto/`): las clases que definen la forma exacta de lo que entra y
  sale por la API — nunca exponemos las entidades JPA directo, para poder
  cambiar el modelo de datos sin romper el "contrato" con el frontend.

### Por dónde empezar a leer (orden sugerido)

1. **`SecurityConfig.java`** (en cualquiera de las 3 APIs — son casi
   idénticas): es el corazón de todo. Explica qué es un *Resource Server*,
   cómo se valida un JWT (firma, expiración, issuer, audience) y por qué no
   hay ningún endpoint de "login" en estas APIs (el login pasa en el
   frontend, contra Azure AD directamente).
2. **`AzureAdProperties.java`**: cómo Spring lee la sección `azure:` del
   `application.yml` y la convierte en un objeto Java tipado
   (`@ConfigurationProperties`).
3. **`AuthController.java`** (auth-api): el flujo completo de una petición
   autenticada, explicado paso a paso en el Javadoc del método `me()`.
4. **`ProductoController.java` → `ProductoService.java`** (catalogo-api): el
   ejemplo más simple de CRUD en 3 capas — léelos en ese orden para ver
   cómo una petición HTTP va bajando de capa en capa.
5. **`CarritoController.java` → `CarritoService.java` → `Carrito.java` /
   `ItemCarrito.java`** (carrito-api): el caso más rico del proyecto. Acá
   vas a encontrar explicado:
   - Cómo se aísla el carrito de cada usuario usando el claim `oid` del JWT
     (nunca confiando en un id que mande el cliente).
   - `Optional`, `.orElseGet()`, `.ifPresentOrElse()`, `.stream()...` —
     Streams y programación funcional de Java aplicados a un caso real.
   - `@Transactional`, `cascade = CascadeType.ALL`, `orphanRemoval = true`:
     cómo JPA propaga automáticamente los cambios del carrito a sus items
     sin que tengamos que guardar cada uno por separado.
   - Por qué el carrito "congela" el precio del producto al momento de
     agregarlo (una decisión de negocio real, no un detalle técnico).

### Conceptos de Spring que vas a encontrar comentados en el código

| Anotación / concepto | Dónde se explica en detalle |
|---|---|
| `@RestController`, `@RequestMapping` | `AuthController.java`, `CarritoController.java` |
| `@Service`, inyección de dependencias por constructor | `ProductoService.java` |
| `@Entity`, `@Id`, `@GeneratedValue`, `@OneToMany`, `@ManyToOne` | `Carrito.java`, `ItemCarrito.java` |
| `@Valid`, `@RequestBody`, `@PathVariable`, `@RequestParam` | `ProductoController.java`, `CarritoController.java` |
| `@ConfigurationProperties` vs `@Value` | `AzureAdProperties.java`, `SecurityConfig.java` |
| `@Transactional`, `cascade`, `orphanRemoval` | `CarritoService.java`, `Carrito.java` |
| JWT: firma, issuer, audience, `@AuthenticationPrincipal` | `SecurityConfig.java`, `AuthController.java` |
| Records de Java como DTOs | `CarritoResponse.java`, `ProductoRequest.java` |
| `Optional`, Streams (`filter`, `map`, `reduce`, `ifPresentOrElse`) | `CarritoService.java`, `CarritoResponse.java` |

### Tip para estudiar esto de verdad
No te limites a leer los comentarios — pon un breakpoint (debugger) en
`AuthController.me()` o en `CarritoService.agregarItem()`, corre el
proyecto en modo Debug desde IntelliJ, y mándale una petición desde
Postman. Ver el `Jwt` real con sus claims, o la lista de items cambiando
paso a paso, enseña mucho más rápido que solo leer código.
