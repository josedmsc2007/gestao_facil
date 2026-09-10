package com.gestaofacil.controller;

import com.gestaofacil.controller.form.TrocaSenhaForm;
import com.gestaofacil.security.UsuarioAutenticado;
import com.gestaofacil.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Tela de definicao de nova senha (RF07 e RN003).
 *
 * Para quem chega com senha temporaria, esta e a UNICA tela acessivel: o
 * SenhaTemporariaInterceptor devolve para ca qualquer outro endereco.
 */
@Controller
public class TrocaSenhaController {

    private final UsuarioService usuarioService;

    /**
     * Objeto do Spring Security que sabe guardar o usuario logado na sessao.
     * Precisamos dele no final do metodo trocar() - o comentario la explica.
     */
    private final SecurityContextRepository repositorioDeSessao =
            new HttpSessionSecurityContextRepository();

    public TrocaSenhaController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /** Mostra a tela. */
    @GetMapping("/trocar-senha")
    public String mostrar(@AuthenticationPrincipal UsuarioAutenticado logado, Model model) {
        model.addAttribute("logado", logado);
        model.addAttribute("trocaSenhaForm", new TrocaSenhaForm());
        return "login/trocar-senha";
    }

    /**
     * Recebe o formulario.
     *
     * @Valid aplica as anotacoes do TrocaSenhaForm (obrigatorio, minimo de 8
     * caracteres). O resultado da validacao chega no BindingResult, que
     * PRECISA vir logo depois do objeto validado na lista de parametros -
     * se ficar em outra posicao, o Spring lanca excecao em vez de preencher.
     */
    @PostMapping("/trocar-senha")
    public String trocar(@AuthenticationPrincipal UsuarioAutenticado logado,
                         @Valid @ModelAttribute TrocaSenhaForm trocaSenhaForm,
                         BindingResult resultado,
                         HttpServletRequest request,
                         HttpServletResponse response,
                         Model model) {

        // As duas senhas digitadas precisam ser iguais. Esta conferencia
        // envolve dois campos, entao nao cabe numa anotacao de um campo so.
        if (!resultado.hasFieldErrors("novaSenha")
                && !trocaSenhaForm.getNovaSenha().equals(trocaSenhaForm.getConfirmacaoSenha())) {
            resultado.rejectValue("confirmacaoSenha", "senhas.diferentes",
                    "As duas senhas digitadas não são iguais.");
        }

        if (resultado.hasErrors()) {
            model.addAttribute("logado", logado);
            return "login/trocar-senha";
        }

        // Grava a nova senha (com hash) e desmarca a senha temporaria.
        usuarioService.definirNovaSenha(
                logado.getId(), logado.getEmpresaId(), trocaSenhaForm.getNovaSenha());

        atualizarUsuarioNaSessao(logado, request, response);

        // A partir daqui o usuario segue o fluxo normal do perfil dele.
        return "redirect:/";
    }

    /**
     * Troca, na sessao, o usuario logado por uma copia sem a marca de senha
     * temporaria.
     *
     * Sem isto, o banco estaria certo mas a sessao continuaria dizendo
     * "senha temporaria = true", e o interceptor mandaria o usuario de volta
     * para esta tela a cada clique, num laco sem fim.
     *
     * Sao tres passos: montar a nova autenticacao, coloca-la no contexto da
     * requisicao atual e gravar esse contexto na sessao, para valer tambem
     * nas proximas requisicoes.
     */
    private void atualizarUsuarioNaSessao(UsuarioAutenticado logado,
                                          HttpServletRequest request,
                                          HttpServletResponse response) {

        UsuarioAutenticado atualizado = logado.comSenhaJaTrocada();

        UsernamePasswordAuthenticationToken novaAutenticacao =
                new UsernamePasswordAuthenticationToken(
                        atualizado, null, atualizado.getAuthorities());

        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(novaAutenticacao);
        SecurityContextHolder.setContext(contexto);
        repositorioDeSessao.saveContext(contexto, request, response);
    }
}
