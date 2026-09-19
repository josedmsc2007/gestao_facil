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

import java.util.List;

/**
 * Carga inicial de dados (seed).
 *
 * POR QUE ISSO E NECESSARIO
 * -------------------------
 * O sistema so deixa entrar quem tem usuario e senha cadastrados, e todo
 * cadastro exige alguem ja logado. Isso e um problema do tipo "ovo e
 * galinha": com o banco vazio nao existe ninguem para fazer o primeiro login.
 *
 * Alem disso as senhas sao gravadas com hash BCrypt (RN010). Nao da para
 * simplesmente inserir uma linha na tabela pelo pgAdmin digitando a
 * senha - ela precisa passar pelo PasswordEncoder. Este arquivo faz isso.
 *
 * O QUE A CARGA CRIA - DUAS ETAPAS INDEPENDENTES
 * 1. A empresa reservada da equipe e as tres contas de Operador (#002-RN010 e
 *    RN011). E por elas que as empresas clientes passam a existir.
 * 2. A empresa "construtora-teste" com o usuario "admin", para demonstrar e
 *    testar as telas da empresa sem precisar cadastrar uma antes.
 *
 * Cada etapa confere SOZINHA se o que ela cria ja existe. Ate o card #002
 * havia uma conferencia so ("a construtora-teste existe? entao pare"). Se ela
 * tivesse ficado, o banco de voces - onde a construtora-teste ja existe -
 * nunca receberia os Operadores.
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

    /**
     * #002-RN011: uma conta de Operador para cada integrante da equipe.
     * Contas nominais, e nao uma "operador" compartilhada: e assim que o
     * sistema sabe QUEM cadastrou cada empresa.
     *
     * Ficam aqui, e nao no application.properties, por causa dos acentos: o
     * Spring le o .properties em ISO-8859-1, e "José" chegaria embaralhado.
     *
     * "record" e uma classe so de dados: o Java escreve o construtor e os
     * metodos login() e nome() sozinho.
     */
    private record ContaDeOperador(String login, String nome) {
    }

    private static final List<ContaDeOperador> OPERADORES = List.of(
            new ContaDeOperador("jose.lopes", "José A. Damasceno Lopes"),
            new ContaDeOperador("victor.ruan", "Victor Ruan"),
            new ContaDeOperador("leonardo.silva", "Leonardo Almeida Silva"));

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

    @Value("${seed.operador.senha}")
    private String operadorSenha;

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
     *
     * A carga e "idempotente": pode rodar quantas vezes for, que nao duplica
     * nada. Isso importa porque o metodo roda a CADA inicializacao do sistema,
     * e o DevTools reinicia varias vezes por dia.
     */
    @Override
    public void run(String... args) {
        garantirEquipeDoSistema();
        garantirEmpresaDeTeste();
    }

    /**
     * Etapa 1: a empresa reservada da equipe e os Operadores (#002).
     *
     * Confere cada Operador separadamente, e nao so a empresa: se um dia a
     * lista ganhar um integrante, a proxima inicializacao cria so ele.
     */
    private void garantirEquipeDoSistema() {

        Empresa equipe = empresaRepository.findByIdentificador(Empresa.IDENTIFICADOR_DA_EQUIPE)
                .orElseGet(() -> {
                    // Os rotulos sao obrigatorios na tabela, mas esta empresa
                    // nunca tera centro de custo: vai o nome generico.
                    Empresa nova = new Empresa("Equipe Gestão Fácil",
                            Empresa.IDENTIFICADOR_DA_EQUIPE,
                            "Centro de custo", "Centros de custo");
                    log.info(" Carga inicial: empresa reservada '{}' criada.",
                            Empresa.IDENTIFICADOR_DA_EQUIPE);
                    return empresaRepository.save(nova);
                });

        for (ContaDeOperador conta : OPERADORES) {
            if (usuarioRepository.existsByEmpresaIdAndLogin(equipe.getId(), conta.login())) {
                continue;
            }

            Usuario operador = new Usuario(
                    equipe,
                    conta.nome(),
                    conta.login(),
                    passwordEncoder.encode(operadorSenha),
                    Perfil.OPERADOR);
            operador.setAtivo(true);

            /*
             * true de proposito, ao contrario do "admin" la embaixo: a senha
             * da carga e a mesma para os tres e esta escrita no
             * application.properties. Cada integrante troca no primeiro
             * acesso e passa a ter uma senha que so ele conhece - sem isso a
             * conta nominal nao provaria quem fez o cadastro.
             */
            operador.setSenhaTemporaria(true);

            usuarioRepository.save(operador);
            log.info(" Carga inicial: Operador '{}' criado (entra em /{}/login).",
                    conta.login(), Empresa.IDENTIFICADOR_DA_EQUIPE);
        }
    }

    /** Etapa 2: a empresa de demonstracao e o seu administrador. */
    private void garantirEmpresaDeTeste() {

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
         * obrigatoria de senha (RF07), para a demonstracao das telas da
         * empresa comecar direto. Os usuarios que ele criar depois nascem
         * com true.
         *
         * Repare que esta empresa nasce com UM administrador, o que a tela de
         * cadastro de empresa nao permite (#002-RN006). E uma excecao da
         * carga de desenvolvimento; empresa de verdade nasce pela tela.
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
