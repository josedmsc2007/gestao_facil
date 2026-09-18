package com.gestaofacil.repository;

import com.gestaofacil.model.AnexoUsuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio da tabela "anexo_usuario".
 *
 * Como em todo repositorio do projeto, nenhum metodo aqui aceita "so o id":
 * a empresa entra em toda consulta (regra 1). E o que impede o administrador
 * de uma construtora de baixar o documento do funcionario de outra, mesmo que
 * descubra o numero do anexo.
 */
public interface AnexoUsuarioRepository extends JpaRepository<AnexoUsuario, Long> {

    /**
     * Anexos de um funcionario, do mais recente para o mais antigo.
     *
     * Repare que o filtro tem DUAS condicoes: a empresa e o usuario. So o
     * usuario ja bastaria (ele pertence a uma empresa), mas escrever a empresa
     * na consulta deixa o isolamento visivel para quem le - e a consulta nao
     * depende de ninguem lembrar dessa cadeia.
     */
    List<AnexoUsuario> findByEmpresaIdAndUsuarioIdOrderByDataRegistroDesc(Long empresaId,
                                                                          Long usuarioId);

    /** Um anexo especifico, conferindo a empresa - usado no download e na remocao. */
    Optional<AnexoUsuario> findByIdAndEmpresaId(Long id, Long empresaId);

    /** Quantos anexos o funcionario tem. Usado pela lista de usuarios. */
    long countByEmpresaIdAndUsuarioId(Long empresaId, Long usuarioId);
}
