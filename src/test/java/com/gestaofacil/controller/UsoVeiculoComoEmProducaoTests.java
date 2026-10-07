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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O caminho inteiro do card #006 - saida, telas e devolucao - com o banco
 * se comportando COMO EM PRODUCAO.
 *
 * O QUE ESTE TESTE PEGA E OS OUTROS NAO
 * Os demais testes sao @Transactional: cada um roda dentro de uma transacao
 * que fica aberta ate o fim. Isso e otimo para limpar os dados, mas esconde
 * um erro classico: a tela pedir um dado LAZY (o nome do veiculo de uma
 * saida, por exemplo) depois que a consulta ja terminou. Em producao o
 * projeto usa open-in-view=false, a conexao ja esta fechada nessa hora, e a
 * tela quebra com LazyInitializationException. Dentro da transacao do teste
 * a conexao continua aberta, e o erro nunca aparece.
 *
 * Por isso esta classe:
 *   - NAO e @Transactional: cada requisicao abre e fecha a sua transacao;
 *   - liga open-in-view=false, que o application.properties dos testes nao
 *     traz (ele substitui o principal);
 *   - usa um banco proprio, como o CargaInicialTests: sem a transacao, nada
 *     e desfeito no fim, e os dados daqui sobrariam para os outros testes.
 */
@SpringBootTest(properties = {
        "spring.jpa.open-in-view=false",
        "spring.datasource.url=jdbc:h2:mem:uso-veiculo-producao;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
class UsoVeiculoComoEmProducaoTests {

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

    @Test
    @DisplayName("#006: saida, telas e devolucao funcionam sem transacao aberta (open-in-view=false)")
    void caminhoInteiroSemTransacaoAberta() throws Exception {

        Empresa empresa = empresaRepository.save(
                new Empresa("Construtora Alfa", "construtora-alfa", "Obra", "Obras"));
        Usuario motorista = usuarioRepository.save(new Usuario(empresa, "Motorista da Alfa",
                "motorista.alfa", passwordEncoder.encode("senha-de-teste-123"),
                Perfil.MOTORISTA));
        Veiculo veiculo = veiculoRepository.save(new Veiculo(empresa, "Caminhão da Alfa", 2020,
                "00123456789", "ABC1D23", TipoCombustivel.DIESEL_S10));
        CentroCusto centroCusto = centroCustoRepository.save(
                new CentroCusto(empresa, "Residencial Parque", "Campinas", "SP", null));

        RequestPostProcessor comoMotorista = user(new UsuarioAutenticado(motorista, empresa));
        LocalDateTime umaHoraAtras = LocalDateTime.now().minusHours(1);

        // A tela de saida abre, com as duas caixas de selecao.
        mockMvc.perform(get("/saidas/nova").with(comoMotorista))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Caminhão da Alfa")))
                .andExpect(content().string(containsString("Residencial Parque")));

        mockMvc.perform(post("/saidas")
                        .param("veiculoId", veiculo.getId().toString())
                        .param("centroCustoId", centroCusto.getId().toString())
                        .param("dataSaida", umaHoraAtras.format(FORMATO_DO_CAMPO))
                        .with(csrf())
                        .with(comoMotorista))
                .andExpect(redirectedUrl("/lancamentos"));

        assertEquals(StatusVeiculo.EM_USO,
                veiculoRepository.findById(veiculo.getId()).orElseThrow().getStatus());

        // As duas telas que mostram o veiculo e o destino de uma saida ja
        // gravada: e aqui que o dado LAZY quebraria.
        mockMvc.perform(get("/lancamentos").with(comoMotorista))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Caminhão da Alfa")))
                .andExpect(content().string(containsString("Residencial Parque")));

        mockMvc.perform(get("/saidas/devolucao").with(comoMotorista))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Caminhão da Alfa")))
                .andExpect(content().string(containsString("Residencial Parque")));

        // Uma devolucao recusada redesenha a tela com os mesmos dados.
        mockMvc.perform(post("/saidas/devolucao")
                        .param("dataDevolucao", umaHoraAtras.minusDays(1).format(FORMATO_DO_CAMPO))
                        .with(csrf())
                        .with(comoMotorista))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Caminhão da Alfa")));

        mockMvc.perform(post("/saidas/devolucao")
                        .param("dataDevolucao", LocalDateTime.now().format(FORMATO_DO_CAMPO))
                        .with(csrf())
                        .with(comoMotorista))
                .andExpect(redirectedUrl("/lancamentos"));

        assertEquals(StatusVeiculo.DISPONIVEL,
                veiculoRepository.findById(veiculo.getId()).orElseThrow().getStatus());
    }

    /**
     * O painel do Administrador mostra TRES relacionamentos de cada saida:
     * veiculo, centro de custo e motorista.
     *
     * Os dados daqui tem outros nomes porque esta classe nao desfaz nada no
     * fim: a empresa do teste acima continua no banco, e o identificador e
     * unico.
     */
    @Test
    @DisplayName("#006-RF03 e RF04: painel e encerramento funcionam sem transacao aberta (open-in-view=false)")
    void painelEEncerramentoSemTransacaoAberta() throws Exception {

        Empresa empresa = empresaRepository.save(
                new Empresa("Construtora Gama", "construtora-gama", "Obra", "Obras"));
        Usuario motorista = usuarioRepository.save(new Usuario(empresa, "Motorista da Gama",
                "motorista.gama", passwordEncoder.encode("senha-de-teste-123"),
                Perfil.MOTORISTA));
        Usuario administrador = usuarioRepository.save(new Usuario(empresa, "Chefe da Gama",
                "chefe.gama", passwordEncoder.encode("senha-de-teste-123"),
                Perfil.ADMINISTRADOR));
        Veiculo veiculo = veiculoRepository.save(new Veiculo(empresa, "Caminhão da Gama", 2020,
                "00123456789", "ABC1D23", TipoCombustivel.DIESEL_S10));
        CentroCusto centroCusto = centroCustoRepository.save(
                new CentroCusto(empresa, "Edifício Central", "Campinas", "SP", null));

        RequestPostProcessor comoMotorista = user(new UsuarioAutenticado(motorista, empresa));
        RequestPostProcessor comoAdministrador =
                user(new UsuarioAutenticado(administrador, empresa));
        LocalDateTime umaHoraAtras = LocalDateTime.now().minusHours(1);

        mockMvc.perform(post("/saidas")
                        .param("veiculoId", veiculo.getId().toString())
                        .param("centroCustoId", centroCusto.getId().toString())
                        .param("dataSaida", umaHoraAtras.format(FORMATO_DO_CAMPO))
                        .with(csrf())
                        .with(comoMotorista))
                .andExpect(redirectedUrl("/lancamentos"));

        // O motorista e desativado com o veiculo em aberto.
        motorista.setAtivo(false);
        usuarioRepository.save(motorista);

        // E aqui que o dado LAZY quebraria.
        mockMvc.perform(get("/veiculos-em-uso").with(comoAdministrador))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Caminhão da Gama")))
                .andExpect(content().string(containsString("Motorista da Gama")))
                .andExpect(content().string(containsString("Edifício Central")))
                .andExpect(content().string(containsString("Motorista desativado")));

        UsoVeiculo uso = usoVeiculoRepository
                .findByEmpresaIdAndDataDevolucaoIsNullOrderByDataSaida(empresa.getId())
                .get(0);

        // A tela de confirmacao mostra os mesmos relacionamentos...
        mockMvc.perform(get("/veiculos-em-uso/" + uso.getId() + "/encerrar")
                        .with(comoAdministrador))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Caminhão da Gama")))
                .andExpect(content().string(containsString("Motorista da Gama")))
                .andExpect(content().string(containsString("Edifício Central")));

        // ...e continua mostrando quando o encerramento e recusado.
        mockMvc.perform(post("/veiculos-em-uso/" + uso.getId() + "/encerrar")
                        .param("dataDevolucao", umaHoraAtras.minusDays(1).format(FORMATO_DO_CAMPO))
                        .with(csrf())
                        .with(comoAdministrador))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Motorista da Gama")));

        mockMvc.perform(post("/veiculos-em-uso/" + uso.getId() + "/encerrar")
                        .param("dataDevolucao", LocalDateTime.now().format(FORMATO_DO_CAMPO))
                        .with(csrf())
                        .with(comoAdministrador))
                .andExpect(redirectedUrl("/veiculos-em-uso"));

        assertEquals(StatusVeiculo.DISPONIVEL,
                veiculoRepository.findById(veiculo.getId()).orElseThrow().getStatus());
    }
}
