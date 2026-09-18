package com.gestaofacil.controller;

import com.gestaofacil.model.Empresa;
import com.gestaofacil.model.Perfil;
import com.gestaofacil.model.Usuario;
import com.gestaofacil.repository.EmpresaRepository;
import com.gestaofacil.repository.UsuarioRepository;
import com.gestaofacil.security.UsuarioAutenticado;
import com.gestaofacil.service.ControleDeTentativasService;
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

import java.time.LocalDateTime;

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
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Card #003, etapa 2: regras de conta.
 *
 * Redefinir senha (RF05), desbloquear (RF06), ativar e desativar (RF07), a
 * regra dos dois administradores (RN015) e a proibicao de mexer na propria
 * conta (RN014).
 *
 * A EMPRESA ALFA NASCE COM DOIS ADMINISTRADORES
 * E o menor cenario em que a RN015 tem o que recusar: com dois, desativar um
 * deixaria a empresa com um so, e a acao e barrada. Os testes que precisam ver
 * a acao PASSAR cadastram um terceiro antes.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UsuarioContaTests {

    private static final String ALFA = "alfa-das-contas";
    private static final String BETA = "beta-das-contas";
    private static final String SENHA_ANTIGA = "senha-antiga-123";
    private static final String CPF_VALIDO = "111.444.777-35";

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

    private Empresa alfa;
    private Empresa beta;
    private Usuario adminAlfa;
    private Usuario adminDois;
    private Usuario motoristaAlfa;
    private Usuario motoristaBeta;

    @BeforeEach
    void prepararDados() {
        alfa = empresaRepository.save(new Empresa("Alfa", ALFA, "Obra", "Obras"));
        beta = empresaRepository.save(new Empresa("Beta", BETA, "Obra", "Obras"));

        adminAlfa = criar(alfa, "chefe", "Chefe da Alfa", Perfil.ADMINISTRADOR);
        adminDois = criar(alfa, "chefe.dois", "Segundo Chefe", Perfil.ADMINISTRADOR);
        motoristaAlfa = criar(alfa, "motorista", "Motorista da Alfa", Perfil.MOTORISTA);

        criar(beta, "chefe.beta", "Chefe da Beta", Perfil.ADMINISTRADOR);
        motoristaBeta = criar(beta, "motorista.beta", "Motorista da Beta", Perfil.MOTORISTA);
    }

    /* ==================================================================
       #003-RF05 - redefinicao de senha
       ================================================================== */

    @Test
    @DisplayName("#003-RF05: redefinir gera outra senha temporaria e invalida a anterior")
    void redefinirSenhaGeraOutraSenha() throws Exception {

        MvcResult resultado = mockMvc.perform(acao(motoristaAlfa, "redefinir-senha")
                        .with(comoUsuario(adminAlfa, alfa)))
                .andExpect(redirectedUrl("/usuarios"))
                .andReturn();

        String novaSenha = (String) resultado.getFlashMap().get("senhaTemporaria");
        assertNotNull(novaSenha, "a nova senha deveria ter sido exibida uma vez");

        Usuario alterado = recarregar(motoristaAlfa);
        assertTrue(alterado.isSenhaTemporaria(), "#001-RF07: vai ter de trocar no acesso");
        assertTrue(passwordEncoder.matches(novaSenha, alterado.getSenha()));

        // A senha antiga deixou de valer...
        mockMvc.perform(login("motorista", SENHA_ANTIGA)).andExpect(unauthenticated());
        // ...e a nova funciona.
        mockMvc.perform(login("motorista", novaSenha)).andExpect(authenticated());
    }

    /**
     * #003-RN016: e um administrador que resolve a vida do outro. Nao existe
     * recuperacao por e-mail no sistema, entao esta e a unica saida de quem
     * esqueceu a senha.
     */
    @Test
    @DisplayName("#003-RN016: um administrador redefine a senha do outro")
    void administradorRedefineSenhaDoOutroAdministrador() throws Exception {
        MvcResult resultado = mockMvc.perform(acao(adminDois, "redefinir-senha")
                        .with(comoUsuario(adminAlfa, alfa)))
                .andExpect(redirectedUrl("/usuarios"))
                .andReturn();

        String novaSenha = (String) resultado.getFlashMap().get("senhaTemporaria");
        mockMvc.perform(login("chefe.dois", novaSenha)).andExpect(authenticated());
    }

    @Test
    @DisplayName("#003-RF05: redefinir a senha tambem libera a conta bloqueada")
    void redefinirSenhaLiberaContaBloqueada() throws Exception {
        bloquear(motoristaAlfa);

        mockMvc.perform(acao(motoristaAlfa, "redefinir-senha")
                .with(comoUsuario(adminAlfa, alfa)));

        Usuario liberado = recarregar(motoristaAlfa);
        assertNull(liberado.getBloqueadoAte(), "a conta deveria ter sido liberada junto");
        assertEquals(0, liberado.getTentativasInvalidas());
    }

    /* ==================================================================
       #003-RF06 - desbloqueio
       ================================================================== */

    @Test
    @DisplayName("#003-RF06: o administrador desbloqueia a conta pela lista de usuarios")
    void desbloquearLiberaAConta() throws Exception {
        bloquear(motoristaAlfa);

        mockMvc.perform(acao(motoristaAlfa, "desbloquear")
                        .with(comoUsuario(adminAlfa, alfa)))
                .andExpect(redirectedUrl("/usuarios"));

        Usuario liberado = recarregar(motoristaAlfa);
        assertNull(liberado.getBloqueadoAte());
        assertEquals(0, liberado.getTentativasInvalidas());

        // e a senha de sempre volta a funcionar
        mockMvc.perform(login("motorista", SENHA_ANTIGA)).andExpect(authenticated());
    }

    @Test
    @DisplayName("#003-RF06: o botao Desbloquear so aparece para conta bloqueada")
    void botaoDesbloquearSoApareceQuandoBloqueado() throws Exception {
        mockMvc.perform(get("/usuarios").with(comoUsuario(adminAlfa, alfa)))
                .andExpect(content().string(not(containsString("Desbloquear"))));

        bloquear(motoristaAlfa);

        mockMvc.perform(get("/usuarios").with(comoUsuario(adminAlfa, alfa)))
                .andExpect(content().string(containsString("Desbloquear")))
                .andExpect(content().string(containsString("Bloqueada")));
    }

    /* ==================================================================
       #003-RF07 e RN010 - ativar e desativar
       ================================================================== */

    @Test
    @DisplayName("#003-RN010: desativar nao apaga o registro")
    void desativarNaoApaga() throws Exception {
        mockMvc.perform(acao(motoristaAlfa, "desativar")
                        .with(comoUsuario(adminAlfa, alfa)))
                .andExpect(redirectedUrl("/usuarios"));

        Usuario desativado = recarregar(motoristaAlfa);
        assertFalse(desativado.isAtivo());
        assertEquals("Motorista da Alfa", desativado.getNome(),
                "o registro continua inteiro, para os lançamentos antigos");
    }

    @Test
    @DisplayName("#003-RF07: usuario desativado nao consegue entrar")
    void desativadoNaoAutentica() throws Exception {
        mockMvc.perform(acao(motoristaAlfa, "desativar").with(comoUsuario(adminAlfa, alfa)));

        mockMvc.perform(login("motorista", SENHA_ANTIGA)).andExpect(unauthenticated());
    }

    @Test
    @DisplayName("#003-RF07: ativar devolve o acesso")
    void ativarDevolveOAcesso() throws Exception {
        mockMvc.perform(acao(motoristaAlfa, "desativar").with(comoUsuario(adminAlfa, alfa)));
        mockMvc.perform(acao(motoristaAlfa, "ativar").with(comoUsuario(adminAlfa, alfa)))
                .andExpect(redirectedUrl("/usuarios"));

        assertTrue(recarregar(motoristaAlfa).isAtivo());
        mockMvc.perform(login("motorista", SENHA_ANTIGA)).andExpect(authenticated());
    }

    @Test
    @DisplayName("#003-RN014: o administrador nao desativa o proprio usuario")
    void naoDesativaASiMesmo() throws Exception {
        mockMvc.perform(acao(adminAlfa, "desativar")
                        .with(comoUsuario(adminAlfa, alfa)))
                .andExpect(redirectedUrl("/usuarios"))
                .andExpect(flash().attribute("erro", containsString("seu próprio usuário")));

        assertTrue(recarregar(adminAlfa).isAtivo());
    }

    /* ==================================================================
       #003-RN015 - a empresa nunca fica com menos de dois administradores
       ================================================================== */

    @Test
    @DisplayName("#003-RN015: desativar um administrador quando restaria um so e recusado")
    void naoDesativaAdministradorQuandoRestariaUm() throws Exception {
        mockMvc.perform(acao(adminDois, "desativar")
                        .with(comoUsuario(adminAlfa, alfa)))
                .andExpect(redirectedUrl("/usuarios"))
                .andExpect(flash().attribute("erro",
                        containsString("administradores ativos")));

        assertTrue(recarregar(adminDois).isAtivo(), "não podia ter sido desativado");
    }

    @Test
    @DisplayName("#003-RN015: com tres administradores, desativar um e permitido")
    void desativaAdministradorQuandoSobramDois() throws Exception {
        criar(alfa, "chefe.tres", "Terceiro Chefe", Perfil.ADMINISTRADOR);

        mockMvc.perform(acao(adminDois, "desativar")
                        .with(comoUsuario(adminAlfa, alfa)))
                .andExpect(redirectedUrl("/usuarios"));

        assertFalse(recarregar(adminDois).isAtivo());
    }

    @Test
    @DisplayName("#003-RN015: rebaixar um administrador quando restaria um so e recusado")
    void naoRebaixaAdministradorQuandoRestariaUm() throws Exception {
        mockMvc.perform(edicaoComoMotorista(adminDois)
                        .with(comoUsuario(adminAlfa, alfa)))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("usuarioForm", "perfil"));

        assertEquals(Perfil.ADMINISTRADOR, recarregar(adminDois).getPerfil());
    }

    @Test
    @DisplayName("#003-RN015: com tres administradores, rebaixar um e permitido")
    void rebaixaAdministradorQuandoSobramDois() throws Exception {
        criar(alfa, "chefe.tres", "Terceiro Chefe", Perfil.ADMINISTRADOR);

        mockMvc.perform(edicaoComoMotorista(adminDois)
                        .with(comoUsuario(adminAlfa, alfa)))
                .andExpect(redirectedUrl("/usuarios"));

        assertEquals(Perfil.MOTORISTA, recarregar(adminDois).getPerfil());
    }

    /**
     * O ponto que o card faz questao de deixar claro: a regra e sobre a ACAO,
     * nao sobre o estado atual. Uma empresa com um administrador so - como a
     * que a carga inicial cria - continua funcionando normalmente. O que ela
     * nao consegue e ficar sem nenhum.
     */
    @Test
    @DisplayName("#003-RN015: empresa com um administrador so continua funcionando")
    void empresaComUmAdministradorContinuaFuncionando() throws Exception {
        Empresa sozinha = empresaRepository.save(
                new Empresa("Sozinha", "empresa-sozinha", "Obra", "Obras"));
        Usuario unicoChefe = criar(sozinha, "unico", "Único Chefe", Perfil.ADMINISTRADOR);
        Usuario ajudante = criar(sozinha, "ajudante", "Ajudante", Perfil.MOTORISTA);

        // ele trabalha normalmente: abre a tela, desativa um motorista...
        mockMvc.perform(get("/usuarios").with(comoUsuario(unicoChefe, sozinha)))
                .andExpect(status().isOk());

        mockMvc.perform(acao(ajudante, "desativar").with(comoUsuario(unicoChefe, sozinha)))
                .andExpect(redirectedUrl("/usuarios"));
        assertFalse(recarregar(ajudante).isAtivo());

        // ...mas nao consegue tirar a si mesmo do caminho.
        mockMvc.perform(acao(unicoChefe, "desativar").with(comoUsuario(unicoChefe, sozinha)))
                .andExpect(flash().attributeExists("erro"));
        assertTrue(recarregar(unicoChefe).isAtivo());
    }

    /* ==================================================================
       #003-RN009 - isolamento por empresa (regra 1 do projeto)
       ================================================================== */

    @Test
    @DisplayName("#003-RN009: nao redefine a senha de usuario de outra empresa")
    void naoRedefineSenhaDeOutraEmpresa() throws Exception {
        String senhaAntes = recarregar(motoristaBeta).getSenha();

        mockMvc.perform(acao(motoristaBeta, "redefinir-senha")
                        .with(comoUsuario(adminAlfa, alfa)))
                .andExpect(status().isNotFound());

        assertEquals(senhaAntes, recarregar(motoristaBeta).getSenha());
    }

    @Test
    @DisplayName("#003-RN009: nao desbloqueia usuario de outra empresa")
    void naoDesbloqueiaDeOutraEmpresa() throws Exception {
        bloquear(motoristaBeta);

        mockMvc.perform(acao(motoristaBeta, "desbloquear")
                        .with(comoUsuario(adminAlfa, alfa)))
                .andExpect(status().isNotFound());

        assertNotNull(recarregar(motoristaBeta).getBloqueadoAte(),
                "a conta da outra empresa deveria continuar bloqueada");
    }

    @Test
    @DisplayName("#003-RN009: nao desativa usuario de outra empresa")
    void naoDesativaDeOutraEmpresa() throws Exception {
        mockMvc.perform(acao(motoristaBeta, "desativar")
                        .with(comoUsuario(adminAlfa, alfa)))
                .andExpect(status().isNotFound());

        assertTrue(recarregar(motoristaBeta).isAtivo());
    }

    @Test
    @DisplayName("#003-RN009: nao ativa usuario de outra empresa")
    void naoAtivaDeOutraEmpresa() throws Exception {
        motoristaBeta.setAtivo(false);
        usuarioRepository.save(motoristaBeta);

        mockMvc.perform(acao(motoristaBeta, "ativar")
                        .with(comoUsuario(adminAlfa, alfa)))
                .andExpect(status().isNotFound());

        assertFalse(recarregar(motoristaBeta).isAtivo());
    }

    @Test
    @DisplayName("#003-RN008: motorista nao executa acao de conta")
    void motoristaNaoExecutaAcaoDeConta() throws Exception {
        mockMvc.perform(acao(adminAlfa, "desativar")
                        .with(comoUsuario(motoristaAlfa, alfa)))
                .andExpect(status().isForbidden());

        assertTrue(recarregar(adminAlfa).isAtivo());
    }

    /* ==================================================================
       Auxiliares
       ================================================================== */

    /** Monta o POST de uma acao de conta: /usuarios/{id}/{acao}. */
    private MockHttpServletRequestBuilder acao(Usuario alvo, String acao) {
        return post("/usuarios/" + alvo.getId() + "/" + acao).with(csrf());
    }

    /** Monta a edicao completa de um usuario, mudando o perfil para motorista. */
    private MockHttpServletRequestBuilder edicaoComoMotorista(Usuario alvo) {
        return post("/usuarios/" + alvo.getId())
                .param("nome", alvo.getNome())
                .param("cpf", CPF_VALIDO)
                .param("login", alvo.getLogin())
                .param("perfil", "MOTORISTA")
                .param("cnh", "98765432100")
                .param("categoriaCnh", "D")
                .param("validadeCnh", "2030-12-31")
                .with(csrf());
    }

    private MockHttpServletRequestBuilder login(String usuario, String senha) {
        return post("/login")
                .param("empresa", ALFA)
                .param("usuario", usuario)
                .param("senha", senha)
                .with(csrf());
    }

    private RequestPostProcessor comoUsuario(Usuario usuario, Empresa empresa) {
        return user(new UsuarioAutenticado(usuario, empresa));
    }

    /** Fecha a conta como o login de verdade faria: cinco senhas erradas. */
    private void bloquear(Usuario usuario) {
        for (int tentativa = 0; tentativa < ControleDeTentativasService.TENTATIVAS_ATE_BLOQUEAR;
             tentativa++) {
            controleDeTentativas.registrarFalha(usuario.getId(), usuario.getEmpresa().getId());
        }
        assertNotNull(recarregar(usuario).getBloqueadoAte(), "deveria estar bloqueado");
        assertTrue(recarregar(usuario).getBloqueadoAte().isAfter(LocalDateTime.now()));
    }

    private Usuario recarregar(Usuario usuario) {
        return usuarioRepository.findById(usuario.getId()).orElseThrow();
    }

    private Usuario criar(Empresa empresa, String login, String nome, Perfil perfil) {
        Usuario usuario = new Usuario(empresa, nome, login,
                passwordEncoder.encode(SENHA_ANTIGA), perfil);
        return usuarioRepository.save(usuario);
    }
}
