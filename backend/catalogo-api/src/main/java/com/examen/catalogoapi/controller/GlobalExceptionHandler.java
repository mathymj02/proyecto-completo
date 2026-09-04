package com.examen.catalogoapi.controller;

import com.examen.catalogoapi.service.RecursoNoEncontradoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @RestControllerAdvice hace que esta clase actue como un "interceptor
 * global de excepciones" para TODOS los controllers de la aplicacion (no
 * hace falta declararla en cada uno). Sin esto, cualquier excepcion no
 * capturada explota como un feo 500 Internal Server Error con stack trace
 * expuesto al cliente — algo que nunca quieres en produccion.
 *
 * Cada metodo anotado con @ExceptionHandler(TipoDeExcepcion.class) se
 * ejecuta automaticamente cuando ESA excepcion (o una subclase) se lanza en
 * cualquier controller, y decide que responder.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Si en cualquier controller se lanza RecursoNoEncontradoException
     * (ej. desde ProductoService.buscarPorId), lo capturamos aca y
     * respondemos 404 con un JSON de error legible, en vez de un 500.
     */
    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> handleNoEncontrado(RecursoNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(cuerpoError(HttpStatus.NOT_FOUND, ex.getMessage()));
    }

    /**
     * MethodArgumentNotValidException es la excepcion que Spring lanza
     * automaticamente cuando @Valid detecta que el body no cumple alguna
     * regla de ProductoRequest (@NotBlank, @Positive, etc). Aca juntamos
     * TODOS los errores de validacion en un solo mensaje legible, en vez de
     * devolver el objeto de excepcion crudo (que es bastante verboso).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidacion(MethodArgumentNotValidException ex) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Datos invalidos");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(cuerpoError(HttpStatus.BAD_REQUEST, mensaje));
    }

    /**
     * Helper privado para no repetir la misma estructura de JSON de error en
     * cada handler. LinkedHashMap (en vez de HashMap) para que las claves
     * salgan siempre en el mismo orden en el JSON final (mas prolijo al
     * leerlo, aunque no cambia el funcionamiento).
     */
    private Map<String, Object> cuerpoError(HttpStatus status, String mensaje) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("mensaje", mensaje);
        return body;
    }
}
