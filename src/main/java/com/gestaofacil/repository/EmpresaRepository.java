package com.gestaofacil.repository;

import com.gestaofacil.model.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;

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

    /** Usado pela carga inicial para saber se a empresa de teste ja existe. */
    boolean existsByIdentificador(String identificador);
}
