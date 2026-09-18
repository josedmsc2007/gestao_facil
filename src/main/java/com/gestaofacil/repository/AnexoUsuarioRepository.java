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

    /**
     * Um anexo especifico, conferindo a empresa E o funcionario - usado no
     * download e na remocao.
     *
     * O endereco do download e /usuarios/{usuarioId}/anexos/{id}. Conferir so
     * a empresa deixaria o endereco /usuarios/1/anexos/99 abrir o anexo 99
     * mesmo que ele fosse do funcionario 2: nao vazaria nada para fora da
     * empresa, mas a tela de um funcionario mostraria o documento de outro.
     */
    Optional<AnexoUsuario> findByIdAndEmpresaIdAndUsuarioId(Long id, Long empresaId,
                                                            Long usuarioId);

    /** Quantos anexos o funcionario tem. Usado pela lista de usuarios. */
    long countByEmpresaIdAndUsuarioId(Long empresaId, Long usuarioId);
}
