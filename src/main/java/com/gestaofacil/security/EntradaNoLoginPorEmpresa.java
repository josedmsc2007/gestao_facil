package com.gestaofacil.security;

import com.gestaofacil.repository.EmpresaRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Decide para QUAL tela de login mandar quem tentou abrir uma tela protegida
 * sem estar logado.
 *
 * O PROBLEMA
 * O atalho que o funcionario poe na tela inicial do celular e /construtora-teste
 * (RF01). Esse endereco precisa levar a tela de login da empresa. A saida
 * obvia seria liberar no SecurityConfig todo endereco de um segmento so - algo
 * como "/*" - mas isso liberaria junto /painel, /lancamentos e qualquer tela
 * interna futura de nome curto. Seria uma armadilha esperando alguem.
 *
 * A SOLUCAO
 * Nao liberar nada a mais. Deixar o Spring Security barrar normalmente e, no
 * momento em que ele vai redirecionar para o login, decidir aqui o destino:
 *
 * - se o primeiro pedaco do endereco e o identificador de uma empresa que
 *   existe, manda para a tela de login DAQUELA empresa;
 * - caso contrario, manda para a tela generica, que pede a empresa.
 *
 * Assim o atalho funciona e nenhuma tela interna fica exposta por engano.
 */
@Component
public class EntradaNoLoginPorEmpresa implements AuthenticationEntryPoint {

    private final EmpresaRepository empresaRepository;

    public EntradaNoLoginPorEmpresa(EmpresaRepository empresaRepository) {
        this.empresaRepository = empresaRepository;
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException excecao) throws IOException {

        String primeiroPedaco = primeiroPedacoDoEndereco(request);

        boolean ehEmpresa = primeiroPedaco.matches("[a-z0-9-]+")
                && empresaRepository.existsByIdentificador(primeiroPedaco);

        String destino = ehEmpresa
                ? "/" + primeiroPedaco + "/login"
                : "/login";

        response.sendRedirect(request.getContextPath() + destino);
    }

    /** De "/construtora-teste/veiculos" devolve "construtora-teste". */
    private String primeiroPedacoDoEndereco(HttpServletRequest request) {
        String caminho = request.getRequestURI()
                .substring(request.getContextPath().length());

        String[] pedacos = caminho.split("/");
        // "/algo" vira ["", "algo"], entao o que interessa esta na posicao 1.
        return pedacos.length >= 2 ? pedacos[1] : "";
    }
}
