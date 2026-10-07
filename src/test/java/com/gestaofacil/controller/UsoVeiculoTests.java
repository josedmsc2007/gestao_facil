package com.gestaofacil.controller;

import com.gestaofacil.model.CentroCusto;
import com.gestaofacil.model.Empresa;
import com.gestaofacil.model.Perfil;
import com.gestaofacil.model.StatusVeiculo;
import com.gestaofacil.model.TipoCombustivel;
import com.gestaofacil.model.UsoVeiculo;
import com.gestaofacil.model.Usuario;
import com.gestaofacil.model.Veiculo;
import com.gestaofacil.repository.CentroCustoRepository;
import com.gestaofacil.repository.EmpresaRepository;
import com.gestaofacil.repository.UsoVeiculoRepository;
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
 * Card #006: saida e devolucao de veiculos.
 *
 * DUAS EMPRESAS, COMO NOS OUTROS CARDS
 * A Alfa chama os centros de custo de "Obra" e a Beta de "Rota". Com as duas
 * da para provar a regra 1 (o motorista da Alfa nao lanca saida com veiculo
 * nem centro de custo da Beta) e a regra 6 (a tela usa o rotulo da empresa).
 *
 * DOIS MOTORISTAS NA ALFA
 * Para provar que um nao sai com o veiculo que esta com o outro, e que a
 * devolucao de um nao fecha a saida do outro.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UsoVeiculoTests {

    /** O formato que o <input type="datetime-local"> envia. */
    private static final DateTimeFormatter FORMATO_DO_CAMPO =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmpresaRepository empresaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private VeiculoRepository veiculoRepository;

    @Autowired
    private CentroCustoRepository centroCustoRepository;

    @Autowired
    private UsoVeiculoRepository usoVeiculoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Empresa alfa;
    private Empresa beta;

    private Usuario motoristaAlfa;
    private Usuario outroMotoristaAlfa;
    private Usuario adminAlfa;
    private Usuario motoristaBeta;

    private Veiculo caminhaoDaAlfa;
    private Veiculo picapeDaAlfa;
    private Veiculo caminhaoDaBeta;

    private CentroCusto obraDaAlfa;
    private CentroCusto rotaDaBeta;

    /** Uma hora atras: data valida para saida (nao esta no futuro). */
    private LocalDateTime umaHoraAtras;

    @BeforeEach
    void prepararDados() {
        alfa = empresaRepository.save(
                new Empresa("Construtora Alfa", "construtora-alfa", "Obra", "Obras"));
        beta = empresaRepository.save(
                new Empresa("Transportadora Beta", "transportadora-beta", "Rota", "Rotas"));

        motoristaAlfa = criarUsuario(alfa, "motorista.alfa", "Motorista da Alfa",
                Perfil.MOTORISTA);
        outroMotoristaAlfa = criarUsuario(alfa, "outro.alfa", "Outro Motorista da Alfa",
                Perfil.MOTORISTA);
        adminAlfa = criarUsuario(alfa, "chefe.alfa", "Chefe da Alfa", Perfil.ADMINISTRADOR);
        motoristaBeta = criarUsuario(beta, "motorista.beta", "Motorista da Beta",
                Perfil.MOTORISTA);

        caminhaoDaAlfa = criarVeiculo(alfa, "Caminhão da Alfa", "ABC1D23", "00123456789");
        picapeDaAlfa = criarVeiculo(alfa, "Picape da Alfa", "DEF2G34", "00987654321");
        caminhaoDaBeta = criarVeiculo(beta, "Caminhão da Beta", "XYZ9876", "99988877766");

        obraDaAlfa = centroCustoRepository.save(
                new CentroCusto(alfa, "Residencial Parque", "Campinas", "SP", null));
        rotaDaBeta = centroCustoRepository.save(
                new CentroCusto(beta, "Litoral Sul", "Santos", "SP", null));

        umaHoraAtras = LocalDateTime.now().minusHours(1).truncatedTo(ChronoUnit.MINUTES);
    }

    /* ==================================================================
       #006-RF01 - registro de saida
       ================================================================== */

    @Test
    @DisplayName("#006-RF01: o motorista registra a saida de um veiculo disponivel")
    void registraSaida() throws Exception {

        mockMvc.perform(saida(caminhaoDaAlfa, obraDaAlfa, umaHoraAtras)
                        .param("observacao", "  Levar cimento  ")
                        .with(comoUsuario(motoristaAlfa, alfa)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/lancamentos"))
                .andExpect(flash().attributeExists("mensagem"));

        UsoVeiculo uso = unicoUso();

        assertEquals(caminhaoDaAlfa.getId(), uso.getVeiculo().getId());
        assertEquals(obraDaAlfa.getId(), uso.getCentroCusto().getId());
        assertEquals(umaHoraAtras, uso.getDataSaida());
        assertEquals("Levar cimento", uso.getObservacao());

        // O motorista e a empresa vem de quem esta logado: o SaidaForm nem
        // tem esses campos.
        assertEquals(motoristaAlfa.getId(), uso.getUsuario().getId());
        assertEquals(alfa.getId(), uso.getEmpresa().getId());

        // Nasce em aberto: sem data de devolucao.
        assertNull(uso.getDataDevolucao());
        assertTrue(uso.isEmAberto());
    }

    @Test
    @DisplayName("#006-RF01: a observacao e opcional e fica nula quando vem em branco")
    void observacaoEOpcional() throws Exception {

        mockMvc.perform(saida(caminhaoDaAlfa, obraDaAlfa, umaHoraAtras)
                        .param("observacao", "   ")
                        .with(comoUsuario(motoristaAlfa, alfa)))
                .andExpect(redirectedUrl("/lancamentos"));

        assertNull(unicoUso().getObservacao());
    }

    @Test
    @DisplayName("#006-RN001: sem veiculo, centro de custo ou data a saida nao passa")
    void saidaSemCamposObrigatoriosVolta() throws Exception {

        mockMvc.perform(post("/saidas")
                        .with(csrf())
                        .with(comoUsuario(motoristaAlfa, alfa)))
                .andExpect(status().isOk())   // volta para o formulario
                .andExpect(model().attributeHasFieldErrors("saidaForm",
                        "veiculoId", "centroCustoId", "dataSaida"));

        assertEquals(0, usoVeiculoRepository.count(), "nada pode ter sido gravado");
    }

    /* ==================================================================
       #006-RN003 - o status do veiculo muda sozinho (regra 2 do projeto)
       ================================================================== */

    @Test
    @DisplayName("#006-RN003: a saida passa o veiculo para EM_USO e a devolucao o devolve para DISPONIVEL")
    void statusMudaNaSaidaENaDevolucao() throws Exception {

        assertEquals(StatusVeiculo.DISPONIVEL, statusDe(caminhaoDaAlfa));

        mockMvc.perform(saida(caminhaoDaAlfa, obraDaAlfa, umaHoraAtras)
                        .with(comoUsuario(motoristaAlfa, alfa)))
                .andExpect(redirectedUrl("/lancamentos"));

        assertEquals(StatusVeiculo.EM_USO, statusDe(caminhaoDaAlfa));

        // O outro veiculo da empresa nao tem nada a ver com isso.
        assertEquals(StatusVeiculo.DISPONIVEL, statusDe(picapeDaAlfa));

        mockMvc.perform(devolucao(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES))
                        .with(comoUsuario(motoristaAlfa, alfa)))
                .andExpect(redirectedUrl("/lancamentos"));

        assertEquals(StatusVeiculo.DISPONIVEL, statusDe(caminhaoDaAlfa));
    }

    @Test
    @DisplayName("#006-RN003: um status enviado a mao pelo formulario e ignorado")
    void statusEnviadoPeloFormularioEIgnorado() throws Exception {

        // O SaidaForm nao tem campo de status: o Spring nao tem onde colocar
        // o valor. O veiculo vai para EM_USO porque SAIU, e nao porque
        // alguem escreveu.
        mockMvc.perform(saida(caminhaoDaAlfa, obraDaAlfa, umaHoraAtras)
                        .param("status", "INATIVO")
                        .param("veiculo.status", "INATIVO")
                        .with(comoUsuario(motoristaAlfa, alfa)))
                .andExpect(redirectedUrl("/lancamentos"));

        assertEquals(StatusVeiculo.EM_USO, statusDe(caminhaoDaAlfa));
    }

    @Test
    @DisplayName("#006-RN003: veiculo que ja esta em uso nao sai com outro motorista")
    void veiculoEmUsoNaoSaiDeNovo() throws Exception {

        mockMvc.perform(saida(caminhaoDaAlfa, obraDaAlfa, umaHoraAtras)
                        .with(comoUsuario(motoristaAlfa, alfa)))
                .andExpect(redirectedUrl("/lancamentos"));

        // A tela so oferece os disponiveis, mas o outro motorista pode estar
        // com ela aberta desde antes: quem barra e o servidor.
        mockMvc.perform(saida(caminhaoDaAlfa, obraDaAlfa, umaHoraAtras)
                        .with(comoUsuario(outroMotoristaAlfa, alfa)))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("erro"));

        assertEquals(1, usoVeiculoRepository.count());
        assertEquals(motoristaAlfa.getId(), unicoUso().getUsuario().getId());
    }

    @Test
    @DisplayName("#006-RN003: veiculo inativo nao sai")
    void veiculoInativoNaoSai() throws Exception {

        caminhaoDaAlfa.setStatus(StatusVeiculo.INATIVO);
        veiculoRepository.save(caminhaoDaAlfa);

        mockMvc.perform(saida(caminhaoDaAlfa, obraDaAlfa, umaHoraAtras)
                        .with(comoUsuario(motoristaAlfa, alfa)))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("erro"));

        assertEquals(0, usoVeiculoRepository.count());
        assertEquals(StatusVeiculo.INATIVO, statusDe(caminhaoDaAlfa));
    }

    @Test
    @DisplayName("#005-RN004: centro de custo inativo nao recebe saida nova")
    void centroDeCustoInativoNaoRecebeSaida() throws Exception {

        obraDaAlfa.setAtivo(false);
        centroCustoRepository.save(obraDaAlfa);

        mockMvc.perform(saida(caminhaoDaAlfa, obraDaAlfa, umaHoraAtras)
                        .with(comoUsuario(motoristaAlfa, alfa)))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("erro"));

        assertEquals(0, usoVeiculoRepository.count());

        // Recusou ANTES de mexer no veiculo: ele nao pode ficar EM_USO sem
        // saida nenhuma.
        assertEquals(StatusVeiculo.DISPONIVEL, statusDe(caminhaoDaAlfa));
    }

    /* ==================================================================
       #006-RN002 - um veiculo em aberto por motorista
       ================================================================== */

    @Test
    @DisplayName("#006-RN002: quem ja esta com um veiculo em aberto nao registra outra saida")
    void motoristaComVeiculoEmAbertoNaoRegistraOutraSaida() throws Exception {

        mockMvc.perform(saida(caminhaoDaAlfa, obraDaAlfa, umaHoraAtras)
                        .with(comoUsuario(motoristaAlfa, alfa)))
                .andExpect(redirectedUrl("/lancamentos"));

        // Segunda saida, com OUTRO veiculo, que esta disponivel: o que barra
        // nao e o veiculo, e o motorista.
        mockMvc.perform(saida(picapeDaAlfa, obraDaAlfa, umaHoraAtras)
                        .with(comoUsuario(motoristaAlfa, alfa)))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("erro"));

        assertEquals(1, usoVeiculoRepository.count());
        assertEquals(StatusVeiculo.DISPONIVEL, statusDe(picapeDaAlfa),
                "a picape nao saiu, entao nao pode ter mudado de status");
    }

    @Test
    @DisplayName("#006-RN002: com veiculo em aberto, a tela de saida manda para a devolucao")
    void telaDeSaidaMandaParaADevolucao() throws Exception {

        mockMvc.perform(saida(caminhaoDaAlfa, obraDaAlfa, umaHoraAtras)
                .with(comoUsuario(motoristaAlfa, alfa)));

        mockMvc.perform(get("/saidas/nova").with(comoUsuario(motoristaAlfa, alfa)))
                .andExpect(redirectedUrl("/saidas/devolucao"))
                .andExpect(flash().attributeExists("erro"));
    }

    @Test
    @DisplayName("#006-RN002: a regra e por motorista - outro motorista sai com outro veiculo")
    void outroMotoristaSaiComOutroVeiculo() throws Exception {

        mockMvc.perform(saida(caminhaoDaAlfa, obraDaAlfa, umaHoraAtras)
                        .with(comoUsuario(motoristaAlfa, alfa)))
                .andExpect(redirectedUrl("/lancamentos"));

        mockMvc.perform(saida(picapeDaAlfa, obraDaAlfa, umaHoraAtras)
                        .with(comoUsuario(outroMotoristaAlfa, alfa)))
                .andExpect(redirectedUrl("/lancamentos"));

        assertEquals(2, usoVeiculoRepository.count());
    }

    @Test
    @DisplayName("#006-RN002: a tela inicial mostra o veiculo em aberto e o botao de devolucao")
    void telaInicialMostraOVeiculoEmAberto() throws Exception {

        // Sem veiculo em aberto: o caminho e a saida.
        mockMvc.perform(get("/lancamentos").with(comoUsuario(motoristaAlfa, alfa)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Registrar saída de veículo")))
                .andExpect(content().string(not(containsString("Registrar devolução"))));

        mockMvc.perform(saida(caminhaoDaAlfa, obraDaAlfa, umaHoraAtras)
                .with(comoUsuario(motoristaAlfa, alfa)));

        // Com veiculo em aberto: a tela diz qual e, e o caminho e a devolucao.
        mockMvc.perform(get("/lancamentos").with(comoUsuario(motoristaAlfa, alfa)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Caminhão da Alfa")))
                .andExpect(content().string(containsString("ABC-1D23")))
                .andExpect(content().string(containsString("Residencial Parque")))
                .andExpect(content().string(containsString("Registrar devolução")))
                .andExpect(content().string(not(containsString("Registrar saída de veículo"))));

        // O outro motorista continua vendo a tela de quem nao tem veiculo.
        mockMvc.perform(get("/lancamentos").with(comoUsuario(outroMotoristaAlfa, alfa)))
                .andExpect(content().string(not(containsString("Caminhão da Alfa"))));
    }

    /* ==================================================================
       #006-RN005 - data informada x data de registro (regra 4 do projeto)
       ================================================================== */

    @Test
    @DisplayName("#006-RN005: a saida aceita data retroativa e grava a data de registro a parte")
    void dataRetroativaEDataDeRegistro() throws Exception {

        LocalDateTime tresDiasAtras = LocalDateTime.now().minusDays(3)
                .truncatedTo(ChronoUnit.MINUTES);
        LocalDateTime antesDoEnvio = LocalDateTime.now();

        // "dataRegistro" enviado a mao: o SaidaForm nao tem esse campo, entao
        // o valor se perde.
        mockMvc.perform(saida(caminhaoDaAlfa, obraDaAlfa, tresDiasAtras)
                        .param("dataRegistro", "2000-01-01T00:00")
                        .with(comoUsuario(motoristaAlfa, alfa)))
                .andExpect(redirectedUrl("/lancamentos"));

        UsoVeiculo uso = unicoUso();

        // A data INFORMADA e a que o motorista digitou...
        assertEquals(tresDiasAtras, uso.getDataSaida());

        // ...e a data de REGISTRO e a hora em que o lancamento entrou no
        // sistema, preenchida pelo servidor.
        assertNotNull(uso.getDataRegistro());
        assertFalse(uso.getDataRegistro().isBefore(antesDoEnvio));
        assertFalse(uso.getDataRegistro().isAfter(LocalDateTime.now()));
    }

    @Test
    @DisplayName("#006-RN005: saida com data no futuro e recusada")
    void saidaNoFuturoERecusada() throws Exception {

        mockMvc.perform(saida(caminhaoDaAlfa, obraDaAlfa, LocalDateTime.now().plusDays(1))
                        .with(comoUsuario(motoristaAlfa, alfa)))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("erro"));

        assertEquals(0, usoVeiculoRepository.count());
        assertEquals(StatusVeiculo.DISPONIVEL, statusDe(caminhaoDaAlfa));
    }

    @Test
    @DisplayName("#006-RN001: data que nao e data volta com erro no campo, sem pagina de erro")
    void dataInvalidaVoltaComErroNoCampo() throws Exception {

        mockMvc.perform(post("/saidas")
                        .param("veiculoId", caminhaoDaAlfa.getId().toString())
                        .param("centroCustoId", obraDaAlfa.getId().toString())
                        .param("dataSaida", "ontem de manhã")
                        .with(csrf())
                        .with(comoUsuario(motoristaAlfa, alfa)))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("saidaForm", "dataSaida"))
                .andExpect(content().string(containsString("Informe uma data e uma hora válidas.")));
    }

    /* ==================================================================
       #006-RF02 - registro de devolucao
       ================================================================== */

    @Test
    @DisplayName("#006-RF02: o motorista registra a devolucao do veiculo que esta com ele")
    void registraDevolucao() throws Exception {

        mockMvc.perform(saida(caminhaoDaAlfa, obraDaAlfa, umaHoraAtras)
                .with(comoUsuario(motoristaAlfa, alfa)));

        LocalDateTime dataRegistroDaSaida = unicoUso().getDataRegistro();
        LocalDateTime meiaHoraAtras = umaHoraAtras.plusMinutes(30);

        mockMvc.perform(devolucao(meiaHoraAtras).with(comoUsuario(motoristaAlfa, alfa)))
                .andExpect(redirectedUrl("/lancamentos"))
                .andExpect(flash().attributeExists("mensagem"));

        UsoVeiculo uso = unicoUso();

        assertEquals(meiaHoraAtras, uso.getDataDevolucao());
        assertFalse(uso.isEmAberto());

        // A devolucao so preenche a data de devolucao: a mesma linha, com a
        // mesma saida e a mesma data de registro.
        assertEquals(1, usoVeiculoRepository.count());
        assertEquals(umaHoraAtras, uso.getDataSaida());
        assertEquals(dataRegistroDaSaida, uso.getDataRegistro());
    }

    @Test
    @DisplayName("#006-RF02: depois de devolver, o motorista pode registrar nova saida")
    void depoisDeDevolverPodeSairDeNovo() throws Exception {

        mockMvc.perform(saida(caminhaoDaAlfa, obraDaAlfa, umaHoraAtras)
                .with(comoUsuario(motoristaAlfa, alfa)));
        mockMvc.perform(devolucao(umaHoraAtras.plusMinutes(10))
                .with(comoUsuario(motoristaAlfa, alfa)));

        mockMvc.perform(saida(caminhaoDaAlfa, obraDaAlfa, umaHoraAtras.plusMinutes(20))
                        .with(comoUsuario(motoristaAlfa, alfa)))
                .andExpect(redirectedUrl("/lancamentos"));

        // Duas linhas: o historico da primeira saida continua la (regra 3).
        assertEquals(2, usoVeiculoRepository.count());
        assertEquals(StatusVeiculo.EM_USO, statusDe(caminhaoDaAlfa));
    }

    @Test
    @DisplayName("#006-RF02: devolucao anterior a saida e recusada")
    void devolucaoAntesDaSaidaERecusada() throws Exception {

        mockMvc.perform(saida(caminhaoDaAlfa, obraDaAlfa, umaHoraAtras)
                .with(comoUsuario(motoristaAlfa, alfa)));

        mockMvc.perform(devolucao(umaHoraAtras.minusMinutes(1))
                        .with(comoUsuario(motoristaAlfa, alfa)))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("erro"));

        assertTrue(unicoUso().isEmAberto());
        assertEquals(StatusVeiculo.EM_USO, statusDe(caminhaoDaAlfa));
    }

    @Test
    @DisplayName("#006-RF02: devolucao com data no futuro e recusada")
    void devolucaoNoFuturoERecusada() throws Exception {

        mockMvc.perform(saida(caminhaoDaAlfa, obraDaAlfa, umaHoraAtras)
                .with(comoUsuario(motoristaAlfa, alfa)));

        mockMvc.perform(devolucao(LocalDateTime.now().plusDays(1))
                        .with(comoUsuario(motoristaAlfa, alfa)))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("erro"));

        assertTrue(unicoUso().isEmAberto());
        assertEquals(StatusVeiculo.EM_USO, statusDe(caminhaoDaAlfa));
    }

    @Test
    @DisplayName("#006-RF02: sem veiculo em aberto nao ha o que devolver")
    void devolucaoSemVeiculoEmAberto() throws Exception {

        mockMvc.perform(get("/saidas/devolucao").with(comoUsuario(motoristaAlfa, alfa)))
                .andExpect(redirectedUrl("/lancamentos"))
                .andExpect(flash().attributeExists("erro"));

        mockMvc.perform(devolucao(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES))
                        .with(comoUsuario(motoristaAlfa, alfa)))
                .andExpect(redirectedUrl("/lancamentos"))
                .andExpect(flash().attributeExists("erro"));
    }

    @Test
    @DisplayName("#006-RF02: a devolucao de um motorista nao fecha a saida de outro")
    void devolucaoNaoFechaSaidaDeOutroMotorista() throws Exception {

        mockMvc.perform(saida(caminhaoDaAlfa, obraDaAlfa, umaHoraAtras)
                .with(comoUsuario(motoristaAlfa, alfa)));

        // O outro motorista nao tem veiculo nenhum. A devolucao nao leva id:
        // a saida fechada e sempre a de quem esta logado.
        mockMvc.perform(devolucao(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES))
                        .with(comoUsuario(outroMotoristaAlfa, alfa)))
                .andExpect(redirectedUrl("/lancamentos"))
                .andExpect(flash().attributeExists("erro"));

        assertTrue(unicoUso().isEmAberto());
        assertEquals(StatusVeiculo.EM_USO, statusDe(caminhaoDaAlfa));
    }

    /* ==================================================================
       Regra 1 do projeto - isolamento por empresa (#006-RN004)
       ================================================================== */

    @Test
    @DisplayName("#006-RN004: motorista nao registra saida com veiculo de outra empresa")
    void naoRegistraSaidaComVeiculoDeOutraEmpresa() throws Exception {

        mockMvc.perform(saida(caminhaoDaBeta, obraDaAlfa, umaHoraAtras)
                        .with(comoUsuario(motoristaAlfa, alfa)))
                .andExpect(status().isNotFound());

        assertEquals(0, usoVeiculoRepository.count());
        assertEquals(StatusVeiculo.DISPONIVEL, statusDe(caminhaoDaBeta),
                "o veiculo da Beta nao pode ter sido tocado");
    }

    @Test
    @DisplayName("#006-RN004: motorista nao registra saida para centro de custo de outra empresa")
    void naoRegistraSaidaParaCentroDeCustoDeOutraEmpresa() throws Exception {

        mockMvc.perform(saida(caminhaoDaAlfa, rotaDaBeta, umaHoraAtras)
                        .with(comoUsuario(motoristaAlfa, alfa)))
                .andExpect(status().isNotFound());

        assertEquals(0, usoVeiculoRepository.count());
        assertEquals(StatusVeiculo.DISPONIVEL, statusDe(caminhaoDaAlfa));
    }

    @Test
    @DisplayName("#006-RN004: a tela de saida so oferece veiculos e centros de custo da propria empresa")
    void telaDeSaidaSoOfereceDadosDaPropriaEmpresa() throws Exception {

        mockMvc.perform(get("/saidas/nova").with(comoUsuario(motoristaAlfa, alfa)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Caminhão da Alfa")))
                .andExpect(content().string(containsString("Residencial Parque")))
                .andExpect(content().string(not(containsString("Caminhão da Beta"))))
                .andExpect(content().string(not(containsString("Litoral Sul"))));
    }

    @Test
    @DisplayName("#006-RN004: a saida em aberto de uma empresa nao aparece para motorista de outra")
    void saidaEmAbertoNaoApareceParaOutraEmpresa() throws Exception {

        mockMvc.perform(saida(caminhaoDaAlfa, obraDaAlfa, umaHoraAtras)
                .with(comoUsuario(motoristaAlfa, alfa)));

        mockMvc.perform(get("/lancamentos").with(comoUsuario(motoristaBeta, beta)))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Caminhão da Alfa"))))
                .andExpect(content().string(containsString("Registrar saída de veículo")));
    }

    @Test
    @DisplayName("#006: a tela de saida so oferece veiculo disponivel e centro de custo ativo")
    void telaDeSaidaSoOfereceOQuePodeSerEscolhido() throws Exception {

        picapeDaAlfa.setStatus(StatusVeiculo.INATIVO);
        veiculoRepository.save(picapeDaAlfa);

        CentroCusto encerrada = centroCustoRepository.save(
                new CentroCusto(alfa, "Galpão Encerrado", "Campinas", "SP", null));
        encerrada.setAtivo(false);
        centroCustoRepository.save(encerrada);

        mockMvc.perform(get("/saidas/nova").with(comoUsuario(motoristaAlfa, alfa)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Caminhão da Alfa")))
                .andExpect(content().string(not(containsString("Picape da Alfa"))))
                .andExpect(content().string(not(containsString("Galpão Encerrado"))));
    }

    /* ==================================================================
       Quem entra nas telas (#006-RN004 e #002-RN008)
       ================================================================== */

    @Test
    @DisplayName("#006-RN004: Administrador nao registra saida nem devolucao")
    void administradorNaoAcessaTelas() throws Exception {

        mockMvc.perform(get("/saidas/nova").with(comoUsuario(adminAlfa, alfa)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/saidas/devolucao").with(comoUsuario(adminAlfa, alfa)))
                .andExpect(status().isForbidden());

        mockMvc.perform(saida(caminhaoDaAlfa, obraDaAlfa, umaHoraAtras)
                        .with(comoUsuario(adminAlfa, alfa)))
                .andExpect(status().isForbidden());

        assertEquals(0, usoVeiculoRepository.count());
    }

    @Test
    @DisplayName("#002-RN008: Operador leva 403 nas telas de saida e devolucao")
    void operadorNaoAcessaTelas() throws Exception {

        Empresa equipe = empresaRepository.save(new Empresa("Equipe Gestão Fácil",
                Empresa.IDENTIFICADOR_DA_EQUIPE, "Centro de custo", "Centros de custo"));
        Usuario operador = criarUsuario(equipe, "jose.lopes", "José Lopes", Perfil.OPERADOR);

        for (String tela : List.of("/lancamentos", "/saidas/nova", "/saidas/devolucao")) {
            mockMvc.perform(get(tela).with(comoUsuario(operador, equipe)))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    @DisplayName("Quem nao esta logado nao registra saida")
    void visitanteNaoRegistraSaida() throws Exception {

        mockMvc.perform(saida(caminhaoDaAlfa, obraDaAlfa, umaHoraAtras))
                .andExpect(status().is3xxRedirection());

        assertEquals(0, usoVeiculoRepository.count());
    }

    /* ==================================================================
       Regra 6 do projeto - o rotulo vem da empresa
       ================================================================== */

    @Test
    @DisplayName("Regra 6: a tela de saida da Alfa diz Obra e a da Beta diz Rota")
    void telaDeSaidaUsaORotuloDaEmpresa() throws Exception {

        mockMvc.perform(get("/saidas/nova").with(comoUsuario(motoristaAlfa, alfa)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(">Obra</label>")));

        // Se algum template tivesse "Obra" fixo - nem que fosse num
        // comentario HTML -, a ultima conferencia quebraria.
        mockMvc.perform(get("/saidas/nova").with(comoUsuario(motoristaBeta, beta)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(">Rota</label>")))
                .andExpect(content().string(not(containsString("Obra"))));
    }

    @Test
    @DisplayName("Regra 6: a tela inicial e a de devolucao tambem usam o rotulo da empresa")
    void telasComVeiculoEmAbertoUsamORotuloDaEmpresa() throws Exception {

        mockMvc.perform(saida(caminhaoDaBeta, rotaDaBeta, umaHoraAtras)
                        .with(comoUsuario(motoristaBeta, beta)))
                .andExpect(redirectedUrl("/lancamentos"));

        for (String tela : List.of("/lancamentos", "/saidas/devolucao")) {
            mockMvc.perform(get(tela).with(comoUsuario(motoristaBeta, beta)))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Rota:")))
                    .andExpect(content().string(containsString("Litoral Sul")))
                    .andExpect(content().string(not(containsString("Obra"))));
        }
    }

    /* ==================================================================
       #006-RN006 - sem lancamento offline
       ================================================================== */

    @Test
    @DisplayName("#006-RN006: as telas de saida e devolucao trazem o aviso de sem conexao")
    void telasTrazemOAvisoDeSemConexao() throws Exception {

        // O aviso em si e mostrado pelo JavaScript, no navegador. O que da
        // para conferir daqui e que a tela traz as tres pecas de que ele
        // precisa: a caixa, o formulario marcado e o script.
        mockMvc.perform(get("/saidas/nova").with(comoUsuario(motoristaAlfa, alfa)))
                .andExpect(content().string(containsString("id=\"aviso-sem-conexao\"")))
                .andExpect(content().string(containsString("data-exige-conexao")))
                .andExpect(content().string(containsString("/js/aviso-sem-conexao.js")));

        mockMvc.perform(saida(caminhaoDaAlfa, obraDaAlfa, umaHoraAtras)
                .with(comoUsuario(motoristaAlfa, alfa)));

        mockMvc.perform(get("/saidas/devolucao").with(comoUsuario(motoristaAlfa, alfa)))
                .andExpect(content().string(containsString("id=\"aviso-sem-conexao\"")))
                .andExpect(content().string(containsString("data-exige-conexao")))
                .andExpect(content().string(containsString("/js/aviso-sem-conexao.js")));

        // O script e um arquivo estatico, liberado no SecurityConfig.
        mockMvc.perform(get("/js/aviso-sem-conexao.js"))
                .andExpect(status().isOk());
    }

    /* ==================================================================
       #006-RN005 e RN008 - o que a devolucao do motorista grava
       ================================================================== */

    @Test
    @DisplayName("#006-RN005 e RN008: a devolucao do motorista grava quando foi lancada e por quem")
    void devolucaoDoMotoristaGravaRegistroEQuemDevolveu() throws Exception {

        mockMvc.perform(saida(caminhaoDaAlfa, obraDaAlfa, umaHoraAtras)
                .with(comoUsuario(motoristaAlfa, alfa)));

        // Em aberto, as duas colunas novas estao nulas.
        assertNull(unicoUso().getDataRegistroDevolucao());
        assertNull(unicoUso().getUsuarioDevolucao());

        LocalDateTime meiaHoraAtras = umaHoraAtras.plusMinutes(30);
        LocalDateTime antesDoEnvio = LocalDateTime.now();

        // "dataRegistroDevolucao" enviado a mao: o DevolucaoForm nao tem esse
        // campo, entao o valor se perde.
        mockMvc.perform(devolucao(meiaHoraAtras)
                        .param("dataRegistroDevolucao", "2000-01-01T00:00")
                        .with(comoUsuario(motoristaAlfa, alfa)))
                .andExpect(redirectedUrl("/lancamentos"));

        UsoVeiculo uso = unicoUso();

        // A data INFORMADA e a retroativa; a de REGISTRO e a hora do servidor.
        assertEquals(meiaHoraAtras, uso.getDataDevolucao());
        assertNotNull(uso.getDataRegistroDevolucao());
        assertFalse(uso.getDataRegistroDevolucao().isBefore(antesDoEnvio));
        assertFalse(uso.getDataRegistroDevolucao().isAfter(LocalDateTime.now()));

        assertEquals(motoristaAlfa.getId(), uso.getUsuarioDevolucao().getId());
    }

    /* ==================================================================
       #006-RF03 - painel de veiculos em uso (Administrador)
       ================================================================== */

    @Test
    @DisplayName("#006-RF03: o Administrador ve so as saidas em aberto da propria empresa")
    void painelMostraSoAsSaidasEmAbertoDaPropriaEmpresa() throws Exception {

        // Em aberto na Alfa: o caminhao.
        mockMvc.perform(saida(caminhaoDaAlfa, obraDaAlfa, umaHoraAtras)
                .with(comoUsuario(motoristaAlfa, alfa)));

        // Ja devolvida na Alfa: a picape.
        mockMvc.perform(saida(picapeDaAlfa, obraDaAlfa, umaHoraAtras)
                .with(comoUsuario(outroMotoristaAlfa, alfa)));
        mockMvc.perform(devolucao(umaHoraAtras.plusMinutes(10))
                .with(comoUsuario(outroMotoristaAlfa, alfa)));

        // Em aberto na Beta.
        mockMvc.perform(saida(caminhaoDaBeta, rotaDaBeta, umaHoraAtras)
                .with(comoUsuario(motoristaBeta, beta)));

        mockMvc.perform(get("/veiculos-em-uso").with(comoUsuario(adminAlfa, alfa)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Caminhão da Alfa")))
                .andExpect(content().string(containsString("ABC-1D23")))
                .andExpect(content().string(containsString("Motorista da Alfa")))
                .andExpect(content().string(containsString("Residencial Parque")))
                .andExpect(content().string(not(containsString("Picape da Alfa"))))
                .andExpect(content().string(not(containsString("Caminhão da Beta"))))
                .andExpect(content().string(not(containsString("Litoral Sul"))));
    }

    @Test
    @DisplayName("#006-RF03: o painel avisa quando o motorista esta desativado")
    void painelAvisaMotoristaDesativado() throws Exception {

        mockMvc.perform(saida(caminhaoDaAlfa, obraDaAlfa, umaHoraAtras)
                .with(comoUsuario(motoristaAlfa, alfa)));

        mockMvc.perform(get("/veiculos-em-uso").with(comoUsuario(adminAlfa, alfa)))
                .andExpect(content().string(not(containsString("Motorista desativado"))));

        motoristaAlfa.setAtivo(false);
        usuarioRepository.save(motoristaAlfa);

        mockMvc.perform(get("/veiculos-em-uso").with(comoUsuario(adminAlfa, alfa)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Caminhão da Alfa")))
                .andExpect(content().string(containsString("Motorista desativado")));
    }

    @Test
    @DisplayName("Regra 6: a coluna de destino do painel usa o rotulo da empresa")
    void painelUsaORotuloDaEmpresa() throws Exception {

        Usuario adminBeta = criarUsuario(beta, "chefe.beta", "Chefe da Beta",
                Perfil.ADMINISTRADOR);

        mockMvc.perform(saida(caminhaoDaBeta, rotaDaBeta, umaHoraAtras)
                .with(comoUsuario(motoristaBeta, beta)));

        mockMvc.perform(get("/veiculos-em-uso").with(comoUsuario(adminBeta, beta)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(">Rota</th>")))
                .andExpect(content().string(not(containsString("Obra"))));

        // A tela de confirmacao do encerramento tambem.
        mockMvc.perform(get("/veiculos-em-uso/" + unicoUso().getId() + "/encerrar")
                        .with(comoUsuario(adminBeta, beta)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Rota:")))
                .andExpect(content().string(containsString("Litoral Sul")))
                .andExpect(content().string(not(containsString("Obra"))));
    }

    @Test
    @DisplayName("#006-RF03: o painel do Administrador tem o botao para os veiculos em uso")
    void painelDoAdministradorTemOBotao() throws Exception {

        mockMvc.perform(get("/painel").with(comoUsuario(adminAlfa, alfa)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/veiculos-em-uso\"")));
    }

    /* ==================================================================
       #006-RF04 - encerramento de saida pelo Administrador
       ================================================================== */

    @Test
    @DisplayName("#006-RF04: o Administrador encerra a saida, o veiculo fica DISPONIVEL e fica registrado quem encerrou")
    void administradorEncerraSaida() throws Exception {

        mockMvc.perform(saida(caminhaoDaAlfa, obraDaAlfa, umaHoraAtras)
                .with(comoUsuario(motoristaAlfa, alfa)));

        LocalDateTime meiaHoraAtras = umaHoraAtras.plusMinutes(30);
        LocalDateTime antesDoEnvio = LocalDateTime.now();

        mockMvc.perform(encerramento(unicoUso(), meiaHoraAtras)
                        .with(comoUsuario(adminAlfa, alfa)))
                .andExpect(redirectedUrl("/veiculos-em-uso"))
                .andExpect(flash().attributeExists("mensagem"));

        UsoVeiculo uso = unicoUso();

        assertFalse(uso.isEmAberto());
        assertEquals(meiaHoraAtras, uso.getDataDevolucao());
        assertEquals(StatusVeiculo.DISPONIVEL, statusDe(caminhaoDaAlfa));

        // #006-RN008: quem lancou a devolucao foi o Administrador...
        assertEquals(adminAlfa.getId(), uso.getUsuarioDevolucao().getId());

        // ...e o motorista da saida continua sendo o motorista.
        assertEquals(motoristaAlfa.getId(), uso.getUsuario().getId());

        // #006-RN005: o momento do lancamento, pelo relogio do servidor.
        assertNotNull(uso.getDataRegistroDevolucao());
        assertFalse(uso.getDataRegistroDevolucao().isBefore(antesDoEnvio));

        // Encerrada, a saida some do painel.
        mockMvc.perform(get("/veiculos-em-uso").with(comoUsuario(adminAlfa, alfa)))
                .andExpect(content().string(not(containsString("Caminhão da Alfa"))));
    }

    @Test
    @DisplayName("#006-RF04: o encerramento funciona com o motorista desativado")
    void encerraSaidaDeMotoristaDesativado() throws Exception {

        mockMvc.perform(saida(caminhaoDaAlfa, obraDaAlfa, umaHoraAtras)
                .with(comoUsuario(motoristaAlfa, alfa)));

        // O caso do veiculo preso: o motorista saiu da empresa sem devolver.
        motoristaAlfa.setAtivo(false);
        usuarioRepository.save(motoristaAlfa);

        mockMvc.perform(encerramento(unicoUso(), umaHoraAtras.plusMinutes(30))
                        .with(comoUsuario(adminAlfa, alfa)))
                .andExpect(redirectedUrl("/veiculos-em-uso"))
                .andExpect(flash().attributeExists("mensagem"));

        assertFalse(unicoUso().isEmAberto());
        assertEquals(StatusVeiculo.DISPONIVEL, statusDe(caminhaoDaAlfa));
    }

    @Test
    @DisplayName("#006-RN007: o encerramento recusa data no futuro e data anterior a saida")
    void encerramentoRecusaDataInvalida() throws Exception {

        mockMvc.perform(saida(caminhaoDaAlfa, obraDaAlfa, umaHoraAtras)
                .with(comoUsuario(motoristaAlfa, alfa)));

        List<LocalDateTime> datasRecusadas = List.of(
                LocalDateTime.now().plusDays(1),
                umaHoraAtras.minusMinutes(1));

        for (LocalDateTime data : datasRecusadas) {
            // A recusa aparece na propria tela de confirmacao, que continua
            // mostrando qual saida esta sendo encerrada.
            mockMvc.perform(encerramento(unicoUso(), data)
                            .with(comoUsuario(adminAlfa, alfa)))
                    .andExpect(status().isOk())
                    .andExpect(model().attributeExists("erro"))
                    .andExpect(content().string(containsString("Caminhão da Alfa")));

            // Recusou sem gravar nada: a saida continua em aberto.
            assertTrue(unicoUso().isEmAberto());
            assertNull(unicoUso().getUsuarioDevolucao());
            assertEquals(StatusVeiculo.EM_USO, statusDe(caminhaoDaAlfa));
        }
    }

    @Test
    @DisplayName("#006-RN001: encerramento sem data volta com erro, sem pagina de erro")
    void encerramentoSemDataERecusado() throws Exception {

        mockMvc.perform(saida(caminhaoDaAlfa, obraDaAlfa, umaHoraAtras)
                .with(comoUsuario(motoristaAlfa, alfa)));

        mockMvc.perform(post("/veiculos-em-uso/" + unicoUso().getId() + "/encerrar")
                        .with(csrf())
                        .with(comoUsuario(adminAlfa, alfa)))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("devolucaoForm", "dataDevolucao"));

        assertTrue(unicoUso().isEmAberto());
    }

    @Test
    @DisplayName("#006-RF04: a lista leva para a tela de confirmacao, que mostra a saida e a data de agora")
    void telaDeConfirmacaoDoEncerramento() throws Exception {

        mockMvc.perform(saida(caminhaoDaAlfa, obraDaAlfa, umaHoraAtras)
                .with(comoUsuario(motoristaAlfa, alfa)));

        String endereco = "/veiculos-em-uso/" + unicoUso().getId() + "/encerrar";

        // Na lista, "Encerrar saida" e um link para a confirmacao: o campo
        // de data e o envio ficam so na tela seguinte.
        mockMvc.perform(get("/veiculos-em-uso").with(comoUsuario(adminAlfa, alfa)))
                .andExpect(content().string(containsString("href=\"" + endereco + "\"")))
                .andExpect(content().string(not(containsString("name=\"dataDevolucao\""))));

        String hoje = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        mockMvc.perform(get(endereco).with(comoUsuario(adminAlfa, alfa)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Caminhão da Alfa")))
                .andExpect(content().string(containsString("ABC-1D23")))
                .andExpect(content().string(containsString("Motorista da Alfa")))
                .andExpect(content().string(containsString(
                        umaHoraAtras.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))))
                .andExpect(content().string(containsString("action=\"" + endereco + "\"")))
                .andExpect(content().string(containsString("value=\"" + hoje + "T")));

        // Abrir a tela nao fecha nada.
        assertTrue(unicoUso().isEmAberto());
        assertEquals(StatusVeiculo.EM_USO, statusDe(caminhaoDaAlfa));
    }

    @Test
    @DisplayName("#006-RN009: a tela de confirmacao de saida de outra empresa responde 404")
    void telaDeConfirmacaoDeOutraEmpresaResponde404() throws Exception {

        mockMvc.perform(saida(caminhaoDaBeta, rotaDaBeta, umaHoraAtras)
                .with(comoUsuario(motoristaBeta, beta)));

        mockMvc.perform(get("/veiculos-em-uso/" + unicoUso().getId() + "/encerrar")
                        .with(comoUsuario(adminAlfa, alfa)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("#006-RN009: Administrador de uma empresa leva 404 ao encerrar saida de outra")
    void administradorNaoEncerraSaidaDeOutraEmpresa() throws Exception {

        mockMvc.perform(saida(caminhaoDaBeta, rotaDaBeta, umaHoraAtras)
                .with(comoUsuario(motoristaBeta, beta)));

        mockMvc.perform(encerramento(unicoUso(), umaHoraAtras.plusMinutes(30))
                        .with(comoUsuario(adminAlfa, alfa)))
                .andExpect(status().isNotFound());

        assertTrue(unicoUso().isEmAberto());
        assertEquals(StatusVeiculo.EM_USO, statusDe(caminhaoDaBeta),
                "o veiculo da Beta nao pode ter sido tocado");
    }

    @Test
    @DisplayName("#006-RF04: encerrar uma saida que ja foi fechada responde 404")
    void encerrarSaidaJaFechadaResponde404() throws Exception {

        mockMvc.perform(saida(caminhaoDaAlfa, obraDaAlfa, umaHoraAtras)
                .with(comoUsuario(motoristaAlfa, alfa)));

        LocalDateTime devolvidoEm = umaHoraAtras.plusMinutes(10);
        mockMvc.perform(devolucao(devolvidoEm).with(comoUsuario(motoristaAlfa, alfa)));

        mockMvc.perform(encerramento(unicoUso(), umaHoraAtras.plusMinutes(30))
                        .with(comoUsuario(adminAlfa, alfa)))
                .andExpect(status().isNotFound());

        // A tela de confirmacao da saida fechada tambem nao abre.
        mockMvc.perform(get("/veiculos-em-uso/" + unicoUso().getId() + "/encerrar")
                        .with(comoUsuario(adminAlfa, alfa)))
                .andExpect(status().isNotFound());

        // A devolucao do motorista continua como ele a lancou.
        assertEquals(devolvidoEm, unicoUso().getDataDevolucao());
        assertEquals(motoristaAlfa.getId(), unicoUso().getUsuarioDevolucao().getId());

        // Saida que nunca existiu: 404 tambem.
        mockMvc.perform(post("/veiculos-em-uso/999999/encerrar")
                        .param("dataDevolucao", umaHoraAtras.format(FORMATO_DO_CAMPO))
                        .with(csrf())
                        .with(comoUsuario(adminAlfa, alfa)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("#006-RN009: Motorista leva 403 no painel e no encerramento")
    void motoristaNaoAcessaPainelNemEncerra() throws Exception {

        mockMvc.perform(saida(caminhaoDaAlfa, obraDaAlfa, umaHoraAtras)
                .with(comoUsuario(motoristaAlfa, alfa)));

        mockMvc.perform(get("/veiculos-em-uso").with(comoUsuario(motoristaAlfa, alfa)))
                .andExpect(status().isForbidden());

        // Nem a propria saida ele encerra por esta porta.
        mockMvc.perform(get("/veiculos-em-uso/" + unicoUso().getId() + "/encerrar")
                        .with(comoUsuario(motoristaAlfa, alfa)))
                .andExpect(status().isForbidden());
        mockMvc.perform(encerramento(unicoUso(), umaHoraAtras.plusMinutes(30))
                        .with(comoUsuario(motoristaAlfa, alfa)))
                .andExpect(status().isForbidden());

        assertTrue(unicoUso().isEmAberto());
    }

    @Test
    @DisplayName("#002-RN008: Operador leva 403 no painel e no encerramento")
    void operadorNaoAcessaPainelNemEncerra() throws Exception {

        Empresa equipe = empresaRepository.save(new Empresa("Equipe Gestão Fácil",
                Empresa.IDENTIFICADOR_DA_EQUIPE, "Centro de custo", "Centros de custo"));
        Usuario operador = criarUsuario(equipe, "jose.lopes", "José Lopes", Perfil.OPERADOR);

        mockMvc.perform(saida(caminhaoDaAlfa, obraDaAlfa, umaHoraAtras)
                .with(comoUsuario(motoristaAlfa, alfa)));

        mockMvc.perform(get("/veiculos-em-uso").with(comoUsuario(operador, equipe)))
                .andExpect(status().isForbidden());

        mockMvc.perform(encerramento(unicoUso(), umaHoraAtras.plusMinutes(30))
                        .with(comoUsuario(operador, equipe)))
                .andExpect(status().isForbidden());

        assertTrue(unicoUso().isEmAberto());
    }

    /* ==================================================================
       Auxiliares
       ================================================================== */

    /**
     * O POST de encerramento pelo Administrador. Ao contrario da devolucao do
     * motorista, este leva o id da saida no endereco.
     */
    private MockHttpServletRequestBuilder encerramento(UsoVeiculo uso,
                                                       LocalDateTime dataDevolucao) {
        return post("/veiculos-em-uso/" + uso.getId() + "/encerrar")
                .param("dataDevolucao", dataDevolucao.format(FORMATO_DO_CAMPO))
                .with(csrf());
    }

    /**
     * Coloca na requisicao o mesmo UsuarioAutenticado que o login de verdade
     * produziria.
     */
    private RequestPostProcessor comoUsuario(Usuario usuario, Empresa empresa) {
        return user(new UsuarioAutenticado(usuario, empresa));
    }

    /**
     * O POST de saida com os tres campos obrigatorios.
     *
     * ATENCAO a armadilha do .param repetido: quem quiser variar um destes
     * tres campos deve passar outro valor AQUI, e nao acrescentar um
     * .param com o mesmo nome depois - o Spring ficaria com o primeiro.
     */
    private MockHttpServletRequestBuilder saida(Veiculo veiculo, CentroCusto centroCusto,
                                                LocalDateTime dataSaida) {
        return post("/saidas")
                .param("veiculoId", veiculo.getId().toString())
                .param("centroCustoId", centroCusto.getId().toString())
                .param("dataSaida", dataSaida.format(FORMATO_DO_CAMPO))
                .with(csrf());
    }

    /** O POST de devolucao. Repare: nao leva id de saida nem de veiculo. */
    private MockHttpServletRequestBuilder devolucao(LocalDateTime dataDevolucao) {
        return post("/saidas/devolucao")
                .param("dataDevolucao", dataDevolucao.format(FORMATO_DO_CAMPO))
                .with(csrf());
    }

    /** O unico lancamento gravado - e a prova de que e um so. */
    private UsoVeiculo unicoUso() {
        List<UsoVeiculo> usos = usoVeiculoRepository.findAll();
        assertEquals(1, usos.size());
        return usos.get(0);
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
