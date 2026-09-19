package com.gestaofacil.config;

import com.gestaofacil.model.Empresa;
import com.gestaofacil.security.AutenticacaoFalhaHandler;
import com.gestaofacil.security.AutenticacaoSucessoHandler;
import com.gestaofacil.security.EntradaNoLoginPorEmpresa;
import com.gestaofacil.security.SaidaPorEmpresaHandler;
import com.gestaofacil.security.DetalhesLoginEmpresa;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuracao do Spring Security.
 *
 * Atende a tarefa #001 por inteiro: autenticacao considerando a empresa,
 * telas de login e troca de senha, redirecionamento por perfil (RN006),
 * bloqueio por tentativas invalidas (RN004) e encerramento da sessao (RF05).
 */
@Configuration
public class SecurityConfig {

    /**
     * BCrypt e o algoritmo de hash recomendado para senhas.
     *
     * Duas caracteristicas importantes para a apresentacao:
     * - e de mao unica: da senha gera-se o hash, mas do hash nao se volta
     *   para a senha;
     * - e propositalmente lento e usa "sal" aleatorio, entao a mesma senha
     *   gera hashes diferentes e ataques de forca bruta ficam caros.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Cadeia de filtros de seguranca: define o que exige login e o que nao,
     * e como o login e processado.
     *
     * Os tratadores chegam prontos por parametro: o Spring ve que sao
     * componentes e entrega aqui.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           AutenticacaoFalhaHandler falhaHandler,
                                           AutenticacaoSucessoHandler sucessoHandler,
                                           EntradaNoLoginPorEmpresa entradaNoLogin,
                                           SaidaPorEmpresaHandler saidaHandler) throws Exception {
        http
                // ATENCAO: NAO acrescente aqui .authenticationProvider(...).
                //
                // O AutenticacaoPorEmpresaProvider esta anotado como
                // componente, e so por isso o Spring Security ja o adota.
                // Registra-lo tambem aqui o coloca DUAS vezes na fila: cada
                // tentativa de login executa o provider duas vezes e conta
                // dois erros. O bloqueio da RN004 disparava na terceira
                // tentativa em vez da quinta. Foi um teste que pegou isso.

                .authorizeHttpRequests(requisicoes -> requisicoes
                        // Arquivos de estilo e imagens: liberados, senao a
                        // propria tela de login ficaria sem CSS.
                        .requestMatchers("/css/**", "/js/**", "/imagens/**",
                                "/favicon.ico", "/manifest.json").permitAll()

                        // As telas de login precisam ser abertas por quem
                        // ainda nao entrou. "/*/login" cobre o endereco com
                        // empresa (/construtora-teste/login) e "/login" cobre
                        // o caminho de excecao, sem empresa no endereco.
                        .requestMatchers("/login", "/*/login").permitAll()

                        // Telas de TODOS os perfis, inclusive o Operador:
                        // a porta de entrada que distribui por perfil, a
                        // troca de senha obrigatoria (RF07), a pagina de erro
                        // do Spring (sem ela, um 404 do Operador virava 403)
                        // e o atalho da empresa da equipe, que e o que os
                        // Operadores poem na tela inicial do celular.
                        .requestMatchers("/", "/trocar-senha", "/error",
                                "/" + Empresa.IDENTIFICADOR_DA_EQUIPE).authenticated()

                        // #002-RN007: cadastro de empresas, so o Operador.
                        .requestMatchers("/empresas/**").hasRole("OPERADOR")

                        // Regras por perfil (RN006). Cada tela inicial so
                        // abre para o perfil a que pertence. Se um motorista
                        // digitar /painel na barra de enderecos, leva 403.
                        .requestMatchers("/painel").hasRole("ADMINISTRADOR")
                        .requestMatchers("/lancamentos").hasRole("MOTORISTA")

                        // Telas de administracao de usuarios, incluindo o
                        // desbloqueio de contas (RN005).
                        .requestMatchers("/usuarios/**").hasRole("ADMINISTRADOR")

                        // Todo o resto: so os perfis DA EMPRESA.
                        //
                        // Ate o card #002 esta linha era
                        // .anyRequest().authenticated(). Com o Operador isso
                        // deixou de servir: toda tela nova (/veiculos,
                        // /abastecimentos...) abriria tambem para ele, e a
                        // #002-RN008 diz que o Operador nao ve dado
                        // operacional nenhum. Agora a regra e "fechado por
                        // padrao": tela nova nasce proibida ao Operador sem
                        // ninguem precisar lembrar disso. Continua valendo
                        // tambem o criterio "sistema impede acesso de
                        // usuarios nao cadastrados": quem nao esta logado nao
                        // tem perfil nenhum.
                        .anyRequest().hasAnyRole("ADMINISTRADOR", "MOTORISTA"))

                .formLogin(formulario -> formulario
                        // Tela de login. Na pratica quem escolhe o destino de
                        // quem nao esta logado e o entradaNoLogin, la embaixo.
                        .loginPage("/login")

                        // Endereco para onde o formulario envia os dados.
                        // E o Spring quem trata este POST - nao existe (nem
                        // deve existir) um metodo de controller para ele.
                        .loginProcessingUrl("/login")

                        // Nomes dos campos do formulario, em portugues.
                        .usernameParameter("usuario")
                        .passwordParameter("senha")

                        // Aqui entra a empresa: para cada tentativa de login,
                        // o Spring monta um DetalhesLoginEmpresa, que le o
                        // campo "empresa" enviado pelo formulario.
                        .authenticationDetailsSource(DetalhesLoginEmpresa::new)

                        // Login recusado: volta para a tela de login DA
                        // EMPRESA, e nao para a tela generica.
                        .failureHandler(falhaHandler)

                        // Login aceito: todo mundo vai para "/", e o
                        // InicioController decide o destino conforme o perfil
                        // (RN006) ou manda trocar a senha temporaria (RF07).
                        // Vale sempre, mesmo que o usuario tenha tentado abrir
                        // outra tela antes de entrar. Ate o #001.1 isto era
                        // .defaultSuccessUrl("/", true); o handler faz o mesmo
                        // e ainda zera o aviso de tentativas da sessao.
                        .successHandler(sucessoHandler))

                // #001-RF05: encerramento da sessao.
                .logout(saida -> saida
                        // O Spring so aceita logout por POST, com token CSRF.
                        // Um simples link nao serve - e proposital: assim
                        // outro site nao consegue deslogar o usuario.
                        .logoutUrl("/logout")

                        // Volta para a tela de login DA EMPRESA.
                        .logoutSuccessHandler(saidaHandler)

                        // Invalida a sessao no servidor: o identificador
                        // antigo deixa de valer, mesmo que alguem o guarde.
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)

                        // Apaga o cookie de sessao do navegador.
                        .deleteCookies("JSESSIONID"))

                // Quem nao esta logado e pede uma tela protegida cai aqui.
                // O atalho /construtora-teste chega neste ponto e e mandado
                // para a tela de login da propria empresa.
                .exceptionHandling(excecoes -> excecoes
                        .authenticationEntryPoint(entradaNoLogin));

        // Repare no que NAO esta escrito aqui: a linha csrf(disable) que
        // existia no esqueleto foi removida. Com isso o CSRF volta ao padrao
        // do Spring, que e LIGADO. Ele impede que outro site force um envio
        // de formulario em nome de quem esta logado. Os formularios Thymeleaf
        // incluem o token sozinhos, desde que usem th:action.

        return http.build();
    }
}
