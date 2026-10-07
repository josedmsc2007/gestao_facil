package com.gestaofacil.repository;

import com.gestaofacil.model.UsoVeiculo;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio da tabela "uso_veiculo".
 *
 * ATENCAO - regra 1 do projeto (isolamento por empresa): TODO metodo daqui
 * leva o id da empresa, mesmo os que ja levam o id do motorista. O motorista
 * so pertence a uma empresa, entao o filtro parece sobrar - mas e ele que
 * mantem a regra valendo sem ninguem precisar raciocinar caso a caso.
 *
 * "DataDevolucaoIsNull" e como o Spring escreve "where data_devolucao is
 * null": a saida em aberto.
 */
public interface UsoVeiculoRepository extends JpaRepository<UsoVeiculo, Long> {

    /**
     * #006-RN002: o motorista ja esta com um veiculo em aberto?
     *
     * "exists" traz so sim ou nao, sem carregar o lancamento - e e tudo de
     * que a regra precisa.
     */
    boolean existsByUsuarioIdAndEmpresaIdAndDataDevolucaoIsNull(Long usuarioId, Long empresaId);

    /**
     * A saida em aberto do motorista, para a tela mostrar e para a devolucao
     * fechar. Pela RN002 existe no maximo uma, por isso o Optional.
     *
     * POR QUE O @EntityGraph
     * veiculo e centroCusto sao LAZY: o Hibernate so iria busca-los quando a
     * tela pedisse o nome do veiculo. So que o projeto usa open-in-view=false,
     * entao nessa hora a conexao com o banco ja foi fechada e a tela quebraria
     * com LazyInitializationException. O @EntityGraph manda trazer os dois
     * JUNTO, na mesma consulta, so neste metodo - que e o unico cujo
     * resultado vai para uma tela.
     */
    @EntityGraph(attributePaths = {"veiculo", "centroCusto"})
    Optional<UsoVeiculo> findByUsuarioIdAndEmpresaIdAndDataDevolucaoIsNull(Long usuarioId,
                                                                          Long empresaId);

    /**
     * #006-RF03: todas as saidas em aberto da empresa, para o painel do
     * Administrador. A mais antiga vem primeiro: e a que mais merece atencao.
     *
     * O @EntityGraph traz tambem o "usuario", porque o painel mostra o nome do
     * motorista e se ele esta desativado.
     */
    @EntityGraph(attributePaths = {"veiculo", "centroCusto", "usuario"})
    List<UsoVeiculo> findByEmpresaIdAndDataDevolucaoIsNullOrderByDataSaida(Long empresaId);

    /**
     * #006-RF04: a saida que o Administrador quer encerrar, CONFERINDO A
     * EMPRESA (regra 1). E a unica busca por id desta tabela, porque a rota do
     * encerramento e a unica que leva o id da saida.
     */
    @EntityGraph(attributePaths = {"veiculo", "centroCusto", "usuario"})
    Optional<UsoVeiculo> findByIdAndEmpresaId(Long id, Long empresaId);
}
