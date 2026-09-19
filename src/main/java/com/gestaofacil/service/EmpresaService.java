package com.gestaofacil.service;

import com.gestaofacil.controller.form.EmpresaEdicaoForm;
import com.gestaofacil.controller.form.EmpresaForm;
import com.gestaofacil.model.Empresa;
import com.gestaofacil.model.Perfil;
import com.gestaofacil.model.Usuario;
import com.gestaofacil.repository.EmpresaRepository;
import com.gestaofacil.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Regras de negocio do cadastro de empresa (#002).
 *
 * Mesma divisao do cadastro de usuarios: o EmpresaController confere o
 * formulario e escolhe a tela; este service grava.
 *
 * A EMPRESA RESERVADA NUNCA E ALCANCADA DAQUI
 * Toda busca por id usa findByIdAndIdentificadorNot(id, IDENTIFICADOR_DA_EQUIPE).
 * E o equivalente, para o Operador, do findByIdAndEmpresaId dos outros
 * cadastros: um id que aponte para a empresa da equipe simplesmente nao e
 * encontrado (#002-RN010).
 */
@Service
public class EmpresaService {

    /**
     * Identificadores que uma empresa NAO pode ter (#002-RN002).
     *
     * O identificador vira o endereco /{identificador}. Se uma empresa se
     * chamasse "login", o atalho dela abriria a tela de login generica; se
     * fosse "css", abriria a pasta de estilos. Por isso ficam proibidos os
     * nomes das rotas do sistema - as que ja existem e as que o modelo de
     * dados preve para os proximos cards - e o da empresa da equipe.
     *
     * Quem criar uma rota nova de primeiro nivel deve acrescentar o nome aqui.
     */
    public static final Set<String> IDENTIFICADORES_PROIBIDOS = Set.of(
            Empresa.IDENTIFICADOR_DA_EQUIPE,
            "login", "logout", "trocar-senha", "error",
            "painel", "lancamentos", "usuarios", "empresas",
            "css", "js", "imagens",
            "veiculos", "centros-de-custo", "abastecimentos", "manutencoes", "relatorios");

    /** Tamanho da coluna empresa.identificador. */
    private static final int TAMANHO_MAXIMO_DO_IDENTIFICADOR = 60;

    private final EmpresaRepository empresaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final GeradorDeSenhaTemporaria geradorDeSenha;
    private final UsuarioService usuarioService;

    public EmpresaService(EmpresaRepository empresaRepository,
                          UsuarioRepository usuarioRepository,
                          PasswordEncoder passwordEncoder,
                          GeradorDeSenhaTemporaria geradorDeSenha,
                          UsuarioService usuarioService) {
        this.empresaRepository = empresaRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.geradorDeSenha = geradorDeSenha;
        this.usuarioService = usuarioService;
    }

    /**
     * O login e a senha temporaria de um administrador recem-criado, para a
     * tela mostrar UMA vez (#002-RF05).
     *
     * "record" e uma classe so de dados: o Java escreve sozinho o construtor
     * e os metodos nome(), login() e senha(). Serializable porque ela viaja
     * como flash attribute, que fica guardado na sessao ate a proxima tela.
     */
    public record AdministradorCriado(String nome, String login, String senha)
            implements Serializable {
    }

    /**
     * Sugere um identificador a partir do nome (#002-RF03).
     *
     * "Construtora São João Ltda." vira "construtora-sao-joao-ltda":
     * 1. Normalizer separa cada letra do seu acento ("ã" vira "a" + "~"), e a
     *    expressao \p{M} apaga os acentos que ficaram soltos;
     * 2. tudo em minusculas;
     * 3. cada sequencia de caracteres que nao seja letra ou numero vira UM
     *    hifen, e os hifens das pontas saem.
     *
     * A MESMA REGRA EXISTE EM JAVASCRIPT, em static/js/cadastro-empresa.js,
     * para sugerir enquanto o Operador digita. Esta aqui e usada quando o
     * formulario chega com o identificador em branco (JavaScript desligado,
     * ou o Operador apagou a sugestao). Se mudar uma, mude a outra.
     */
    public static String sugerirIdentificador(String nome) {
        if (nome == null) {
            return "";
        }
        String semAcentos = Normalizer.normalize(nome, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        String sugestao = semAcentos.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");

        if (sugestao.length() > TAMANHO_MAXIMO_DO_IDENTIFICADOR) {
            sugestao = sugestao.substring(0, TAMANHO_MAXIMO_DO_IDENTIFICADOR)
                    .replaceAll("-+$", "");
        }
        return sugestao;
    }

    /**
     * Cria a empresa e os seus dois administradores, tudo junto (#002-RF02 e
     * RF05).
     *
     * @Transactional: ou nasce a empresa COM os dois administradores, ou nao
     * nasce nada. Se a gravacao do segundo falhasse, uma empresa com um
     * administrador so - exatamente o que a RN006 proibe - nao fica para tras.
     *
     * @param operadorId      quem esta cadastrando, tirado da sessao
     * @param empresaDoOperador a empresa da sessao (a reservada da equipe),
     *                          usada para buscar o Operador conferindo a
     *                          empresa, como manda a regra 1 do projeto
     * @return os dois administradores com as senhas em texto legivel, para a
     *         tela mostrar uma vez. No banco fica so o hash.
     */
    @Transactional
    public List<AdministradorCriado> cadastrar(EmpresaForm form,
                                               Long operadorId,
                                               Long empresaDoOperador) {

        Usuario operador = usuarioRepository.findByIdAndEmpresaId(operadorId, empresaDoOperador)
                .orElseThrow(() -> new IllegalArgumentException("Operador não encontrado."));

        Empresa empresa = new Empresa(
                form.getNome(),
                form.getIdentificador(),
                form.getRotuloCcSingular(),
                form.getRotuloCcPlural());
        empresa.setAtiva(true);
        empresa.setDataCadastro(LocalDateTime.now());
        empresa.setCadastradaPor(operador);
        empresa = empresaRepository.save(empresa);

        return List.of(
                criarAdministrador(empresa, form.getNomeAdministrador1(), form.getLoginAdministrador1()),
                criarAdministrador(empresa, form.getNomeAdministrador2(), form.getLoginAdministrador2()));
    }

    /**
     * Altera nome e rotulos (#002-RF07). O identificador nao e tocado
     * (#002-RN003) - e o EmpresaEdicaoForm nem tem esse campo.
     */
    @Transactional
    public void editar(Long empresaId, EmpresaEdicaoForm form) {
        Empresa empresa = buscarCliente(empresaId);
        empresa.setNome(form.getNome());
        empresa.setRotuloCcSingular(form.getRotuloCcSingular());
        empresa.setRotuloCcPlural(form.getRotuloCcPlural());
        empresaRepository.save(empresa);
    }

    /**
     * #002-RF06: desliga a empresa. A partir daqui nenhum usuario dela entra
     * (#002-RN013) - quem confere e o AutenticacaoPorEmpresaProvider.
     * Nada e apagado (regra 3): os dados continuam la, esperando uma
     * eventual reativacao.
     */
    @Transactional
    public void inativar(Long empresaId) {
        Empresa empresa = buscarCliente(empresaId);
        empresa.setAtiva(false);
        empresaRepository.save(empresa);
    }

    @Transactional
    public void ativar(Long empresaId) {
        Empresa empresa = buscarCliente(empresaId);
        empresa.setAtiva(true);
        empresaRepository.save(empresa);
    }

    /**
     * #002-RF08: nova senha temporaria para um ADMINISTRADOR de uma empresa
     * cliente - o socorro de quando a construtora perdeu o acesso aos dois.
     *
     * Reaproveita o UsuarioService.redefinirSenha do card #003, que ja sabe
     * gerar a senha, gravar o hash, marcar a troca obrigatoria e desbloquear
     * a conta. O que este metodo acrescenta sao as duas conferencias que o
     * Operador exige:
     * - a empresa e cliente (nunca a reservada, onde estao os outros
     *   Operadores);
     * - o usuario e um administrador ATIVO daquela empresa. Motorista nao:
     *   o Operador nao mexe nos funcionarios da empresa (#002-RN008), e a
     *   senha do motorista e assunto do administrador dela.
     *
     * @return a nova senha em texto legivel, para ser exibida uma vez.
     */
    @Transactional
    public String redefinirSenhaDeAdministrador(Long empresaId, Long usuarioId) {
        Empresa empresa = buscarCliente(empresaId);

        Usuario usuario = usuarioRepository.findByIdAndEmpresaId(usuarioId, empresa.getId())
                .filter(encontrado -> encontrado.getPerfil() == Perfil.ADMINISTRADOR)
                .filter(Usuario::isAtivo)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Administrador não encontrado nesta empresa."));

        return usuarioService.redefinirSenha(usuario.getId(), empresa.getId());
    }

    /**
     * A busca que todo metodo desta classe usa: empresa CLIENTE pelo id.
     * A empresa reservada da equipe nunca volta daqui.
     */
    private Empresa buscarCliente(Long empresaId) {
        return empresaRepository.findByIdAndIdentificadorNot(empresaId, Empresa.IDENTIFICADOR_DA_EQUIPE)
                .orElseThrow(() -> new IllegalArgumentException("Empresa não encontrada."));
    }

    /**
     * Cria um administrador com senha temporaria - o mesmo nascimento de um
     * usuario cadastrado pelo #003: ativo, obrigado a trocar a senha no
     * primeiro acesso (#001-RF07) e com apenas o hash no banco (#003-RN011).
     */
    private AdministradorCriado criarAdministrador(Empresa empresa, String nome, String login) {
        String senhaTemporaria = geradorDeSenha.gerar();

        Usuario administrador = new Usuario(
                empresa, nome, login,
                passwordEncoder.encode(senhaTemporaria),
                Perfil.ADMINISTRADOR);
        administrador.setAtivo(true);
        administrador.setSenhaTemporaria(true);
        usuarioRepository.save(administrador);

        return new AdministradorCriado(nome, login, senhaTemporaria);
    }
}
