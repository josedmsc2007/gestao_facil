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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes da Etapa 2: telas de login e de troca de senha, e o redirecionamento
 * por perfil.
 *
 * Como na Etapa 1, cada teste cria os proprios dados e tudo e desfeito no
 * final (Transactional).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TelasLoginTests {

    private static final String EMPRESA = "construtora-do-teste";
    private static final String SENHA = "senha-boa-12345";

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
        empresa = empresaRepository.save(
                new Empresa("Construtora do Teste", EMPRESA, "Obra", "Obras"));
        criarUsuario("chefe", Perfil.ADMINISTRADOR, false);
        criarUsuario("motorista", Perfil.MOTORISTA, false);
        criarUsuario("novato", Perfil.MOTORISTA, true);   // senha temporaria
    }

    /* ------------------------------------------------------------------
       A tela de login
       ------------------------------------------------------------------ */

    @Test
    @DisplayName("#001-RF02: no endereco da empresa nao aparece campo de empresa")
    void telaDaEmpresaNaoPedeEmpresa() throws Exception {
        mockMvc.perform(get("/" + EMPRESA + "/login"))
                .andExpect(status().isOk())
                // mostra o nome da empresa, para o funcionario conferir
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Construtora do Teste")))
                // e o campo de empresa vai escondido, nao preenchivel
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "type=\"hidden\" name=\"empresa\"")));
    }

    @Test
    @DisplayName("#001-RF02: endereco sem empresa mostra o campo de empresa")
    void telaSemEmpresaPedeEmpresa() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "<label for=\"empresa\"")));
    }

    @Test
    @DisplayName("O atalho /empresa leva para a tela de login da empresa")
    void atalhoDaEmpresa() throws Exception {
        mockMvc.perform(get("/" + EMPRESA))
                .andExpect(redirectedUrl("/" + EMPRESA + "/login"));
    }

    /* ------------------------------------------------------------------
       Falha de login
       ------------------------------------------------------------------ */

    @Test
    @DisplayName("Login recusado volta para a tela de login DA EMPRESA")
    void falhaVoltaParaAEmpresa() throws Exception {
        mockMvc.perform(login(EMPRESA, "chefe", "senha-errada"))
                .andExpect(redirectedUrl("/" + EMPRESA + "/login?erro"));
    }

    /**
     * O campo "empresa" vem do formulario, ou seja, pode ser adulterado.
     * Se aceitassemos qualquer texto, um endereco externo no campo faria o
     * sistema redirecionar o usuario para fora - a falha "open redirect".
     */
    @Test
    @DisplayName("Empresa adulterada no formulario nao redireciona para fora do sistema")
    void falhaNaoRedirecionaParaForaDoSistema() throws Exception {
        mockMvc.perform(login("https://site-falso.example.com", "chefe", SENHA))
                .andExpect(redirectedUrl("/login?erro"));
    }

    /* ------------------------------------------------------------------
       RN006 - redirecionamento por perfil
       ------------------------------------------------------------------ */

    @Test
    @DisplayName("RN006: administrador entra no painel de gestao")
    void administradorVaiParaOPainel() throws Exception {
        var sessao = mockMvc.perform(login(EMPRESA, "chefe", SENHA))
                .andExpect(authenticated())
                .andExpect(redirectedUrl("/"))
                .andReturn().getRequest().getSession();

        mockMvc.perform(get("/").session((org.springframework.mock.web.MockHttpSession) sessao))
                .andExpect(redirectedUrl("/painel"));
    }

    @Test
    @DisplayName("RN006: motorista entra na tela de lancamentos")
    void motoristaVaiParaLancamentos() throws Exception {
        var sessao = mockMvc.perform(login(EMPRESA, "motorista", SENHA))
                .andExpect(authenticated())
                .andReturn().getRequest().getSession();

        mockMvc.perform(get("/").session((org.springframework.mock.web.MockHttpSession) sessao))
                .andExpect(redirectedUrl("/lancamentos"));
    }

    /**
     * Os dois testes acima param no redirecionamento. Estes abrem mesmo as
     * telas: se houver erro de Thymeleaf num template, e aqui que aparece.
     */
    @Test
    @DisplayName("O painel do administrador abre e usa o rotulo da empresa")
    void painelAbreEUsaORotulo() throws Exception {
        var sessao = (org.springframework.mock.web.MockHttpSession)
                mockMvc.perform(login(EMPRESA, "chefe", SENHA))
                        .andReturn().getRequest().getSession();

        mockMvc.perform(get("/painel").session(sessao))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Construtora do Teste")))
                // regra 6 do projeto: o texto vem do rotulo da empresa
                .andExpect(content().string(org.hamcrest.Matchers.containsString("obras")));
    }

    @Test
    @DisplayName("A tela de lancamentos do motorista abre")
    void lancamentosAbre() throws Exception {
        var sessao = (org.springframework.mock.web.MockHttpSession)
                mockMvc.perform(login(EMPRESA, "motorista", SENHA))
                        .andReturn().getRequest().getSession();

        mockMvc.perform(get("/lancamentos").session(sessao))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Meus lançamentos")));
    }

    @Test
    @DisplayName("Motorista nao entra no painel do administrador")
    void motoristaNaoEntraNoPainel() throws Exception {
        var sessao = mockMvc.perform(login(EMPRESA, "motorista", SENHA))
                .andReturn().getRequest().getSession();

        mockMvc.perform(get("/painel").session((org.springframework.mock.web.MockHttpSession) sessao))
                .andExpect(status().isForbidden());
    }

    /* ------------------------------------------------------------------
       RF07 - troca obrigatoria da senha temporaria
       ------------------------------------------------------------------ */

    @Test
    @DisplayName("RF07: senha temporaria e levada direto para a troca de senha")
    void senhaTemporariaVaiParaATroca() throws Exception {
        var sessao = (org.springframework.mock.web.MockHttpSession)
                mockMvc.perform(login(EMPRESA, "novato", SENHA))
                        .andExpect(authenticated())
                        .andReturn().getRequest().getSession();

        mockMvc.perform(get("/").session(sessao))
                .andExpect(redirectedUrl("/trocar-senha"));
    }

    @Test
    @DisplayName("RF07: senha temporaria nao alcanca as demais telas")
    void senhaTemporariaNaoAlcancaOutrasTelas() throws Exception {
        var sessao = (org.springframework.mock.web.MockHttpSession)
                mockMvc.perform(login(EMPRESA, "novato", SENHA))
                        .andReturn().getRequest().getSession();

        // tenta pular a troca digitando o endereco na barra do navegador
        mockMvc.perform(get("/lancamentos").session(sessao))
                .andExpect(redirectedUrl("/trocar-senha"));
    }

    @Test
    @DisplayName("RN003: senha com menos de 8 caracteres e recusada")
    void senhaCurtaERecusada() throws Exception {
        var sessao = (org.springframework.mock.web.MockHttpSession)
                mockMvc.perform(login(EMPRESA, "novato", SENHA))
                        .andReturn().getRequest().getSession();

        mockMvc.perform(post("/trocar-senha").session(sessao)
                        .param("novaSenha", "curta")
                        .param("confirmacaoSenha", "curta")
                        .with(csrf()))
                .andExpect(status().isOk())   // volta para a tela, sem redirecionar
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "pelo menos 8 caracteres")));

        // e a senha no banco continua sendo a antiga
        Usuario novato = usuarioRepository
                .findByEmpresaIdAndLogin(empresa.getId(), "novato").orElseThrow();
        assertTrue(novato.isSenhaTemporaria(), "a senha deveria continuar temporária");
        assertTrue(passwordEncoder.matches(SENHA, novato.getSenha()),
                "a senha no banco não deveria ter mudado");
    }

    @Test
    @DisplayName("Senhas diferentes nos dois campos sao recusadas")
    void senhasDiferentesSaoRecusadas() throws Exception {
        var sessao = (org.springframework.mock.web.MockHttpSession)
                mockMvc.perform(login(EMPRESA, "novato", SENHA))
                        .andReturn().getRequest().getSession();

        mockMvc.perform(post("/trocar-senha").session(sessao)
                        .param("novaSenha", "senha-nova-1234")
                        .param("confirmacaoSenha", "senha-nova-9999")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "não são iguais")));
    }

    @Test
    @DisplayName("RF07: troca concluida libera o sistema e vale no banco")
    void trocaConcluidaLiberaOSistema() throws Exception {
        var sessao = (org.springframework.mock.web.MockHttpSession)
                mockMvc.perform(login(EMPRESA, "novato", SENHA))
                        .andReturn().getRequest().getSession();

        mockMvc.perform(post("/trocar-senha").session(sessao)
                        .param("novaSenha", "minha-senha-nova")
                        .param("confirmacaoSenha", "minha-senha-nova")
                        .with(csrf()))
                .andExpect(redirectedUrl("/"));

        // O banco foi atualizado...
        Usuario novato = usuarioRepository
                .findByEmpresaIdAndLogin(empresa.getId(), "novato").orElseThrow();
        assertFalse(novato.isSenhaTemporaria(), "a senha não deveria mais ser temporária");
        assertTrue(passwordEncoder.matches("minha-senha-nova", novato.getSenha()),
                "a nova senha deveria estar gravada (em hash)");

        // ...e a SESSAO tambem: o usuario nao volta mais para a tela de troca.
        mockMvc.perform(get("/").session(sessao))
                .andExpect(redirectedUrl("/lancamentos"));
    }

    /* ------------------------------------------------------------------
       Auxiliares
       ------------------------------------------------------------------ */

    private MockHttpServletRequestBuilder login(String empresa, String usuario, String senha) {
        return post("/login")
                .param("empresa", empresa)
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
