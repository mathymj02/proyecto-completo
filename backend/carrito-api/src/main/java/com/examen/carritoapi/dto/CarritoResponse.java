package com.examen.carritoapi.dto;

import com.examen.carritoapi.model.Carrito;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * DTO ("Data Transfer Object") de salida: la forma exacta en que le
 * mostramos el carrito al frontend, distinta de como lo guardamos
 * internamente (la entidad Carrito). Es un "record" de Java (desde Java 16):
 * una forma corta de declarar una clase inmutable que solo transporta
 * datos — el compilador genera automaticamente constructor, getters
 * (con el mismo nombre del campo, ej. id() en vez de getId()), equals(),
 * hashCode() y toString(). Ideal para DTOs porque no necesitan logica ni
 * setters.
 */
public record CarritoResponse(
        Long id,
        String usuarioId,
        Instant fechaCreacion,
        List<ItemResponse> items,
        BigDecimal total // <- este campo NO existe en la entidad Carrito, se calcula aca abajo
) {
    /** Record anidado: el "shape" de cada item dentro de la respuesta. */
    public record ItemResponse(
            Long id,
            Long productoId,
            String nombreProducto,
            BigDecimal precioUnitario,
            Integer cantidad,
            BigDecimal subtotal
    ) {}

    /**
     * Metodo "factory" estatico: convierte una entidad Carrito (con toda su
     * complejidad de JPA) en este DTO plano. Centralizar esta conversion
     * aca (en vez de hacerla en cada Controller) evita repetir el mismo
     * codigo de mapeo 5 veces.
     */
    public static CarritoResponse desde(Carrito carrito) {
        // .stream().map(...).toList(): por cada ItemCarrito de la entidad,
        // creamos su ItemResponse correspondiente. Es el equivalente
        // funcional a hacer un for-each creando una lista nueva a mano.
        List<ItemResponse> items = carrito.getItems().stream()
                .map(i -> new ItemResponse(i.getId(), i.getProductoId(), i.getNombreProducto(),
                        i.getPrecioUnitario(), i.getCantidad(), i.getSubtotal()))
                .toList();

        // .reduce(valorInicial, operacion): suma todos los subtotales en
        // un solo BigDecimal. Empieza en BigDecimal.ZERO y va acumulando
        // con BigDecimal::add (referencia a metodo, equivalente a
        // (a, b) -> a.add(b)). Es la forma funcional de sumar una lista sin
        // un for con un acumulador declarado a mano.
        BigDecimal total = items.stream()
                .map(ItemResponse::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CarritoResponse(carrito.getId(), carrito.getUsuarioId(), carrito.getFechaCreacion(), items, total);
    }
}
