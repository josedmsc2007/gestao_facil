package com.gestaofacil.repository;

import com.gestaofacil.model.StatusVeiculo;
import com.gestaofacil.model.Veiculo;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio da tabela "veiculo".
 *
 * ATENCAO - regra 1 do projeto (isolamento por empresa): TODO metodo daqui
 * leva o id da empresa. Nao existe "buscar veiculo pela placa" sozinho, nem
 * "listar todos os veiculos": duas empresas podem ter a mesma placa
 * e nenhuma delas pode ver a frota da outra. O filtro por
 * empresa mora na consulta, nao na tela.
 */
public interface VeiculoRepository extends JpaRepository<Veiculo, Long> {

    /**
     * Lista TODOS os veiculos da empresa, inclusive os inativos.
     *
     * O inativo continua na lista porque e de la que o Administrador o
     * reativa - nada e apagado, entao "inativo" nao quer dizer
     * "sumiu da tela". Mesma decisao da lista de usuarios.
     */
    List<Veiculo> findByEmpresaIdOrderByNome(Long empresaId);

    /**
     * Busca um veiculo pelo id CONFERINDO A EMPRESA.
     *
     * E este metodo que impede alguem de abrir /veiculos/57 de outra empresa:
     * se o 57 nao for da empresa de quem esta logado, volta Optional vazio e
     * o controller responde 404.
     */
    Optional<Veiculo> findByIdAndEmpresaId(Long id, Long empresaId);

    /**
     * A mesma busca, mas TRAVANDO a linha do veiculo ate o fim da transacao
     * (no banco vira "select ... for update"). Usada so por quem vai mudar o
     * status na saida e na devolucao.
     *
     * O problema que a trava resolve: dois motoristas escolhem o mesmo
     * caminhao no mesmo segundo (ou um so toca duas vezes no botao). Sem ela
     * os dois pedidos leriam DISPONIVEL e os dois gravariam a saida. Com ela
     * o segundo ESPERA o primeiro terminar, le EM_USO e e recusado.
     *
     * O texto entre "find" e "By" e livre: o Spring o ignora, e ele serve so
     * para dar nome ao metodo.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Veiculo> findParaMudarStatusByIdAndEmpresaId(Long id, Long empresaId);

    /**
     * Os veiculos da empresa num status so.
     *
     * Vai servir aos proximos cards - a tela de saida precisa oferecer
     * apenas os DISPONIVEIS. Repare que o status vai DENTRO da consulta: os
     * outros veiculos nem saem do banco.
     */
    List<Veiculo> findByEmpresaIdAndStatusOrderByNome(Long empresaId, StatusVeiculo status);

    /** a placa ja existe nesta empresa? Usado no cadastro. */
    boolean existsByEmpresaIdAndPlaca(Long empresaId, String placa);

    boolean existsByEmpresaIdAndPlacaAndIdNot(Long empresaId, String placa, Long id);

    /** agora para o RENAVAM. */
    boolean existsByEmpresaIdAndRenavam(Long empresaId, String renavam);

    /** na edicao, pelo mesmo motivo da placa. */
    boolean existsByEmpresaIdAndRenavamAndIdNot(Long empresaId, String renavam, Long id);
}
