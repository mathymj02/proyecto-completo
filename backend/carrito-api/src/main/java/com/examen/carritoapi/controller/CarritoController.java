package com.examen.carritoapi.controller;

import com.examen.carritoapi.dto.ActualizarCantidadRequest;
import com.examen.carritoapi.dto.AgregarItemRequest;
import com.examen.carritoapi.dto.CarritoResponse;
import com.examen.carritoapi.service.CarritoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/**
 * Capa "Controller": el unico trabajo de esta clase es traducir HTTP a
 * llamadas Java y de vuelta. NO deberia tener logica de negocio (eso vive
 * en CarritoService) — aca solo: leemos el JWT, sacamos el usuarioId,
 * llamamos al service, y envolvemos el resultado en un ResponseEntity.
 *
 * @RestController = @Controller + @ResponseBody: le dice a Spring que esta
 * clase maneja peticiones HTTP y que todo lo que devuelvan sus metodos se
 * debe convertir automaticamente a JSON en el body de la respuesta (via
 * Jackson, la libreria de serializacion que trae Spring Boot por defecto).
 *
 * @RequestMapping("/api/v1/carritos") define el prefijo comun de ruta para
 * TODOS los metodos de esta clase — asi no hay que repetirlo en cada uno.
 */
@RestController
@RequestMapping("/api/v1/carritos")
public class CarritoController {

    private final CarritoService service;

    public CarritoController(CarritoService service) {
        this.service = service;
    }

    /**
     * Obtiene el identificador estable del usuario a partir del JWT.
     * "oid" es el Object ID del usuario en Azure AD (recomendado, no cambia).
     * Si no viene (por ejemplo en tokens v1 sin ese claim), se usa "sub".
     *
     * Este metodo es el corazon de la seguridad "por usuario" de este API:
     * TODOS los endpoints de abajo llaman a este metodo para saber DE QUIEN
     * es el carrito, y nunca confian en un valor que venga en la URL o el
     * body (el cliente jamas puede decir "dame el carrito del usuario X" —
     * solo puede pedir "dame MI carrito", segun lo que diga su propio token).
     */
    private String obtenerUsuarioId(Jwt jwt) {
        String oid = jwt.getClaimAsString("oid");
        return (oid != null && !oid.isBlank()) ? oid : jwt.getSubject();
    }

    /**
     * GET /api/v1/carritos/mios
     *
     * @AuthenticationPrincipal Jwt jwt: Spring Security ya valido este token
     * en el filtro (ver SecurityConfig) ANTES de que la peticion llegue
     * aca. Esta anotacion simplemente le dice a Spring "inyectame el objeto
     * Jwt ya parseado, del usuario que hizo esta peticion" — no hay que
     * decodificar el header "Authorization" a mano en ningun lado.
     */
    @GetMapping("/mios")
    public ResponseEntity<CarritoResponse> obtenerMiCarrito(@AuthenticationPrincipal Jwt jwt) {
        var carrito = service.obtenerOCrearCarrito(obtenerUsuarioId(jwt));
        // CarritoResponse.desde(carrito) convierte la ENTIDAD (que tiene
        // detalles internos de JPA) en un DTO limpio pensado para el
        // frontend, calculando ademas el total. Nunca devolvemos la entidad
        // JPA directamente — es una buena practica separar "lo que guardo
        // en la base de datos" de "lo que expongo por la API".
        return ResponseEntity.ok(CarritoResponse.desde(carrito));
    }

    /**
     * POST /api/v1/carritos/items
     *
     * @Valid le dice a Spring "antes de ejecutar este metodo, revisa las
     * anotaciones de validacion del DTO (@NotNull, @Positive, etc. definidas
     * en AgregarItemRequest) y si algo no cumple, corta la ejecucion y
     * responde 400 automaticamente" (el manejo de ese error 400 esta en
     * GlobalExceptionHandler). @RequestBody le dice que tome el JSON del
     * cuerpo de la peticion y lo convierta a un objeto AgregarItemRequest.
     */
    @PostMapping("/items")
    public ResponseEntity<CarritoResponse> agregarItem(@AuthenticationPrincipal Jwt jwt,
                                                         @Valid @RequestBody AgregarItemRequest request) {
        var carrito = service.agregarItem(obtenerUsuarioId(jwt), request);
        return ResponseEntity.ok(CarritoResponse.desde(carrito));
    }

    /**
     * PUT /api/v1/carritos/items/{itemId}
     * @PathVariable Long itemId: extrae el "{itemId}" de la URL y lo
     * convierte a Long automaticamente.
     */
    @PutMapping("/items/{itemId}")
    public ResponseEntity<CarritoResponse> actualizarCantidad(@AuthenticationPrincipal Jwt jwt,
                                                                @PathVariable Long itemId,
                                                                @Valid @RequestBody ActualizarCantidadRequest request) {
        var carrito = service.actualizarCantidad(obtenerUsuarioId(jwt), itemId, request.cantidad());
        return ResponseEntity.ok(CarritoResponse.desde(carrito));
    }

    /** DELETE /api/v1/carritos/items/{itemId} — elimina un item puntual. */
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<CarritoResponse> eliminarItem(@AuthenticationPrincipal Jwt jwt,
                                                          @PathVariable Long itemId) {
        var carrito = service.eliminarItem(obtenerUsuarioId(jwt), itemId);
        return ResponseEntity.ok(CarritoResponse.desde(carrito));
    }

    /** DELETE /api/v1/carritos/mios — vacia el carrito completo. */
    @DeleteMapping("/mios")
    public ResponseEntity<CarritoResponse> vaciarCarrito(@AuthenticationPrincipal Jwt jwt) {
        var carrito = service.vaciarCarrito(obtenerUsuarioId(jwt));
        return ResponseEntity.ok(CarritoResponse.desde(carrito));
    }
}
