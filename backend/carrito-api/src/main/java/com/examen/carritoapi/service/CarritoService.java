package com.examen.carritoapi.service;

import com.examen.carritoapi.dto.AgregarItemRequest;
import com.examen.carritoapi.model.Carrito;
import com.examen.carritoapi.model.ItemCarrito;
import com.examen.carritoapi.repository.CarritoRepository;
import com.examen.carritoapi.repository.ItemCarritoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Logica de negocio del carrito. La regla de oro de este service: NUNCA
 * confia en un "carritoId" que venga del cliente. Todo se busca a partir
 * del usuarioId que saca el Controller del JWT — asi es imposible que un
 * usuario autenticado toque el carrito de otro, aunque adivine un id.
 */
@Service
public class CarritoService {

    // Dos repositorios: uno para el carrito (la "cabecera") y otro para
    // los items sueltos. En la practica casi no usamos itemRepository
    // directamente porque JPA guarda los items automaticamente en cascada
    // cuando guardamos el carrito (ver "cascade = CascadeType.ALL" en la
    // entidad Carrito) — lo dejamos inyectado por si se necesita a futuro
    // (ej. una consulta que busque un item por su id sin pasar por el carrito).
    private final CarritoRepository carritoRepository;
    private final ItemCarritoRepository itemRepository;

    public CarritoService(CarritoRepository carritoRepository, ItemCarritoRepository itemRepository) {
        this.carritoRepository = carritoRepository;
        this.itemRepository = itemRepository;
    }

    /**
     * Patron "find or create": intenta encontrar el carrito del usuario;
     * si es la primera vez que compra (no tiene carrito todavia), crea uno
     * vacio en el momento. Asi el frontend nunca necesita un endpoint
     * separado de "crear carrito" — simplemente pedir "mi carrito" ya lo
     * crea si hace falta.
     *
     * findByUsuarioId() devuelve Optional<Carrito> (puede venir vacio).
     * .orElseGet(...) recibe una funcion (Supplier) que SOLO se ejecuta si
     * el Optional esta vacio — a diferencia de .orElse(valor), que siempre
     * evalua el valor aunque no lo termine usando. Aca importa porque crear
     * el carrito (save) es una operacion con efecto secundario (escribe en
     * la base de datos), y no queremos ejecutarla si ya existia.
     *
     * @Transactional agrupa todo el metodo en una sola transaccion de base
     * de datos: si algo falla a la mitad, se revierte todo (no queda un
     * carrito "a medias"). Tambien evita errores de "LazyInitialization"
     * al acceder a carrito.getItems() mas tarde en el mismo hilo.
     */
    @Transactional
    public Carrito obtenerOCrearCarrito(String usuarioId) {
        return carritoRepository.findByUsuarioId(usuarioId)
                .orElseGet(() -> carritoRepository.save(new Carrito(usuarioId)));
    }

    /**
     * Agrega un producto al carrito. La regla de negocio clave: si el
     * producto YA esta en el carrito, no se crea un item duplicado — se
     * suma la cantidad al item existente (asi el carrito nunca muestra
     * "Teclado x1" y "Teclado x1" en dos filas separadas).
     */
    @Transactional
    public Carrito agregarItem(String usuarioId, AgregarItemRequest request) {
        Carrito carrito = obtenerOCrearCarrito(usuarioId);

        // .stream() convierte la lista de items en un flujo que podemos
        // procesar de forma declarativa (decimos "que" queremos, no "como"
        // iterar a mano con un for).
        carrito.getItems().stream()
                // .filter() se queda solo con los items cuyo productoId
                // coincide con el que estamos agregando.
                .filter(item -> item.getProductoId().equals(request.productoId()))
                // .findFirst() toma el primero que haya sobrevivido al
                // filtro (deberia haber a lo sumo uno, ya que no permitimos
                // duplicados). Devuelve Optional<ItemCarrito>.
                .findFirst()
                // .ifPresentOrElse(siExiste, siNoExiste): ejecuta la PRIMERA
                // funcion si el Optional trae un valor, o la SEGUNDA si esta
                // vacio. Es el equivalente moderno a un if/else sobre
                // isPresent(), pero mas expresivo con Optional.
                .ifPresentOrElse(
                        // Caso "ya existe": sumamos la cantidad nueva a la
                        // que ya tenia.
                        item -> item.setCantidad(item.getCantidad() + request.cantidad()),
                        // Caso "no existe": creamos un ItemCarrito nuevo y
                        // lo agregamos a la lista del carrito. Al pasarle
                        // "carrito" en el constructor, JPA sabe a que fila
                        // padre pertenece este item (relacion @ManyToOne).
                        () -> carrito.getItems().add(new ItemCarrito(
                                carrito,
                                request.productoId(),
                                request.nombreProducto(),
                                request.precioUnitario(),
                                request.cantidad()
                        ))
                );

        // save() sobre el carrito (la entidad "padre") tambien persiste los
        // cambios en sus items gracias a "cascade = CascadeType.ALL" — no
        // hace falta llamar itemRepository.save() por separado.
        return carritoRepository.save(carrito);
    }

    /**
     * Cambia la cantidad de UN item puntual dentro del carrito del usuario.
     * Nota que buscamos el item DENTRO de carrito.getItems() (la lista ya
     * filtrada por usuarioId via obtenerOCrearCarrito), nunca directo por
     * itemRepository.findById(itemId) — si hicieramos eso, un usuario
     * podria mandar el id de un item ajeno y editarlo. Buscarlo siempre
     * "dentro de mi propio carrito" es lo que garantiza el aislamiento
     * entre usuarios.
     */
    @Transactional
    public Carrito actualizarCantidad(String usuarioId, Long itemId, Integer nuevaCantidad) {
        Carrito carrito = obtenerOCrearCarrito(usuarioId);
        ItemCarrito item = carrito.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                // Si el item no esta en ESTE carrito (no existe, o es de
                // otro usuario), tiramos 404 en vez de dejar pasar el cambio.
                .orElseThrow(() -> new RecursoNoEncontradoException("Item " + itemId + " no existe en tu carrito"));

        item.setCantidad(nuevaCantidad);
        return carritoRepository.save(carrito);
    }

    /**
     * Elimina un item especifico. removeIf() recorre la lista, borra los
     * elementos que cumplen la condicion y devuelve true/false segun si
     * borro algo o no — nos sirve para saber si el item realmente existia
     * y decidir si lanzar 404.
     */
    @Transactional
    public Carrito eliminarItem(String usuarioId, Long itemId) {
        Carrito carrito = obtenerOCrearCarrito(usuarioId);
        boolean existia = carrito.getItems().removeIf(i -> i.getId().equals(itemId));
        if (!existia) {
            throw new RecursoNoEncontradoException("Item " + itemId + " no existe en tu carrito");
        }
        // Gracias a "orphanRemoval = true" en la entidad Carrito, quitar el
        // item de la lista en memoria es suficiente para que JPA lo borre
        // tambien de la base de datos al hacer save() — no hace falta un
        // itemRepository.delete() manual.
        return carritoRepository.save(carrito);
    }

    /** Vacia el carrito completo: misma logica de orphanRemoval que arriba. */
    @Transactional
    public Carrito vaciarCarrito(String usuarioId) {
        Carrito carrito = obtenerOCrearCarrito(usuarioId);
        carrito.getItems().clear();
        return carritoRepository.save(carrito);
    }
}
