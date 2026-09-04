package com.examen.catalogoapi.service;

/**
 * Excepcion propia (custom) que lanzamos cuando se busca un producto que no
 * existe. Extiende RuntimeException (no Exception "checked") para no
 * obligar a poner try/catch o "throws" en cada metodo que la pueda lanzar —
 * asi el codigo del service queda mas limpio.
 *
 * ¿Por que crear una excepcion propia en vez de tirar un
 * "throw new RuntimeException(...)" generico? Porque en GlobalExceptionHandler
 * podemos capturar ESPECIFICAMENTE este tipo de excepcion y responder con
 * 404 Not Found, mientras que otros errores inesperados devuelven un 500.
 * Es la forma de que el controller "sepa" que tipo de error paso, sin
 * mezclar codigos HTTP con logica de negocio en el service.
 */
public class RecursoNoEncontradoException extends RuntimeException {
    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
