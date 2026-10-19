package com.gestaofacil.controller;

import com.gestaofacil.model.Abastecimento;
import com.gestaofacil.model.Empresa;
import com.gestaofacil.model.Perfil;
import com.gestaofacil.model.StatusVeiculo;
import com.gestaofacil.model.TipoCombustivel;
import com.gestaofacil.model.Usuario;
import com.gestaofacil.model.Veiculo;
import com.gestaofacil.repository.AbastecimentoRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
 * Card #007: controle de abastecimento.
 *
 * Duas empresas, como no UsoVeiculoTests, para provar a regra 1: o
 * motorista da Alfa nao lanca abastecimento em veiculo da Beta.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AbastecimentoTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmpresaRepository empresaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private VeiculoRepository veiculoRepository;

    @Autowired
    private AbastecimentoRepository abastecimentoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Empresa alfa;
    private Empresa beta;

    private Usuario motoristaAlfa;
    private Usuario adminAlfa;

    private Veiculo caminhaoDaAlfa;
    private Veiculo caminhaoDaBeta;

    @BeforeEach
    void prepararDados() {
        alfa = empresaRepository.save(
                new Empresa("Construtora Alfa", "construtora-alfa", "Obra", "Obras"));
        beta = empresaRepository.save(
                new Empresa("Transportadora Beta", "transportadora-beta", "Rota", "Rotas"));

        motoristaAlfa = criarUsuario(alfa, "motorista.alfa", "Motorista da Alfa",
                Perfil.MOTORISTA);
        adminAlfa = criarUsuario(alfa, "chefe.alfa", "Chefe da Alfa", Perfil.ADMINISTRADOR);

        caminhaoDaAlfa = criarVeiculo(alfa, "Caminhão da Alfa", "ABC1D23", "00123456789");
        caminhaoDaBeta = criarVeiculo(beta, "Caminhão da Beta", "XYZ9876", "99988877766");
    }

    /* ==================================================================
       #007-RF01 - registro
       ================================================================== */

    @Test
    @DisplayName("#007-RF01: o motorista registra um abastecimento com todos os campos")
    void registraAbastecimento() throws Exception {

        LocalDate hoje = LocalDate.now();

        mockMvc.perform(abastecimento(caminhaoDaAlfa, hoje, "15230", "45,5", "280,90")
                        .param("posto", "  Posto Central  ")
                        .with(comoUsuario(motoristaAlfa, alfa)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/lancamentos"))
                .andExpect(flash().attributeExists("mensagem"));

        Abastecimento abastecimento = unicoAbastecimento();

        assertEquals(caminhaoDaAlfa.getId(), abastecimento.getVeiculo().getId());
        assertEquals(hoje, abastecimento.getData());
        assertEquals(15230, abastecimento.getQuilometragem());
        assertEquals(0, new BigDecimal("45.5").compareTo(abastecimento.getLitros()));
        assertEquals(0, new BigDecimal("280.90").compareTo(abastecimento.getValor()));
        assertEquals("Posto Central", abastecimento.getPosto());

        // #007-RN007: empresa e motorista vem de quem esta logado.
        assertEquals(motoristaAlfa.getId(), abastecimento.getUsuario().getId());
        assertEquals(alfa.getId(), abastecimento.getEmpresa().getId());
    }

    @Test
    @DisplayName("#007: litros e valor aceitam ponto como separador decimal")
    void aceitaPontoComoSeparador() throws Exception {

        mockMvc.perform(abastecimento(caminhaoDaAlfa, LocalDate.now(), "15230", "45.5", "280.90")
                        .with(comoUsuario(motoristaAlfa, alfa)))
                .andExpect(redirectedUrl("/lancamentos"));

        Abastecimento abastecimento = unicoAbastecimento();
        assertEquals(0, new BigDecimal("45.5").compareTo(abastecimento.getLitros()));
        assertEquals(0, new BigDecimal("280.90").compareTo(abastecimento.getValor()));
    }

    @Test
    @DisplayName("#007: valor com milhar no padrao brasileiro (1.234,56) e aceito")
    void aceitaMilharComPonto() throws Exception {

        mockMvc.perform(abastecimento(caminhaoDaAlfa, LocalDate.now(), "15230", "300", "1.234,56")
                        .with(comoUsuario(motoristaAlfa, alfa)))
                .andExpect(redirectedUrl("/lancamentos"));

        assertEquals(0, new BigDecimal("1234.56").compareTo(unicoAbastecimento().getValor()));
    }

    @Test
    @DisplayName("#007-RN002: posto em branco e aceito e fica nulo")
    void postoEOpcional() throws Exception {

        mockMvc.perform(abastecimento(caminhaoDaAlfa, LocalDate.now(), "15230", "45,5", "280,90")
                        .param("posto", "   ")
                        .with(comoUsuario(motoristaAlfa, alfa)))
                .andExpect(redirectedUrl("/lancamentos"));

        assertNull(unicoAbastecimento().getPosto());
    }

    @Test
    @DisplayName("#007-RN001: sem os campos obrigatorios o abastecimento nao passa")
    void semCamposObrigatoriosVolta() throws Exception {

        mockMvc.perform(post("/abastecimentos")
                        .with(csrf())
                        .with(comoUsuario(motoristaAlfa, alfa)))
                .andExpect(status().isOk())   // volta para o formulario
                .andExpect(model().attributeHasFieldErrors("abastecimentoForm",
                        "veiculoId", "data", "quilometragem", "litros", "valor"));

        assertEquals(0, abastecimentoRepository.count(), "nada pode ter sido gravado");
    }

    @Test
    @DisplayName("#007-RN001: faltando so um campo, o abastecimento tambem nao passa")
    void faltandoUmCampoVolta() throws Exception {

        // Montado a mao, sem o auxiliar: o "valor" fica de fora.
        mockMvc.perform(post("/abastecimentos")
                        .param("veiculoId", caminhaoDaAlfa.getId().toString())
                        .param("data", LocalDate.now().toString())
                        .param("quilometragem", "15230")
                        .param("litros", "45,5")
                        .with(csrf())
                        .with(comoUsuario(motoristaAlfa, alfa)))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("abastecimentoForm", "valor"));

        assertEquals(0, abastecimentoRepository.count());
    }

    @Test
    @DisplayName("#007: numero que nao e numero volta com erro no campo, sem pagina de erro")
    void numeroInvalidoVoltaComErroNoCampo() throws Exception {

        mockMvc.perform(abastecimento(caminhaoDaAlfa, LocalDate.now(), "15.230", "muito", "1,2,3")
                        .with(comoUsuario(motoristaAlfa, alfa)))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("abastecimentoForm",
                        "quilometragem", "litros", "valor"))
                .andExpect(content().string(containsString("Informe um número válido")));

        assertEquals(0, abastecimentoRepository.count());
    }

    /* ==================================================================
       #007-RN006 - maior que zero e data que ja aconteceu
       ================================================================== */

    @Test
    @DisplayName("#007-RN006: quilometragem, litros e valor zero ou negativos sao recusados")
    void zeroOuNegativoERecusado() throws Exception {

        LocalDate hoje = LocalDate.now();

        // Cada linha: quilometragem, litros, valor - um so campo errado por vez.
        List<String[]> casos = List.of(
                new String[]{"0", "45,5", "280,90"},
                new String[]{"-10", "45,5", "280,90"},
                new String[]{"15230", "0", "280,90"},
                new String[]{"15230", "-1,5", "280,90"},
                new String[]{"15230", "45,5", "0,00"},
                new String[]{"15230", "45,5", "-20"});

        for (String[] caso : casos) {
            mockMvc.perform(abastecimento(caminhaoDaAlfa, hoje, caso[0], caso[1], caso[2])
                            .with(comoUsuario(motoristaAlfa, alfa)))
                    .andExpect(status().isOk())
                    .andExpect(model().attributeExists("erro"));
        }

        assertEquals(0, abastecimentoRepository.count());
    }

    @Test
    @DisplayName("#007-RN006: data no futuro e recusada")
    void dataNoFuturoERecusada() throws Exception {

        mockMvc.perform(abastecimento(caminhaoDaAlfa, LocalDate.now().plusDays(1),
                        "15230", "45,5", "280,90")
                        .with(comoUsuario(motoristaAlfa, alfa)))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("erro"));

        assertEquals(0, abastecimentoRepository.count());
    }

    /* ==================================================================
       #007-RN005 - data informada x data de registro (regra 4)
       ================================================================== */

    @Test
    @DisplayName("#007-RN005: aceita data passada e grava a data de registro pelo sistema")
    void dataPassadaEDataDeRegistro() throws Exception {

        LocalDate tresDiasAtras = LocalDate.now().minusDays(3);
        LocalDateTime antesDoEnvio = LocalDateTime.now();

        // "dataRegistro" enviado a mao: o AbastecimentoForm nao tem esse
        // campo, entao o valor se perde.
        mockMvc.perform(abastecimento(caminhaoDaAlfa, tresDiasAtras, "15230", "45,5", "280,90")
                        .param("dataRegistro", "2000-01-01T00:00")
                        .with(comoUsuario(motoristaAlfa, alfa)))
                .andExpect(redirectedUrl("/lancamentos"));

        Abastecimento abastecimento = unicoAbastecimento();

        // A data INFORMADA e a que o motorista digitou...
        assertEquals(tresDiasAtras, abastecimento.getData());

        // ...e a de REGISTRO e a hora do servidor, num outro dia.
        assertNotNull(abastecimento.getDataRegistro());
        assertFalse(abastecimento.getDataRegistro().isBefore(antesDoEnvio));
        assertFalse(abastecimento.getDataRegistro().isAfter(LocalDateTime.now()));
        assertNotEquals(abastecimento.getData(), abastecimento.getDataRegistro().toLocalDate());
    }

    /* ==================================================================
       #007-RN003 - veiculo existente, ativo e da propria empresa
       ================================================================== */

    @Test
    @DisplayName("#007-RN003: motorista nao lanca abastecimento em veiculo de outra empresa")
    void naoLancaEmVeiculoDeOutraEmpresa() throws Exception {

        mockMvc.perform(abastecimento(caminhaoDaBeta, LocalDate.now(), "15230", "45,5", "280,90")
                        .with(comoUsuario(motoristaAlfa, alfa)))
                .andExpect(status().isNotFound());

        assertEquals(0, abastecimentoRepository.count());
    }

    @Test
    @DisplayName("#007-RN003: veiculo que nao existe responde 404")
    void veiculoInexistenteResponde404() throws Exception {

        mockMvc.perform(post("/abastecimentos")
                        .param("veiculoId", "999999")
                        .param("data", LocalDate.now().toString())
                        .param("quilometragem", "15230")
                        .param("litros", "45,5")
                        .param("valor", "280,90")
                        .with(csrf())
                        .with(comoUsuario(motoristaAlfa, alfa)))
                .andExpect(status().isNotFound());

        assertEquals(0, abastecimentoRepository.count());
    }

    @Test
    @DisplayName("#007-RN003: veiculo inativo nao recebe abastecimento")
    void veiculoInativoERecusado() throws Exception {

        caminhaoDaAlfa.setStatus(StatusVeiculo.INATIVO);
        veiculoRepository.save(caminhaoDaAlfa);

        mockMvc.perform(abastecimento(caminhaoDaAlfa, LocalDate.now(), "15230", "45,5", "280,90")
                        .with(comoUsuario(motoristaAlfa, alfa)))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("erro"));

        assertEquals(0, abastecimentoRepository.count());
        assertEquals(StatusVeiculo.INATIVO, statusDe(caminhaoDaAlfa));
    }

    @Test
    @DisplayName("#007-RN003: veiculo EM_USO recebe abastecimento e o status nao muda")
    void veiculoEmUsoRecebeEStatusNaoMuda() throws Exception {

        caminhaoDaAlfa.setStatus(StatusVeiculo.EM_USO);
        veiculoRepository.save(caminhaoDaAlfa);

        mockMvc.perform(abastecimento(caminhaoDaAlfa, LocalDate.now(), "15230", "45,5", "280,90")
                        .with(comoUsuario(motoristaAlfa, alfa)))
                .andExpect(redirectedUrl("/lancamentos"));

        assertEquals(1, abastecimentoRepository.count());
        assertEquals(StatusVeiculo.EM_USO, statusDe(caminhaoDaAlfa));
    }

    @Test
    @DisplayName("Regra 2: abastecer veiculo DISPONIVEL tambem nao muda o status")
    void veiculoDisponivelContinuaDisponivel() throws Exception {

        mockMvc.perform(abastecimento(caminhaoDaAlfa, LocalDate.now(), "15230", "45,5", "280,90")
                        .with(comoUsuario(motoristaAlfa, alfa)))
                .andExpect(redirectedUrl("/lancamentos"));

        assertEquals(StatusVeiculo.DISPONIVEL, statusDe(caminhaoDaAlfa));
    }

    @Test
    @DisplayName("#007-RN003: a tela oferece os veiculos ativos da empresa, inclusive o EM_USO")
    void telaOfereceSoVeiculosAtivosDaEmpresa() throws Exception {

        Veiculo picape = criarVeiculo(alfa, "Picape da Alfa", "DEF2G34", "00987654321");
        picape.setStatus(StatusVeiculo.EM_USO);
        veiculoRepository.save(picape);

        Veiculo vendido = criarVeiculo(alfa, "Furgão Vendido", "GHI3J45", "00111222333");
        vendido.setStatus(StatusVeiculo.INATIVO);
        veiculoRepository.save(vendido);

        String hoje = LocalDate.now().toString();

        mockMvc.perform(get("/abastecimentos/novo").with(comoUsuario(motoristaAlfa, alfa)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Caminhão da Alfa")))
                .andExpect(content().string(containsString("Picape da Alfa")))
                .andExpect(content().string(not(containsString("Furgão Vendido"))))
                .andExpect(content().string(not(containsString("Caminhão da Beta"))))
                // a data ja vem preenchida com hoje
                .andExpect(content().string(containsString("value=\"" + hoje + "\"")));
    }

    /* ==================================================================
       Quem entra nas telas (#007-RN004 e #002-RN008)
       ================================================================== */

    @Test
    @DisplayName("#007-RN004: Administrador nao registra abastecimento")
    void administradorNaoAcessa() throws Exception {

        mockMvc.perform(get("/abastecimentos/novo").with(comoUsuario(adminAlfa, alfa)))
                .andExpect(status().isForbidden());

        mockMvc.perform(abastecimento(caminhaoDaAlfa, LocalDate.now(), "15230", "45,5", "280,90")
                        .with(comoUsuario(adminAlfa, alfa)))
                .andExpect(status().isForbidden());

        assertEquals(0, abastecimentoRepository.count());
    }

    @Test
    @DisplayName("#002-RN008: Operador leva 403 nas rotas de abastecimento")
    void operadorNaoAcessa() throws Exception {

        Empresa equipe = empresaRepository.save(new Empresa("Equipe Gestão Fácil",
                Empresa.IDENTIFICADOR_DA_EQUIPE, "Centro de custo", "Centros de custo"));
        Usuario operador = criarUsuario(equipe, "jose.lopes", "José Lopes", Perfil.OPERADOR);

        mockMvc.perform(get("/abastecimentos/novo").with(comoUsuario(operador, equipe)))
                .andExpect(status().isForbidden());

        mockMvc.perform(abastecimento(caminhaoDaAlfa, LocalDate.now(), "15230", "45,5", "280,90")
                        .with(comoUsuario(operador, equipe)))
                .andExpect(status().isForbidden());

        assertEquals(0, abastecimentoRepository.count());
    }

    @Test
    @DisplayName("Quem nao esta logado nao registra abastecimento")
    void visitanteNaoRegistra() throws Exception {

        mockMvc.perform(abastecimento(caminhaoDaAlfa, LocalDate.now(), "15230", "45,5", "280,90"))
                .andExpect(status().is3xxRedirection());

        assertEquals(0, abastecimentoRepository.count());
    }

    /* ==================================================================
       Tela inicial e aviso de sem conexao
       ================================================================== */

    @Test
    @DisplayName("#007: a tela inicial do motorista leva ao abastecimento")
    void telaInicialTemOLink() throws Exception {

        mockMvc.perform(get("/lancamentos").with(comoUsuario(motoristaAlfa, alfa)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/abastecimentos/novo\"")))
                .andExpect(content().string(not(containsString("em breve"))));
    }

    @Test
    @DisplayName("#007: a tela de abastecimento traz o aviso de sem conexao")
    void telaTrazOAvisoDeSemConexao() throws Exception {

        mockMvc.perform(get("/abastecimentos/novo").with(comoUsuario(motoristaAlfa, alfa)))
                .andExpect(content().string(containsString("id=\"aviso-sem-conexao\"")))
                .andExpect(content().string(containsString("data-exige-conexao")))
                .andExpect(content().string(containsString("/js/aviso-sem-conexao.js")));
    }

    @Test
    @DisplayName("Regra 6: a tela de abastecimento nao tem rotulo fixo de centro de custo")
    void telaNaoTemRotuloFixo() throws Exception {

        Usuario motoristaBeta = criarUsuario(beta, "motorista.beta", "Motorista da Beta",
                Perfil.MOTORISTA);

        mockMvc.perform(get("/abastecimentos/novo").with(comoUsuario(motoristaBeta, beta)))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Obra"))));
    }

    /* ==================================================================
       Auxiliares
       ================================================================== */

    private RequestPostProcessor comoUsuario(Usuario usuario, Empresa empresa) {
        return user(new UsuarioAutenticado(usuario, empresa));
    }

    /**
     * O POST de abastecimento com os cinco campos obrigatorios, como texto -
     * do jeito que o navegador envia.
     *
     * ATENCAO a armadilha do .param repetido: para variar um destes campos,
     * passe outro valor AQUI, e nao um .param com o mesmo nome depois.
     */
    private MockHttpServletRequestBuilder abastecimento(Veiculo veiculo, LocalDate data,
                                                        String quilometragem, String litros,
                                                        String valor) {
        return post("/abastecimentos")
                .param("veiculoId", veiculo.getId().toString())
                .param("data", data.toString())
                .param("quilometragem", quilometragem)
                .param("litros", litros)
                .param("valor", valor)
                .with(csrf());
    }

    private Abastecimento unicoAbastecimento() {
        List<Abastecimento> abastecimentos = abastecimentoRepository.findAll();
        assertEquals(1, abastecimentos.size());
        return abastecimentos.get(0);
    }

    private StatusVeiculo statusDe(Veiculo veiculo) {
        return veiculoRepository.findById(veiculo.getId()).orElseThrow().getStatus();
    }

    private Usuario criarUsuario(Empresa empresa, String login, String nome, Perfil perfil) {
        Usuario usuario = new Usuario(empresa, nome, login,
                passwordEncoder.encode("senha-de-teste-123"), perfil);
        return usuarioRepository.save(usuario);
    }

    private Veiculo criarVeiculo(Empresa empresa, String nome, String placa, String renavam) {
        return veiculoRepository.save(new Veiculo(empresa, nome, 2020, renavam, placa,
                TipoCombustivel.DIESEL_S10));
    }
}
