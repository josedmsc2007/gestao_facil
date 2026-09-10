package com.gestaofacil.service;

import com.gestaofacil.model.Usuario;
import com.gestaofacil.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Controla o bloqueio da conta por tentativas invalidas (RN004 e RN005).
 *
 * POR QUE UM SERVICE SO PARA ISSO
 * A contagem e o bloqueio sao uma regra de negocio com comeco, meio e fim,
 * usada em tres lugares diferentes: no login que falha, no login que da certo
 * e na tela em que o Administrador desbloqueia. Deixar a regra num lugar so
 * evita que as tres pontas discordem entre si.
 *
 * AS DUAS COLUNAS ENVOLVIDAS (acrescentadas ao usuario pelo card #001)
 * - tentativas_invalidas: quantas senhas erradas seguidas. Zera ao acertar.
 * - bloqueado_ate: ate quando a conta esta fechada. Nulo = liberada.
 */
@Service
public class ControleDeTentativasService {

    /** RN004: quantos erros seguidos fecham a conta. */
    public static final int TENTATIVAS_ATE_BLOQUEAR = 5;

    /** RN004: por quanto tempo a conta fica fechada. */
    public static final int MINUTOS_DE_BLOQUEIO = 30;

    private final UsuarioRepository usuarioRepository;

    public ControleDeTentativasService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * A conta esta fechada NESTE momento?
     *
     * Repare que nao basta bloqueado_ate estar preenchido: se a data ja passou,
     * o bloqueio venceu sozinho e a conta esta livre de novo. Nao existe tarefa
     * agendada limpando a coluna - a comparacao com o relogio resolve.
     */
    public boolean estaBloqueado(Usuario usuario) {
        LocalDateTime bloqueadoAte = usuario.getBloqueadoAte();
        return bloqueadoAte != null && bloqueadoAte.isAfter(LocalDateTime.now());
    }

    /** Quantos minutos ainda faltam para a conta abrir. Para a mensagem da tela. */
    public long minutosRestantes(Usuario usuario) {
        if (!estaBloqueado(usuario)) {
            return 0;
        }
        return java.time.Duration.between(LocalDateTime.now(), usuario.getBloqueadoAte())
                .toMinutes() + 1;
    }

    /**
     * Registra uma senha errada (RN004).
     *
     * Busca de novo pelo id CONFERINDO A EMPRESA (regra 1 do projeto), porque
     * o objeto que veio do provider ja saiu da conexao com o banco.
     */
    @Transactional
    public void registrarFalha(Long usuarioId, Long empresaId) {
        Usuario usuario = usuarioRepository.findByIdAndEmpresaId(usuarioId, empresaId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Usuário não encontrado na empresa."));

        // Se havia um bloqueio que ja venceu, a contagem recomeca do zero.
        // Sem isto, um unico erro depois dos 30 minutos fecharia a conta de
        // novo na hora, porque o contador teria ficado em 5.
        if (usuario.getBloqueadoAte() != null && !estaBloqueado(usuario)) {
            usuario.setTentativasInvalidas(0);
            usuario.setBloqueadoAte(null);
        }

        usuario.setTentativasInvalidas(usuario.getTentativasInvalidas() + 1);

        if (usuario.getTentativasInvalidas() >= TENTATIVAS_ATE_BLOQUEAR) {
            usuario.setBloqueadoAte(LocalDateTime.now().plusMinutes(MINUTOS_DE_BLOQUEIO));
        }

        usuarioRepository.save(usuario);
    }

    /**
     * Registra um login bem-sucedido: a contagem zera.
     *
     * "Consecutivas" na RN004 quer dizer exatamente isto - quatro erros
     * seguidos de um acerto nao deixam resto para a proxima vez.
     */
    @Transactional
    public void registrarSucesso(Long usuarioId, Long empresaId) {
        Usuario usuario = usuarioRepository.findByIdAndEmpresaId(usuarioId, empresaId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Usuário não encontrado na empresa."));

        // Evita uma gravacao no banco a cada login quando nao ha nada a zerar.
        if (usuario.getTentativasInvalidas() == 0 && usuario.getBloqueadoAte() == null) {
            return;
        }

        usuario.setTentativasInvalidas(0);
        usuario.setBloqueadoAte(null);
        usuarioRepository.save(usuario);
    }

    /**
     * RN005: o Administrador libera a conta antes dos 30 minutos.
     *
     * O empresaId vem do Administrador logado, entao um administrador de uma
     * empresa nunca alcanca usuario de outra - mesmo que descubra o id.
     *
     * @return true se liberou; false se o usuario nao e daquela empresa.
     */
    @Transactional
    public boolean desbloquear(Long usuarioId, Long empresaId) {

        // Devolve false, em vez de estourar, quando o usuario nao e da
        // empresa: para quem esta do lado de fora, o registro simplesmente
        // NAO EXISTE. O controller transforma isso num 404.
        var encontrado = usuarioRepository.findByIdAndEmpresaId(usuarioId, empresaId);
        if (encontrado.isEmpty()) {
            return false;
        }

        Usuario usuario = encontrado.get();
        usuario.setTentativasInvalidas(0);
        usuario.setBloqueadoAte(null);
        usuarioRepository.save(usuario);
        return true;
    }
}
