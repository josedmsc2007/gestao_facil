package com.gestaofacil.service;

import com.gestaofacil.model.Usuario;
import com.gestaofacil.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regras de negocio ligadas ao usuario.
 *
 * O controller cuida da tela (recebe o formulario, escolhe a proxima pagina);
 * o service cuida da REGRA. Separar os dois e o que permite, mais tarde,
 * chamar a mesma regra de outro lugar - por exemplo, da tela do Administrador
 * que gera senha temporaria (#001-RF06) - sem repetir codigo.
 */
@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Define uma nova senha definitiva para o usuario (RF07).
     *
     * REPARE NO PRIMEIRO COMANDO: a busca usa findByIdAndEmpresaId, e nao
     * findById. E a regra 1 do projeto em acao - mesmo aqui, onde o id vem da
     * sessao e nao da URL, a consulta confere a empresa. Se um dia alguem
     * conseguir forjar o id, ainda assim nao alcanca usuario de outra empresa.
     *
     * @Transactional: as duas alteracoes (senha e marcacao de temporaria)
     * acontecem juntas ou nenhuma acontece.
     */
    @Transactional
    public void definirNovaSenha(Long usuarioId, Long empresaId, String novaSenha) {

        Usuario usuario = usuarioRepository.findByIdAndEmpresaId(usuarioId, empresaId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Usuário não encontrado na empresa do usuário logado."));

        // RN010: guarda o hash, nunca o texto digitado.
        usuario.setSenha(passwordEncoder.encode(novaSenha));

        // A senha deixa de ser temporaria: o usuario para de cair na tela
        // de troca obrigatoria a cada acesso.
        usuario.setSenhaTemporaria(false);

        usuarioRepository.save(usuario);
    }
}
