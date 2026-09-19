package com.gestaofacil.repository;

import com.gestaofacil.model.Perfil;
import com.gestaofacil.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio da tabela "usuario".
 *
 * ATENCAO - regra 1 do projeto (isolamento por empresa): TODO metodo daqui
 * recebe o id da empresa. Nao existe "buscar usuario pelo login" sozinho,
 * porque dois usuarios de empresas diferentes podem ter o mesmo login (RN007)
 * e porque credenciais de uma empresa nao podem autenticar em outra (RN008).
 * O filtro por empresa mora na consulta, nao na tela.
 */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /**
     * Busca o usuario pelo login DENTRO de uma empresa.
     *
     * "EmpresaId" navega pelo relacionamento: o Spring entende como
     * "usuario.empresa.id" e gera o join / o filtro por empresa_id.
     * E o metodo usado pela tela de login.
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

    /**
     * Lista TODOS os usuarios da empresa, ativos e inativos (#003-RF08).
     *
     * A tela de usuarios precisa mostrar os inativos tambem: e de la que o
     * Administrador reativa alguem que voltou para a empresa. Nada e apagado
     * no sistema (regra 3 do projeto), entao "inativo" nao quer dizer
     * "sumiu da lista".
     */
    List<Usuario> findByEmpresaIdOrderByNome(Long empresaId);

    /** Verifica se o login ja existe naquela empresa, antes de cadastrar. */
    boolean existsByEmpresaIdAndLogin(Long empresaId, String login);

    /**
     * O mesmo, para a EDICAO: ignora o proprio usuario que esta sendo editado
     * (IdNot = "id diferente de").
     *
     * Sem isto, salvar a edicao sem mexer no login acusaria login duplicado -
     * o registro encontrado seria ele mesmo.
     */
    boolean existsByEmpresaIdAndLoginAndIdNot(Long empresaId, String login, Long id);

    /** #003-RN004: o CPF e unico dentro da empresa. Usado no cadastro. */
    boolean existsByEmpresaIdAndCpf(Long empresaId, String cpf);

    /** #003-RN004 na edicao: ignora o proprio usuario, pelo mesmo motivo do login. */
    boolean existsByEmpresaIdAndCpfAndIdNot(Long empresaId, String cpf, Long id);

    /**
     * Quantos administradores ATIVOS a empresa tem agora (#003-RN015).
     *
     * E a consulta que sustenta a regra dos dois administradores: antes de
     * desativar ou rebaixar um administrador, o sistema pergunta quantos
     * sobrariam. "count" traz so o numero, sem carregar os usuarios - e o
     * numero e tudo de que a regra precisa.
     */
    long countByEmpresaIdAndPerfilAndAtivoTrue(Long empresaId, Perfil perfil);

    /**
     * Os usuarios ativos de um perfil so, dentro de uma empresa.
     *
     * Usado pelo Operador para ver os administradores de uma empresa cliente
     * (#002-RF08). Repare que o perfil vai NA CONSULTA: os motoristas nem
     * saem do banco, porque o Operador nao pode ve-los (#002-RN008).
     */
    List<Usuario> findByEmpresaIdAndPerfilAndAtivoTrueOrderByNome(Long empresaId, Perfil perfil);

    /**
     * Usuarios da empresa com bloqueio ATIVO agora (RN005).
     *
     * "BloqueadoAteAfter" vira "where bloqueado_ate > ?". Passando o momento
     * atual, ficam de fora os bloqueios que ja venceram sozinhos - eles nao
     * precisam mais da acao do Administrador.
     */
    List<Usuario> findByEmpresaIdAndBloqueadoAteAfterOrderByNome(Long empresaId,
                                                                 LocalDateTime momento);
}
