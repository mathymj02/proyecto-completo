package com.examen.catalogoapi.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Esta es una "entidad JPA": una clase Java que representa una tabla de la
 * base de datos. Cada instancia de Producto = una fila de la tabla
 * "productos". JPA (Java Persistence API, implementada aca por Hibernate,
 * que Spring Data JPA usa por debajo) se encarga de traducir entre objetos
 * Java y filas SQL sin que tengamos que escribir INSERT/UPDATE/SELECT a mano.
 *
 * @Entity le dice a Hibernate "esta clase mapea a una tabla".
 * @Table(name = "productos") especifica el nombre exacto de la tabla (si no
 * lo pones, Hibernate usa el nombre de la clase por defecto).
 */
@Entity
@Table(name = "productos")
public class Producto {

    /**
     * @Id marca este campo como la clave primaria de la tabla.
     * @GeneratedValue(strategy = GenerationType.IDENTITY) le dice a Hibernate
     * "no generes tu el numero, dejaselo a la base de datos" (equivalente a
     * una columna AUTO_INCREMENT/SERIAL). Por eso al crear un Producto nuevo
     * pasamos "null" como id: la base de datos le asigna el numero real al
     * guardarlo.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** nullable = false -> esta columna es NOT NULL en la base de datos. */
    @Column(nullable = false)
    private String nombre;

    private String descripcion;

    /**
     * BigDecimal (no double/float) para dinero: evita errores de redondeo
     * de coma flotante que SI pasan con double (ej. 0.1 + 0.2 != 0.3 exacto
     * en double). Para plata, siempre BigDecimal.
     */
    @Column(nullable = false)
    private BigDecimal precio;

    @Column(nullable = false)
    private Integer stock;

    private String categoria;

    private String imagenUrl;

    /**
     * Constructor vacio: JPA/Hibernate lo necesita OBLIGATORIAMENTE para
     * poder crear instancias de la entidad el mismo cuando lee filas de la
     * base de datos (las llena despues con los setters/reflection). Si no
     * existe este constructor, la aplicacion falla al arrancar.
     */
    public Producto() {}

    /** Constructor con todos los campos, para crear productos comodamente desde el codigo. */
    public Producto(Long id, String nombre, String descripcion, BigDecimal precio,
                     Integer stock, String categoria, String imagenUrl) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.precio = precio;
        this.stock = stock;
        this.categoria = categoria;
        this.imagenUrl = imagenUrl;
    }

    // Getters y setters: Hibernate y Jackson (la libreria que convierte a/desde
    // JSON) los usan por reflection para leer y escribir los valores de cada
    // campo. Sin ellos, la entidad no se podria ni guardar en la base de
    // datos ni convertir a JSON en las respuestas del controller.

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }

    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public String getImagenUrl() { return imagenUrl; }
    public void setImagenUrl(String imagenUrl) { this.imagenUrl = imagenUrl; }
}
