package com.gestaofacil.config;

import com.gestaofacil.model.Empresa;
import com.gestaofacil.model.Perfil;
import com.gestaofacil.model.Usuario;
import com.gestaofacil.repository.EmpresaRepository;
import com.gestaofacil.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A carga inicial (#002): empresa reservada, tres Operadores e a
 * construtora-teste.
 *
 * POR QUE UM BANCO SO PARA ESTE TESTE
 * E o unico teste que liga a carga (seed.habilitado=true). Se ele usasse o
 * mesmo H2 dos outros, a empresa "gestao-facil" criada aqui continuaria la
 * para os testes seguintes - e o EmpresaCadastroTests, que cria a sua propria
 * empresa da equipe, esbarraria no identificador repetido. Com outra URL,
 * o Spring sobe um banco separado.
 */
@SpringBootTest(properties = {
        "seed.habilitado=true",
        "spring.datasource.url=jdbc:h2:mem:carga-inicial;DB_CLOSE_DELAY=-1",
        // O application.properties dos testes SUBSTITUI o principal (tem o
        // mesmo nome), entao os valores da carga precisam vir daqui.
        "seed.empresa.identificador=construtora-teste",
        "seed.empresa.nome=Construtora Teste",
        "seed.empresa.rotulo-cc-singular=Obra",
        "seed.empresa.rotulo-cc-plural=Obras",
        "seed.admin.login=admin",
        "seed.admin.nome=Administrador de Teste",
        "seed.admin.senha=admin12345",
        "seed.operador.senha=operador12345"
})
@Transactional
class CargaInicialTests {

    @Autowired
    private CargaInicial cargaInicial;

    @Autowired
    private EmpresaRepository empresaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    @DisplayName("#002-RN010 e RN011: a carga cria a empresa da equipe com tres Operadores")
    void criaAEquipeDoSistema() {
        Empresa equipe = empresaRepository.findByIdentificador(Empresa.IDENTIFICADOR_DA_EQUIPE).orElseThrow();

        List<Usuario> operadores = usuarioRepository
                .findByEmpresaIdAndPerfilAndAtivoTrueOrderByNome(equipe.getId(), Perfil.OPERADOR);
        assertEquals(3, operadores.size());
        operadores.forEach(operador -> assertTrue(operador.isSenhaTemporaria(),
                "cada Operador troca a senha da carga no primeiro acesso"));

        assertTrue(empresaRepository.existsByIdentificador("construtora-teste"));
    }

    @Test
    @DisplayName("A carga pode rodar de novo sem duplicar nada")
    void cargaEIdempotente() throws Exception {
        long empresas = empresaRepository.count();
        long usuarios = usuarioRepository.count();

        cargaInicial.run();

        assertEquals(empresas, empresaRepository.count());
        assertEquals(usuarios, usuarioRepository.count());
    }

    /**
     * O banco de desenvolvimento do grupo ja tinha a construtora-teste antes
     * do card #002. Ate entao a carga parava ao encontra-la - e os Operadores
     * nunca seriam criados. Este teste simula esse banco apagando os
     * Operadores e confere que a proxima inicializacao os cria.
     *
     * (O delete aqui e so para montar o cenario do teste. O sistema em si
     * nunca apaga usuario - regra 3.)
     */
    @Test
    @DisplayName("Banco antigo, com a construtora-teste e sem Operadores, recebe os Operadores")
    void bancoAntigoRecebeOsOperadores() throws Exception {
        Empresa equipe = empresaRepository.findByIdentificador(Empresa.IDENTIFICADOR_DA_EQUIPE).orElseThrow();
        usuarioRepository.deleteAll(usuarioRepository
                .findByEmpresaIdAndPerfilAndAtivoTrueOrderByNome(equipe.getId(), Perfil.OPERADOR));
        assertTrue(empresaRepository.existsByIdentificador("construtora-teste"));

        cargaInicial.run();

        assertEquals(3, usuarioRepository
                .findByEmpresaIdAndPerfilAndAtivoTrueOrderByNome(equipe.getId(), Perfil.OPERADOR).size());
    }
}
