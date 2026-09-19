package com.gestaofacil.controller;

import com.gestaofacil.model.Empresa;
import com.gestaofacil.model.Perfil;
import com.gestaofacil.model.Usuario;
import com.gestaofacil.repository.EmpresaRepository;
import com.gestaofacil.repository.UsuarioRepository;
import com.gestaofacil.security.UsuarioAutenticado;
import com.gestaofacil.service.EmpresaService;
import com.gestaofacil.service.EmpresaService.AdministradorCriado;
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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Card #002: cadastro de empresa e o perfil Operador.
 *
 * O CENARIO
 * - a empresa reservada da equipe, com um Operador (e um segundo, para provar
 *   que um Operador nao mexe na conta do outro);
 * - a cliente Alfa, com dois administradores e um motorista;
 * - a cliente Beta, com um administrador.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EmpresaCadastroTests {

    private static final String SENHA = "senha-certa-123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmpresaRepository empresaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Empresa equipe;
    private Usuario operador;
    private Usuario outroOperador;

    private Empresa alfa;
    private Usuario adminAlfa;
    private Usuario motoristaAlfa;

    @BeforeEach
    void prepararDados() {
        equipe = empresaRepository.save(new Empresa("Equipe Gestão Fácil",
                Empresa.IDENTIFICADOR_DA_EQUIPE, "Centro de custo", "Centros de custo"));
        operador = criar(equipe, "jose.lopes", "José Lopes", Perfil.OPERADOR);
        outroOperador = criar(equipe, "victor.ruan", "Victor Ruan", Perfil.OPERADOR);

        alfa = empresaRepository.save(new Empresa("Alfa Engenharia", "alfa-engenharia", "Obra", "Obras"));
        adminAlfa = criar(alfa, "chefe", "Chefe da Alfa", Perfil.ADMINISTRADOR);
        criar(alfa, "chefe.dois", "Segundo Chefe da Alfa", Perfil.ADMINISTRADOR);
        motoristaAlfa = criar(alfa, "motorista", "Motorista da Alfa", Perfil.MOTORISTA);

        Empresa beta = empresaRepository.save(new Empresa("Beta Transportes", "beta-transportes", "Rota", "Rotas"));
        criar(beta, "chefe.beta", "Chefe da Beta", Perfil.ADMINISTRADOR);
    }

    /* ==================================================================
       Cadastro (#002-RF02 e RF05)
       ================================================================== */

    @Test
    @DisplayName("#002: Operador cadastra empresa com os dois administradores e o historico")
    void operadorCadastraEmpresa() throws Exception {

        MvcResult resultado = mockMvc.perform(cadastro(formularioValido()))
                .andExpect(redirectedUrl("/empresas"))
                .andReturn();

        Empresa criada = empresaRepository.findByIdentificador("construtora-silva").orElseThrow();
        assertEquals("Construtora Silva", criada.getNome());
        assertTrue(criada.isAtiva());

        // O historico: qual Operador cadastrou, e quando.
        assertEquals(operador.getId(), criada.getCadastradaPor().getId());
        assertNotNull(criada.getDataCadastro());

        // Os dois administradores nasceram junto, com senha temporaria.
        List<Usuario> administradores = usuarioRepository
                .findByEmpresaIdAndPerfilAndAtivoTrueOrderByNome(criada.getId(), Perfil.ADMINISTRADOR);
        assertEquals(2, administradores.size());
        administradores.forEach(administrador ->
                assertTrue(administrador.isSenhaTemporaria(), "#001-RF07: troca no primeiro acesso"));

        // As senhas foram exibidas uma vez - e no banco so ficou o hash.
        @SuppressWarnings("unchecked")
        List<AdministradorCriado> exibidos =
                (List<AdministradorCriado>) resultado.getFlashMap().get("administradoresCriados");
        assertEquals(2, exibidos.size());
        for (AdministradorCriado exibido : exibidos) {
            Usuario gravado = usuarioRepository
                    .findByEmpresaIdAndLogin(criada.getId(), exibido.login()).orElseThrow();
            assertTrue(passwordEncoder.matches(exibido.senha(), gravado.getSenha()));
            assertFalse(gravado.getSenha().equals(exibido.senha()), "senha nunca em texto legivel");
        }
    }

    @Test
    @DisplayName("#002: os dois administradores entram em /{identificador}/login e caem na troca de senha")
    void administradoresCriadosEntramETrocamASenha() throws Exception {

        MvcResult resultado = mockMvc.perform(cadastro(formularioValido())).andReturn();

        @SuppressWarnings("unchecked")
        List<AdministradorCriado> exibidos =
                (List<AdministradorCriado>) resultado.getFlashMap().get("administradoresCriados");
        Empresa criada = empresaRepository.findByIdentificador("construtora-silva").orElseThrow();

        for (AdministradorCriado exibido : exibidos) {
            mockMvc.perform(post("/login")
                            .param("empresa", "construtora-silva")
                            .param("usuario", exibido.login())
                            .param("senha", exibido.senha())
                            .with(csrf()))
                    .andExpect(authenticated());

            Usuario gravado = usuarioRepository
                    .findByEmpresaIdAndLogin(criada.getId(), exibido.login()).orElseThrow();
            mockMvc.perform(get("/").with(user(new UsuarioAutenticado(gravado, criada))))
                    .andExpect(redirectedUrl("/trocar-senha"));
        }
    }

    /**
     * Desenha a lista logo depois do cadastro, como o navegador faria ao
     * seguir o redirecionamento: as senhas aparecem, e o historico mostra
     * quem cadastrou.
     */
    @Test
    @DisplayName("#002: a lista mostra as senhas uma vez e quem cadastrou a empresa")
    void listaMostraSenhasEHistorico() throws Exception {
        MvcResult cadastro = mockMvc.perform(cadastro(formularioValido())).andReturn();

        mockMvc.perform(get("/empresas")
                        .flashAttrs(cadastro.getFlashMap())
                        .with(comoUsuario(operador, equipe)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("maria.silva")))
                .andExpect(content().string(containsString("pedro.silva")))
                .andExpect(content().string(containsString("por <span>José Lopes</span>")));

        // Sem o flash (recarregar a pagina), as senhas nao voltam.
        mockMvc.perform(get("/empresas").with(comoUsuario(operador, equipe)))
                .andExpect(content().string(not(containsString("maria.silva"))));
    }

    @Test
    @DisplayName("#002-RN003: a tela de edicao mostra o identificador, mas sem campo para ele")
    void telaDeEdicaoNaoTemCampoDeIdentificador() throws Exception {
        mockMvc.perform(get("/empresas/" + alfa.getId() + "/editar")
                        .with(comoUsuario(operador, equipe)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/alfa-engenharia")))
                .andExpect(content().string(not(containsString("name=\"identificador\""))));
    }

    @Test
    @DisplayName("#002-RN004: o formulario em branco ja sugere Obra e Obras")
    void formularioSugereRotulosPadrao() throws Exception {
        mockMvc.perform(get("/empresas/nova").with(comoUsuario(operador, equipe)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("value=\"Obra\"")))
                .andExpect(content().string(containsString("value=\"Obras\"")));
    }

    /* ==================================================================
       Validacoes (#002-RN001, RN002, RN004, RN005 e RN006)
       ================================================================== */

    @Test
    @DisplayName("#002-RN001: sem nome, a empresa nao e cadastrada")
    void recusaSemNome() throws Exception {
        MultiValueMap<String, String> formulario = formularioValido();
        formulario.set("nome", "");

        mockMvc.perform(cadastro(formulario))
                .andExpect(view().name("empresas/formulario"))
                .andExpect(model().attributeHasFieldErrors("empresaForm", "nome"));

        assertFalse(empresaRepository.existsByIdentificador("construtora-silva"));
    }

    /**
     * #002-RF03: identificador em branco nao e gravado em silencio - volta
     * para a tela ja preenchido com a sugestao, para o Operador conferir.
     */
    @Test
    @DisplayName("#002-RF03: sem identificador, o sistema sugere um a partir do nome")
    void semIdentificadorSugereAPartirDoNome() throws Exception {
        MultiValueMap<String, String> formulario = formularioValido();
        formulario.set("nome", "Construtora São João Ltda.");
        formulario.set("identificador", "");

        mockMvc.perform(cadastro(formulario))
                .andExpect(view().name("empresas/formulario"))
                .andExpect(model().attributeHasFieldErrors("empresaForm", "identificador"))
                .andExpect(content().string(containsString("value=\"construtora-sao-joao-ltda\"")));

        assertFalse(empresaRepository.existsByIdentificador("construtora-sao-joao-ltda"),
                "a sugestao precisa ser conferida antes de gravar");
    }

    @Test
    @DisplayName("#002-RF03: sem identificador e sem nome, pede o identificador")
    void semIdentificadorESemNome() throws Exception {
        MultiValueMap<String, String> formulario = formularioValido();
        formulario.set("nome", "");
        formulario.set("identificador", "");

        mockMvc.perform(cadastro(formulario))
                .andExpect(model().attributeHasFieldErrors("empresaForm", "nome", "identificador"));
    }

    @Test
    @DisplayName("#002-RF03: a sugestao tira acento, espaco, maiuscula e pontuacao")
    void regraDaSugestao() {
        assertEquals("construtora-sao-joao", EmpresaService.sugerirIdentificador("Construtora São João"));
        assertEquals("abc-engenharia-ltda", EmpresaService.sugerirIdentificador("  ABC Engenharia Ltda "));
        assertEquals("acai-cia", EmpresaService.sugerirIdentificador("Açaí & Cia."));
        assertEquals("obra-2026", EmpresaService.sugerirIdentificador("--Obra 2026--"));
        assertEquals("", EmpresaService.sugerirIdentificador("!!!"));
        assertEquals("", EmpresaService.sugerirIdentificador(null));
    }

    @Test
    @DisplayName("#002-RN002: identificador repetido e recusado")
    void recusaIdentificadorRepetido() throws Exception {
        MultiValueMap<String, String> formulario = formularioValido();
        formulario.set("identificador", "alfa-engenharia");

        mockMvc.perform(cadastro(formulario))
                .andExpect(model().attributeHasFieldErrors("empresaForm", "identificador"));

        assertEquals(1, empresaRepository.findAll().stream()
                .filter(empresa -> empresa.getIdentificador().equals("alfa-engenharia")).count());
    }

    @Test
    @DisplayName("#002-RN002: identificador com espaco, acento ou maiuscula e recusado")
    void recusaIdentificadorForaDoFormato() throws Exception {
        for (String invalido : List.of("construtora silva", "construtora-são", "Construtora-Silva")) {
            MultiValueMap<String, String> formulario = formularioValido();
            formulario.set("identificador", invalido);

            mockMvc.perform(cadastro(formulario))
                    .andExpect(view().name("empresas/formulario"))
                    .andExpect(model().attributeHasFieldErrors("empresaForm", "identificador"));
        }
        assertEquals(3, empresaRepository.count(), "nenhuma empresa nova");
    }

    /**
     * O identificador vira endereco: "login" apontaria para a tela de login
     * generica, "empresas" para as telas do Operador. E o da equipe ja e
     * usado pela empresa reservada.
     */
    @Test
    @DisplayName("#002-RN002: nomes de rotas do sistema nao servem de identificador")
    void recusaIdentificadorReservado() throws Exception {
        for (String reservado : List.of("login", "empresas", "usuarios", Empresa.IDENTIFICADOR_DA_EQUIPE)) {
            MultiValueMap<String, String> formulario = formularioValido();
            formulario.set("identificador", reservado);

            mockMvc.perform(cadastro(formulario))
                    .andExpect(model().attributeHasFieldErrors("empresaForm", "identificador"));
        }
        assertEquals(3, empresaRepository.count());
    }

    @Test
    @DisplayName("#002-RN004: sem os rotulos do centro de custo, nao cadastra")
    void recusaSemRotulos() throws Exception {
        MultiValueMap<String, String> formulario = formularioValido();
        formulario.set("rotuloCcSingular", "");
        formulario.set("rotuloCcPlural", " ");

        mockMvc.perform(cadastro(formulario))
                .andExpect(model().attributeHasFieldErrors("empresaForm",
                        "rotuloCcSingular", "rotuloCcPlural"));

        assertFalse(empresaRepository.existsByIdentificador("construtora-silva"));
    }

    @Test
    @DisplayName("#002-RN006: nao cadastra com um administrador so")
    void recusaUmAdministradorSo() throws Exception {
        MultiValueMap<String, String> formulario = formularioValido();
        formulario.set("nomeAdministrador2", "");
        formulario.set("loginAdministrador2", "");

        mockMvc.perform(cadastro(formulario))
                .andExpect(model().attributeHasFieldErrors("empresaForm",
                        "nomeAdministrador2", "loginAdministrador2"));

        assertFalse(empresaRepository.existsByIdentificador("construtora-silva"));
    }

    @Test
    @DisplayName("#002-RN005: os dois administradores nao podem ter o mesmo login")
    void recusaLoginsIguais() throws Exception {
        MultiValueMap<String, String> formulario = formularioValido();
        formulario.set("loginAdministrador2", formulario.getFirst("loginAdministrador1"));

        mockMvc.perform(cadastro(formulario))
                .andExpect(model().attributeHasFieldErrors("empresaForm", "loginAdministrador2"));

        assertFalse(empresaRepository.existsByIdentificador("construtora-silva"));
    }

    /* ==================================================================
       Rotulo (#002-RF04) e edicao (#002-RF07, RN003)
       ================================================================== */

    @Test
    @DisplayName("#002-RF04: as telas da nova empresa usam o rotulo cadastrado por ela")
    void telasUsamORotuloDaEmpresa() throws Exception {
        MultiValueMap<String, String> formulario = formularioValido();
        formulario.set("rotuloCcSingular", "Contrato");
        formulario.set("rotuloCcPlural", "Contratos");
        mockMvc.perform(cadastro(formulario));

        Empresa criada = empresaRepository.findByIdentificador("construtora-silva").orElseThrow();
        Usuario administrador = usuarioRepository
                .findByEmpresaIdAndLogin(criada.getId(), "maria.silva").orElseThrow();
        administrador.setSenhaTemporaria(false);   // como se ja tivesse trocado

        mockMvc.perform(get("/painel").with(user(new UsuarioAutenticado(administrador, criada))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("contratos")));
    }

    @Test
    @DisplayName("#002-RF07: Operador edita nome e rotulos")
    void operadorEditaEmpresa() throws Exception {
        mockMvc.perform(post("/empresas/" + alfa.getId())
                        .param("nome", "Alfa Engenharia S.A.")
                        .param("rotuloCcSingular", "Canteiro")
                        .param("rotuloCcPlural", "Canteiros")
                        .with(csrf())
                        .with(comoUsuario(operador, equipe)))
                .andExpect(redirectedUrl("/empresas"));

        Empresa editada = recarregar(alfa);
        assertEquals("Alfa Engenharia S.A.", editada.getNome());
        assertEquals("Canteiro", editada.getRotuloCcSingular());
        assertEquals("Canteiros", editada.getRotuloCcPlural());
    }

    /**
     * O formulario de edicao nem mostra o identificador como campo. Este teste
     * envia o campo mesmo assim - como faria alguem editando o HTML no
     * navegador - e confere que nada muda.
     */
    @Test
    @DisplayName("#002-RN003: o identificador nao pode ser editado depois de criado")
    void identificadorNaoMudaNaEdicao() throws Exception {
        mockMvc.perform(post("/empresas/" + alfa.getId())
                        .param("nome", "Alfa Nova")
                        .param("identificador", "alfa-nova")
                        .param("rotuloCcSingular", "Obra")
                        .param("rotuloCcPlural", "Obras")
                        .with(csrf())
                        .with(comoUsuario(operador, equipe)))
                .andExpect(redirectedUrl("/empresas"));

        Empresa editada = recarregar(alfa);
        assertEquals("Alfa Nova", editada.getNome());
        assertEquals("alfa-engenharia", editada.getIdentificador());
        assertFalse(empresaRepository.existsByIdentificador("alfa-nova"));
    }

    /* ==================================================================
       Situacao (#002-RF06 e RN013)
       ================================================================== */

    @Test
    @DisplayName("#002-RN013: usuario de empresa inativa nao entra, nem com a senha certa")
    void empresaInativaNaoAceitaLogin() throws Exception {
        mockMvc.perform(loginNaAlfa()).andExpect(authenticated());

        mockMvc.perform(post("/empresas/" + alfa.getId() + "/inativar")
                        .with(csrf())
                        .with(comoUsuario(operador, equipe)))
                .andExpect(redirectedUrl("/empresas"));
        assertFalse(recarregar(alfa).isAtiva());

        mockMvc.perform(loginNaAlfa()).andExpect(unauthenticated());

        // A tentativa recusada nao conta para o bloqueio (#001-RN004): a
        // culpa nao e da senha.
        assertEquals(0, usuarioRepository.findById(adminAlfa.getId())
                .orElseThrow().getTentativasInvalidas());

        // Reativada, volta a aceitar.
        mockMvc.perform(post("/empresas/" + alfa.getId() + "/ativar")
                .with(csrf())
                .with(comoUsuario(operador, equipe)));
        mockMvc.perform(loginNaAlfa()).andExpect(authenticated());
    }

    /* ==================================================================
       Recuperacao de acesso (#002-RF08 e RN009)
       ================================================================== */

    @Test
    @DisplayName("#002-RF08: Operador gera nova senha temporaria para um administrador")
    void operadorRedefineSenhaDeAdministrador() throws Exception {
        MvcResult resultado = mockMvc.perform(
                        post("/empresas/" + alfa.getId() + "/administradores/"
                                + adminAlfa.getId() + "/redefinir-senha")
                                .with(csrf())
                                .with(comoUsuario(operador, equipe)))
                .andExpect(redirectedUrl("/empresas/" + alfa.getId() + "/administradores"))
                .andReturn();

        String novaSenha = (String) resultado.getFlashMap().get("senhaTemporaria");
        assertNotNull(novaSenha);

        Usuario alterado = usuarioRepository.findById(adminAlfa.getId()).orElseThrow();
        assertTrue(alterado.isSenhaTemporaria());

        mockMvc.perform(loginNaAlfa()).andExpect(unauthenticated());
        mockMvc.perform(post("/login")
                        .param("empresa", "alfa-engenharia")
                        .param("usuario", "chefe")
                        .param("senha", novaSenha)
                        .with(csrf()))
                .andExpect(authenticated());
    }

    @Test
    @DisplayName("#002-RN008: a tela de administradores nao mostra os motoristas")
    void telaDeAdministradoresSoMostraAdministradores() throws Exception {
        mockMvc.perform(get("/empresas/" + alfa.getId() + "/administradores")
                        .with(comoUsuario(operador, equipe)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Chefe da Alfa")))
                .andExpect(content().string(not(containsString("Motorista da Alfa"))));
    }

    @Test
    @DisplayName("#002-RN008: Operador nao redefine a senha de um motorista")
    void operadorNaoRedefineSenhaDeMotorista() throws Exception {
        String hashAntes = motoristaAlfa.getSenha();

        mockMvc.perform(post("/empresas/" + alfa.getId() + "/administradores/"
                        + motoristaAlfa.getId() + "/redefinir-senha")
                        .with(csrf())
                        .with(comoUsuario(operador, equipe)))
                .andExpect(status().isNotFound());

        assertEquals(hashAntes, usuarioRepository.findById(motoristaAlfa.getId()).orElseThrow().getSenha());
    }

    /* ==================================================================
       Isolamento
       ================================================================== */

    @Test
    @DisplayName("#002-RN007: Administrador nao acessa o cadastro de empresas")
    void administradorNaoAcessaEmpresas() throws Exception {
        RequestPostProcessor comoAdministrador = comoUsuario(adminAlfa, alfa);

        mockMvc.perform(get("/empresas").with(comoAdministrador)).andExpect(status().isForbidden());
        mockMvc.perform(get("/empresas/nova").with(comoAdministrador)).andExpect(status().isForbidden());
        mockMvc.perform(cadastroComo(formularioValido(), comoAdministrador))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/empresas/" + alfa.getId() + "/inativar")
                        .with(csrf()).with(comoAdministrador))
                .andExpect(status().isForbidden());

        assertFalse(empresaRepository.existsByIdentificador("construtora-silva"));
        assertTrue(recarregar(alfa).isAtiva());
    }

    @Test
    @DisplayName("#002-RN007: Motorista nao acessa o cadastro de empresas")
    void motoristaNaoAcessaEmpresas() throws Exception {
        RequestPostProcessor comoMotorista = comoUsuario(motoristaAlfa, alfa);

        mockMvc.perform(get("/empresas").with(comoMotorista)).andExpect(status().isForbidden());
        mockMvc.perform(get("/empresas/nova").with(comoMotorista)).andExpect(status().isForbidden());
        mockMvc.perform(cadastroComo(formularioValido(), comoMotorista))
                .andExpect(status().isForbidden());
    }

    /**
     * #002-RN008: o Operador nao abre nenhuma tela de dados de empresa. A
     * ultima rota, /veiculos, ainda nem existe - e o ponto do teste: o
     * SecurityConfig e "fechado por padrao", entao uma tela nova nasce
     * proibida ao Operador sem ninguem precisar lembrar.
     */
    @Test
    @DisplayName("#002-RN008: Operador nao abre nenhuma tela de dados de uma empresa")
    void operadorNaoAbreTelasDeDados() throws Exception {
        RequestPostProcessor comoOperador = comoUsuario(operador, equipe);

        for (String tela : List.of(
                "/painel",
                "/lancamentos",
                "/usuarios",
                "/usuarios/novo",
                "/usuarios/" + motoristaAlfa.getId() + "/editar",
                "/usuarios/" + motoristaAlfa.getId() + "/anexos",
                "/veiculos")) {
            mockMvc.perform(get(tela).with(comoOperador)).andExpect(status().isForbidden());
        }
    }

    @Test
    @DisplayName("#002-RN010: a empresa reservada da equipe nao aparece na listagem")
    void listagemNaoMostraAEmpresaDaEquipe() throws Exception {
        MvcResult resultado = mockMvc.perform(get("/empresas").with(comoUsuario(operador, equipe)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Alfa Engenharia")))
                .andExpect(content().string(containsString("Beta Transportes")))
                .andReturn();

        @SuppressWarnings("unchecked")
        List<Empresa> listadas = (List<Empresa>) resultado.getModelAndView().getModel().get("empresas");
        assertEquals(2, listadas.size());
        assertTrue(listadas.stream().noneMatch(Empresa::isDaEquipe));
    }

    /**
     * Sem esta protecao, um Operador poderia inativar a empresa da equipe -
     * trancando os tres Operadores do lado de fora - ou gerar senha para a
     * conta de outro Operador.
     */
    @Test
    @DisplayName("#002-RN010: nenhuma tela do Operador alcanca a empresa da equipe")
    void operadorNaoAlcancaAEmpresaDaEquipe() throws Exception {
        RequestPostProcessor comoOperador = comoUsuario(operador, equipe);
        String base = "/empresas/" + equipe.getId();

        mockMvc.perform(get(base + "/editar").with(comoOperador)).andExpect(status().isNotFound());
        mockMvc.perform(get(base + "/administradores").with(comoOperador)).andExpect(status().isNotFound());
        mockMvc.perform(post(base + "/inativar").with(csrf()).with(comoOperador))
                .andExpect(status().isNotFound());
        mockMvc.perform(post(base + "/administradores/" + outroOperador.getId() + "/redefinir-senha")
                        .with(csrf()).with(comoOperador))
                .andExpect(status().isNotFound());

        assertTrue(recarregar(equipe).isAtiva());
        assertFalse(usuarioRepository.findById(outroOperador.getId()).orElseThrow().isSenhaTemporaria());
    }

    /* ==================================================================
       Chegada do Operador
       ================================================================== */

    @Test
    @DisplayName("#002-RF01: depois do login, o Operador cai na lista de empresas")
    void operadorComecaNaListaDeEmpresas() throws Exception {
        mockMvc.perform(get("/").with(comoUsuario(operador, equipe)))
                .andExpect(redirectedUrl("/empresas"));

        // O atalho da equipe na tela inicial do celular tambem funciona com a
        // sessao aberta - nao pode dar 403.
        mockMvc.perform(get("/" + Empresa.IDENTIFICADOR_DA_EQUIPE).with(comoUsuario(operador, equipe)))
                .andExpect(redirectedUrl("/"));
    }

    @Test
    @DisplayName("#002-RF01: o Operador entra pela empresa da equipe")
    void operadorEntraPelaEmpresaDaEquipe() throws Exception {
        mockMvc.perform(post("/login")
                        .param("empresa", Empresa.IDENTIFICADOR_DA_EQUIPE)
                        .param("usuario", "jose.lopes")
                        .param("senha", SENHA)
                        .with(csrf()))
                .andExpect(authenticated().withRoles("OPERADOR"));
    }

    /* ==================================================================
       Auxiliares
       ================================================================== */

    /** Um formulario de cadastro que passa em todas as regras. */
    private MultiValueMap<String, String> formularioValido() {
        MultiValueMap<String, String> formulario = new LinkedMultiValueMap<>();
        formulario.set("nome", "Construtora Silva");
        formulario.set("identificador", "construtora-silva");
        formulario.set("rotuloCcSingular", "Obra");
        formulario.set("rotuloCcPlural", "Obras");
        formulario.set("nomeAdministrador1", "Maria Silva");
        formulario.set("loginAdministrador1", "maria.silva");
        formulario.set("nomeAdministrador2", "Pedro Silva");
        formulario.set("loginAdministrador2", "pedro.silva");
        return formulario;
    }

    /** O POST do cadastro, feito pelo Operador. */
    private MockHttpServletRequestBuilder cadastro(MultiValueMap<String, String> formulario) {
        return cadastroComo(formulario, comoUsuario(operador, equipe));
    }

    /** O mesmo POST, feito por quem o teste escolher. */
    private MockHttpServletRequestBuilder cadastroComo(MultiValueMap<String, String> formulario,
                                                       RequestPostProcessor quem) {
        return post("/empresas")
                .params(formulario)
                .with(csrf())
                .with(quem);
    }

    private MockHttpServletRequestBuilder loginNaAlfa() {
        return post("/login")
                .param("empresa", "alfa-engenharia")
                .param("usuario", "chefe")
                .param("senha", SENHA)
                .with(csrf());
    }

    private RequestPostProcessor comoUsuario(Usuario usuario, Empresa empresa) {
        return user(new UsuarioAutenticado(usuario, empresa));
    }

    private Empresa recarregar(Empresa empresa) {
        return empresaRepository.findById(empresa.getId()).orElseThrow();
    }

    private Usuario criar(Empresa empresa, String login, String nome, Perfil perfil) {
        return usuarioRepository.save(
                new Usuario(empresa, nome, login, passwordEncoder.encode(SENHA), perfil));
    }
}
