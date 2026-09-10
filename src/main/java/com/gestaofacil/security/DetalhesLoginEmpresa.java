package com.gestaofacil.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

/**
 * Leva o identificador da empresa da requisicao ate o provider.
 *
 * O PROBLEMA QUE ESTA CLASSE RESOLVE
 * ----------------------------------
 * O formulario de login do Spring Security trabalha com dois campos apenas:
 * usuario e senha. Mas o nosso login e unico so dentro da empresa (RN007), e
 * credenciais de uma empresa nao podem valer em outra (RN008). Ou seja: a
 * verificacao precisa de tres informacoes, nao duas.
 *
 * O Spring ja monta, em toda tentativa de login, um objeto de "detalhes" com
 * dados extras da requisicao (endereco IP, id da sessao). Esta classe herda
 * dele e acrescenta o campo que falta. E o caminho previsto pelo proprio
 * Spring para levar informacao adicional ate a autenticacao.
 *
 * DE ONDE VEM O IDENTIFICADOR
 * ---------------------------
 * Sempre do parametro "empresa" enviado pelo formulario. Ele aparece de duas
 * formas, e o codigo aqui nao precisa saber qual das duas:
 *
 * - no caminho normal, a tela e /construtora-teste/login e o controller poe o
 *   identificador num campo escondido (hidden). O motorista nao ve nem digita
 *   nada disso - continua informando so usuario e senha (#001-RF02);
 * - no caminho de excecao, quando alguem entra por um endereco sem empresa,
 *   a mesma tela mostra o campo "empresa" preenchivel.
 */
public class DetalhesLoginEmpresa extends WebAuthenticationDetails {

    private final String identificadorEmpresa;

    public DetalhesLoginEmpresa(HttpServletRequest request) {
        super(request);
        String enviado = request.getParameter("empresa");
        this.identificadorEmpresa = (enviado == null) ? "" : enviado.trim();
    }

    public String getIdentificadorEmpresa() {
        return identificadorEmpresa;
    }
}
