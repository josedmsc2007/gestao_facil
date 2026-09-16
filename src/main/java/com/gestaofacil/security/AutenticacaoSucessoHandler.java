package com.gestaofacil.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * O que acontece quando o login da certo.
 *
 * ANTES DO #001.1 o SecurityConfig usava .defaultSuccessUrl("/", true). Por
 * dentro, essa linha cria um SavedRequestAwareAuthenticationSuccessHandler
 * apontando para "/" com o "sempre" ligado. Esta classe herda exatamente dele
 * e liga as mesmas duas opcoes, entao o destino continua o mesmo: todo mundo
 * vai para "/", e o InicioController decide conforme o perfil.
 *
 * A UNICA COISA NOVA e zerar a contagem de falhas da sessao para o login que
 * acabou de entrar (#001.1-RN006). A contagem do BANCO continua sendo zerada
 * pelo provider, como sempre foi.
 */
@Component
public class AutenticacaoSucessoHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final AvisoDeTentativasNaSessao avisoDeTentativas;

    public AutenticacaoSucessoHandler(AvisoDeTentativasNaSessao avisoDeTentativas) {
        this.avisoDeTentativas = avisoDeTentativas;
        setDefaultTargetUrl("/");
        setAlwaysUseDefaultTargetUrl(true);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication autenticacao)
            throws ServletException, IOException {

        avisoDeTentativas.registrarSucesso(request);
        super.onAuthenticationSuccess(request, response, autenticacao);
    }
}
