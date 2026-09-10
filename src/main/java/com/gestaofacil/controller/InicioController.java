package com.gestaofacil.controller;

import com.gestaofacil.model.Perfil;
import com.gestaofacil.security.UsuarioAutenticado;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Telas iniciais depois do login.
 *
 * O REDIRECIONAMENTO POR PERFIL (RN006) MORA AQUI, NUM LUGAR SO
 * Depois de um login bem-sucedido, o Spring Security manda todo mundo para
 * "/" (ver defaultSuccessUrl no SecurityConfig). E o metodo inicio() abaixo
 * que decide o destino. Concentrar a decisao num unico metodo evita que a
 * regra fique espalhada e comecem a divergir.
 *
 * As telas /painel e /lancamentos sao PROVISORIAS: existem para o
 * redirecionamento ter para onde ir e serao substituidas pelos cards das
 * proximas funcionalidades.
 */
@Controller
public class InicioController {

    /**
     * Porta de entrada do sistema para quem ja esta logado.
     *
     * @AuthenticationPrincipal entrega o objeto que guardamos na sessao no
     * momento do login. Como o SecurityConfig exige autenticacao em "/",
     * aqui ele nunca chega nulo.
     */
    @GetMapping("/")
    public String inicio(@AuthenticationPrincipal UsuarioAutenticado logado) {

        // RF07 tem prioridade sobre tudo: senha temporaria nao ve mais nada.
        if (logado.isSenhaTemporaria()) {
            return "redirect:/trocar-senha";
        }

        // RN006: cada perfil comeca na sua tela.
        if (logado.getPerfil() == Perfil.ADMINISTRADOR) {
            return "redirect:/painel";
        }
        return "redirect:/lancamentos";
    }

    /** Tela inicial do Administrador. Provisoria. */
    @GetMapping("/painel")
    public String painel(@AuthenticationPrincipal UsuarioAutenticado logado, Model model) {
        model.addAttribute("logado", logado);
        return "painel/painel";
    }

    /** Tela inicial do Motorista. Provisoria. */
    @GetMapping("/lancamentos")
    public String lancamentos(@AuthenticationPrincipal UsuarioAutenticado logado, Model model) {
        model.addAttribute("logado", logado);
        return "painel/lancamentos";
    }
}
