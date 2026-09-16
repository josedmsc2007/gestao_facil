package com.gestaofacil.controller;

import com.gestaofacil.model.Empresa;
import com.gestaofacil.model.Perfil;
import com.gestaofacil.model.Usuario;
import com.gestaofacil.repository.EmpresaRepository;
import com.gestaofacil.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * #001.1-RF01: atalhos de empresa na tela /login.
 *
 * O QUE ESTES TESTES CONSEGUEM PROVAR
 * Gravar e exibir os atalhos acontece no navegador, em JavaScript, e o MockMvc
 * nao executa JavaScript. O que se testa aqui e a parte do SERVIDOR, que e
 * onde moram as regras de seguranca:
 * - a tela /login nunca manda empresa cadastrada nenhuma (RN001);
 * - a ordem de gravar so chega a quem fez login com sucesso (RN002).
 * O comportamento no navegador (aparecer, clicar, remover) e testado a mao.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AtalhosDeEmpresaTests {

    private static final String EMPRESA = "construtora-do-atalho";
    private static final String NOME_EMPRESA = "Construtora do Atalho";
    private static final String SENHA = "senha-boa-12345";

    /** Marca da tag que manda o navegador gravar a empresa. */
    private static final String ORDEM_DE_GRAVAR = "data-gravar-identificador";

    /** Marca da caixa que exibe os atalhos. */
    private static final String CAIXA_DE_ATALHOS = "id=\"atalhos-empresa\"";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmpresaRepository empresaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Empresa empresa;

    @BeforeEach
    void prepararDados() {
        empresa = empresaRepository.save(new Empresa(NOME_EMPRESA, EMPRESA, "Obra", "Obras"));
        criarUsuario("chefe", Perfil.ADMINISTRADOR, false);
        criarUsuario("motorista", Perfil.MOTORISTA, false);
        criarUsuario("novato", Perfil.MOTORISTA, true);
    }

    /* ------------------------------------------------------------------
       Onde a caixa de atalhos aparece
       ------------------------------------------------------------------ */

    @Test
    @DisplayName("#001.1-RF01: a tela /login tem a caixa de atalhos")
    void telaSemEmpresaTemACaixa() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(CAIXA_DE_ATALHOS)))
                .andExpect(content().string(containsString("/js/atalhos-empresa.js")));
    }

    @Test
    @DisplayName("#001.1-RF01: a tela com empresa no endereco nao tem a caixa")
    void telaDaEmpresaNaoTemACaixa() throws Exception {
        mockMvc.perform(get("/" + EMPRESA + "/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(CAIXA_DE_ATALHOS))));
    }

    @Test
    @DisplayName("#001.1-RF01: empresa nao encontrada no endereco tambem nao tem a caixa")
    void empresaNaoEncontradaNaoTemACaixa() throws Exception {
        mockMvc.perform(get("/empresa-que-nao-existe/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(CAIXA_DE_ATALHOS))));
    }

    /* ------------------------------------------------------------------
       RN001 - a tela publica nao revela empresas cadastradas
       ------------------------------------------------------------------ */

    @Test
    @DisplayName("#001.1-RN001: a tela /login nao contem nenhuma empresa cadastrada")
    void telaSemEmpresaNaoRevelaEmpresas() throws Exception {
        empresaRepository.save(new Empresa("Outra Cliente Ltda", "outra-cliente", "Obra", "Obras"));

        mockMvc.perform(get("/login"))
                .andExpect(content().string(not(containsString(EMPRESA))))
                .andExpect(content().string(not(containsString(NOME_EMPRESA))))
                .andExpect(content().string(not(containsString("outra-cliente"))))
                .andExpect(content().string(not(containsString("Outra Cliente Ltda"))));
    }

    @Test
    @DisplayName("#001.1-RN001: o script dos atalhos e publico e nao traz empresa alguma")
    void scriptNaoTrazEmpresas() throws Exception {
        mockMvc.perform(get("/js/atalhos-empresa.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(EMPRESA))))
                .andExpect(content().string(not(containsString(NOME_EMPRESA))));
    }

    /* ------------------------------------------------------------------
       RN002 - so grava depois de login com sucesso
       ------------------------------------------------------------------ */

    @Test
    @DisplayName("#001.1-RN002: visitar o endereco da empresa nao manda gravar")
    void visitaNaoGrava() throws Exception {
        mockMvc.perform(get("/" + EMPRESA + "/login"))
                .andExpect(content().string(not(containsString(ORDEM_DE_GRAVAR))));
    }

    @Test
    @DisplayName("#001.1-RN002: login recusado nao manda gravar")
    void loginRecusadoNaoGrava() throws Exception {
        MockHttpSession sessao = (MockHttpSession) mockMvc
                .perform(login("chefe", "senha-errada"))
                .andReturn().getRequest().getSession();

        mockMvc.perform(get("/" + EMPRESA + "/login").param("erro", "").session(sessao))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(ORDEM_DE_GRAVAR))));
    }

    @Test
    @DisplayName("#001.1-RN002: depois do login, a tela do administrador manda gravar a empresa")
    void painelGrava() throws Exception {
        telaInternaMandaGravar("chefe", "/painel");
    }

    @Test
    @DisplayName("#001.1-RN002: depois do login, a tela do motorista manda gravar a empresa")
    void lancamentosGrava() throws Exception {
        telaInternaMandaGravar("motorista", "/lancamentos");
    }

    @Test
    @DisplayName("#001.1-RN002: a troca de senha temporaria tambem manda gravar a empresa")
    void trocaDeSenhaGrava() throws Exception {
        telaInternaMandaGravar("novato", "/trocar-senha");
    }

    /* ------------------------------------------------------------------
       Auxiliares
       ------------------------------------------------------------------ */

    private void telaInternaMandaGravar(String login, String tela) throws Exception {
        MockHttpSession sessao = (MockHttpSession) mockMvc
                .perform(login(login, SENHA))
                .andReturn().getRequest().getSession();

        mockMvc.perform(get(tela).session(sessao))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        ORDEM_DE_GRAVAR + "=\"" + EMPRESA + "\"")))
                .andExpect(content().string(containsString(
                        "data-gravar-nome=\"" + NOME_EMPRESA + "\"")));
    }

    private MockHttpServletRequestBuilder login(String usuario, String senha) {
        return post("/login")
                .param("empresa", EMPRESA)
                .param("usuario", usuario)
                .param("senha", senha)
                .with(csrf());
    }

    private void criarUsuario(String login, Perfil perfil, boolean senhaTemporaria) {
        Usuario usuario = new Usuario(empresa, "Usuário " + login, login,
                passwordEncoder.encode(SENHA), perfil);
        usuario.setSenhaTemporaria(senhaTemporaria);
        usuarioRepository.save(usuario);
    }
}
