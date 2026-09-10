package com.gestaofacil.repository;

import com.gestaofacil.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio da tabela "usuario".
 *
 * ATENCAO - regra 1 do projeto (isolamento por empresa): praticamente todo
 * metodo daqui recebe o id da empresa. Nao existe "buscar usuario pelo login"
 * sozinho, porque dois usuarios de empresas diferentes podem ter o mesmo
 * login (RN007) e porque credenciais de uma empresa nao podem autenticar em
 * outra (RN008). O filtro por empresa mora na consulta, nao na tela.
 */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /**
     * Busca o usuario pelo login DENTRO de uma empresa.
     *
     * "EmpresaId" navega pelo relacionamento: o Spring entende como
     * "usuario.empresa.id" e gera o join / o filtro por empresa_id.
     * Sera o metodo usado pela tela de login (proxima tarefa).
     */
    Optional<Usuario> findByEmpresaIdAndLogin(Long empresaId, String login);

    /**
     * Mesma busca, mas so entre os usuarios ativos (RN009: usuario inativo
     * nao autentica).
     */
    Optional<Usuario> findByEmpresaIdAndLoginAndAtivoTrue(Long empresaId, String login);

    /**
     * Busca um usuario pelo id conferindo a empresa - regra 1 do projeto.
     * E este o metodo que impede alguem de abrir /usuarios/57 de outra empresa:
     * se o 57 nao for da empresa do usuario logado, volta Optional vazio.
     */
    Optional<Usuario> findByIdAndEmpresaId(Long id, Long empresaId);

    /** Lista os usuarios ativos de uma empresa, em ordem alfabetica. */
    List<Usuario> findByEmpresaIdAndAtivoTrueOrderByNome(Long empresaId);

    /** Verifica se o login ja existe naquela empresa, antes de cadastrar. */
    boolean existsByEmpresaIdAndLogin(Long empresaId, String login);
}
