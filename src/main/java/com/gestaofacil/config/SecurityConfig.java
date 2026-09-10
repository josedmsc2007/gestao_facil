package com.gestaofacil.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuracao do Spring Security.
 *
 * ATENCAO: esta classe esta PROVISORIA. Como a tela de login ainda nao existe
 * (tarefa #001), aqui so ficam as duas coisas que o esqueleto precisa:
 *
 * 1. o PasswordEncoder, que gera e confere o hash das senhas (RN010);
 * 2. uma regra temporaria liberando todas as URLs, para que a aplicacao suba
 *    sem a tela de login padrao do Spring atrapalhar o teste.
 *
 * Na tarefa #001 o metodo filterChain sera reescrito para exigir autenticacao,
 * apontar para a tela de login propria e aplicar as regras por perfil (RN006).
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
     *
     * @Bean significa: "Spring, guarde este objeto e entregue a quem pedir um
     * PasswordEncoder" - e assim que a carga inicial recebe ele pronto.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Cadeia de filtros de seguranca: define o que exige login e o que nao exige.
     *
     * Enquanto nao existe tela de login, tudo e liberado. O CSRF fica desligado
     * porque ainda nao ha formularios; ele volta a ser ligado (padrao do Spring)
     * junto com a tela de login.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(requisicoes -> requisicoes
                        .anyRequest().permitAll())
                .csrf(csrf -> csrf.disable());
        return http.build();
    }
}
