package com.gestaofacil.service;

import com.gestaofacil.controller.form.DevolucaoForm;
import com.gestaofacil.controller.form.SaidaForm;
import com.gestaofacil.model.CentroCusto;
import com.gestaofacil.model.UsoVeiculo;
import com.gestaofacil.model.Usuario;
import com.gestaofacil.model.Veiculo;
import com.gestaofacil.repository.CentroCustoRepository;
import com.gestaofacil.repository.UsoVeiculoRepository;
import com.gestaofacil.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Regras de negocio da saida e da devolucao de veiculos (card #006).
 *
 * O controller cuida da tela; o service cuida da REGRA. Toda recusa sai
 * daqui como LancamentoRecusadoException, com a mensagem pronta para o
 * motorista ler.
 *
 * DOIS PADROES QUE SE REPETEM EM TODOS OS METODOS
 *
 * 1) Motorista e empresa chegam por PARAMETRO, tirados da sessao pelo
 *    controller, e toda busca leva a empresa junto (regra 1 e #006-RN004).
 *
 * 2) CONFERE TUDO PRIMEIRO, GRAVA DEPOIS. Nenhuma linha grava nada antes de
 *    a ultima conferencia passar. Assim uma recusa nunca deixa o trabalho
 *    pela metade - por exemplo, o veiculo EM_USO sem saida nenhuma.
 */
@Service
public class UsoVeiculoService {

    /** Como as datas aparecem nas mensagens: 01/10/2026 14:30. */
    private static final DateTimeFormatter DATA_E_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final UsoVeiculoRepository usoVeiculoRepository;
    private final UsuarioRepository usuarioRepository;
    private final CentroCustoRepository centroCustoRepository;
    private final VeiculoService veiculoService;

    public UsoVeiculoService(UsoVeiculoRepository usoVeiculoRepository,
                             UsuarioRepository usuarioRepository,
                             CentroCustoRepository centroCustoRepository,
                             VeiculoService veiculoService) {
        this.usoVeiculoRepository = usoVeiculoRepository;
        this.usuarioRepository = usuarioRepository;
        this.centroCustoRepository = centroCustoRepository;
        this.veiculoService = veiculoService;
    }

    /**
     * Registra a saida de um veiculo com o motorista logado (#006-RF01).
     *
     * @Transactional: a saida gravada e o status do veiculo mudam JUNTOS ou
     * nao mudam. Se algo falhar no meio, o banco desfaz tudo - nunca fica um
     * veiculo EM_USO sem saida, nem uma saida de veiculo DISPONIVEL.
     *
     * @throws LancamentoRecusadoException quando uma regra recusa a saida
     */
    @Transactional
    public UsoVeiculo registrarSaida(SaidaForm form, Long usuarioId, Long empresaId) {

        Usuario motorista = usuarioRepository.findByIdAndEmpresaId(usuarioId, empresaId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));

        // #006-RN004: o destino precisa ser da empresa do motorista.
        CentroCusto centroCusto = centroCustoRepository
                .findByIdAndEmpresaId(form.getCentroCustoId(), empresaId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Centro de custo não encontrado na empresa do usuário logado."));

        // #006-RN002: um veiculo por vez.
        if (usoVeiculoRepository.existsByUsuarioIdAndEmpresaIdAndDataDevolucaoIsNull(
                usuarioId, empresaId)) {
            throw new LancamentoRecusadoException(
                    "Você já está com um veículo em aberto. Registre a devolução dele"
                            + " antes de registrar uma nova saída.");
        }

        // O centro de custo encerrado nao recebe lancamento novo (#005-RN004).
        // A tela so oferece os ativos; isto cobre o caso de ele ser desativado
        // com a tela ja aberta. A mensagem nao usa o rotulo da empresa para
        // nao precisar de genero (regra 6).
        if (!centroCusto.isAtivo()) {
            throw new LancamentoRecusadoException(
                    "O destino " + centroCusto.getNome() + " foi desativado e não recebe"
                            + " novos lançamentos. Escolha outro destino.");
        }

        // #006-RN005: retroativa pode. O que nao pode e uma saida que ainda
        // nao aconteceu.
        if (form.getDataSaida().isAfter(LocalDateTime.now())) {
            throw new LancamentoRecusadoException(
                    "A data e a hora da saída não podem estar no futuro. Confira o"
                            + " que foi informado.");
        }

        // #006-RN003: a ULTIMA conferencia e a primeira gravacao. O
        // VeiculoService confere a empresa e se o veiculo esta DISPONIVEL, e
        // so entao o passa para EM_USO.
        Veiculo veiculo = veiculoService.marcarComoEmUso(form.getVeiculoId(), empresaId);

        // A data de registro (#006-RN005) nao aparece aqui: quem a preenche
        // e o construtor do UsoVeiculo, com a hora de agora.
        UsoVeiculo uso = new UsoVeiculo(
                motorista.getEmpresa(),
                veiculo,
                centroCusto,
                motorista,
                form.getDataSaida(),
                form.getObservacao());

        return usoVeiculoRepository.save(uso);
    }

    /**
     * Registra a devolucao do veiculo que esta com o motorista logado
     * (#006-RF02).
     *
     * NAO RECEBE ID DE SAIDA NENHUM. A saida fechada e a que esta em aberto
     * para este motorista nesta empresa - pela RN002 so existe uma. Assim nao
     * ha como um motorista devolver, por engano ou de proposito, o veiculo
     * que esta com outro.
     *
     * @throws LancamentoRecusadoException quando uma regra recusa a devolucao
     */
    @Transactional
    public UsoVeiculo registrarDevolucao(DevolucaoForm form, Long usuarioId, Long empresaId) {

        UsoVeiculo uso = usoVeiculoRepository
                .findByUsuarioIdAndEmpresaIdAndDataDevolucaoIsNull(usuarioId, empresaId)
                .orElseThrow(() -> new LancamentoRecusadoException(
                        "Você não tem veículo em aberto para devolver."));

        // #006-RN008: quem lancou a devolucao foi o proprio motorista. A saida
        // foi achada pelo id dele, entao uso.getUsuario() e quem esta logado.
        return fecharSaida(uso, form.getDataDevolucao(), uso.getUsuario(), empresaId);
    }

    /**
     * O Administrador encerra uma saida em aberto no lugar do motorista
     * (#006-RF04).
     *
     * AQUI O ID DA SAIDA VEM DA TELA, ao contrario da devolucao do motorista.
     * Por isso a busca leva a empresa junto (regra 1 e #006-RN009): a saida de
     * outra empresa simplesmente nao e encontrada.
     *
     * NAO CONFERE SE O MOTORISTA ESTA ATIVO, de proposito: o caso que este
     * metodo existe para resolver e justamente o do motorista desativado com
     * o veiculo em aberto.
     *
     * @throws LancamentoRecusadoException quando uma regra recusa o encerramento
     */
    @Transactional
    public UsoVeiculo encerrarSaida(Long usoId, DevolucaoForm form,
                                    Long administradorId, Long empresaId) {

        Usuario administrador = usuarioRepository
                .findByIdAndEmpresaId(administradorId, empresaId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));

        UsoVeiculo uso = usoVeiculoRepository.findByIdAndEmpresaId(usoId, empresaId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Saída não encontrada na empresa do usuário logado."));

        // O controller ja respondeu 404 para a saida fechada. Isto cobre o
        // instante entre a conferencia dele e esta: o motorista pode ter
        // devolvido nesse meio-tempo.
        if (!uso.isEmAberto()) {
            throw new LancamentoRecusadoException(
                    "Esta saída já foi encerrada. O veículo já tinha sido devolvido.");
        }

        // #006-RN008: fica registrado que foi o Administrador.
        return fecharSaida(uso, form.getDataDevolucao(), administrador, empresaId);
    }

    /**
     * O que a devolucao do motorista e o encerramento pelo Administrador tem
     * em comum: as conferencias de data (#006-RN007), a gravacao e a volta do
     * veiculo para DISPONIVEL (#006-RN003).
     *
     * Estar num lugar so garante que as duas portas seguem as MESMAS regras.
     * Continua valendo "confere tudo primeiro, grava depois".
     */
    private UsoVeiculo fecharSaida(UsoVeiculo uso, LocalDateTime dataDevolucao,
                                   Usuario quemDevolveu, Long empresaId) {

        // O veiculo nao pode voltar antes de ter saido.
        if (dataDevolucao.isBefore(uso.getDataSaida())) {
            throw new LancamentoRecusadoException(
                    "A devolução não pode ser anterior à saída, registrada em "
                            + uso.getDataSaida().format(DATA_E_HORA) + ".");
        }

        if (dataDevolucao.isAfter(LocalDateTime.now())) {
            throw new LancamentoRecusadoException(
                    "A data e a hora da devolução não podem estar no futuro. Confira o"
                            + " que foi informado.");
        }

        // Os tres campos da devolucao de uma vez: a data informada, quem
        // lancou e o momento do lancamento (#006-RN005 e RN008).
        uso.registrarDevolucao(dataDevolucao, quemDevolveu);
        usoVeiculoRepository.save(uso);

        // #006-RN003: devolvido, o veiculo volta para DISPONIVEL.
        veiculoService.marcarComoDisponivel(uso.getVeiculo().getId(), empresaId);

        return uso;
    }
}
