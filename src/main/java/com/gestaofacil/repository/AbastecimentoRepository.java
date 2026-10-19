package com.gestaofacil.repository;

import com.gestaofacil.model.Abastecimento;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio da tabela "abastecimento".
 *
 * Por enquanto so grava (o save ja vem do JpaRepository): o #007 nao tem
 * tela que liste abastecimentos. Quando o historico e os relatorios
 * chegarem, as consultas novas entram aqui - e, pela regra 1, TODAS levando
 * o id da empresa, como no UsoVeiculoRepository.
 */
public interface AbastecimentoRepository extends JpaRepository<Abastecimento, Long> {
}
