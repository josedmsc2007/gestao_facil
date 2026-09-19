package com.gestaofacil.repository;

import com.gestaofacil.model.Empresa;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio = a classe que conversa com a tabela "empresa".
 *
 * Repare que e uma interface e nao tem nenhum codigo dentro: quem escreve a
 * implementacao e o Spring Data JPA, em tempo de execucao. Herdando de
 * JpaRepository<Empresa, Long> (entidade, tipo da chave primaria) ja vem
 * pronto: save, findById, findAll, count, delete...
 */
public interface EmpresaRepository extends JpaRepository<Empresa, Long> {

    /**
     * Busca a empresa pelo identificador do endereco (ex.: "construtora-teste").
     *
     * Nao escrevemos SQL: o Spring le o NOME do metodo - findBy + Identificador -
     * e monta sozinho "select * from empresa where identificador = ?".
     *
     * Devolve Optional porque pode nao existir empresa com aquele endereco;
     * o Optional obriga quem chama a tratar esse caso em vez de receber null.
     */
    Optional<Empresa> findByIdentificador(String identificador);

    /**
     * Usado pela carga inicial para saber se uma empresa ja existe, e pelo
     * cadastro de empresa para recusar identificador repetido (#002-RN002).
     */
    boolean existsByIdentificador(String identificador);

    /**
     * Lista as empresas para o Operador (#002-RF07), deixando de fora a
     * empresa reservada da equipe (#002-RN010): quem chama passa
     * Empresa.IDENTIFICADOR_DA_EQUIPE, e "IdentificadorNot" vira
     * "where identificador <> ?".
     *
     * O @EntityGraph manda o Hibernate trazer, NA MESMA CONSULTA, o Operador
     * que cadastrou cada empresa. Sem ele, a tela tentaria ler
     * empresa.cadastradaPor.nome depois de a consulta terminar e estouraria
     * LazyInitializationException - o projeto usa open-in-view=false.
     */
    @EntityGraph(attributePaths = "cadastradaPor")
    List<Empresa> findByIdentificadorNotOrderByNome(String identificador);

    /**
     * Busca uma empresa cliente pelo id - nunca a reservada da equipe.
     *
     * E o equivalente, nas telas do Operador, ao findByIdAndEmpresaId dos
     * outros cadastros: um id que aponte para a empresa da equipe volta vazio,
     * e a tela responde 404. Sem isso, /empresas/{id da equipe}/editar
     * deixaria um Operador inativar a empresa de todos os Operadores.
     */
    Optional<Empresa> findByIdAndIdentificadorNot(Long id, String identificador);
}
