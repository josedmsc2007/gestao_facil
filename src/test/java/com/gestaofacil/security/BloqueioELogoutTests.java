package com.gestaofacil.security;

import com.gestaofacil.model.Empresa;
import com.gestaofacil.model.Perfil;
import com.gestaofacil.model.Usuario;
import com.gestaofacil.repository.EmpresaRepository;
import com.gestaofacil.repository.UsuarioRepository;
import com.gestaofacil.service.ControleDeTentativasService;
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

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes da Etapa 3: bloqueio por tentativas invalidas (RN004 e RN005) e
 * encerramento da sessao (#001-RF05).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BloqueioELogoutTests {

    private static final String EMPRESA = "empresa-do-bloqueio";
    private static final String SENHA = "senha-certa-123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmpresaRepository empresaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ControleDeTentativasService controleDeTentativas;

    private Empresa empresa;

    @BeforeEach
    void prepararDados() {
        empresa = empresaRepository.save(
                new Empresa("Empresa do Bloqueio", EMPRESA, "Obra", "Obras"));
        criarUsuario("motorista", Perfil.MOTORISTA);
        criarUsuario("chefe", Perfil.ADMINISTRADOR);
    }

    /* ------------------------------------------------------------------
       RN004 - bloqueio por tentativas invalidas
       ------------------------------------------------------------------ */

    @Test
    @DisplayName("RN004: quatro erros seguidos ainda nao bloqueiam")
    void quatroErrosNaoBloqueiam() throws Exception {
        errarSenha(4);

        Usuario usuario = buscar("motorista");
        assertEquals(4, usuario.getTentativasInvalidas());
        assertNull(usuario.getBloqueadoAte(), "não deveria estar bloqueado ainda");

        // e a senha certa continua funcionando
        mockMvc.perform(login("motorista", SENHA)).andExpect(authenticated());
    }

    @Test
    @DisplayName("RN004: o quinto erro bloqueia a conta por 30 minutos")
    void quintoErroBloqueia() throws Exception {
        errarSenha(5);

        Usuario usuario = buscar("motorista");
        assertEquals(5, usuario.getTentativasInvalidas());
        assertNotNull(usuario.getBloqueadoAte(), "deveria estar bloqueado");
        assertTrue(controleDeTentativas.estaBloqueado(usuario));

        // o bloqueio dura cerca de 30 minutos
        long minutos = controleDeTentativas.minutosRestantes(usuario);
        assertTrue(minutos > 28 && minutos <= 31,
                "o bloqueio deveria durar cerca de 30 minutos, mas faltam " + minutos);
    }

    /**
     * O ponto que mais importa da RN004: bloqueado nao entra NEM COM A SENHA
     * CERTA. Se entrasse, o bloqueio nao serviria para nada.
     */
    @Test
    @DisplayName("RN004: conta bloqueada nao entra nem com a senha certa")
    void bloqueadoNaoEntraNemComSenhaCerta() throws Exception {
        errarSenha(5);

        mockMvc.perform(login("motorista", SENHA))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/" + EMPRESA + "/login?bloqueado"));
    }

    /**
     * Tentar de novo enquanto bloqueado nao pode empurrar o fim do bloqueio
     * para a frente - senao a conta nunca mais abriria sozinha.
     */
    @Test
    @DisplayName("RN004: tentar durante o bloqueio nao aumenta o castigo")
    void tentativaDuranteBloqueioNaoRenovaOPrazo() throws Exception {
        errarSenha(5);
        LocalDateTime fimOriginal = buscar("motorista").getBloqueadoAte();

        errarSenha(3);   // insiste durante o bloqueio

        Usuario usuario = buscar("motorista");
        assertEquals(fimOriginal, usuario.getBloqueadoAte(),
                "o fim do bloqueio não deveria ter mudado");
        assertEquals(5, usuario.getTentativasInvalidas(),
                "a contagem não deveria continuar subindo");
    }

    @Test
    @DisplayName("RN004: acertar a senha zera a contagem de tentativas")
    void acertarZeraAContagem() throws Exception {
        errarSenha(3);
        assertEquals(3, buscar("motorista").getTentativasInvalidas());

        mockMvc.perform(login("motorista", SENHA)).andExpect(authenticated());

        assertEquals(0, buscar("motorista").getTentativasInvalidas(),
                "as tentativas deveriam ter zerado - a RN004 fala em erros CONSECUTIVOS");
    }

    @Test
    @DisplayName("RN004: passados os 30 minutos a conta abre sozinha")
    void bloqueioVenceSozinho() throws Exception {
        errarSenha(5);

        // Simula a passagem do tempo: joga o fim do bloqueio para o passado.
        Usuario usuario = buscar("motorista");
        usuario.setBloqueadoAte(LocalDateTime.now().minusMinutes(1));
        usuarioRepository.save(usuario);

        mockMvc.perform(login("motorista", SENHA)).andExpect(authenticated());
    }

    /* ------------------------------------------------------------------
       RN005 - desbloqueio pelo Administrador
       ------------------------------------------------------------------ */

    @Test
    @DisplayName("RN005: o administrador libera a conta antes dos 30 minutos")
    void administradorDesbloqueia() throws Exception {
        errarSenha(5);
        Long idBloqueado = buscar("motorista").getId();

        MockHttpSession sessaoDoChefe = entrarComo("chefe");

        // a conta aparece na lista de bloqueadas
        mockMvc.perform(get("/usuarios/bloqueados").session(sessaoDoChefe))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .content().string(org.hamcrest.Matchers.containsString("motorista")));

        // o administrador libera
        mockMvc.perform(post("/usuarios/" + idBloqueado + "/desbloquear")
                        .session(sessaoDoChefe).with(csrf()))
                .andExpect(redirectedUrl("/usuarios/bloqueados"));

        Usuario liberado = buscar("motorista");
        assertNull(liberado.getBloqueadoAte());
        assertEquals(0, liberado.getTentativasInvalidas());

        // e ja consegue entrar
        mockMvc.perform(login("motorista", SENHA)).andExpect(authenticated());
    }

    @Test
    @DisplayName("Motorista nao alcanca a tela de desbloqueio")
    void motoristaNaoDesbloqueia() throws Exception {
        MockHttpSession sessao = entrarComo("motorista");

        mockMvc.perform(get("/usuarios/bloqueados").session(sessao))
                .andExpect(status().isForbidden());
    }

    /**
     * Regra 1 do projeto: o id vem da URL, ou seja, do lado de fora. Um
     * administrador de uma empresa nao pode desbloquear usuario de outra,
     * mesmo acertando o id.
     */
    @Test
    @DisplayName("Regra 1: administrador nao desbloqueia usuario de outra empresa")
    void naoDesbloqueiaDeOutraEmpresa() throws Exception {
        Empresa vizinha = empresaRepository.save(
                new Empresa("Vizinha", "empresa-vizinha", "Obra", "Obras"));
        Usuario deOutraEmpresa = usuarioRepository.save(
                new Usuario(vizinha, "De Fora", "defora",
                        passwordEncoder.encode(SENHA), Perfil.MOTORISTA));
        controleDeTentativas.registrarFalha(deOutraEmpresa.getId(), vizinha.getId());

        MockHttpSession sessaoDoChefe = entrarComo("chefe");

        mockMvc.perform(post("/usuarios/" + deOutraEmpresa.getId() + "/desbloquear")
                        .session(sessaoDoChefe).with(csrf()))
                .andExpect(status().isNotFound());

        // e o usuario da outra empresa continua com a tentativa registrada
        assertEquals(1, usuarioRepository.findById(deOutraEmpresa.getId())
                .orElseThrow().getTentativasInvalidas());
    }

    /* ------------------------------------------------------------------
       #001-RF05 - encerramento da sessao
       ------------------------------------------------------------------ */

    @Test
    @DisplayName("RF05: sair encerra a sessao e volta ao login da empresa")
    void sairEncerraASessao() throws Exception {
        MockHttpSession sessao = entrarComo("motorista");

        mockMvc.perform(post("/logout").session(sessao).with(csrf()))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/" + EMPRESA + "/login?saiu"));
    }

    @Test
    @DisplayName("RF05: depois de sair, as telas internas nao abrem mais")
    void depoisDeSairNaoAbreTelaInterna() throws Exception {
        MockHttpSession sessao = entrarComo("motorista");

        // antes de sair, abre normalmente
        mockMvc.perform(get("/lancamentos").session(sessao))
                .andExpect(status().isOk());

        mockMvc.perform(post("/logout").session(sessao).with(csrf()));

        // com a MESMA sessao de antes, ja nao abre
        mockMvc.perform(get("/lancamentos").session(sessao))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    /**
     * O criterio "as paginas internas nao voltam pelo botao voltar do
     * navegador" depende de o servidor mandar o navegador NAO guardar a
     * pagina em cache. Sem esses cabecalhos, o botao voltar mostraria a tela
     * antiga, tirada da memoria do navegador, sem passar pelo servidor.
     */
    @Test
    @DisplayName("RF05: telas internas proibem o cache do navegador")
    void telasInternasNaoFicamNoCache() throws Exception {
        MockHttpSession sessao = entrarComo("motorista");

        mockMvc.perform(get("/lancamentos").session(sessao))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control",
                        org.hamcrest.Matchers.containsString("no-store")));
    }

    @Test
    @DisplayName("RF05: logout por GET nao funciona")
    void logoutPorGetNaoFunciona() throws Exception {
        MockHttpSession sessao = entrarComo("motorista");

        // O que importa nao e o codigo devolvido, e sim que a sessao
        // CONTINUE valendo: um GET nao pode derrubar ninguem.
        mockMvc.perform(get("/logout").session(sessao));

        mockMvc.perform(get("/lancamentos").session(sessao))
                .andExpect(status().isOk());
    }

    /* ------------------------------------------------------------------
       Auxiliares
       ------------------------------------------------------------------ */

    private void errarSenha(int vezes) throws Exception {
        for (int i = 0; i < vezes; i++) {
            mockMvc.perform(login("motorista", "senha-errada-" + i))
                    .andExpect(unauthenticated());
        }
    }

    private MockHttpSession entrarComo(String login) throws Exception {
        return (MockHttpSession) mockMvc.perform(login(login, SENHA))
                .andExpect(authenticated())
                .andReturn().getRequest().getSession();
    }

    private Usuario buscar(String login) {
        return usuarioRepository.findByEmpresaIdAndLogin(empresa.getId(), login).orElseThrow();
    }

    private MockHttpServletRequestBuilder login(String usuario, String senha) {
        return post("/login")
                .param("empresa", EMPRESA)
                .param("usuario", usuario)
                .param("senha", senha)
                .with(csrf());
    }

    private void criarUsuario(String login, Perfil perfil) {
        usuarioRepository.save(new Usuario(empresa, "Usuário " + login, login,
                passwordEncoder.encode(SENHA), perfil));
    }
}
