package com.gestaofacil.controller;

import com.gestaofacil.model.Empresa;
import com.gestaofacil.model.Perfil;
import com.gestaofacil.model.Usuario;
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
import org.springframework.test.web.servlet.MvcResult;
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
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Card #003, etapa 1: cadastro, consulta e edicao de usuarios.
 *
 * COMO ESTES TESTES ENTRAM NO SISTEMA
 * Em vez de fazer POST /login e guardar a sessao, eles usam
 * .with(user(...)) do spring-security-test, que coloca um UsuarioAutenticado
 * direto na requisicao. E o mesmo objeto que o login de verdade produz, so
 * que sem repetir o formulario em cada teste. Quem testa o login em si sao os
 * testes do card #001.
 *
 * DOIS CPFs VALIDOS DE VERDADE
 * 111.444.777-35 e 529.982.247-25 passam nos digitos verificadores - da para
 * conferir a conta no papel. 111.111.111-11 e usado como exemplo de invalido.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UsuarioCadastroTests {

    private static final String CPF_VALIDO = "111.444.777-35";
    private static final String CPF_VALIDO_SO_NUMEROS = "11144477735";
    private static final String OUTRO_CPF_VALIDO = "529.982.247-25";
    private static final String CPF_INVALIDO = "111.111.111-11";

    private static final String ALFA = "construtora-alfa";
    private static final String BETA = "construtora-beta";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmpresaRepository empresaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Empresa alfa;
    private Empresa beta;
    private Usuario adminAlfa;
    private Usuario motoristaAlfa;
    private Usuario adminBeta;

    @BeforeEach
    void prepararDados() {
        alfa = empresaRepository.save(new Empresa("Construtora Alfa", ALFA, "Obra", "Obras"));
        beta = empresaRepository.save(new Empresa("Construtora Beta", BETA, "Obra", "Obras"));

        adminAlfa = criar(alfa, "chefe.alfa", "Chefe da Alfa", Perfil.ADMINISTRADOR);
        motoristaAlfa = criar(alfa, "motorista.alfa", "Motorista da Alfa", Perfil.MOTORISTA);
        adminBeta = criar(beta, "chefe.beta", "Chefe da Beta", Perfil.ADMINISTRADOR);
    }

    /* ==================================================================
       #003-RF01 - cadastro
       ================================================================== */

    @Test
    @DisplayName("#003-RF01: o Administrador cadastra um motorista com todos os campos")
    void cadastraMotoristaCompleto() throws Exception {

        MvcResult resultado = mockMvc.perform(cadastroDeMotorista("joao.silva", CPF_VALIDO)
                        .with(comoAdmin(adminAlfa, alfa)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/usuarios"))
                .andReturn();

        Usuario criado = buscar(alfa, "joao.silva").orElseThrow();

        assertEquals("João da Silva", criado.getNome());
        assertEquals(Perfil.MOTORISTA, criado.getPerfil());
        assertEquals(alfa.getId(), criado.getEmpresa().getId(), "#003-RF03: empresa do logado");
        assertTrue(criado.isAtivo());

        // #003-RF02 e RN003: a habilitacao e gravada em campos proprios.
        assertEquals("98765432100", criado.getCnh());
        assertEquals("D", criado.getCategoriaCnh());
        assertEquals("2030-12-31", criado.getValidadeCnh().toString());

        // #003-RN006: o CPF foi gravado so com numeros, sem a pontuacao digitada.
        assertEquals(CPF_VALIDO_SO_NUMEROS, criado.getCpf());

        // #003-RF04: nasce obrigado a trocar a senha no primeiro acesso.
        assertTrue(criado.isSenhaTemporaria());

        // #003-RN011: no banco ficou o hash BCrypt, nunca a senha legivel.
        String senhaMostrada = (String) resultado.getFlashMap().get("senhaTemporaria");
        assertNotNull(senhaMostrada, "a senha temporária deveria ter sido exibida");
        assertFalse(senhaMostrada.equals(criado.getSenha()));
        assertTrue(criado.getSenha().startsWith("$2a$"));
        assertTrue(passwordEncoder.matches(senhaMostrada, criado.getSenha()));
    }

    @Test
    @DisplayName("#003-RN001: sem nome, CPF, login ou perfil o cadastro nao passa")
    void cadastroSemCamposObrigatoriosVolta() throws Exception {
        long antes = usuarioRepository.count();

        mockMvc.perform(post("/usuarios")
                        .param("nome", "")
                        .param("cpf", "")
                        .param("login", "")
                        .with(csrf())
                        .with(comoAdmin(adminAlfa, alfa)))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors(
                        "usuarioForm", "nome", "cpf", "login", "perfil"));

        assertEquals(antes, usuarioRepository.count(), "nada deveria ter sido gravado");
    }

    @Test
    @DisplayName("#003-RN002: motorista sem CNH, categoria ou validade e recusado")
    void motoristaExigeHabilitacao() throws Exception {
        mockMvc.perform(post("/usuarios")
                        .param("nome", "Sem Habilitação")
                        .param("cpf", CPF_VALIDO)
                        .param("login", "sem.cnh")
                        .param("perfil", "MOTORISTA")
                        .with(csrf())
                        .with(comoAdmin(adminAlfa, alfa)))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors(
                        "usuarioForm", "cnh", "categoriaCnh", "validadeCnh"));

        assertTrue(buscar(alfa, "sem.cnh").isEmpty());
    }

    @Test
    @DisplayName("#003-RN002: administrador nao precisa de CNH")
    void administradorNaoExigeHabilitacao() throws Exception {
        mockMvc.perform(cadastroDeAdministrador("outro.chefe", CPF_VALIDO)
                        .with(comoAdmin(adminAlfa, alfa)))
                .andExpect(redirectedUrl("/usuarios"));

        Usuario criado = buscar(alfa, "outro.chefe").orElseThrow();
        assertEquals(Perfil.ADMINISTRADOR, criado.getPerfil());
        assertNull(criado.getCnh(), "administrador nao guarda CNH");
    }

    @Test
    @DisplayName("#003-RN006: CPF com digitos verificadores errados e recusado")
    void cpfInvalidoRecusado() throws Exception {
        mockMvc.perform(cadastroDeAdministrador("cpf.ruim", CPF_INVALIDO)
                        .with(comoAdmin(adminAlfa, alfa)))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("usuarioForm", "cpf"));

        assertTrue(buscar(alfa, "cpf.ruim").isEmpty());
    }

    /* ==================================================================
       #003-RN004 e RN005 - unicidade DENTRO da empresa
       ================================================================== */

    @Test
    @DisplayName("#003-RN004: o mesmo CPF nao entra duas vezes na mesma empresa")
    void cpfDuplicadoNaMesmaEmpresaRecusado() throws Exception {
        mockMvc.perform(cadastroDeAdministrador("primeiro", CPF_VALIDO)
                .with(comoAdmin(adminAlfa, alfa)));

        mockMvc.perform(cadastroDeAdministrador("segundo", CPF_VALIDO)
                        .with(comoAdmin(adminAlfa, alfa)))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("usuarioForm", "cpf"));

        assertTrue(buscar(alfa, "segundo").isEmpty());
    }

    @Test
    @DisplayName("#003-RN004: o mesmo CPF pode existir em duas empresas diferentes")
    void mesmoCpfEmOutraEmpresaAceito() throws Exception {
        mockMvc.perform(cadastroDeAdministrador("na.alfa", CPF_VALIDO)
                        .with(comoAdmin(adminAlfa, alfa)))
                .andExpect(redirectedUrl("/usuarios"));

        mockMvc.perform(cadastroDeAdministrador("na.beta", CPF_VALIDO)
                        .with(comoAdmin(adminBeta, beta)))
                .andExpect(redirectedUrl("/usuarios"));

        assertEquals(CPF_VALIDO_SO_NUMEROS, buscar(alfa, "na.alfa").orElseThrow().getCpf());
        assertEquals(CPF_VALIDO_SO_NUMEROS, buscar(beta, "na.beta").orElseThrow().getCpf());
    }

    @Test
    @DisplayName("#003-RN005: o mesmo login nao entra duas vezes na mesma empresa")
    void loginDuplicadoNaMesmaEmpresaRecusado() throws Exception {
        mockMvc.perform(cadastroDeAdministrador("repetido", CPF_VALIDO)
                .with(comoAdmin(adminAlfa, alfa)));

        mockMvc.perform(cadastroDeAdministrador("repetido", OUTRO_CPF_VALIDO)
                        .with(comoAdmin(adminAlfa, alfa)))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("usuarioForm", "login"));
    }

    @Test
    @DisplayName("#003-RN005: o mesmo login pode existir em duas empresas diferentes")
    void mesmoLoginEmOutraEmpresaAceito() throws Exception {
        mockMvc.perform(cadastroDeAdministrador("joao", CPF_VALIDO)
                        .with(comoAdmin(adminAlfa, alfa)))
                .andExpect(redirectedUrl("/usuarios"));

        mockMvc.perform(cadastroDeAdministrador("joao", OUTRO_CPF_VALIDO)
                        .with(comoAdmin(adminBeta, beta)))
                .andExpect(redirectedUrl("/usuarios"));

        assertTrue(buscar(alfa, "joao").isPresent());
        assertTrue(buscar(beta, "joao").isPresent());
    }

    /* ==================================================================
       #003-RF04 - senha temporaria
       ================================================================== */

    @Test
    @DisplayName("#003-RN011: a senha temporaria aparece uma vez e nao volta a aparecer")
    void senhaTemporariaApareceUmaVezSo() throws Exception {
        MvcResult cadastro = mockMvc.perform(cadastroDeAdministrador("efemero", CPF_VALIDO)
                        .with(comoAdmin(adminAlfa, alfa)))
                .andReturn();

        String senha = (String) cadastro.getFlashMap().get("senhaTemporaria");
        assertNotNull(senha);

        // Uma visita normal a lista (sem o flash attribute) nao traz a senha.
        mockMvc.perform(get("/usuarios").with(comoAdmin(adminAlfa, alfa)))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(senha))));
    }

    @Test
    @DisplayName("#003-RF04 + #001-RF07: o usuario criado entra e cai na troca de senha")
    void usuarioCriadoEntraECaiNaTrocaDeSenha() throws Exception {
        MvcResult cadastro = mockMvc.perform(cadastroDeMotorista("recem.criado", CPF_VALIDO)
                        .with(comoAdmin(adminAlfa, alfa)))
                .andReturn();

        String senhaTemporaria = (String) cadastro.getFlashMap().get("senhaTemporaria");

        // Faz o login de verdade, pelo formulario, com a senha sorteada.
        var sessao = mockMvc.perform(post("/login")
                        .param("empresa", ALFA)
                        .param("usuario", "recem.criado")
                        .param("senha", senhaTemporaria)
                        .with(csrf()))
                .andExpect(authenticated())
                .andReturn().getRequest().getSession();

        mockMvc.perform(get("/").session((org.springframework.mock.web.MockHttpSession) sessao))
                .andExpect(redirectedUrl("/trocar-senha"));
    }

    /* ==================================================================
       #003-RN007 - o perfil Operador nao e oferecido
       ================================================================== */

    @Test
    @DisplayName("#003-RN007: a tela so oferece Administrador e Motorista")
    void telaSoOfereceDoisPerfis() throws Exception {
        mockMvc.perform(get("/usuarios/novo").with(comoAdmin(adminAlfa, alfa)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("perfis",
                        List.of(Perfil.ADMINISTRADOR, Perfil.MOTORISTA)))
                // A caixa de selecao tem exatamente duas opcoes, e nenhuma
                // delas e OPERADOR. Repare que a conferencia procura
                // value="OPERADOR", e nao a palavra solta: a palavra aparece
                // no comentario do template que explica esta mesma regra.
                .andExpect(content().string(containsString("value=\"ADMINISTRADOR\"")))
                .andExpect(content().string(containsString("value=\"MOTORISTA\"")))
                .andExpect(content().string(not(containsString("value=\"OPERADOR\""))));

        /*
         * Este segundo trecho e o que protege a regra no futuro. O card #002
         * vai acrescentar o perfil OPERADOR ao enum; se alguem trocar a lista
         * por Perfil.values() naquele dia, ESTE teste quebra - e nao a tela em
         * producao, semanas depois.
         */
        assertEquals(List.of(Perfil.ADMINISTRADOR, Perfil.MOTORISTA),
                Perfil.atribuiveisPelaEmpresa(),
                "a lista de perfis atribuíveis deve continuar escrita à mão");
    }

    /* ==================================================================
       #003-RF08 - edicao
       ================================================================== */

    @Test
    @DisplayName("#003-RF08: a edicao altera os dados e nao mexe na senha")
    void edicaoAlteraOsDados() throws Exception {
        String senhaAntes = motoristaAlfa.getSenha();

        mockMvc.perform(post("/usuarios/" + motoristaAlfa.getId())
                        .param("nome", "Nome Corrigido")
                        .param("cargo", "Operador de máquinas")
                        .param("cpf", OUTRO_CPF_VALIDO)
                        .param("login", "motorista.alfa")
                        .param("perfil", "MOTORISTA")
                        .param("cnh", "11122233344")
                        .param("categoriaCnh", "E")
                        .param("validadeCnh", "2031-01-31")
                        .with(csrf())
                        .with(comoAdmin(adminAlfa, alfa)))
                .andExpect(redirectedUrl("/usuarios"));

        Usuario alterado = usuarioRepository.findById(motoristaAlfa.getId()).orElseThrow();
        assertEquals("Nome Corrigido", alterado.getNome());
        assertEquals("52998224725", alterado.getCpf());
        assertEquals("E", alterado.getCategoriaCnh());
        assertEquals(senhaAntes, alterado.getSenha(), "a edição não pode trocar a senha");
    }

    @Test
    @DisplayName("#003-RN014: o Administrador nao rebaixa o proprio usuario")
    void administradorNaoRebaixaASiMesmo() throws Exception {
        mockMvc.perform(post("/usuarios/" + adminAlfa.getId())
                        .param("nome", adminAlfa.getNome())
                        .param("cpf", CPF_VALIDO)
                        .param("login", adminAlfa.getLogin())
                        .param("perfil", "MOTORISTA")
                        .param("cnh", "98765432100")
                        .param("categoriaCnh", "B")
                        .param("validadeCnh", "2030-12-31")
                        .with(csrf())
                        .with(comoAdmin(adminAlfa, alfa)))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("usuarioForm", "perfil"));

        Usuario aindaAdmin = usuarioRepository.findById(adminAlfa.getId()).orElseThrow();
        assertEquals(Perfil.ADMINISTRADOR, aindaAdmin.getPerfil());
    }

    /* ==================================================================
       #003-RN009 - isolamento por empresa (regra 1 do projeto)
       ================================================================== */

    @Test
    @DisplayName("#003-RN009: a lista mostra so os usuarios da propria empresa")
    void listaNaoMostraUsuarioDeOutraEmpresa() throws Exception {
        mockMvc.perform(get("/usuarios").with(comoAdmin(adminAlfa, alfa)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Motorista da Alfa")))
                .andExpect(content().string(not(containsString("Chefe da Beta"))));
    }

    @Test
    @DisplayName("#003-RN009: abrir a edicao de usuario de outra empresa responde 404")
    void naoAbreEdicaoDeUsuarioDeOutraEmpresa() throws Exception {
        mockMvc.perform(get("/usuarios/" + adminBeta.getId() + "/editar")
                        .with(comoAdmin(adminAlfa, alfa)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("#003-RN009: salvar edicao de usuario de outra empresa responde 404")
    void naoSalvaEdicaoDeUsuarioDeOutraEmpresa() throws Exception {
        mockMvc.perform(post("/usuarios/" + adminBeta.getId())
                        .param("nome", "Invadido")
                        .param("cpf", CPF_VALIDO)
                        .param("login", "invadido")
                        .param("perfil", "ADMINISTRADOR")
                        .with(csrf())
                        .with(comoAdmin(adminAlfa, alfa)))
                .andExpect(status().isNotFound());

        Usuario intacto = usuarioRepository.findById(adminBeta.getId()).orElseThrow();
        assertEquals("Chefe da Beta", intacto.getNome());
        assertEquals("chefe.beta", intacto.getLogin());
    }

    @Test
    @DisplayName("#003-RN008: motorista nao entra na tela de usuarios")
    void motoristaNaoAcessaTelaDeUsuarios() throws Exception {
        mockMvc.perform(get("/usuarios").with(comoAdmin(motoristaAlfa, alfa)))
                .andExpect(status().isForbidden());
    }

    /* ==================================================================
       Auxiliares
       ================================================================== */

    /**
     * Coloca na requisicao o mesmo UsuarioAutenticado que o login de verdade
     * produziria. O nome do metodo fala em "admin" porque e o uso comum, mas
     * ele serve para qualquer perfil - e assim o teste do motorista consegue
     * provar que a tela o barra.
     */
    private RequestPostProcessor comoAdmin(Usuario usuario, Empresa empresa) {
        return user(new UsuarioAutenticado(usuario, empresa));
    }

    private MockHttpServletRequestBuilder cadastroDeMotorista(String login, String cpf) {
        return post("/usuarios")
                .param("nome", "João da Silva")
                .param("cargo", "Motorista")
                .param("cpf", cpf)
                .param("login", login)
                .param("perfil", "MOTORISTA")
                .param("cnh", "98765432100")
                .param("categoriaCnh", "D")
                .param("validadeCnh", "2030-12-31")
                .with(csrf());
    }

    private MockHttpServletRequestBuilder cadastroDeAdministrador(String login, String cpf) {
        return post("/usuarios")
                .param("nome", "Administrador " + login)
                .param("cpf", cpf)
                .param("login", login)
                .param("perfil", "ADMINISTRADOR")
                .with(csrf());
    }

    private Optional<Usuario> buscar(Empresa empresa, String login) {
        return usuarioRepository.findByEmpresaIdAndLogin(empresa.getId(), login);
    }

    private Usuario criar(Empresa empresa, String login, String nome, Perfil perfil) {
        Usuario usuario = new Usuario(empresa, nome, login,
                passwordEncoder.encode("senha-de-teste-123"), perfil);
        return usuarioRepository.save(usuario);
    }
}
