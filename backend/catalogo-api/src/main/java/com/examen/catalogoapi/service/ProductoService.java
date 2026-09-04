package com.examen.catalogoapi.service;

import com.examen.catalogoapi.dto.ProductoRequest;
import com.examen.catalogoapi.model.Producto;
import com.examen.catalogoapi.repository.ProductoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * La capa "Service" contiene la LOGICA DE NEGOCIO: las reglas y decisiones
 * de la aplicacion (que campos actualizar, que pasa si no existe algo, como
 * combinar datos, etc). El Controller NO deberia tener esta logica — su
 * unico trabajo es traducir HTTP <-> llamadas a metodos Java. El Repository
 * tampoco: el solo sabe hablar con la base de datos.
 *
 * Esta separacion en 3 capas (Controller -> Service -> Repository) es EL
 * patron estandar en aplicaciones Spring, y la razon es mantenibilidad:
 * si mañana cambias de base de datos, solo tocas el Repository. Si cambias
 * de REST a GraphQL, solo tocas el Controller. La logica de negocio del
 * Service no se entera de ninguno de esos cambios.
 *
 * @Service es una especializacion de @Component: le dice a Spring "esta
 * clase es un bean, creala y administrala tu, y ademas semanticamente es
 * parte de la capa de servicio" (esto ultimo es solo para claridad del
 * codigo, Spring lo trata igual que @Component internamente).
 */
@Service
public class ProductoService {

    /**
     * Inyeccion de dependencias por constructor: Spring ve que este service
     * necesita un ProductoRepository, busca el bean correspondiente (que el
     * mismo genero automaticamente al ver la interfaz que extiende
     * JpaRepository) y lo pasa aca. Nosotros nunca escribimos "new
     * ProductoRepository()" en ningun lado — eso lo hace Spring por nosotros.
     */
    private final ProductoRepository repository;

    public ProductoService(ProductoRepository repository) {
        this.repository = repository;
    }

    /**
     * Si viene una categoria, filtra por ella; si no, trae todos. Esto es
     * logica de negocio simple pero real: decide QUE consulta ejecutar segun
     * el parametro recibido.
     */
    public List<Producto> listarTodos(String categoria) {
        if (categoria != null && !categoria.isBlank()) {
            return repository.findByCategoriaIgnoreCase(categoria);
        }
        return repository.findAll();
    }

    /**
     * findById() devuelve un Optional<Producto> (puede o no traer un valor).
     * .orElseThrow(...) es la forma idiomatica en Java moderno de decir
     * "si hay valor, devuelvelo; si no, lanza esta excepcion en su lugar" —
     * evita tener que hacer "if (optional.isPresent()) {...} else {...}"
     * a mano cada vez.
     */
    public Producto buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado con id " + id));
    }

    /**
     * Convierte el DTO de entrada (ProductoRequest) en una entidad Producto
     * y la guarda. id=null porque es un producto NUEVO: la base de datos le
     * asigna el id real (ver @GeneratedValue en la entidad).
     */
    public Producto crear(ProductoRequest request) {
        Producto producto = new Producto(
                null,
                request.nombre(),
                request.descripcion(),
                request.precio(),
                request.stock(),
                request.categoria(),
                request.imagenUrl()
        );
        return repository.save(producto);
    }

    /**
     * Busca el producto existente (lanza 404 si no existe, gracias a
     * buscarPorId), le pisa todos los campos con los nuevos valores, y lo
     * vuelve a guardar. save() detecta que el id ya existe y hace UPDATE en
     * vez de INSERT.
     */
    public Producto actualizar(Long id, ProductoRequest request) {
        Producto existente = buscarPorId(id);
        existente.setNombre(request.nombre());
        existente.setDescripcion(request.descripcion());
        existente.setPrecio(request.precio());
        existente.setStock(request.stock());
        existente.setCategoria(request.categoria());
        existente.setImagenUrl(request.imagenUrl());
        return repository.save(existente);
    }

    /** Primero verifica que exista (404 si no), despues lo borra. */
    public void eliminar(Long id) {
        Producto existente = buscarPorId(id);
        repository.delete(existente);
    }
}
