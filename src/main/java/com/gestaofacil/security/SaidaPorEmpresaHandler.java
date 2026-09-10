package com.gestaofacil.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Decide para onde ir depois do "Sair" (#001-RF05).
 *
 * Mesma ideia do AutenticacaoFalhaHandler: um endereco fixo mandaria o
 * funcionario para a tela generica, obrigando-o a digitar a empresa no proximo
 * acesso. Como o usuario que esta saindo ainda esta na nossa mao, dele mesmo
 * tiramos o identificador da empresa e devolvemos a pessoa para a tela de
 * login correta.
 *
 * Aqui NAO ha risco de "open redirect": o identificador vem do objeto que o
 * proprio sistema montou no login, e nao de um campo de formulario.
 */
@Component
public class SaidaPorEmpresaHandler implements LogoutSuccessHandler {

    @Override
    public void onLogoutSuccess(HttpServletRequest request,
                                HttpServletResponse response,
                                Authentication autenticacao) throws IOException {

        String destino = "/login?saiu";

        if (autenticacao != null
                && autenticacao.getPrincipal() instanceof UsuarioAutenticado saindo) {
            destino = "/" + saindo.getEmpresaIdentificador() + "/login?saiu";
        }

        response.sendRedirect(request.getContextPath() + destino);
    }
}
