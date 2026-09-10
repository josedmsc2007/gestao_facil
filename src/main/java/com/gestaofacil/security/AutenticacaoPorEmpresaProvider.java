package com.gestaofacil.security;

import com.gestaofacil.model.Empresa;
import com.gestaofacil.model.Usuario;
import com.gestaofacil.repository.EmpresaRepository;
import com.gestaofacil.repository.UsuarioRepository;
import com.gestaofacil.service.ControleDeTentativasService;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Confere usuario, senha e empresa. E o coracao do RF01.
 *
 * O Spring Security tem um provider pronto (o DaoAuthenticationProvider), mas
 * ele so sabe procurar o usuario por UM texto. Como precisamos procurar por
 * empresa + login, escrevemos o nosso. A vantagem para o grupo e que todo o
 * fluxo de autenticacao cabe neste arquivo, de cima a baixo.
 *
 * ORDEM DAS CONFERENCIAS
 * 1. a empresa do endereco existe?
 * 2. existe um usuario com esse login DENTRO dessa empresa? (RN007, RN008)
 * 3. esse usuario esta ativo? (RN009)
 * 4. a senha digitada bate com o hash guardado? (RN010)
 *
 * Em qualquer falha o erro e o MESMO (#001-RF04): a tela nunca revela se o
 * problema foi o usuario, a senha, a empresa ou a situacao da conta. Revelar
 * isso entregaria a quem esta tentando adivinhar metade da resposta.
 */
@Component
public class AutenticacaoPorEmpresaProvider implements AuthenticationProvider {

    /**
     * Uma unica mensagem para todos os casos de falha. Constante, para que
     * ninguem escreva uma variacao mais "informativa" por engano depois.
     */
    private static final String MENSAGEM_GENERICA = "Usuário ou senha incorretos.";

    /**
     * Mensagem do bloqueio (RN004). Esta NAO e generica, e a escolha e
     * deliberada: o funcionario precisa entender por que a senha certa parou
     * de funcionar, senao fica tentando de novo sem nunca compreender.
     *
     * O preco e que a mensagem confirma que aquela conta existe. Aceitamos
     * esse preco porque os usuarios do sistema sao motoristas com pouca
     * familiaridade com tecnologia, e uma mensagem generica aqui os deixaria
     * presos do lado de fora sem saber o que fazer.
     */
    private static final String MENSAGEM_BLOQUEIO =
            "Conta bloqueada por excesso de tentativas.";

    private final EmpresaRepository empresaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final ControleDeTentativasService controleDeTentativas;

    public AutenticacaoPorEmpresaProvider(EmpresaRepository empresaRepository,
                                          UsuarioRepository usuarioRepository,
                                          PasswordEncoder passwordEncoder,
                                          ControleDeTentativasService controleDeTentativas) {
        this.empresaRepository = empresaRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.controleDeTentativas = controleDeTentativas;
    }

    /**
     * O Spring chama este metodo passando o que o formulario enviou.
     * Devolver um Authentication = login aceito.
     * Lancar AuthenticationException = login recusado.
     */
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {

        String login = authentication.getName();
        String senhaDigitada = String.valueOf(authentication.getCredentials());
        String identificadorEmpresa = identificadorDaEmpresa(authentication);

        // 1. A empresa do endereco existe?
        Empresa empresa = empresaRepository.findByIdentificador(identificadorEmpresa)
                .orElseThrow(() -> new BadCredentialsException(MENSAGEM_GENERICA));

        // 2. O usuario existe DENTRO dessa empresa?
        //    Este e o ponto que garante a RN008: o mesmo login em outra
        //    empresa simplesmente nao e encontrado aqui.
        Usuario usuario = usuarioRepository
                .findByEmpresaIdAndLogin(empresa.getId(), login)
                .orElseThrow(() -> new BadCredentialsException(MENSAGEM_GENERICA));

        // 3. RN009 - usuario inativo nao entra.
        if (!usuario.isAtivo()) {
            throw new BadCredentialsException(MENSAGEM_GENERICA);
        }

        // 4. RN004 - conta bloqueada por excesso de tentativas.
        //    Esta conferencia vem ANTES da senha, de proposito: quem esta
        //    bloqueado nao entra nem acertando a senha, e a tentativa nem
        //    sequer conta - senao o bloqueio se renovaria sozinho para sempre.
        if (controleDeTentativas.estaBloqueado(usuario)) {
            throw new LockedException(MENSAGEM_BLOQUEIO);
        }

        // 5. RN010 - compara a senha digitada com o hash guardado.
        //    O metodo matches() aplica o BCrypt na senha digitada e compara o
        //    resultado. O caminho inverso (hash -> senha) nao existe.
        if (!passwordEncoder.matches(senhaDigitada, usuario.getSenha())) {
            // Errou: soma mais uma tentativa e, na quinta, fecha a conta.
            controleDeTentativas.registrarFalha(usuario.getId(), empresa.getId());
            throw new BadCredentialsException(MENSAGEM_GENERICA);
        }

        // Acertou: a contagem de tentativas seguidas volta a zero.
        controleDeTentativas.registrarSucesso(usuario.getId(), empresa.getId());

        // Deu certo. O objeto devolvido e o que fica guardado na sessao.
        UsuarioAutenticado usuarioAutenticado = new UsuarioAutenticado(usuario, empresa);
        return new UsernamePasswordAuthenticationToken(
                usuarioAutenticado,
                null,                                  // a senha nao fica guardada
                usuarioAutenticado.getAuthorities());
    }

    /**
     * Pega o identificador da empresa do objeto de detalhes montado pelo
     * DetalhesLoginEmpresa. Se por algum motivo ele nao vier, devolve texto
     * vazio - e a busca da empresa falha logo em seguida, com a mensagem
     * generica de sempre.
     */
    private String identificadorDaEmpresa(Authentication authentication) {
        if (authentication.getDetails() instanceof DetalhesLoginEmpresa detalhes) {
            return detalhes.getIdentificadorEmpresa();
        }
        return "";
    }

    /**
     * Diz ao Spring em quais tipos de tentativa de login este provider deve
     * ser usado. O formulario de usuario e senha produz exatamente este tipo.
     */
    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
