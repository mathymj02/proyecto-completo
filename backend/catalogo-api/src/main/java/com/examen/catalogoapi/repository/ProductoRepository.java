package com.examen.catalogoapi.repository;

import com.examen.catalogoapi.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Un "Repository" es la capa que habla directamente con la base de datos.
 * Aca esta la magia de Spring Data JPA: NO escribimos ninguna implementacion,
 * solo declaramos una interfaz. Spring genera la implementacion real por
 * detras, en tiempo de ejecucion (usando un "proxy dinamico").
 *
 * Al extender JpaRepository<Producto, Long> ya heredamos gratis metodos como:
 *   - findAll()          -> SELECT * FROM productos
 *   - findById(Long id)  -> SELECT * FROM productos WHERE id = ?
 *   - save(Producto p)   -> INSERT o UPDATE segun si el id ya existe
 *   - deleteById(Long id)-> DELETE FROM productos WHERE id = ?
 *   - count()            -> SELECT COUNT(*) FROM productos
 * (Long es el tipo del Id de la entidad Producto).
 *
 * findByCategoriaIgnoreCase es un metodo que ESCRIBIMOS NOSOTROS declarando
 * solo la firma, siguiendo una convencion de nombres que Spring Data JPA
 * entiende y traduce automaticamente a SQL:
 *   findBy + NombreDeLaPropiedad + IgnoreCase
 *   -> SELECT * FROM productos WHERE UPPER(categoria) = UPPER(?)
 * Esto se llama "query derivada" (derived query): Spring "parsea" el nombre
 * del metodo para saber que consulta generar. No hace falta escribir SQL a
 * mano para casos simples como este.
 */
public interface ProductoRepository extends JpaRepository<Producto, Long> {
    List<Producto> findByCategoriaIgnoreCase(String categoria);
}
