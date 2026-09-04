package com.examen.catalogoapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * DTO de ENTRADA (lo que el cliente manda en el body de POST/PUT). Notese
 * que NO reutilizamos la entidad Producto directamente en el controller —
 * esto es una buena practica deliberada, no un descuido: separar el "modelo
 * de base de datos" (Producto) del "modelo de la API" (ProductoRequest)
 * evita que, por ejemplo, alguien pueda mandar un "id" en el body e
 * intentar sobreescribir el de otro producto, y te da libertad de cambiar
 * la tabla sin romper el contrato de la API (o viceversa).
 *
 * Las anotaciones de jakarta.validation (@NotBlank, @NotNull, @Positive,
 * etc) son "Bean Validation": declaras las reglas aca, y en el controller
 * con @Valid, Spring las chequea automaticamente ANTES de que tu codigo se
 * ejecute. Si algo no cumple, Spring lanza una excepcion
 * (MethodArgumentNotValidException) que capturamos en GlobalExceptionHandler
 * para devolver un 400 Bad Request con un mensaje claro, en vez de un 500
 * feo o, peor, guardar datos invalidos.
 */
public record ProductoRequest(
        @NotBlank(message = "El nombre es obligatorio")
        String nombre,

        String descripcion, // opcional, sin validacion

        @NotNull(message = "El precio es obligatorio")
        @Positive(message = "El precio debe ser mayor a 0")
        BigDecimal precio,

        @NotNull(message = "El stock es obligatorio")
        @PositiveOrZero(message = "El stock no puede ser negativo") // permite 0, pero no negativos
        Integer stock,

        String categoria,
        String imagenUrl
) {}
