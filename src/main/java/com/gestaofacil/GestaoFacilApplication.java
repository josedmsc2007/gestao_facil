package com.gestaofacil;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Ponto de partida do sistema.
 *
 * A anotacao @SpringBootApplication liga tres coisas de uma vez:
 * - varre o pacote com.gestaofacil e os subpacotes procurando as classes
 *   anotadas (@Entity, @Repository, @Service, @Controller, @Configuration);
 * - liga a configuracao automatica do Spring Boot (banco, Thymeleaf, seguranca);
 * - marca esta classe como a configuracao principal da aplicacao.
 *
 * Por isso ela precisa ficar no pacote raiz com.gestaofacil: tudo que esta
 * "abaixo" dela e encontrado sozinho.
 */
@SpringBootApplication
public class GestaoFacilApplication {

    public static void main(String[] args) {
        SpringApplication.run(GestaoFacilApplication.class, args);
    }
}
