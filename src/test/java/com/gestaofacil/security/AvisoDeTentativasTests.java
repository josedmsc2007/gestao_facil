package com.gestaofacil.security;

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
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

/**
 * Testes do #001.1-RF02: aviso de tentativas restantes.
 *
 * COMO ESTES TESTES SIMULAM "O MESMO NAVEGADOR"
 * A contagem vive na sessao. Por isso cada teste cria um MockHttpSession e o
 * passa em todas as requisicoes - e o equivalente ao cookie JSESSIONID que o
 * navegador de verdade reenvia a cada pedido. Uma sessao nova = outro
 * navegador (ou aba anonima).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AvisoDeTentativasTests {

    private static final String EMPRESA = "empresa-do-aviso";
    private static final String SENHA = "senha-certa-123";

    private static final String AVISO_RESTAM_2 = "restam 2 tentativas";
    private static final String AVISO_RESTA_1 = "resta 1 tentativa";
    private static final String AVISO_LIMITE = "limite de tentativas foi atingido";
    private static final String QUALQUER_AVISO = "antes do bloqueio da conta";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmpresaRepository empresaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AvisoDeTentativasNaSessao avisoDeTentativas;

    private Empresa empresa;

    @BeforeEach
    void prepararDados() {
        empresa = empresaRepository.save(
                new Empresa("Empresa do Aviso", EMPRESA, "Obra", "Obras"));
        criarUsuario("joao", Perfil.MOTORISTA);
        criarUsuario("maria", Perfil.MOTORISTA);
        criarUsuario("chefe", Perfil.ADMINISTRADOR);
    }

    @Test
    @DisplayName("RN004: primeira e segunda falha mostram so a mensagem generica")
    void duasPrimeirasFalhasSemAviso() throws Exception {
        MockHttpSession navegador = new MockHttpSession();

        for (int i = 0; i < 2; i++) {
            errarEAbrirATela(navegador, "joao")
                    .andExpect(content().string(containsString("Usuário ou senha incorretos.")))
                    .andExpect(content().string(not(containsString(QUALQUER_AVISO))));
        }
    }

    @Test
    @DisplayName("RF02: a partir da terceira falha a tela informa quantas tentativas restam")
    void terceiraFalhaEmDianteMostraOAviso() throws Exception {
        MockHttpSession navegador = new MockHttpSession();
        errarEAbrirATela(navegador, "joao");
        errarEAbrirATela(navegador, "joao");

        errarEAbrirATela(navegador, "joao")
                .andExpect(content().string(containsString("Usuário ou senha incorretos.")))
                .andExpect(content().string(containsString(AVISO_RESTAM_2)));
        errarEAbrirATela(navegador, "joao")
                .andExpect(content().string(containsString(AVISO_RESTA_1)));
        errarEAbrirATela(navegador, "joao")
                .andExpect(content().string(containsString(AVISO_LIMITE)));
    }

    /**
     * O teste mais importante do card. Dois navegadores fazem a mesma
     * sequencia: um com login real, outro com login inventado. As telas
     * precisam sair IGUAIS, caractere por caractere. Se a tela mostrasse nome,
     * perfil ou qualquer coisa ligada ao usuario (RN009), ou se o aviso so
     * existisse para usuario real (RN005), as duas paginas seriam diferentes.
     *
     * A unica diferenca permitida e o token CSRF, que e sorteado por sessao;
     * por isso ele e apagado antes da comparacao.
     */
    @Test
    @DisplayName("RN005/RN009: a tela e identica para login existente e inventado")
    void telaIdenticaParaLoginExistenteEInventado() throws Exception {
        MockHttpSession navegadorReal = new MockHttpSession();
        MockHttpSession navegadorInventado = new MockHttpSession();

        // Quatro falhas: cobre as duas sem aviso e as duas com aviso. A quinta
        // fica de fora porque, dali em diante, o #001 ja mostra "conta
        // bloqueada" so para usuario real - diferenca aceita no card #001.
        for (int i = 1; i <= 4; i++) {
            String telaReal = errarEAbrirATela(navegadorReal, "joao")
                    .andReturn().getResponse().getContentAsString();
            String telaInventada = errarEAbrirATela(navegadorInventado, "login-inventado")
                    .andReturn().getResponse().getContentAsString();

            assertEquals(semTokenCsrf(telaReal), semTokenCsrf(telaInventada),
                    "a tela da falha " + i + " não deveria revelar se o login existe");
        }
    }

    @Test
    @DisplayName("Errar o nome do usuario tambem faz a contagem exibida avancar")
    void loginInventadoTambemContaTentativas() throws Exception {
        MockHttpSession navegador = new MockHttpSession();
        errarEAbrirATela(navegador, "nao-existe");
        errarEAbrirATela(navegador, "nao-existe");

        errarEAbrirATela(navegador, "nao-existe")
                .andExpect(content().string(containsString(AVISO_RESTAM_2)));
    }

    /**
     * RN007: cada login tem a sua contagem. Errar "maria" no meio nao herda as
     * falhas de "joao" - e tambem nao zera as dele (RN006: errar o nome nao
     * reinicia contagem alguma).
     */
    @Test
    @DisplayName("RN007: trocar o login digitado reinicia a contagem exibida")
    void cadaLoginTemASuaContagem() throws Exception {
        MockHttpSession navegador = new MockHttpSession();
        errarEAbrirATela(navegador, "joao");
        errarEAbrirATela(navegador, "joao");

        // primeira falha de maria: sem aviso, apesar das duas de joao
        errarEAbrirATela(navegador, "maria")
                .andExpect(content().string(not(containsString(QUALQUER_AVISO))));

        // joao volta e esta na terceira falha dele
        errarEAbrirATela(navegador, "joao")
                .andExpect(content().string(containsString(AVISO_RESTAM_2)));
    }

    /**
     * O mesmo login em OUTRA empresa e outra pessoa (RN007 do #001), entao
     * tambem tem contagem separada.
     */
    @Test
    @DisplayName("A contagem e separada por empresa")
    void contagemSeparadaPorEmpresa() throws Exception {
        MockHttpSession navegador = new MockHttpSession();
        errarEAbrirATela(navegador, "joao");
        errarEAbrirATela(navegador, "joao");

        mockMvc.perform(tentativa("outra-empresa", "joao", "errada").session(navegador));

        assertEquals(2, avisoDeTentativas.falhasRegistradas(navegador, EMPRESA, "joao"));
        assertEquals(1, avisoDeTentativas.falhasRegistradas(navegador, "outra-empresa", "joao"));
    }

    @Test
    @DisplayName("RN006: login bem-sucedido zera a contagem so daquele login")
    void sucessoZeraSoAqueleLogin() throws Exception {
        MockHttpSession navegador = new MockHttpSession();
        errarEAbrirATela(navegador, "joao");
        errarEAbrirATela(navegador, "joao");
        errarEAbrirATela(navegador, "joao");
        errarEAbrirATela(navegador, "maria");

        mockMvc.perform(tentativa(EMPRESA, "joao", SENHA).session(navegador))
                .andExpect(authenticated())
                .andExpect(redirectedUrl("/"));   // o destino continua o do #001

        assertEquals(0, avisoDeTentativas.falhasRegistradas(navegador, EMPRESA, "joao"));
        assertEquals(1, avisoDeTentativas.falhasRegistradas(navegador, EMPRESA, "maria"),
                "o sucesso de joao não deveria mexer na contagem de maria");
    }

    /**
     * RN008: quem bloqueia e o banco. A pessoa erra 3 vezes num navegador,
     * troca de navegador (sessao nova, aviso recomeca) e erra mais 2. A tela
     * do segundo navegador ainda nao mostra aviso nenhum, mas a conta bloqueia
     * mesmo assim - na quinta falha REAL.
     */
    @Test
    @DisplayName("RN008: a conta bloqueia na quinta falha mesmo com o aviso reiniciado")
    void bancoBloqueiaMesmoComAvisoDivergente() throws Exception {
        MockHttpSession primeiroNavegador = new MockHttpSession();
        errarEAbrirATela(primeiroNavegador, "joao");
        errarEAbrirATela(primeiroNavegador, "joao");
        errarEAbrirATela(primeiroNavegador, "joao")
                .andExpect(content().string(containsString(AVISO_RESTAM_2)));

        MockHttpSession segundoNavegador = new MockHttpSession();
        errarEAbrirATela(segundoNavegador, "joao");
        errarEAbrirATela(segundoNavegador, "joao")
                .andExpect(content().string(not(containsString(QUALQUER_AVISO))));

        Usuario joao = buscar("joao");
        assertEquals(5, joao.getTentativasInvalidas());
        assertNotNull(joao.getBloqueadoAte(), "a conta deveria estar bloqueada pelo banco");

        mockMvc.perform(tentativa(EMPRESA, "joao", SENHA).session(segundoNavegador))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/" + EMPRESA + "/login?bloqueado"));
    }

    @Test
    @DisplayName("O Administrador continua sendo bloqueado como os demais perfis")
    void administradorTambemBloqueia() throws Exception {
        MockHttpSession navegador = new MockHttpSession();
        for (int i = 0; i < 5; i++) {
            errarEAbrirATela(navegador, "chefe");
        }

        assertNotNull(buscar("chefe").getBloqueadoAte());
        mockMvc.perform(tentativa(EMPRESA, "chefe", SENHA).session(navegador))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/" + EMPRESA + "/login?bloqueado"));
    }

    /** O aviso vale para uma exibicao: recarregar a tela nao o mostra de novo. */
    @Test
    @DisplayName("O aviso some ao recarregar a tela")
    void avisoNaoSobreviveAoRecarregar() throws Exception {
        MockHttpSession navegador = new MockHttpSession();
        errarEAbrirATela(navegador, "joao");
        errarEAbrirATela(navegador, "joao");
        errarEAbrirATela(navegador, "joao")
                .andExpect(content().string(containsString(AVISO_RESTAM_2)));

        mockMvc.perform(get("/" + EMPRESA + "/login").param("erro", "").session(navegador))
                .andExpect(content().string(not(containsString(QUALQUER_AVISO))));
    }

    @Test
    @DisplayName("O aviso tambem aparece na tela /login, sem empresa no endereco")
    void avisoNaTelaSemEmpresa() throws Exception {
        MockHttpSession navegador = new MockHttpSession();
        // empresa em formato invalido: o #001 devolve para /login?erro
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(tentativa("Empresa Invalida", "joao", "errada").session(navegador))
                    .andExpect(redirectedUrl("/login?erro"));
        }

        mockMvc.perform(get("/login").param("erro", "").session(navegador))
                .andExpect(content().string(containsString(AVISO_RESTAM_2)));
    }

    /* ------------------------------------------------------------------
       Auxiliares
       ------------------------------------------------------------------ */

    /**
     * Faz o que o navegador faz: envia uma senha errada, segue o
     * redirecionamento e abre a tela de login que voltou.
     */
    private org.springframework.test.web.servlet.ResultActions errarEAbrirATela(
            MockHttpSession navegador, String login) throws Exception {

        String destino = mockMvc.perform(tentativa(EMPRESA, login, "senha-errada").session(navegador))
                .andExpect(unauthenticated())
                .andReturn().getResponse().getRedirectedUrl();

        return mockMvc.perform(get(destino).session(navegador));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
            tentativa(String empresa, String usuario, String senha) {
        return post("/login")
                .param("empresa", empresa)
                .param("usuario", usuario)
                .param("senha", senha)
                .with(csrf());
    }

    private String semTokenCsrf(String html) {
        return html.replaceAll("name=\"_csrf\" value=\"[^\"]*\"", "name=\"_csrf\" value=\"\"");
    }

    private Usuario buscar(String login) {
        return usuarioRepository.findByEmpresaIdAndLogin(empresa.getId(), login).orElseThrow();
    }

    private void criarUsuario(String login, Perfil perfil) {
        usuarioRepository.save(new Usuario(empresa, "Usuário " + login, login,
                passwordEncoder.encode(SENHA), perfil));
    }
}
