package com.examen.authapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada de la aplicacion. Un proyecto Spring Boot SIEMPRE
 * necesita una clase con un metodo main() que llame a SpringApplication.run().
 *
 * ¿Que hace @SpringBootApplication?
 * Es una "anotacion combo" que en realidad equivale a poner estas 3 juntas:
 *   - @Configuration      -> esta clase puede definir beans (objetos que Spring administra)
 *   - @EnableAutoConfiguration -> Spring intenta adivinar y configurar solo
 *       cosas como el servidor Tomcat, Jackson (JSON), etc, en base a las
 *       dependencias que pusiste en el pom.xml.
 *   - @ComponentScan      -> Spring escanea automaticamente este paquete
 *       (com.examen.authapi) y todos sus subpaquetes buscando clases anotadas
 *       con @Component, @Service, @Repository, @RestController, @Configuration,
 *       etc, para crearlas como "beans" (objetos administrados por Spring).
 *
 * Por eso es tan importante que TODAS tus clases vivan dentro de
 * com.examen.authapi (o subpaquetes): si las sacas de ahi, Spring no las
 * encuentra y no las "activa".
 */
@SpringBootApplication
public class AuthApiApplication {

    /**
     * SpringApplication.run() hace todo el trabajo pesado:
     * 1. Crea el "contenedor" de Spring (ApplicationContext).
     * 2. Escanea las clases (@ComponentScan) y crea los beans en el orden
     *    correcto segun sus dependencias (inyeccion de dependencias).
     * 3. Levanta el servidor web embebido (Tomcat, en nuestro caso) en el
     *    puerto configurado en application.yml (8083 para este servicio).
     * 4. Deja la aplicacion escuchando peticiones HTTP hasta que la detengas.
     */
    public static void main(String[] args) {
        SpringApplication.run(AuthApiApplication.class, args);
    }
}
