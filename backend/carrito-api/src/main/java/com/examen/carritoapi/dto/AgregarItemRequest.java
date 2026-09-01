package com.examen.carritoapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record AgregarItemRequest(
        @NotNull(message = "El productoId es obligatorio") Long productoId,
        @NotBlank(message = "El nombreProducto es obligatorio") String nombreProducto,
        @NotNull(message = "El precioUnitario es obligatorio") @Positive(message = "El precio debe ser mayor a 0") BigDecimal precioUnitario,
        @NotNull(message = "La cantidad es obligatoria") @Positive(message = "La cantidad debe ser mayor a 0") Integer cantidad
) {}
