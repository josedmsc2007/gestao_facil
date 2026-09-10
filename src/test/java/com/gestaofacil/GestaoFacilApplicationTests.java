package com.gestaofacil;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Teste de fumaca ("smoke test"): sobe a aplicacao inteira e falha se algo
 * estiver mal configurado. Roda no H2 em memoria (ver
 * src/test/resources/application.properties), entao nao precisa do PostgreSQL.
 */
@SpringBootTest
class GestaoFacilApplicationTests {

    @Test
    void contextoCarrega() {
        // Sem codigo: se o Spring conseguir subir, o teste passa.
    }
}
