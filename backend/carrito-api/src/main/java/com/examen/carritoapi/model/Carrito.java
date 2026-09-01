package com.examen.carritoapi.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * @Entity le dice a Hibernate/JPA "esta clase representa una tabla en la
 * base de datos, mapea sus objetos automaticamente". @Table(name = "...")
 * fija el nombre exacto de la tabla (si lo omites, JPA usa el nombre de la
 * clase, pero es buena practica ser explicito).
 *
 * Esta clase es una entidad JPA, NO un DTO: vive acoplada al esquema de la
 * base de datos. Por eso el Controller nunca la devuelve directo — usa
 * CarritoResponse (un DTO) para decidir que campos exponer al frontend.
 */
@Entity
@Table(name = "carritos")
public class Carrito {

    /**
     * @Id marca este campo como clave primaria.
     * @GeneratedValue(strategy = IDENTITY) delega en la base de datos la
     * generacion del numero (auto-incremental) — nosotros nunca asignamos
     * un id a mano, por eso en el constructor de mas abajo no se recibe.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Identificador del usuario duenio del carrito.
     * Se llena con el claim "oid" (o "sub" como respaldo) del JWT de Azure AD:
     * asi cada usuario autenticado tiene su propio carrito, sin que el
     * cliente pueda elegir de quien es (evita que alguien lea el carrito de otro).
     *
     * unique = true es la restriccion mas importante de esta clase: le dice
     * a la base de datos "no permitas dos filas con el mismo usuarioId".
     * Gracias a esto, CarritoService.obtenerOCrearCarrito() puede confiar en
     * que buscar por usuarioId siempre trae 0 o 1 resultado, nunca mas de uno.
     */
    @Column(nullable = false, unique = true)
    private String usuarioId;

    /** Se asigna sola al crear el objeto (Instant.now()), no requiere setearla desde afuera. */
    @Column(nullable = false)
    private Instant fechaCreacion = Instant.now();

    /**
     * Relacion "uno a muchos": UN carrito tiene MUCHOS items.
     *   - mappedBy = "carrito": le dice a JPA que la relacion ya esta
     *     definida del otro lado (en el campo "carrito" de ItemCarrito, con
     *     @ManyToOne) — evita crear una tabla intermedia innecesaria.
     *   - cascade = CascadeType.ALL: cualquier operacion que hagas sobre el
     *     Carrito (guardar, borrar) se propaga automaticamente a sus items.
     *     Por eso en CarritoService nunca llamamos itemRepository.save()
     *     a mano: basta con guardar el carrito.
     *   - orphanRemoval = true: si sacas un item de esta lista en memoria
     *     (ej. list.remove(x) o list.clear()) y despues guardas el carrito,
     *     JPA entiende que ese item quedo "huerfano" y lo borra el solo de
     *     la base de datos. Esto es lo que usan eliminarItem() y
     *     vaciarCarrito() en el service.
     *   - fetch = EAGER: al traer un Carrito, tambien trae sus items en la
     *     misma consulta (en vez de esperar a que alguien los pida). Tiene
     *     sentido aca porque SIEMPRE necesitamos los items para calcular el
     *     total (ver CarritoResponse) — no tendria sentido traerlos "de a poco".
     */
    @OneToMany(mappedBy = "carrito", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ItemCarrito> items = new ArrayList<>();

    /** Constructor vacio: JPA lo exige internamente para poder reconstruir objetos desde la base de datos. */
    public Carrito() {}

    /** Constructor que usamos nosotros al crear un carrito nuevo (ver CarritoService.obtenerOCrearCarrito). */
    public Carrito(String usuarioId) {
        this.usuarioId = usuarioId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsuarioId() { return usuarioId; }
    public void setUsuarioId(String usuarioId) { this.usuarioId = usuarioId; }

    public Instant getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(Instant fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public List<ItemCarrito> getItems() { return items; }
    public void setItems(List<ItemCarrito> items) { this.items = items; }
}
