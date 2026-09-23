package com.gestaofacil.repository;

import com.gestaofacil.model.StatusVeiculo;
import com.gestaofacil.model.Veiculo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio da tabela "veiculo".
 *
 * ATENCAO - regra 1 do projeto (isolamento por empresa): TODO metodo daqui
 * leva o id da empresa. Nao existe "buscar veiculo pela placa" sozinho, nem
 * "listar todos os veiculos": duas empresas podem ter a mesma placa
 * (#004-RN001) e nenhuma delas pode ver a frota da outra. O filtro por
 * empresa mora na consulta, nao na tela.
 */
public interface VeiculoRepository extends JpaRepository<Veiculo, Long> {

    /**
     * Lista TODOS os veiculos da empresa, inclusive os inativos
     * (#004-RF02 e RF03).
     *
     * O inativo continua na lista porque e de la que o Administrador o
     * reativa - nada e apagado (regra 3), entao "inativo" nao quer dizer
     * "sumiu da tela". Mesma decisao da lista de usuarios.
     */
    List<Veiculo> findByEmpresaIdOrderByNome(Long empresaId);

    /**
     * Busca um veiculo pelo id CONFERINDO A EMPRESA - regra 1 do projeto.
     *
     * E este metodo que impede alguem de abrir /veiculos/57 de outra empresa:
     * se o 57 nao for da empresa de quem esta logado, volta Optional vazio e
     * o controller responde 404.
     */
    Optional<Veiculo> findByIdAndEmpresaId(Long id, Long empresaId);

    /**
     * Os veiculos da empresa num status so, em ordem alfabetica.
     *
     * Vai servir aos proximos cards - a tela de saida (#006) precisa oferecer
     * apenas os DISPONIVEIS. Repare que o status vai DENTRO da consulta: os
     * outros veiculos nem saem do banco.
     */
    List<Veiculo> findByEmpresaIdAndStatusOrderByNome(Long empresaId, StatusVeiculo status);

    /** #004-RN001: a placa ja existe nesta empresa? Usado no cadastro. */
    boolean existsByEmpresaIdAndPlaca(Long empresaId, String placa);

    /**
     * O mesmo, para a EDICAO: ignora o proprio veiculo que esta sendo editado
     * (IdNot = "id diferente de").
     *
     * Sem isto, salvar a edicao sem mexer na placa acusaria placa duplicada -
     * o registro encontrado seria ele mesmo.
     */
    boolean existsByEmpresaIdAndPlacaAndIdNot(Long empresaId, String placa, Long id);

    /** #004-RN001, agora para o RENAVAM. */
    boolean existsByEmpresaIdAndRenavam(Long empresaId, String renavam);

    /** #004-RN001 na edicao, pelo mesmo motivo da placa. */
    boolean existsByEmpresaIdAndRenavamAndIdNot(Long empresaId, String renavam, Long id);
}
