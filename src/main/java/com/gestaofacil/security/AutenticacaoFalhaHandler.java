package com.gestaofacil.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Decide para onde voltar quando o login falha.
 *
 * O PROBLEMA QUE ELE RESOLVE
 * O Spring Security permite configurar um endereco fixo de falha. Mas o nosso
 * endereco de login muda conforme a empresa (/construtora-teste/login). Com um
 * endereco fixo, quem errasse a senha seria jogado na tela generica e teria de
 * digitar a empresa - justamente o que o #001-RF02 quer evitar.
 *
 * Entao lemos de volta o campo "empresa" que veio no formulario e devolvemos
 * o usuario para a tela da empresa dele, com ?erro no final para a tela saber
 * que deve mostrar a mensagem.
 *
 * #001.1-RF02: e aqui tambem que a falha entra na contagem da sessao, usada
 * so para o aviso de tentativas restantes. Este ponto recebe TODA falha - de
 * senha, de usuario inexistente, de empresa inexistente - e por isso a
 * contagem nao depende de o usuario existir.
 */
@Component
public class AutenticacaoFalhaHandler extends SimpleUrlAuthenticationFailureHandler {

    private final AvisoDeTentativasNaSessao avisoDeTentativas;

    public AutenticacaoFalhaHandler(AvisoDeTentativasNaSessao avisoDeTentativas) {
        this.avisoDeTentativas = avisoDeTentativas;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException excecao)
            throws IOException, jakarta.servlet.ServletException {

        avisoDeTentativas.registrarFalha(request);

        String empresa = request.getParameter("empresa");

        // RN004: a conta bloqueada tem aviso proprio, para o funcionario
        // entender que nao adianta continuar tentando a senha certa.
        String aviso = (excecao instanceof LockedException) ? "?bloqueado" : "?erro";

        // Sem empresa (caminho de excecao) volta para a tela generica.
        String destino = ehIdentificadorValido(empresa)
                ? "/" + empresa + "/login" + aviso
                : "/login" + aviso;

        setDefaultFailureUrl(destino);
        super.onAuthenticationFailure(request, response, excecao);
    }

    /**
     * So aceita o formato de identificador que o sistema usa: letras
     * minusculas, numeros e hifen.
     *
     * Isto NAO e frescura. Sem a conferencia, alguem poderia enviar no campo
     * "empresa" um texto com barras ou com "http://" e o nosso redirecionamento
     * levaria o usuario para fora do sistema - a falha conhecida como
     * "open redirect". Como o valor vem do formulario, ele e sempre suspeito.
     */
    private boolean ehIdentificadorValido(String empresa) {
        return empresa != null && empresa.matches("[a-z0-9-]+");
    }
}
