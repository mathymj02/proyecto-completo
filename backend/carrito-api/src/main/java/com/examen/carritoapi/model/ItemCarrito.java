package com.examen.carritoapi.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * Representa UNA fila dentro de un carrito (un producto + cantidad).
 * "Congela" el nombre y precio del producto en el momento en que se agrego
 * al carrito: si mañana el precio del producto cambia en catalogo-api, el
 * carrito de un usuario que ya lo agrego NO se actualiza solo — asi evitas
 * que a alguien le suba el precio a mitad de compra. Esto es una decision
 * de diseño real que se usa en carritos de compra de verdad (piensa en
 * Amazon: el precio que viste al agregar es el que pagas).
 */
@Entity
@Table(name = "items_carrito")
public class ItemCarrito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Relacion "muchos a uno": MUCHOS items pertenecen a UN carrito.
     * @JoinColumn(name = "carrito_id") crea la columna de clave foranea en
     * la tabla items_carrito que apunta de vuelta al carrito dueño.
     * optional = false: un item SIEMPRE debe pertenecer a un carrito, nunca
     * puede quedar "suelto".
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "carrito_id")
    private Carrito carrito;

    /**
     * Referencia al producto del catalogo (catalogo-api). No se usa
     * relacion JPA porque viven en bases de datos/servicios distintos.
     */
    @Column(nullable = false)
    private Long productoId;

    @Column(nullable = false)
    private String nombreProducto;

    @Column(nullable = false)
    private BigDecimal precioUnitario;

    @Column(nullable = false)
    private Integer cantidad;

    public ItemCarrito() {}

    public ItemCarrito(Carrito carrito, Long productoId, String nombreProducto,
                        BigDecimal precioUnitario, Integer cantidad) {
        this.carrito = carrito;
        this.productoId = productoId;
        this.nombreProducto = nombreProducto;
        this.precioUnitario = precioUnitario;
        this.cantidad = cantidad;
    }

    /**
     * Calcula precio x cantidad. No es un campo guardado en la base de
     * datos (no tiene @Column) — se calcula al vuelo cada vez que se pide,
     * asi nunca puede quedar "desincronizado" del precio o la cantidad real.
     * Usamos BigDecimal (no double/float) para dinero porque los tipos
     * decimales binarios (double) pierden precision en operaciones
     * financieras — es la practica estandar en Java para manejar plata.
     */
    public BigDecimal getSubtotal() {
        return precioUnitario.multiply(BigDecimal.valueOf(cantidad));
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Carrito getCarrito() { return carrito; }
    public void setCarrito(Carrito carrito) { this.carrito = carrito; }

    public Long getProductoId() { return productoId; }
    public void setProductoId(Long productoId) { this.productoId = productoId; }

    public String getNombreProducto() { return nombreProducto; }
    public void setNombreProducto(String nombreProducto) { this.nombreProducto = nombreProducto; }

    public BigDecimal getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(BigDecimal precioUnitario) { this.precioUnitario = precioUnitario; }

    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
}
