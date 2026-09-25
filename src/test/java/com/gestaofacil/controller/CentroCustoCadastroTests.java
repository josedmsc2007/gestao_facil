package com.gestaofacil.controller;

import com.gestaofacil.model.CentroCusto;
import com.gestaofacil.model.Empresa;
import com.gestaofacil.model.Perfil;
import com.gestaofacil.model.Usuario;
import com.gestaofacil.repository.CentroCustoRepository;
import com.gestaofacil.repository.EmpresaRepository;
import com.gestaofacil.repository.UsuarioRepository;
import com.gestaofacil.security.UsuarioAutenticado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Card #005: cadastro, consulta, edicao e desativacao de centros de custo.
 *
 * DUAS EMPRESAS COM ROTULOS DIFERENTES, DE PROPOSITO
 * A Alfa chama os centros de custo de "Obra"/"Obras" e a Beta de
 * "Rota"/"Rotas". So assim da para provar a regra 6 do projeto e a
 * #005-RN005: se a palavra "Obra" estivesse fixa em algum template, a tela da
 * Beta a mostraria e o teste quebraria.
 *
 * As duas empresas servem tambem para provar a regra 1 (isolamento): a Alfa
 * nao alcanca registro da Beta.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CentroCustoCadastroTests {

    private static final String ALFA = "construtora-alfa";
    private static final String BETA = "transportadora-beta";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmpresaRepository empresaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CentroCustoRepository centroCustoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Empresa alfa;
    private Empresa beta;
    private Usuario adminAlfa;
    private Usuario motoristaAlfa;
    private Usuario adminBeta;
    private CentroCusto rotaDaBeta;

    @BeforeEach
    void prepararDados() {
        alfa = empresaRepository.save(
                new Empresa("Construtora Alfa", ALFA, "Obra", "Obras"));
        beta = empresaRepository.save(
                new Empresa("Transportadora Beta", BETA, "Rota", "Rotas"));

        adminAlfa = criarUsuario(alfa, "chefe.alfa", "Chefe da Alfa", Perfil.ADMINISTRADOR);
        motoristaAlfa = criarUsuario(alfa, "motorista.alfa", "Motorista da Alfa",
                Perfil.MOTORISTA);
        adminBeta = criarUsuario(beta, "chefe.beta", "Chefe da Beta", Perfil.ADMINISTRADOR);

        rotaDaBeta = criarCentroCusto(beta, "Rota Litoral", "Santos", "SP");
    }

    /* ==================================================================
       #005-RF01 - cadastro
       ================================================================== */

    @Test
    @DisplayName("#005-RF01: o Administrador cadastra um centro de custo com todos os campos")
    void cadastraCentroDeCustoCompleto() throws Exception {

        mockMvc.perform(cadastro("Residencial Parque", "Campinas", "SP")
                        .param("endereco", "Rua das Flores, 100")
                        .with(comoUsuario(adminAlfa, alfa)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/centros-de-custo"));

        CentroCusto criado = buscar(alfa, "Residencial Parque").orElseThrow();

        assertEquals("Campinas", criado.getCidade());
        assertEquals("SP", criado.getEstado());
        assertEquals("Rua das Flores, 100", criado.getEndereco());

        // #005-RN003: a empresa vem de quem esta logado, nunca do formulario -
        // o CentroCustoForm nem tem esse campo.
        assertEquals(alfa.getId(), criado.getEmpresa().getId());

        // Regra 3: nasce ativo, e a desativacao e uma acao separada.
        assertTrue(criado.isAtivo());
    }

    @Test
    @DisplayName("#005-RF01: o endereco e opcional e fica nulo quando vem em branco")
    void enderecoEOpcional() throws Exception {

        mockMvc.perform(cadastro("Obra sem endereço", "Belo Horizonte", "MG")
                        .param("endereco", "   ")
                        .with(comoUsuario(adminAlfa, alfa)))
                .andExpect(redirectedUrl("/centros-de-custo"));

        // O navegador envia "" (ou espacos); o form transforma em nulo, para
        // "endereco em branco" nao ficar diferente de "nao informado".
        assertNull(buscar(alfa, "Obra sem endereço").orElseThrow().getEndereco());
    }

    @Test
    @DisplayName("#005-RN001: sem nome, cidade ou estado o cadastro nao passa")
    void cadastroSemCamposObrigatoriosVolta() throws Exception {
        long antes = centroCustoRepository.count();

        mockMvc.perform(post("/centros-de-custo")
                        .with(csrf())
                        .with(comoUsuario(adminAlfa, alfa)))
                .andExpect(status().isOk())   // volta para o formulario
                .andExpect(model().attributeHasFieldErrors("centroCustoForm",
                        "nome", "cidade", "estado"));

        assertEquals(antes, centroCustoRepository.count(), "nada pode ter sido gravado");
    }

    @Test
    @DisplayName("#005-RN001: sigla de estado inexistente e recusada")
    void estadoInvalidoERecusado() throws Exception {

        // A tela oferece so as 27 siglas, mas o POST e texto vindo do
        // navegador: quem barra de verdade e o servidor.
        mockMvc.perform(cadastro("Obra do XX", "Cidade", "XX")
                        .with(comoUsuario(adminAlfa, alfa)))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("centroCustoForm", "estado"));

        assertTrue(centroCustoRepository.findByEmpresaIdOrderByNome(alfa.getId()).isEmpty());
    }

    @Test
    @DisplayName("#005-RF01: a sigla do estado e gravada em maiusculas")
    void estadoEGravadoEmMaiusculas() throws Exception {

        mockMvc.perform(cadastro("Obra do interior", "Ribeirão Preto", "sp")
                        .with(comoUsuario(adminAlfa, alfa)))
                .andExpect(redirectedUrl("/centros-de-custo"));

        CentroCusto criado = buscar(alfa, "Obra do interior").orElseThrow();
        assertEquals("SP", criado.getEstado());
        assertEquals("Ribeirão Preto/SP", criado.getCidadeComEstado());
    }

    /* ==================================================================
       Regra 6 do projeto e #005-RN005 - o rotulo vem da empresa
       ================================================================== */

    @Test
    @DisplayName("#005-RN005: a tela da Alfa diz Obras e a da Beta diz Rotas")
    void telaUsaORotuloDaEmpresa() throws Exception {

        // A Construtora Alfa chama de "Obra"/"Obras"...
        mockMvc.perform(get("/centros-de-custo").with(comoUsuario(adminAlfa, alfa)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Obras")))
                .andExpect(content().string(containsString("Cadastrar Obra")))
                .andExpect(content().string(not(containsString("Rotas"))));

        // ...e a Transportadora Beta, de "Rota"/"Rotas", sem uma linha de
        // codigo diferente. Se algum template tivesse "Obra" fixo, esta
        // ultima conferencia quebraria.
        mockMvc.perform(get("/centros-de-custo").with(comoUsuario(adminBeta, beta)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Rotas")))
                .andExpect(content().string(containsString("Cadastrar Rota")))
                .andExpect(content().string(not(containsString("Obra"))));
    }

    @Test
    @DisplayName("#005-RN005: o formulario tambem usa o rotulo de cada empresa")
    void formularioUsaORotuloDaEmpresa() throws Exception {

        mockMvc.perform(get("/centros-de-custo/novo").with(comoUsuario(adminAlfa, alfa)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Cadastrar Obra")));

        mockMvc.perform(get("/centros-de-custo/novo").with(comoUsuario(adminBeta, beta)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Cadastrar Rota")))
                .andExpect(content().string(not(containsString("Obra"))));
    }

    /* ==================================================================
       #005-RF02 - consulta e edicao
       ================================================================== */

    @Test
    @DisplayName("#005-RF02: o Administrador edita um centro de custo ja cadastrado")
    void editaCentroDeCusto() throws Exception {

        CentroCusto centroCusto = criarCentroCusto(alfa, "Nome antigo", "Campinas", "SP");

        mockMvc.perform(post("/centros-de-custo/" + centroCusto.getId())
                        .param("nome", "Residencial Parque das Árvores")
                        .param("cidade", "Jundiaí")
                        .param("estado", "SP")
                        .param("endereco", "Avenida Brasil, 500")
                        .with(csrf())
                        .with(comoUsuario(adminAlfa, alfa)))
                .andExpect(redirectedUrl("/centros-de-custo"));

        CentroCusto salvo = centroCustoRepository.findById(centroCusto.getId()).orElseThrow();
        assertEquals("Residencial Parque das Árvores", salvo.getNome());
        assertEquals("Jundiaí", salvo.getCidade());
        assertEquals("Avenida Brasil, 500", salvo.getEndereco());
    }

    @Test
    @DisplayName("#005-RN004: a edicao nao mexe no campo ativo")
    void edicaoNaoAlteraAtivo() throws Exception {

        CentroCusto centroCusto = criarCentroCusto(alfa, "Obra encerrada", "Campinas", "SP");
        centroCusto.setAtivo(false);
        centroCustoRepository.save(centroCusto);

        // "ativo=true" enviado a mao: como o CentroCustoForm nao tem esse
        // campo, o Spring nao tem onde colocar o valor e ele se perde.
        mockMvc.perform(post("/centros-de-custo/" + centroCusto.getId())
                        .param("nome", "Obra encerrada (renomeada)")
                        .param("cidade", "Campinas")
                        .param("estado", "SP")
                        .param("ativo", "true")
                        .with(csrf())
                        .with(comoUsuario(adminAlfa, alfa)))
                .andExpect(redirectedUrl("/centros-de-custo"));

        CentroCusto salvo = centroCustoRepository.findById(centroCusto.getId()).orElseThrow();
        assertEquals("Obra encerrada (renomeada)", salvo.getNome());
        assertFalse(salvo.isAtivo(), "ativar e desativar tem acao propria");
    }

    /* ==================================================================
       #005-RF03 - ativacao e desativacao
       ================================================================== */

    @Test
    @DisplayName("#005-RF03 e RN004: desativar nao apaga o registro")
    void desativarNaoApaga() throws Exception {

        CentroCusto centroCusto = criarCentroCusto(alfa, "Obra terminada", "Campinas", "SP");
        long antes = centroCustoRepository.count();

        mockMvc.perform(post("/centros-de-custo/" + centroCusto.getId() + "/desativar")
                        .with(csrf())
                        .with(comoUsuario(adminAlfa, alfa)))
                .andExpect(redirectedUrl("/centros-de-custo"));

        assertEquals(antes, centroCustoRepository.count(), "regra 3: nada e apagado");
        assertFalse(centroCustoRepository.findById(centroCusto.getId())
                .orElseThrow().isAtivo());
    }

    @Test
    @DisplayName("#005-RF03: o inativo continua na lista, de onde e reativado")
    void inativoContinuaNaListaEVoltaAoSerAtivado() throws Exception {

        CentroCusto centroCusto = criarCentroCusto(alfa, "Obra parada", "Campinas", "SP");
        centroCusto.setAtivo(false);
        centroCustoRepository.save(centroCusto);

        mockMvc.perform(get("/centros-de-custo").with(comoUsuario(adminAlfa, alfa)))
                .andExpect(content().string(containsString("Obra parada")));

        mockMvc.perform(post("/centros-de-custo/" + centroCusto.getId() + "/ativar")
                        .with(csrf())
                        .with(comoUsuario(adminAlfa, alfa)))
                .andExpect(redirectedUrl("/centros-de-custo"));

        assertTrue(centroCustoRepository.findById(centroCusto.getId()).orElseThrow().isAtivo());
    }

    /**
     * Criterio do card: "centro de custo cadastrado fica disponivel para
     * vinculo na saida de veiculos".
     *
     * A tela de saida e do card #006, mas a consulta que ela vai usar ja
     * existe - e o inativo precisa ficar de fora dela, senao uma obra
     * encerrada continuaria recebendo lancamento novo.
     */
    @Test
    @DisplayName("#005-RF03: a consulta dos ativos (base do #006) deixa o inativo de fora")
    void consultaDosAtivosNaoTrazInativo() {

        criarCentroCusto(alfa, "Obra em andamento", "Campinas", "SP");
        CentroCusto encerrada = criarCentroCusto(alfa, "Obra encerrada", "Campinas", "SP");
        encerrada.setAtivo(false);
        centroCustoRepository.save(encerrada);

        List<CentroCusto> ativos = centroCustoRepository
                .findByEmpresaIdAndAtivoTrueOrderByNome(alfa.getId());

        assertEquals(1, ativos.size());
        assertEquals("Obra em andamento", ativos.get(0).getNome());
    }

    /* ==================================================================
       Regra 1 do projeto - isolamento por empresa (#005-RN003)
       ================================================================== */

    @Test
    @DisplayName("#005-RN003: a lista mostra so os centros de custo da propria empresa")
    void listaNaoMostraCentroDeCustoDeOutraEmpresa() throws Exception {

        criarCentroCusto(alfa, "Residencial Parque", "Campinas", "SP");

        mockMvc.perform(get("/centros-de-custo").with(comoUsuario(adminAlfa, alfa)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Residencial Parque")))
                .andExpect(content().string(not(containsString("Rota Litoral"))));
    }

    @Test
    @DisplayName("#005-RN003: abrir a edicao de registro de outra empresa responde 404")
    void naoAbreEdicaoDeOutraEmpresa() throws Exception {
        mockMvc.perform(get("/centros-de-custo/" + rotaDaBeta.getId() + "/editar")
                        .with(comoUsuario(adminAlfa, alfa)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("#005-RN003: salvar edicao de registro de outra empresa responde 404")
    void naoSalvaEdicaoDeOutraEmpresa() throws Exception {

        mockMvc.perform(post("/centros-de-custo/" + rotaDaBeta.getId())
                        .param("nome", "Invadida")
                        .param("cidade", "Campinas")
                        .param("estado", "SP")
                        .with(csrf())
                        .with(comoUsuario(adminAlfa, alfa)))
                .andExpect(status().isNotFound());

        CentroCusto intacto = centroCustoRepository.findById(rotaDaBeta.getId()).orElseThrow();
        assertEquals("Rota Litoral", intacto.getNome());
        assertEquals("Santos", intacto.getCidade());
    }

    @Test
    @DisplayName("#005-RN003: desativar registro de outra empresa responde 404")
    void naoDesativaDeOutraEmpresa() throws Exception {

        mockMvc.perform(post("/centros-de-custo/" + rotaDaBeta.getId() + "/desativar")
                        .with(csrf())
                        .with(comoUsuario(adminAlfa, alfa)))
                .andExpect(status().isNotFound());

        assertTrue(centroCustoRepository.findById(rotaDaBeta.getId()).orElseThrow().isAtivo());
    }

    @Test
    @DisplayName("#005-RN002: motorista nao entra na tela de centros de custo")
    void motoristaNaoAcessaTela() throws Exception {

        mockMvc.perform(get("/centros-de-custo").with(comoUsuario(motoristaAlfa, alfa)))
                .andExpect(status().isForbidden());

        mockMvc.perform(cadastro("Obra do motorista", "Campinas", "SP")
                        .with(comoUsuario(motoristaAlfa, alfa)))
                .andExpect(status().isForbidden());
    }

    /**
     * #002-RN008: o Operador nao ve dado operacional de empresa nenhuma.
     *
     * Ele nem precisa de linha no SecurityConfig: a ultima regra de la e
     * "fechado por padrao" (so ADMINISTRADOR e MOTORISTA), entao a tela nova
     * ja nasce proibida para ele. Este teste existe para provar isso - se um
     * dia alguem afrouxar aquela linha, ele quebra.
     */
    @Test
    @DisplayName("#002-RN008: Operador leva 403 na tela de centros de custo")
    void operadorNaoAcessaTela() throws Exception {

        Empresa equipe = empresaRepository.save(new Empresa("Equipe Gestão Fácil",
                Empresa.IDENTIFICADOR_DA_EQUIPE, "Centro de custo", "Centros de custo"));
        Usuario operador = criarUsuario(equipe, "jose.lopes", "José Lopes", Perfil.OPERADOR);

        mockMvc.perform(get("/centros-de-custo").with(comoUsuario(operador, equipe)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/centros-de-custo/novo").with(comoUsuario(operador, equipe)))
                .andExpect(status().isForbidden());
    }

    /* ==================================================================
       Auxiliares
       ================================================================== */

    /**
     * Coloca na requisicao o mesmo UsuarioAutenticado que o login de verdade
     * produziria. E por ele que o rotulo da empresa chega a tela: o
     * UsuarioAutenticado copia rotuloCcSingular e rotuloCcPlural da empresa.
     */
    private RequestPostProcessor comoUsuario(Usuario usuario, Empresa empresa) {
        return user(new UsuarioAutenticado(usuario, empresa));
    }

    /** O POST de cadastro, sem o endereco (que e opcional). */
    private MockHttpServletRequestBuilder cadastro(String nome, String cidade, String estado) {
        return post("/centros-de-custo")
                .param("nome", nome)
                .param("cidade", cidade)
                .param("estado", estado)
                .with(csrf());
    }

    private Optional<CentroCusto> buscar(Empresa empresa, String nome) {
        return centroCustoRepository.findByEmpresaIdOrderByNome(empresa.getId()).stream()
                .filter(centroCusto -> centroCusto.getNome().equals(nome))
                .findFirst();
    }

    private Usuario criarUsuario(Empresa empresa, String login, String nome, Perfil perfil) {
        Usuario usuario = new Usuario(empresa, nome, login,
                passwordEncoder.encode("senha-de-teste-123"), perfil);
        return usuarioRepository.save(usuario);
    }

    private CentroCusto criarCentroCusto(Empresa empresa, String nome,
                                         String cidade, String estado) {
        CentroCusto salvo = centroCustoRepository.save(
                new CentroCusto(empresa, nome, cidade, estado, null));
        assertNotNull(salvo.getId());
        return salvo;
    }
}
