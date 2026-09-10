package com.gestaofacil.controller;

import com.gestaofacil.model.Usuario;
import com.gestaofacil.repository.UsuarioRepository;
import com.gestaofacil.security.UsuarioAutenticado;
import com.gestaofacil.service.ControleDeTentativasService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Desbloqueio de contas pelo Administrador (RN005).
 *
 * Tela minima, feita so para o card #001. Quando o cadastro completo de
 * usuarios for implementado, o botao de desbloquear passa para la e esta
 * tela deixa de existir.
 *
 * Quem pode entrar aqui e definido no SecurityConfig
 * (/usuarios/** exige o perfil ADMINISTRADOR), e nao por uma checagem
 * espalhada nos metodos.
 */
@Controller
public class UsuarioBloqueioController {

    private final UsuarioRepository usuarioRepository;
    private final ControleDeTentativasService controleDeTentativas;

    public UsuarioBloqueioController(UsuarioRepository usuarioRepository,
                                     ControleDeTentativasService controleDeTentativas) {
        this.usuarioRepository = usuarioRepository;
        this.controleDeTentativas = controleDeTentativas;
    }

    /** Lista as contas bloqueadas AGORA, so da empresa do Administrador. */
    @GetMapping("/usuarios/bloqueados")
    public String listar(@AuthenticationPrincipal UsuarioAutenticado logado, Model model) {

        List<Usuario> bloqueados = usuarioRepository
                .findByEmpresaIdAndBloqueadoAteAfterOrderByNome(
                        logado.getEmpresaId(), LocalDateTime.now());

        model.addAttribute("logado", logado);
        model.addAttribute("bloqueados", bloqueados);
        return "usuarios/bloqueados";
    }

    /**
     * Libera a conta na hora, sem esperar os 30 minutos (RN005).
     *
     * O id vem da URL, ou seja, do lado de fora - mas o service busca sempre
     * com o empresaId do Administrador logado. Um administrador da empresa A
     * que digitar o id de um usuario da empresa B recebe erro, nao o
     * desbloqueio. E a regra 1 do projeto.
     */
    @PostMapping("/usuarios/{id}/desbloquear")
    public String desbloquear(@PathVariable Long id,
                              @AuthenticationPrincipal UsuarioAutenticado logado,
                              RedirectAttributes atributos) {

        boolean liberou = controleDeTentativas.desbloquear(id, logado.getEmpresaId());

        if (!liberou) {
            // O usuario existe, mas e de outra empresa. Respondemos 404, e
            // nao 403: um "acesso negado" confirmaria que aquele id existe
            // em algum lugar. Para esta empresa, ele nao existe. Ponto.
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado.");
        }

        // Mensagem que sobrevive ao redirecionamento e aparece uma vez so.
        atributos.addFlashAttribute("mensagem", "Conta liberada.");
        return "redirect:/usuarios/bloqueados";
    }
}
