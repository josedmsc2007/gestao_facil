package com.gestaofacil.config;

import com.gestaofacil.model.Empresa;
import com.gestaofacil.model.Perfil;
import com.gestaofacil.model.Usuario;
import com.gestaofacil.repository.EmpresaRepository;
import com.gestaofacil.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Carga inicial de dados (seed).
 *
 * POR QUE ISSO E NECESSARIO
 * -------------------------
 * O sistema so deixa entrar quem tem usuario e senha cadastrados, e o cadastro
 * de usuarios so pode ser feito por um Administrador que ja esteja logado.
 * Isso e um problema do tipo "ovo e galinha": com o banco vazio nao existe
 * ninguem para fazer o primeiro login, e sem o primeiro login ninguem
 * consegue cadastrar o primeiro usuario.
 *
 * Alem disso as senhas sao gravadas com hash BCrypt (RN010). Nao da para
 * simplesmente inserir uma linha na tabela pelo pgAdmin digitando a
 * senha - ela precisa passar pelo PasswordEncoder. Este arquivo faz isso.
 *
 * A carga tambem cria a empresa "construtora-teste": como todo dado do sistema
 * pertence a uma empresa (regra 1 - isolamento multiempresa), o usuario nao
 * pode existir sem ela.
 *
 * IMPORTANTE: e um recurso de DESENVOLVIMENTO. Em producao a linha
 * seed.habilitado=false desliga a classe inteira, e a senha padrao nunca deve
 * ir para o servidor de verdade.
 */
@Component
@ConditionalOnProperty(name = "seed.habilitado", havingValue = "true")
public class CargaInicial implements CommandLineRunner {

    /** Escreve mensagens no console, em vez de System.out.println. */
    private static final Logger log = LoggerFactory.getLogger(CargaInicial.class);

    /*
     * Injecao por construtor: o Spring cria esta classe e entrega os tres
     * objetos prontos. E a forma preferida - deixa claro do que a classe
     * depende e permite marcar os campos como final.
     */
    private final EmpresaRepository empresaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    /* Valores lidos do application.properties. */
    @Value("${seed.empresa.identificador}")
    private String empresaIdentificador;

    @Value("${seed.empresa.nome}")
    private String empresaNome;

    @Value("${seed.empresa.rotulo-cc-singular}")
    private String rotuloCcSingular;

    @Value("${seed.empresa.rotulo-cc-plural}")
    private String rotuloCcPlural;

    @Value("${seed.admin.login}")
    private String adminLogin;

    @Value("${seed.admin.nome}")
    private String adminNome;

    @Value("${seed.admin.senha}")
    private String adminSenha;

    public CargaInicial(EmpresaRepository empresaRepository,
                        UsuarioRepository usuarioRepository,
                        PasswordEncoder passwordEncoder) {
        this.empresaRepository = empresaRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * CommandLineRunner: o Spring Boot chama este metodo uma vez, logo depois
     * que a aplicacao termina de subir e o banco ja esta pronto.
     */
    @Override
    public void run(String... args) {

        /*
         * A carga e "idempotente": pode rodar quantas vezes for, que nao
         * duplica nada. Isso importa porque o metodo roda a CADA inicializacao
         * do sistema, e o DevTools reinicia varias vezes por dia.
         */
        if (empresaRepository.existsByIdentificador(empresaIdentificador)) {
            log.info("Carga inicial: empresa '{}' ja existe, nada a fazer.",
                    empresaIdentificador);
            return;
        }

        Empresa empresa = new Empresa(
                empresaNome, empresaIdentificador, rotuloCcSingular, rotuloCcPlural);
        empresa = empresaRepository.save(empresa);

        /*
         * passwordEncoder.encode() transforma "admin12345" em um hash de 60
         * caracteres parecido com "$2a$10$N9qo8uLO...". E o hash que vai para
         * a coluna senha - a senha digitada nunca e gravada (RN010).
         */
        Usuario admin = new Usuario(
                empresa,
                adminNome,
                adminLogin,
                passwordEncoder.encode(adminSenha),
                Perfil.ADMINISTRADOR);
        admin.setCargo("Administrador");
        admin.setAtivo(true);

        /*
         * false de proposito: este e o unico usuario que NAO cai na troca
         * obrigatoria de senha (RF07), porque ele e o ponto de entrada do
         * sistema. Os usuarios que ele criar depois nascem com true.
         */
        admin.setSenhaTemporaria(false);

        usuarioRepository.save(admin);

        log.info("=================================================");
        log.info(" Carga inicial concluida.");
        log.info(" Empresa .... {} (identificador: {})", empresaNome, empresaIdentificador);
        log.info(" Login ...... {}", adminLogin);
        log.info(" Senha ...... {}", adminSenha);
        log.info(" Troque essa senha antes de qualquer uso real.");
        log.info("=================================================");
    }
}
