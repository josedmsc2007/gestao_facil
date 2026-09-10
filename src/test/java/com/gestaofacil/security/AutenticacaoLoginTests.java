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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Testes da Etapa 1: a autenticacao considerando a empresa.
 *
 * Cada teste corresponde a um criterio de aceitacao do card #001. Eles
 * substituem o "testar na mao" enquanto a tela nao existe, e continuam valendo
 * depois: se alguem quebrar o isolamento entre empresas no futuro, o teste
 * acusa.
 *
 * DUAS DECISOES QUE VALE ENTENDER
 *
 * 1. Os testes criam as proprias empresas e usuarios, em vez de usar os do
 *    seed. Assim eles nao quebram se alguem trocar a senha do administrador
 *    de teste no application.properties - um teste deve depender so do que
 *    ele mesmo preparou.
 *
 * 2. Transactional: tudo que o teste grava e desfeito no final. O banco de
 *    desenvolvimento nao fica sujo com as empresas inventadas aqui.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AutenticacaoLoginTests {

    /* Dados usados pelos testes. As duas empresas tem um usuario com o MESMO
       login de proposito: e assim que a RN007 e a RN008 sao exercitadas. */
    private static final String EMPRESA_A = "empresa-teste-a";
    private static final String EMPRESA_B = "empresa-teste-b";
    private static final String LOGIN = "joao";
    private static final String SENHA_A = "senha-da-empresa-a";
    private static final String SENHA_B = "senha-da-empresa-b";

    /** Simula o navegador enviando o formulario, sem precisar subir o Tomcat. */
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmpresaRepository empresaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /** Roda antes de CADA teste, e e desfeito depois de cada um. */
    @BeforeEach
    void prepararDados() {
        criarEmpresaComUsuario(EMPRESA_A, LOGIN, SENHA_A, Perfil.ADMINISTRADOR, true);
        criarEmpresaComUsuario(EMPRESA_B, LOGIN, SENHA_B, Perfil.MOTORISTA, true);
    }

    @Test
    @DisplayName("Credenciais validas autenticam o usuario")
    void loginValido() throws Exception {
        mockMvc.perform(tentativaDeLogin(EMPRESA_A, LOGIN, SENHA_A))
                .andExpect(authenticated().withUsername(LOGIN));
    }

    @Test
    @DisplayName("Senha errada nao autentica")
    void senhaErrada() throws Exception {
        mockMvc.perform(tentativaDeLogin(EMPRESA_A, LOGIN, "senha-qualquer"))
                .andExpect(unauthenticated());
    }

    @Test
    @DisplayName("Usuario inexistente nao autentica")
    void usuarioInexistente() throws Exception {
        mockMvc.perform(tentativaDeLogin(EMPRESA_A, "nao-existe", SENHA_A))
                .andExpect(unauthenticated());
    }

    @Test
    @DisplayName("Empresa inexistente no endereco nao autentica")
    void empresaInexistente() throws Exception {
        mockMvc.perform(tentativaDeLogin("empresa-que-nao-existe", LOGIN, SENHA_A))
                .andExpect(unauthenticated());
    }

    /**
     * RN008 - o criterio de aceitacao mais importante desta etapa.
     *
     * As duas empresas tem um usuario "joao". A senha de uma nao pode servir
     * no endereco da outra, e cada uma precisa continuar funcionando na sua.
     */
    @Test
    @DisplayName("RN008: credenciais de uma empresa nao autenticam em outra")
    void credenciaisNaoAtravessamEmpresas() throws Exception {
        // senha da empresa B no endereco da empresa A: recusado
        mockMvc.perform(tentativaDeLogin(EMPRESA_A, LOGIN, SENHA_B))
                .andExpect(unauthenticated());

        // senha da empresa A no endereco da empresa B: recusado
        mockMvc.perform(tentativaDeLogin(EMPRESA_B, LOGIN, SENHA_A))
                .andExpect(unauthenticated());

        // cada uma com a sua propria senha: aceito (RN007 - o login se repete)
        mockMvc.perform(tentativaDeLogin(EMPRESA_A, LOGIN, SENHA_A))
                .andExpect(authenticated().withUsername(LOGIN));
        mockMvc.perform(tentativaDeLogin(EMPRESA_B, LOGIN, SENHA_B))
                .andExpect(authenticated().withUsername(LOGIN));
    }

    /** RN009 - usuario marcado como inativo nao entra. */
    @Test
    @DisplayName("RN009: usuario inativo nao autentica")
    void usuarioInativo() throws Exception {
        Empresa empresaA = empresaRepository.findByIdentificador(EMPRESA_A).orElseThrow();
        Usuario demitido = new Usuario(empresaA, "Motorista Demitido", "demitido",
                passwordEncoder.encode("senha12345"), Perfil.MOTORISTA);
        demitido.setAtivo(false);
        usuarioRepository.save(demitido);

        mockMvc.perform(tentativaDeLogin(EMPRESA_A, "demitido", "senha12345"))
                .andExpect(unauthenticated());
    }

    /**
     * O usuario autenticado precisa carregar a empresa dele: e desse dado que
     * todas as telas vao depender para filtrar as consultas (regra 1 do
     * projeto) e para montar os rotulos das telas (regra 6).
     */
    @Test
    @DisplayName("O usuario autenticado carrega empresa, perfil e rotulos")
    void usuarioAutenticadoCarregaEmpresa() throws Exception {
        mockMvc.perform(tentativaDeLogin(EMPRESA_B, LOGIN, SENHA_B))
                .andExpect(authenticated().withAuthentication(autenticacao -> {
                    UsuarioAutenticado logado = (UsuarioAutenticado) autenticacao.getPrincipal();
                    assertEquals(EMPRESA_B, logado.getEmpresaIdentificador());
                    assertEquals(Perfil.MOTORISTA, logado.getPerfil());
                    assertEquals("Obra", logado.getRotuloCcSingular());
                }));
    }

    /* ------------------------------------------------------------------
       Metodos auxiliares
       ------------------------------------------------------------------ */

    /**
     * Monta o envio do formulario de login.
     *
     * O with(csrf()) acrescenta o token que o Spring Security exige nos POST.
     * No navegador quem faz isso e o Thymeleaf, sozinho; aqui, como nao ha
     * tela, o teste precisa acrescentar na mao.
     */
    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
            tentativaDeLogin(String empresa, String usuario, String senha) {
        return post("/login")
                .param("empresa", empresa)
                .param("usuario", usuario)
                .param("senha", senha)
                .with(csrf());
    }

    private void criarEmpresaComUsuario(String identificador, String login, String senha,
                                        Perfil perfil, boolean ativo) {
        Empresa empresa = empresaRepository.save(
                new Empresa("Empresa " + identificador, identificador, "Obra", "Obras"));
        Usuario usuario = new Usuario(empresa, "Usuario de teste", login,
                passwordEncoder.encode(senha), perfil);
        usuario.setAtivo(ativo);
        usuarioRepository.save(usuario);
    }
}
