package com.gestaofacil.config;

import com.gestaofacil.security.SenhaTemporariaInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuracao das telas (camada web).
 *
 * Por enquanto serve so para ligar o SenhaTemporariaInterceptor. Escrever a
 * classe nao basta: sem registra-la aqui, o Spring nunca a chama.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final SenhaTemporariaInterceptor senhaTemporariaInterceptor;

    public WebMvcConfig(SenhaTemporariaInterceptor senhaTemporariaInterceptor) {
        this.senhaTemporariaInterceptor = senhaTemporariaInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registro) {
        registro.addInterceptor(senhaTemporariaInterceptor)
                // Vale para todas as telas...
                .addPathPatterns("/**")
                // ...menos os arquivos de estilo e imagem, que nao sao telas.
                .excludePathPatterns("/css/**", "/js/**", "/imagens/**",
                        "/favicon.ico", "/manifest.json");
    }
}
