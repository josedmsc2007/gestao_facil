package com.gestaofacil.controller;

import com.gestaofacil.model.Empresa;
import com.gestaofacil.model.Perfil;
import com.gestaofacil.model.StatusVeiculo;
import com.gestaofacil.model.TipoCombustivel;
import com.gestaofacil.model.Usuario;
import com.gestaofacil.model.Veiculo;
import com.gestaofacil.repository.EmpresaRepository;
import com.gestaofacil.repository.UsuarioRepository;
import com.gestaofacil.repository.VeiculoRepository;
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

import java.time.Year;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Card #004: cadastro, consulta, edicao e desativacao de veiculos.
 *
 * COMO ESTES TESTES ENTRAM NO SISTEMA
 * Como os do card #003: em vez de fazer POST /login e guardar a sessao, usam
 * .with(user(...)) do spring-security-test, que coloca um UsuarioAutenticado
 * direto na requisicao - o mesmo objeto que o login de verdade produz.
 *
 * DUAS EMPRESAS EM TODO TESTE
 * O @BeforeEach cria a Alfa e a Beta de proposito: e o que permite provar a
 * regra 1 do projeto (a Alfa nao alcanca veiculo da Beta) e tambem a
 * #004-RN001 (a mesma placa pode existir nas duas).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class VeiculoCadastroTests {

    private static final String ALFA = "construtora-alfa";
    private static final String BETA = "construtora-beta";

    /** Placa no padrao Mercosul, usada como exemplo valido. */
    private static final String PLACA = "ABC1D23";

    /** O mesmo RENAVAM escrito dos dois jeitos que aparecem nos documentos. */
    private static final String RENAVAM_COM_9_DIGITOS = "123456789";
    private static final String RENAVAM_COM_11_DIGITOS = "00123456789";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmpresaRepository empresaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private VeiculoRepository veiculoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Empresa alfa;
    private Empresa beta;
    private Usuario adminAlfa;
    private Usuario motoristaAlfa;
    private Usuario adminBeta;
    private Veiculo caminhaoDaBeta;

    @BeforeEach
    void prepararDados() {
        alfa = empresaRepository.save(new Empresa("Construtora Alfa", ALFA, "Obra", "Obras"));
        beta = empresaRepository.save(new Empresa("Construtora Beta", BETA, "Obra", "Obras"));

        adminAlfa = criarUsuario(alfa, "chefe.alfa", "Chefe da Alfa", Perfil.ADMINISTRADOR);
        motoristaAlfa = criarUsuario(alfa, "motorista.alfa", "Motorista da Alfa",
                Perfil.MOTORISTA);
        adminBeta = criarUsuario(beta, "chefe.beta", "Chefe da Beta", Perfil.ADMINISTRADOR);

        caminhaoDaBeta = criarVeiculo(beta, "Caminhão da Beta", "XYZ9876", "99988877766");
    }

    /* ==================================================================
       #004-RF01 - cadastro
       ================================================================== */

    @Test
    @DisplayName("#004-RF01: o Administrador cadastra um veiculo com todos os campos")
    void cadastraVeiculoCompleto() throws Exception {

        mockMvc.perform(cadastro(PLACA, RENAVAM_COM_11_DIGITOS)
                        .with(comoUsuario(adminAlfa, alfa)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/veiculos"));

        Veiculo criado = buscar(alfa, PLACA).orElseThrow();

        assertEquals("Caminhão Ford Cargo", criado.getNome());
        assertEquals(2020, criado.getAno());
        assertEquals(RENAVAM_COM_11_DIGITOS, criado.getRenavam());
        assertEquals(TipoCombustivel.DIESEL_S10, criado.getTipoCombustivel());

        // Regra 1 do projeto: a empresa vem de quem esta logado, nunca do
        // formulario - o VeiculoForm nem tem esse campo.
        assertEquals(alfa.getId(), criado.getEmpresa().getId());

        // #004-RN003: nasce DISPONIVEL sem ninguem ter escolhido isso.
        assertEquals(StatusVeiculo.DISPONIVEL, criado.getStatus());
    }

    @Test
    @DisplayName("#004-RN003: status enviado a mao no POST e ignorado; o veiculo nasce disponivel")
    void statusEnviadoNoFormularioEIgnorado() throws Exception {

        // Simula alguem acrescentando <input name="status" value="EM_USO"> pelo
        // proprio navegador. Como o VeiculoForm nao tem campo de status, o
        // Spring nao tem onde colocar o valor: ele simplesmente se perde.
        mockMvc.perform(cadastro(PLACA, RENAVAM_COM_11_DIGITOS)
                        .param("status", "EM_USO")
                        .with(comoUsuario(adminAlfa, alfa)))
                .andExpect(redirectedUrl("/veiculos"));

        assertEquals(StatusVeiculo.DISPONIVEL,
                buscar(alfa, PLACA).orElseThrow().getStatus());
    }

    @Test
    @DisplayName("#004-RF01: sem nome, ano, placa, RENAVAM ou combustivel o cadastro nao passa")
    void cadastroSemCamposObrigatoriosVolta() throws Exception {
        long antes = veiculoRepository.count();

        mockMvc.perform(post("/veiculos").with(csrf()).with(comoUsuario(adminAlfa, alfa)))
                .andExpect(status().isOk())   // volta para o formulario
                .andExpect(model().attributeHasFieldErrors("veiculoForm",
                        "nome", "ano", "placa", "renavam", "tipoCombustivel"));

        assertEquals(antes, veiculoRepository.count(), "nada pode ter sido gravado");
    }

    @Test
    @DisplayName("#004-RF01: a placa e gravada em maiusculas e sem hifen")
    void placaEGravadaNormalizada() throws Exception {

        mockMvc.perform(cadastro("abc-1d23", RENAVAM_COM_11_DIGITOS)
                        .with(comoUsuario(adminAlfa, alfa)))
                .andExpect(redirectedUrl("/veiculos"));

        Veiculo criado = buscar(alfa, PLACA).orElseThrow();
        assertEquals(PLACA, criado.getPlaca());

        // A tela repoe o tracinho para a leitura.
        assertEquals("ABC-1D23", criado.getPlacaFormatada());
    }

    @Test
    @DisplayName("#004-RF01: o RENAVAM de 9 digitos e completado com zeros a esquerda")
    void renavamDeNoveDigitosEComplementado() throws Exception {

        mockMvc.perform(cadastro(PLACA, RENAVAM_COM_9_DIGITOS)
                        .with(comoUsuario(adminAlfa, alfa)))
                .andExpect(redirectedUrl("/veiculos"));

        assertEquals(RENAVAM_COM_11_DIGITOS, buscar(alfa, PLACA).orElseThrow().getRenavam());
    }

    @Test
    @DisplayName("#004-RF01: placa fora dos padroes brasileiros e recusada")
    void placaInvalidaERecusada() throws Exception {

        mockMvc.perform(cadastro("A1", RENAVAM_COM_11_DIGITOS)
                        .with(comoUsuario(adminAlfa, alfa)))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("veiculoForm", "placa"));

        assertTrue(veiculoRepository.findByEmpresaIdOrderByNome(alfa.getId()).isEmpty());
    }

    @Test
    @DisplayName("#004-RF01: ano muito no futuro e recusado")
    void anoNoFuturoERecusado() throws Exception {

        int anoDemais = Year.now().getValue() + 5;

        // O POST e montado a mao, e nao pelo metodo cadastro(...): ele ja
        // envia um ano, e dois parametros com o mesmo nome fariam o Spring
        // usar o primeiro - o teste passaria sem testar nada.
        mockMvc.perform(post("/veiculos")
                        .param("nome", "Caminhão do futuro")
                        .param("ano", String.valueOf(anoDemais))
                        .param("placa", PLACA)
                        .param("renavam", RENAVAM_COM_11_DIGITOS)
                        .param("tipoCombustivel", "DIESEL_S10")
                        .with(csrf())
                        .with(comoUsuario(adminAlfa, alfa)))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("veiculoForm", "ano"));
    }

    /* ==================================================================
       #004-RN001 - placa e RENAVAM unicos DENTRO da empresa
       ================================================================== */

    @Test
    @DisplayName("#004-RN001: a mesma placa nao entra duas vezes na mesma empresa")
    void placaDuplicadaNaMesmaEmpresaERecusada() throws Exception {

        criarVeiculo(alfa, "Caminhão antigo", PLACA, "55544433322");

        // De proposito escrita de outro jeito: sem a normalizacao, "abc-1d23"
        // e "ABC1D23" seriam duas placas diferentes para o banco.
        mockMvc.perform(cadastro("abc-1d23", RENAVAM_COM_11_DIGITOS)
                        .with(comoUsuario(adminAlfa, alfa)))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("veiculoForm", "placa"));

        assertEquals(1, veiculoRepository.findByEmpresaIdOrderByNome(alfa.getId()).size());
    }

    @Test
    @DisplayName("#004-RN001: o mesmo RENAVAM nao entra duas vezes na mesma empresa")
    void renavamDuplicadoNaMesmaEmpresaERecusado() throws Exception {

        criarVeiculo(alfa, "Caminhão antigo", "AAA1111", RENAVAM_COM_11_DIGITOS);

        // Escrito com 9 digitos: depois de completado com zeros e o mesmo
        // RENAVAM do veiculo que ja existe.
        mockMvc.perform(cadastro(PLACA, RENAVAM_COM_9_DIGITOS)
                        .with(comoUsuario(adminAlfa, alfa)))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("veiculoForm", "renavam"));
    }

    @Test
    @DisplayName("#004-RN001: a mesma placa e o mesmo RENAVAM sao aceitos em outra empresa")
    void mesmaPlacaEmOutraEmpresaEAceita() throws Exception {

        // O caminhao existe na Alfa, inclusive ja desativado - como ficaria
        // depois de vendido (regra 3: nada e apagado).
        Veiculo vendido = criarVeiculo(alfa, "Caminhão vendido", PLACA, RENAVAM_COM_11_DIGITOS);
        vendido.setStatus(StatusVeiculo.INATIVO);
        veiculoRepository.save(vendido);

        // A Beta, que comprou o caminhao, precisa conseguir cadastra-lo.
        mockMvc.perform(cadastro(PLACA, RENAVAM_COM_11_DIGITOS)
                        .with(comoUsuario(adminBeta, beta)))
                .andExpect(redirectedUrl("/veiculos"));

        assertTrue(buscar(alfa, PLACA).isPresent(), "o registro da Alfa continua existindo");
        assertEquals(StatusVeiculo.DISPONIVEL, buscar(beta, PLACA).orElseThrow().getStatus());
    }

    /* ==================================================================
       #004-RF02 - consulta e edicao
       ================================================================== */

    @Test
    @DisplayName("#004-RF02: o Administrador edita os dados de um veiculo ja cadastrado")
    void editaVeiculo() throws Exception {

        Veiculo veiculo = criarVeiculo(alfa, "Nome antigo", "AAA1111", RENAVAM_COM_11_DIGITOS);

        mockMvc.perform(post("/veiculos/" + veiculo.getId())
                        .param("nome", "Caminhão Volvo VM")
                        .param("ano", "2022")
                        .param("placa", "BBB2222")
                        .param("renavam", "22233344455")
                        .param("tipoCombustivel", "DIESEL")
                        .with(csrf())
                        .with(comoUsuario(adminAlfa, alfa)))
                .andExpect(redirectedUrl("/veiculos"));

        Veiculo salvo = veiculoRepository.findById(veiculo.getId()).orElseThrow();
        assertEquals("Caminhão Volvo VM", salvo.getNome());
        assertEquals(2022, salvo.getAno());
        assertEquals("BBB2222", salvo.getPlaca());
        assertEquals(TipoCombustivel.DIESEL, salvo.getTipoCombustivel());
    }

    @Test
    @DisplayName("#004-RN003: a edicao nao mexe no status do veiculo")
    void edicaoNaoAlteraStatus() throws Exception {

        // Veiculo na rua, como o card #006 vai deixa-lo.
        Veiculo veiculo = criarVeiculo(alfa, "Caminhão na rua", "AAA1111",
                RENAVAM_COM_11_DIGITOS);
        veiculo.setStatus(StatusVeiculo.EM_USO);
        veiculoRepository.save(veiculo);

        mockMvc.perform(post("/veiculos/" + veiculo.getId())
                        .param("nome", "Caminhão na rua (renomeado)")
                        .param("ano", "2020")
                        .param("placa", "AAA1111")
                        .param("renavam", RENAVAM_COM_11_DIGITOS)
                        .param("tipoCombustivel", "DIESEL")
                        .param("status", "DISPONIVEL")   // enviado a mao: ignorado
                        .with(csrf())
                        .with(comoUsuario(adminAlfa, alfa)))
                .andExpect(redirectedUrl("/veiculos"));

        Veiculo salvo = veiculoRepository.findById(veiculo.getId()).orElseThrow();
        assertEquals("Caminhão na rua (renomeado)", salvo.getNome());
        assertEquals(StatusVeiculo.EM_USO, salvo.getStatus(), "o status e calculado, nao digitado");
    }

    @Test
    @DisplayName("#004-RN001: salvar a edicao sem mexer na placa nao acusa duplicidade")
    void edicaoComAPropriaPlacaEAceita() throws Exception {

        Veiculo veiculo = criarVeiculo(alfa, "Caminhão", PLACA, RENAVAM_COM_11_DIGITOS);

        mockMvc.perform(post("/veiculos/" + veiculo.getId())
                        .param("nome", "Caminhão com nome novo")
                        .param("ano", "2020")
                        .param("placa", PLACA)
                        .param("renavam", RENAVAM_COM_11_DIGITOS)
                        .param("tipoCombustivel", "DIESEL")
                        .with(csrf())
                        .with(comoUsuario(adminAlfa, alfa)))
                .andExpect(redirectedUrl("/veiculos"));

        assertEquals("Caminhão com nome novo",
                veiculoRepository.findById(veiculo.getId()).orElseThrow().getNome());
    }

    /* ==================================================================
       #004-RF03 - ativacao e desativacao
       ================================================================== */

    @Test
    @DisplayName("#004-RF03 e RN005: desativar marca como INATIVO sem apagar o registro")
    void desativarNaoApaga() throws Exception {

        Veiculo veiculo = criarVeiculo(alfa, "Caminhão vendido", PLACA, RENAVAM_COM_11_DIGITOS);
        long antes = veiculoRepository.count();

        mockMvc.perform(post("/veiculos/" + veiculo.getId() + "/desativar")
                        .with(csrf())
                        .with(comoUsuario(adminAlfa, alfa)))
                .andExpect(redirectedUrl("/veiculos"));

        assertEquals(antes, veiculoRepository.count(), "regra 3: nada e apagado");
        assertEquals(StatusVeiculo.INATIVO,
                veiculoRepository.findById(veiculo.getId()).orElseThrow().getStatus());
    }

    @Test
    @DisplayName("#004-RF03: o veiculo reativado volta como disponivel")
    void ativarVoltaParaDisponivel() throws Exception {

        Veiculo veiculo = criarVeiculo(alfa, "Caminhão parado", PLACA, RENAVAM_COM_11_DIGITOS);
        veiculo.setStatus(StatusVeiculo.INATIVO);
        veiculoRepository.save(veiculo);

        mockMvc.perform(post("/veiculos/" + veiculo.getId() + "/ativar")
                        .with(csrf())
                        .with(comoUsuario(adminAlfa, alfa)))
                .andExpect(redirectedUrl("/veiculos"));

        assertEquals(StatusVeiculo.DISPONIVEL,
                veiculoRepository.findById(veiculo.getId()).orElseThrow().getStatus());
    }

    /**
     * Decisao da implementacao, nao escrita no card: o veiculo que esta na rua
     * ou na oficina nao pode ser desativado, porque isso apagaria um status
     * CALCULADO (regra 2 do projeto). Ver VeiculoService.podeSerDesativado.
     */
    @Test
    @DisplayName("Regra 2: veiculo em uso nao pode ser desativado, e a tela explica o motivo")
    void naoDesativaVeiculoEmUso() throws Exception {

        Veiculo veiculo = criarVeiculo(alfa, "Caminhão na rua", PLACA, RENAVAM_COM_11_DIGITOS);
        veiculo.setStatus(StatusVeiculo.EM_USO);
        veiculoRepository.save(veiculo);

        mockMvc.perform(post("/veiculos/" + veiculo.getId() + "/desativar")
                        .with(csrf())
                        .with(comoUsuario(adminAlfa, alfa)))
                .andExpect(redirectedUrl("/veiculos"))
                .andExpect(flash().attributeExists("erro"));

        assertEquals(StatusVeiculo.EM_USO,
                veiculoRepository.findById(veiculo.getId()).orElseThrow().getStatus());
    }

    /* ==================================================================
       Regra 1 do projeto - isolamento por empresa
       ================================================================== */

    @Test
    @DisplayName("Regra 1: a lista mostra so os veiculos da propria empresa")
    void listaNaoMostraVeiculoDeOutraEmpresa() throws Exception {

        criarVeiculo(alfa, "Caminhão da Alfa", PLACA, RENAVAM_COM_11_DIGITOS);

        mockMvc.perform(get("/veiculos").with(comoUsuario(adminAlfa, alfa)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Caminhão da Alfa")))
                .andExpect(content().string(not(containsString("Caminhão da Beta"))));
    }

    @Test
    @DisplayName("Regra 1: abrir a edicao de veiculo de outra empresa responde 404")
    void naoAbreEdicaoDeVeiculoDeOutraEmpresa() throws Exception {
        mockMvc.perform(get("/veiculos/" + caminhaoDaBeta.getId() + "/editar")
                        .with(comoUsuario(adminAlfa, alfa)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Regra 1: salvar edicao de veiculo de outra empresa responde 404")
    void naoSalvaEdicaoDeVeiculoDeOutraEmpresa() throws Exception {

        mockMvc.perform(post("/veiculos/" + caminhaoDaBeta.getId())
                        .param("nome", "Invadido")
                        .param("ano", "2020")
                        .param("placa", PLACA)
                        .param("renavam", RENAVAM_COM_11_DIGITOS)
                        .param("tipoCombustivel", "DIESEL")
                        .with(csrf())
                        .with(comoUsuario(adminAlfa, alfa)))
                .andExpect(status().isNotFound());

        Veiculo intacto = veiculoRepository.findById(caminhaoDaBeta.getId()).orElseThrow();
        assertEquals("Caminhão da Beta", intacto.getNome());
        assertEquals("XYZ9876", intacto.getPlaca());
    }

    @Test
    @DisplayName("Regra 1: desativar veiculo de outra empresa responde 404")
    void naoDesativaVeiculoDeOutraEmpresa() throws Exception {

        mockMvc.perform(post("/veiculos/" + caminhaoDaBeta.getId() + "/desativar")
                        .with(csrf())
                        .with(comoUsuario(adminAlfa, alfa)))
                .andExpect(status().isNotFound());

        assertEquals(StatusVeiculo.DISPONIVEL,
                veiculoRepository.findById(caminhaoDaBeta.getId()).orElseThrow().getStatus());
    }

    @Test
    @DisplayName("#004-RN004: motorista nao entra na tela de veiculos")
    void motoristaNaoAcessaTelaDeVeiculos() throws Exception {
        mockMvc.perform(get("/veiculos").with(comoUsuario(motoristaAlfa, alfa)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/veiculos")
                        .param("nome", "Caminhão do motorista")
                        .param("ano", "2020")
                        .param("placa", PLACA)
                        .param("renavam", RENAVAM_COM_11_DIGITOS)
                        .param("tipoCombustivel", "DIESEL")
                        .with(csrf())
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
    @DisplayName("#002-RN008: Operador leva 403 na tela de veiculos")
    void operadorNaoAcessaTelaDeVeiculos() throws Exception {

        Empresa equipe = empresaRepository.save(new Empresa("Equipe Gestão Fácil",
                Empresa.IDENTIFICADOR_DA_EQUIPE, "Centro de custo", "Centros de custo"));
        Usuario operador = criarUsuario(equipe, "jose.lopes", "José Lopes", Perfil.OPERADOR);

        mockMvc.perform(get("/veiculos").with(comoUsuario(operador, equipe)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/veiculos/novo").with(comoUsuario(operador, equipe)))
                .andExpect(status().isForbidden());
    }

    /* ==================================================================
       Auxiliares
       ================================================================== */

    /**
     * Coloca na requisicao o mesmo UsuarioAutenticado que o login de verdade
     * produziria - serve para qualquer perfil, e e assim que os testes do
     * motorista e do Operador provam que a tela os barra.
     */
    private RequestPostProcessor comoUsuario(Usuario usuario, Empresa empresa) {
        return user(new UsuarioAutenticado(usuario, empresa));
    }

    /** O POST de cadastro completo, com placa e RENAVAM variaveis. */
    private MockHttpServletRequestBuilder cadastro(String placa, String renavam) {
        return post("/veiculos")
                .param("nome", "Caminhão Ford Cargo")
                .param("ano", "2020")
                .param("placa", placa)
                .param("renavam", renavam)
                .param("tipoCombustivel", "DIESEL_S10")
                .with(csrf());
    }

    private Optional<Veiculo> buscar(Empresa empresa, String placa) {
        return veiculoRepository.findByEmpresaIdOrderByNome(empresa.getId()).stream()
                .filter(veiculo -> veiculo.getPlaca().equals(placa))
                .findFirst();
    }

    private Usuario criarUsuario(Empresa empresa, String login, String nome, Perfil perfil) {
        Usuario usuario = new Usuario(empresa, nome, login,
                passwordEncoder.encode("senha-de-teste-123"), perfil);
        return usuarioRepository.save(usuario);
    }

    private Veiculo criarVeiculo(Empresa empresa, String nome, String placa, String renavam) {
        Veiculo veiculo = new Veiculo(empresa, nome, 2015, renavam, placa,
                TipoCombustivel.DIESEL);
        Veiculo salvo = veiculoRepository.save(veiculo);
        assertNotNull(salvo.getId());
        return salvo;
    }
}
