package com.gestaofacil.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Prende na tela de troca de senha quem entrou com senha temporaria (RF07).
 *
 * COMO FUNCIONA UM INTERCEPTOR
 * O metodo preHandle roda ANTES de qualquer controller. Devolver true deixa a
 * requisicao seguir; devolver false a interrompe. E o lugar certo para uma
 * regra que vale para todas as telas de uma vez.
 *
 * POR QUE NAO BASTA REDIRECIONAR NO LOGIN
 * Redirecionar so no momento do login resolveria o primeiro clique. Mas o
 * usuario poderia digitar /painel na barra de enderecos e entrar assim mesmo.
 * O criterio do card e claro: a troca bloqueia o acesso as DEMAIS telas
 * enquanto nao for concluida - por isso a checagem roda a cada requisicao.
 */
@Component
public class SenhaTemporariaInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();

        // Ninguem logado (ou login de outro tipo): nao e assunto deste
        // interceptor. Quem barra visitante e o Spring Security.
        if (autenticacao == null
                || !(autenticacao.getPrincipal() instanceof UsuarioAutenticado logado)) {
            return true;
        }

        // Senha ja definitiva: segue o fluxo normal.
        if (!logado.isSenhaTemporaria()) {
            return true;
        }

        // A propria tela de troca precisa continuar acessivel, senao o
        // usuario seria redirecionado para ela eternamente. O logout tambem,
        // para ninguem ficar preso dentro do sistema.
        String caminho = request.getRequestURI().substring(request.getContextPath().length());
        if (caminho.equals("/trocar-senha") || caminho.equals("/logout")) {
            return true;
        }

        response.sendRedirect(request.getContextPath() + "/trocar-senha");
        return false;
    }
}
