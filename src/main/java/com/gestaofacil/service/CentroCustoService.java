package com.gestaofacil.service;

import com.gestaofacil.controller.form.CentroCustoForm;
import com.gestaofacil.model.CentroCusto;
import com.gestaofacil.model.Empresa;
import com.gestaofacil.repository.CentroCustoRepository;
import com.gestaofacil.repository.EmpresaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regras de negocio do centro de custo (card #005).
 *
 * O controller cuida da tela; o service cuida da REGRA.
 *
 * O PADRAO QUE SE REPETE EM TODO METODO DESTA CLASSE
 * Todos recebem o empresaId de quem esta logado e buscam com
 * findByIdAndEmpresaId. Nenhum metodo aqui aceita "so o id" - e a regra 1 do
 * projeto escrita em codigo: mesmo que um id chegue forjado pela URL, a
 * consulta so encontra registro da empresa de quem esta logado.
 */
@Service
public class CentroCustoService {

    private final CentroCustoRepository centroCustoRepository;
    private final EmpresaRepository empresaRepository;

    public CentroCustoService(CentroCustoRepository centroCustoRepository,
                              EmpresaRepository empresaRepository) {
        this.centroCustoRepository = centroCustoRepository;
        this.empresaRepository = empresaRepository;
    }

    /**
     * Cadastra um centro de custo na empresa do Administrador logado
     * (#005-RF01 e RN003).
     *
     * A EMPRESA NAO VEM DO FORMULARIO: vem do parametro empresaId, que o
     * controller tira da sessao. O CentroCustoForm nem tem campo de empresa,
     * entao o vinculo automatico da RN003 nao e uma promessa da tela e sim
     * uma impossibilidade tecnica.
     *
     * Ele nasce ATIVO - e o valor inicial do campo na entidade. Nao ha uma
     * linha setAtivo(true) aqui: se houvesse, alguem poderia troca-la por
     * form.isAtivo() um dia. Nao existe o que trocar.
     */
    @Transactional
    public CentroCusto cadastrar(CentroCustoForm form, Long empresaId) {

        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new IllegalArgumentException("Empresa não encontrada."));

        CentroCusto centroCusto = new CentroCusto(
                empresa,
                form.getNome(),
                form.getCidade(),
                UnidadesFederativas.normalizada(form.getEstado()),
                form.getEndereco());

        return centroCustoRepository.save(centroCusto);
    }

    /**
     * Altera os dados de um centro de custo (#005-RF02).
     *
     * NAO MEXE NO "ativo": desativar e reativar tem acao propria, com botao
     * proprio. Uma tela que altera tudo de uma vez e uma tela em que ninguem
     * consegue dizer o que aconteceu.
     */
    @Transactional
    public void editar(Long centroCustoId, Long empresaId, CentroCustoForm form) {

        CentroCusto centroCusto = buscarNaEmpresa(centroCustoId, empresaId);

        centroCusto.setNome(form.getNome());
        centroCusto.setCidade(form.getCidade());
        centroCusto.setEstado(UnidadesFederativas.normalizada(form.getEstado()));
        centroCusto.setEndereco(form.getEndereco());

        centroCustoRepository.save(centroCusto);
    }

    /**
     * Encerra um centro de custo (#005-RF03 e RN004).
     *
     * NADA E APAGADO - regra 3 do projeto. A obra que terminou continua na
     * tabela, com ativo = false: ela deixa de aparecer na tela de saida de
     * veiculo (#006), mas os usos, abastecimentos e manutencoes ja lancados
     * nela seguem apontando para um registro que existe, e os relatorios
     * daquele periodo continuam fechando. Um delete de verdade levaria junto
     * o historico de meses de trabalho.
     */
    @Transactional
    public void desativar(Long centroCustoId, Long empresaId) {
        CentroCusto centroCusto = buscarNaEmpresa(centroCustoId, empresaId);
        centroCusto.setAtivo(false);
        centroCustoRepository.save(centroCusto);
    }

    /**
     * Devolve o centro de custo as telas de lancamento (#005-RF03).
     *
     * Nao tem regra nenhuma para conferir: reativar so aumenta o numero de
     * opcoes disponiveis.
     */
    @Transactional
    public void ativar(Long centroCustoId, Long empresaId) {
        CentroCusto centroCusto = buscarNaEmpresa(centroCustoId, empresaId);
        centroCusto.setAtivo(true);
        centroCustoRepository.save(centroCusto);
    }

    /**
     * A busca que TODO metodo desta classe usa (regra 1 do projeto).
     *
     * Nunca findById(id) sozinho: a consulta leva sempre a empresa junto. Um
     * id que nao seja daquela empresa simplesmente nao e encontrado.
     */
    private CentroCusto buscarNaEmpresa(Long centroCustoId, Long empresaId) {
        return centroCustoRepository.findByIdAndEmpresaId(centroCustoId, empresaId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Centro de custo não encontrado na empresa do usuário logado."));
    }
}
