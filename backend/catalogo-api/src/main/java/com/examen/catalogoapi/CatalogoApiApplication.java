package com.examen.catalogoapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import com.examen.catalogoapi.model.Producto;
import com.examen.catalogoapi.repository.ProductoRepository;

import java.math.BigDecimal;

@SpringBootApplication
public class CatalogoApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(CatalogoApiApplication.class, args);
    }

    /**
     * Carga datos de ejemplo al arrancar, para que GET /api/v1/catalogo/productos
     * no devuelva vacio en la primera prueba.
     */
    @Bean
    CommandLineRunner cargarDatosDemo(ProductoRepository repo) {
        return args -> {
            if (repo.count() == 0) {
                repo.save(new Producto(null, "Teclado mecanico", "Switches rojos, retroiluminado", new BigDecimal("29990"), 25, "Perifericos", null));
                repo.save(new Producto(null, "Mouse inalambrico", "2.4GHz, sensor optico", new BigDecimal("12990"), 40, "Perifericos", null));
                repo.save(new Producto(null, "Monitor 24 pulgadas", "Full HD, 75Hz", new BigDecimal("119990"), 15, "Monitores", null));
                repo.save(new Producto(null, "Notebook 15 pulgadas", "16GB RAM, 512GB SSD", new BigDecimal("549990"), 8, "Computadores", null));
            }
        };
    }
}
