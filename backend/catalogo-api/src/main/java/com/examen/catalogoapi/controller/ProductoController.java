package com.examen.catalogoapi.controller;

import com.examen.catalogoapi.dto.ProductoRequest;
import com.examen.catalogoapi.model.Producto;
import com.examen.catalogoapi.service.ProductoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * La capa "Controller" es la puerta de entrada HTTP: traduce peticiones REST
 * (verbo + URL + body) a llamadas a metodos Java, y el resultado de esos
 * metodos de vuelta a una respuesta HTTP (status code + JSON).
 * NO deberia tener logica de negocio — eso vive en ProductoService.
 *
 * Todas las rutas de este controller quedan protegidas automaticamente por
 * SecurityConfig (".anyRequest().authenticated()"), o sea que Spring exige
 * un JWT valido ANTES de que cualquiera de estos metodos se ejecute.
 */
@RestController
@RequestMapping("/api/v1/catalogo/productos")
public class ProductoController {

    private final ProductoService service;

    public ProductoController(ProductoService service) {
        this.service = service;
    }

    /**
     * GET /api/v1/catalogo/productos
     * GET /api/v1/catalogo/productos?categoria=Perifericos
     *
     * @RequestParam(required = false) hace que el query param "categoria"
     * sea opcional: si no viene en la URL, el parametro llega como null.
     */
    @GetMapping
    public ResponseEntity<List<Producto>> listar(@RequestParam(required = false) String categoria) {
        return ResponseEntity.ok(service.listarTodos(categoria));
    }

    /**
     * GET /api/v1/catalogo/productos/5
     * @PathVariable extrae el "{id}" de la URL y lo convierte al tipo Long
     * automaticamente.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Producto> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    /**
     * POST /api/v1/catalogo/productos
     * @RequestBody le dice a Spring "convierte el JSON del body de la
     * peticion en un objeto ProductoRequest" (usando Jackson, la misma
     * libreria que convierte de objeto a JSON en las respuestas, pero en
     * sentido inverso).
     * @Valid activa las validaciones de Bean Validation que pusimos en
     * ProductoRequest (@NotBlank, @Positive, etc) ANTES de ejecutar el
     * cuerpo del metodo.
     *
     * HttpStatus.CREATED = 201, el codigo correcto para "se creo un recurso
     * nuevo exitosamente" (no 200, que es mas generico).
     */
    @PostMapping
    public ResponseEntity<Producto> crear(@Valid @RequestBody ProductoRequest request) {
        Producto creado = service.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    /** PUT /api/v1/catalogo/productos/5 — reemplaza todos los campos del producto 5. */
    @PutMapping("/{id}")
    public ResponseEntity<Producto> actualizar(@PathVariable Long id, @Valid @RequestBody ProductoRequest request) {
        return ResponseEntity.ok(service.actualizar(id, request));
    }

    /**
     * DELETE /api/v1/catalogo/productos/5
     * ResponseEntity.noContent() = 204 No Content: la convencion REST para
     * "la operacion funciono pero no hay nada que devolver en el body".
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
