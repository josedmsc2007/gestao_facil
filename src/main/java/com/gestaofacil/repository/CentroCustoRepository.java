package com.gestaofacil.repository;

import com.gestaofacil.model.CentroCusto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio da tabela "centro_custo".
 *
 * ATENCAO - regra 1 do projeto (isolamento por empresa): TODO metodo daqui
 * leva o id da empresa. Nao existe "listar todos os centros de custo": duas
 * empresas podem ter uma obra com o mesmo nome, e nenhuma delas pode ver a
 * da outra (#005-RN003). O filtro por empresa mora na consulta, nao na tela.
 */
public interface CentroCustoRepository extends JpaRepository<CentroCusto, Long> {

    /**
     * Lista TODOS os centros de custo da empresa, ativos e inativos
     * (#005-RF02 e RF03).
     *
     * O inativo continua na lista porque e de la que o Administrador o
     * reativa - nada e apagado (regra 3), entao "inativo" nao quer dizer
     * "sumiu da tela". Mesma decisao das listas de usuarios e de veiculos.
     */
    List<CentroCusto> findByEmpresaIdOrderByNome(Long empresaId);

    /**
     * So os ATIVOS, em ordem alfabetica.
     *
     * E o metodo que a saida de veiculo (#006) vai usar para montar a caixa
     * de selecao: uma obra encerrada nao pode receber lancamento novo, mas
     * continua existindo nos antigos. Repare que o "ativo" vai DENTRO da
     * consulta - os inativos nem saem do banco.
     */
    List<CentroCusto> findByEmpresaIdAndAtivoTrueOrderByNome(Long empresaId);

    /**
     * Busca um centro de custo pelo id CONFERINDO A EMPRESA - regra 1 do
     * projeto.
     *
     * E este metodo que impede alguem de abrir /centros-de-custo/57 de outra
     * empresa: se o 57 nao for da empresa de quem esta logado, volta Optional
     * vazio e o controller responde 404.
     */
    Optional<CentroCusto> findByIdAndEmpresaId(Long id, Long empresaId);
}
