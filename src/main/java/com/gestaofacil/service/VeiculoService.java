package com.gestaofacil.service;

import com.gestaofacil.controller.form.VeiculoForm;
import com.gestaofacil.model.Empresa;
import com.gestaofacil.model.StatusVeiculo;
import com.gestaofacil.model.Veiculo;
import com.gestaofacil.repository.EmpresaRepository;
import com.gestaofacil.repository.VeiculoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regras de negocio do veiculo (card #004).
 *
 * O controller cuida da tela; o service cuida da REGRA. E daqui que os
 * proximos cards (#006 saida/devolucao e #008 manutencao) vao mexer no status
 * do veiculo, sem passar por formulario nenhum.
 *
 * O PADRAO QUE SE REPETE EM TODO METODO DESTA CLASSE
 * Todos recebem o empresaId de quem esta logado e buscam com
 * findByIdAndEmpresaId. Nenhum metodo aqui aceita "so o id" - e a regra 1 do
 * projeto escrita em codigo.
 */
@Service
public class VeiculoService {

    private final VeiculoRepository veiculoRepository;
    private final EmpresaRepository empresaRepository;

    public VeiculoService(VeiculoRepository veiculoRepository,
                          EmpresaRepository empresaRepository) {
        this.veiculoRepository = veiculoRepository;
        this.empresaRepository = empresaRepository;
    }

    /**
     * Cadastra um veiculo na frota da empresa do Administrador logado
     * (#004-RF01).
     *
     * A EMPRESA NAO VEM DO FORMULARIO: vem do parametro empresaId, que o
     * controller tira da sessao. O VeiculoForm nem tem campo de empresa.
     *
     * O STATUS TAMBEM NAO VEM DO FORMULARIO (#004-RN003): o veiculo nasce
     * DISPONIVEL porque e esse o valor inicial do campo na entidade. Nao ha
     * uma linha aqui dizendo setStatus(DISPONIVEL) - se houvesse, alguem
     * poderia trocar esta linha por form.getStatus() um dia. Nao existe o que
     * trocar.
     */
    @Transactional
    public Veiculo cadastrar(VeiculoForm form, Long empresaId) {

        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new IllegalArgumentException("Empresa não encontrada."));

        Veiculo veiculo = new Veiculo(
                empresa,
                form.getNome(),
                form.getAno(),
                FormatoDeVeiculo.renavamNormalizado(form.getRenavam()),
                FormatoDeVeiculo.placaNormalizada(form.getPlaca()),
                form.getTipoCombustivel());

        return veiculoRepository.save(veiculo);
    }

    /**
     * Altera os dados cadastrais de um veiculo (#004-RF02).
     *
     * NAO MEXE NO STATUS. Mudar de situacao tem acao propria - o botao
     * Desativar / Ativar, aqui embaixo - e, nos proximos cards, a saida e a
     * manutencao. Uma tela que altera tudo de uma vez e uma tela em que
     * ninguem consegue dizer o que aconteceu.
     */
    @Transactional
    public void editar(Long veiculoId, Long empresaId, VeiculoForm form) {

        Veiculo veiculo = buscarNaEmpresa(veiculoId, empresaId);

        veiculo.setNome(form.getNome());
        veiculo.setAno(form.getAno());
        veiculo.setRenavam(FormatoDeVeiculo.renavamNormalizado(form.getRenavam()));
        veiculo.setPlaca(FormatoDeVeiculo.placaNormalizada(form.getPlaca()));
        veiculo.setTipoCombustivel(form.getTipoCombustivel());

        veiculoRepository.save(veiculo);
    }

    /**
     * Tira o veiculo de circulacao (#004-RF03 e RN005).
     *
     * NADA E APAGADO - regra 3 do projeto. O veiculo vendido ou baixado
     * continua na tabela, com status INATIVO: as saidas, os abastecimentos e
     * as manutencoes antigas seguem apontando para um veiculo que existe, e
     * os relatorios continuam fechando. Um delete de verdade quebraria o
     * historico da empresa.
     *
     * Quem confere SE o veiculo pode ser desativado e o controller, que tem a
     * mensagem para explicar a recusa na tela (ver podeSerDesativado).
     */
    @Transactional
    public void desativar(Long veiculoId, Long empresaId) {
        Veiculo veiculo = buscarNaEmpresa(veiculoId, empresaId);
        veiculo.setStatus(StatusVeiculo.INATIVO);
        veiculoRepository.save(veiculo);
    }

    /**
     * Devolve o veiculo a circulacao (#004-RF03).
     *
     * Ele volta como DISPONIVEL, e nao com o status que tinha antes de ser
     * desativado. O motivo e a regra 2: o status descreve a situacao de
     * AGORA. Um veiculo inativo nao esta com motorista nenhum e nao tem
     * manutencao correndo, entao "disponivel" e a unica resposta verdadeira.
     */
    @Transactional
    public void ativar(Long veiculoId, Long empresaId) {
        Veiculo veiculo = buscarNaEmpresa(veiculoId, empresaId);
        veiculo.setStatus(StatusVeiculo.DISPONIVEL);
        veiculoRepository.save(veiculo);
    }

    /**
     * O veiculo pode ser desativado agora?
     *
     * So quando ele esta DISPONIVEL ou ja INATIVO. Um veiculo EM_USO esta na
     * rua com um motorista, e um EM_MANUTENCAO esta na oficina: desativar
     * qualquer um dos dois apagaria um status CALCULADO (regra 2) e
     * escondido da frota um veiculo que o sistema ainda espera de volta.
     * Pior: quando o motorista registrasse a devolucao (#006), o veiculo
     * "inativo" voltaria sozinho para DISPONIVEL, e ninguem entenderia por
     * que ele reapareceu.
     *
     * O caminho certo, nesses casos, e registrar a devolucao ou encerrar a
     * manutencao primeiro - e e isso que a mensagem da tela explica.
     *
     * ISTO NAO ESTA ESCRITO NO CARD #004: foi uma decisao da implementacao,
     * para a regra 2 do projeto continuar valendo quando os cards #006 e #008
     * chegarem. Vale confirmar com o PO.
     */
    public boolean podeSerDesativado(Veiculo veiculo) {
        return veiculo.getStatus() == StatusVeiculo.DISPONIVEL
                || veiculo.getStatus() == StatusVeiculo.INATIVO;
    }

    /* ==================================================================
       Status calculado pela saida e pela devolucao (#006-RN003)

       Os dois metodos abaixo sao chamados pelo UsoVeiculoService, DENTRO da
       transacao dele. Nenhum controller os chama, e nenhum formulario tem
       campo de status: e a regra 2 do projeto.
       ================================================================== */

    /**
     * O veiculo saiu com um motorista: DISPONIVEL vira EM_USO.
     *
     * So sai veiculo DISPONIVEL. Um EM_USO ja esta na rua, um EM_MANUTENCAO
     * esta na oficina e um INATIVO saiu da frota. A tela de saida so oferece
     * os disponiveis, mas entre abrir a tela e tocar no botao outro motorista
     * pode ter levado o mesmo veiculo - por isso a conferencia e aqui, com a
     * linha travada (ver findParaMudarStatusByIdAndEmpresaId).
     *
     * @throws LancamentoRecusadoException quando o veiculo nao esta disponivel
     */
    @Transactional
    public Veiculo marcarComoEmUso(Long veiculoId, Long empresaId) {

        Veiculo veiculo = buscarParaMudarStatus(veiculoId, empresaId);

        if (veiculo.getStatus() != StatusVeiculo.DISPONIVEL) {
            throw new LancamentoRecusadoException(
                    "O veículo " + veiculo.getNome() + " está "
                            + veiculo.getStatus().getRotulo().toLowerCase()
                            + " e não pode sair agora. Escolha outro veículo.");
        }

        veiculo.setStatus(StatusVeiculo.EM_USO);
        return veiculoRepository.save(veiculo);
    }

    /**
     * O motorista devolveu o veiculo: EM_USO volta para DISPONIVEL.
     *
     * So mexe em quem esta EM_USO. Hoje nao ha como o veiculo estar em outro
     * status com uma saida em aberto (desativar um veiculo em uso e
     * recusado), mas quando a manutencao (#008) chegar pode haver: e uma
     * devolucao nao pode tirar da oficina um veiculo EM_MANUTENCAO.
     */
    @Transactional
    public void marcarComoDisponivel(Long veiculoId, Long empresaId) {

        Veiculo veiculo = buscarParaMudarStatus(veiculoId, empresaId);

        if (veiculo.getStatus() == StatusVeiculo.EM_USO) {
            veiculo.setStatus(StatusVeiculo.DISPONIVEL);
            veiculoRepository.save(veiculo);
        }
    }

    /** Como o buscarNaEmpresa, mas travando a linha ate o fim da transacao. */
    private Veiculo buscarParaMudarStatus(Long veiculoId, Long empresaId) {
        return veiculoRepository.findParaMudarStatusByIdAndEmpresaId(veiculoId, empresaId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Veículo não encontrado na empresa do usuário logado."));
    }

    /**
     * A busca que TODO metodo desta classe usa (regra 1 do projeto).
     *
     * Nunca findById(id) sozinho: a consulta leva sempre a empresa junto. Um
     * id que nao seja daquela empresa simplesmente nao e encontrado.
     */
    private Veiculo buscarNaEmpresa(Long veiculoId, Long empresaId) {
        return veiculoRepository.findByIdAndEmpresaId(veiculoId, empresaId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Veículo não encontrado na empresa do usuário logado."));
    }
}
