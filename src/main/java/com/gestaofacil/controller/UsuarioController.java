package com.gestaofacil.controller;

import com.gestaofacil.controller.form.UsuarioForm;
import com.gestaofacil.model.Perfil;
import com.gestaofacil.model.Usuario;
import com.gestaofacil.repository.UsuarioRepository;
import com.gestaofacil.security.UsuarioAutenticado;
import com.gestaofacil.service.ControleDeTentativasService;
import com.gestaofacil.service.UsuarioService;
import com.gestaofacil.service.ValidadorDeCpf;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Cadastro, consulta e edicao de usuarios (#003).
 *
 * QUEM PODE ENTRAR AQUI
 * Ninguem confere o perfil dentro destes metodos: o SecurityConfig ja exige
 * ROLE_ADMINISTRADOR para /usuarios/**, e uma regra escrita num lugar so nao
 * tem como ser esquecida num metodo novo (#003-RN008).
 *
 * DE ONDE VEM A EMPRESA
 * Sempre de logado.getEmpresaId(), nunca da URL nem do formulario. Todas as
 * consultas deste arquivo levam esse id junto (#003-RN009). Quando o id do
 * usuario vem da URL, a busca e findByIdAndEmpresaId e a resposta para quem
 * nao e da empresa e 404 - e nao 403, que confirmaria a existencia do
 * registro em outra empresa.
 */
@Controller
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioService usuarioService;
    private final ControleDeTentativasService controleDeTentativas;

    public UsuarioController(UsuarioRepository usuarioRepository,
                             UsuarioService usuarioService,
                             ControleDeTentativasService controleDeTentativas) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioService = usuarioService;
        this.controleDeTentativas = controleDeTentativas;
    }

    /* ==================================================================
       Consulta (#003-RF08)
       ================================================================== */

    /**
     * Lista os usuarios da empresa, ativos e inativos.
     *
     * Alem da lista, manda para a tela os ids das contas bloqueadas AGORA
     * (#003-RF06). A consulta e a mesma que a tela provisoria de contas
     * bloqueadas usava - ela nao compara so "bloqueado_ate esta preenchido",
     * e sim "bloqueado_ate ainda nao passou": um bloqueio vencido ja nao
     * precisa da acao do Administrador, porque a conta abriu sozinha.
     */
    @GetMapping
    public String listar(@AuthenticationPrincipal UsuarioAutenticado logado, Model model) {

        Set<Long> bloqueados = usuarioRepository
                .findByEmpresaIdAndBloqueadoAteAfterOrderByNome(
                        logado.getEmpresaId(), LocalDateTime.now())
                .stream()
                .map(Usuario::getId)
                .collect(Collectors.toSet());

        model.addAttribute("logado", logado);
        model.addAttribute("usuarios",
                usuarioRepository.findByEmpresaIdOrderByNome(logado.getEmpresaId()));
        model.addAttribute("bloqueados", bloqueados);
        return "usuarios/lista";
    }

    /* ==================================================================
       Cadastro (#003-RF01)
       ================================================================== */

    /** Mostra o formulario em branco. */
    @GetMapping("/novo")
    public String novo(@AuthenticationPrincipal UsuarioAutenticado logado, Model model) {
        model.addAttribute("usuarioForm", new UsuarioForm());
        return prepararFormulario(model, logado, "Novo usuário");
    }

    /**
     * Recebe o formulario de cadastro.
     *
     * @Valid aplica as anotacoes do UsuarioForm (obrigatorios, tamanhos,
     * formato do login). O BindingResult PRECISA vir logo depois do objeto
     * validado - em outra posicao o Spring lanca excecao em vez de preencher.
     * As regras que uma anotacao nao alcanca ficam em validarRegrasDoCadastro.
     */
    @PostMapping
    public String cadastrar(@AuthenticationPrincipal UsuarioAutenticado logado,
                            @Valid @ModelAttribute UsuarioForm usuarioForm,
                            BindingResult resultado,
                            Model model,
                            RedirectAttributes atributos) {

        validarRegrasDoCadastro(usuarioForm, resultado, logado.getEmpresaId(), null);

        if (resultado.hasErrors()) {
            return prepararFormulario(model, logado, "Novo usuário");
        }

        String senhaTemporaria = usuarioService.cadastrar(usuarioForm, logado.getEmpresaId());

        /*
         * #003-RF04 e RN011: a senha aparece UMA vez.
         *
         * addFlashAttribute guarda o valor na sessao so ate a proxima tela ser
         * desenhada, e o descarta em seguida. Se o Administrador recarregar a
         * pagina, a senha ja nao esta mais la - e nao existe tela nenhuma capaz
         * de mostra-la de novo, porque no banco ficou apenas o hash. A saida,
         * nesse caso, e gerar outra senha (#003-RF05).
         */
        atributos.addFlashAttribute("mensagem", "Usuário cadastrado.");
        atributos.addFlashAttribute("loginDaSenha", usuarioForm.getLogin());
        atributos.addFlashAttribute("senhaTemporaria", senhaTemporaria);

        return "redirect:/usuarios";
    }

    /* ==================================================================
       Edicao (#003-RF08)
       ================================================================== */

    /** Mostra o formulario preenchido com os dados de um usuario da empresa. */
    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id,
                         @AuthenticationPrincipal UsuarioAutenticado logado,
                         Model model) {

        Usuario usuario = buscarNaEmpresa(id, logado);

        model.addAttribute("usuarioForm", UsuarioForm.deUsuario(usuario));
        return prepararFormulario(model, logado, "Editar usuário");
    }

    /** Recebe o formulario de edicao. */
    @PostMapping("/{id}")
    public String salvarEdicao(@PathVariable Long id,
                               @AuthenticationPrincipal UsuarioAutenticado logado,
                               @Valid @ModelAttribute UsuarioForm usuarioForm,
                               BindingResult resultado,
                               Model model,
                               RedirectAttributes atributos) {

        // Confere ANTES de qualquer outra coisa que o usuario e desta empresa.
        // Sem esta linha, um formulario montado a mao alcancaria o id de outra
        // empresa mesmo com todos os campos validos.
        Usuario usuario = buscarNaEmpresa(id, logado);

        // O id manda a tela enviar o formulario de volta para o endereco certo
        // quando ela for redesenhada com erros.
        usuarioForm.setId(id);

        validarRegrasDoCadastro(usuarioForm, resultado, logado.getEmpresaId(), id);
        validarRegrasDaEdicao(usuarioForm, resultado, logado, usuario);

        if (resultado.hasErrors()) {
            return prepararFormulario(model, logado, "Editar usuário");
        }

        usuarioService.editar(id, logado.getEmpresaId(), usuarioForm);

        atributos.addFlashAttribute("mensagem",
                "Dados de " + usuario.getNome() + " atualizados.");
        return "redirect:/usuarios";
    }

    /* ==================================================================
       Regras de conta (#003-RF05, RF06 e RF07)

       Todas sao POST, nunca link. Um link pode ser disparado por outro site
       ou por um pre-carregamento do navegador; o POST com token CSRF, nao.
       Todas comecam por buscarNaEmpresa, que responde 404 quando o id nao e
       da empresa de quem esta logado.
       ================================================================== */

    /** #003-RF05: gera uma nova senha temporaria para quem esqueceu a senha. */
    @PostMapping("/{id}/redefinir-senha")
    public String redefinirSenha(@PathVariable Long id,
                                 @AuthenticationPrincipal UsuarioAutenticado logado,
                                 RedirectAttributes atributos) {

        Usuario usuario = buscarNaEmpresa(id, logado);

        String senhaTemporaria = usuarioService.redefinirSenha(id, logado.getEmpresaId());

        atributos.addFlashAttribute("mensagem",
                "Nova senha gerada para " + usuario.getNome() + ".");
        atributos.addFlashAttribute("loginDaSenha", usuario.getLogin());
        atributos.addFlashAttribute("senhaTemporaria", senhaTemporaria);

        return "redirect:/usuarios";
    }

    /**
     * #003-RF06: libera a conta fechada por tentativas invalidas, sem esperar
     * os 30 minutos (#001-RN005).
     *
     * Ate a etapa 1 deste card isto vivia numa tela so para ele,
     * /usuarios/bloqueados, criada como provisoria no card #001. O endereco
     * da acao continua o mesmo; o que mudou e que agora ela e um botao na
     * propria lista de usuarios, onde o Administrador ja esta.
     */
    @PostMapping("/{id}/desbloquear")
    public String desbloquear(@PathVariable Long id,
                              @AuthenticationPrincipal UsuarioAutenticado logado,
                              RedirectAttributes atributos) {

        Usuario usuario = buscarNaEmpresa(id, logado);

        // O service devolve false quando o usuario nao e da empresa; aqui isso
        // ja foi conferido na linha acima, entao o retorno nao acrescenta nada.
        controleDeTentativas.desbloquear(id, logado.getEmpresaId());

        atributos.addFlashAttribute("mensagem",
                "Conta de " + usuario.getNome() + " liberada.");
        return "redirect:/usuarios";
    }

    /** #003-RF07 e RN010: desliga o acesso sem apagar o registro. */
    @PostMapping("/{id}/desativar")
    public String desativar(@PathVariable Long id,
                            @AuthenticationPrincipal UsuarioAutenticado logado,
                            RedirectAttributes atributos) {

        Usuario usuario = buscarNaEmpresa(id, logado);

        // #003-RN014: nao a si mesmo. O Administrador que se desativa sai do
        // sistema no clique seguinte e nao tem como voltar sozinho.
        if (usuario.getId().equals(logado.getId())) {
            atributos.addFlashAttribute("erro",
                    "Você não pode desativar o seu próprio usuário. "
                            + "Peça a outro administrador da empresa.");
            return "redirect:/usuarios";
        }

        // #003-RN015: a empresa nao pode ficar com menos de dois administradores.
        if (!usuarioService.podeDeixarDeSerAdministradorAtivo(logado.getEmpresaId(), usuario)) {
            atributos.addFlashAttribute("erro", mensagemDosDoisAdministradores(
                    "desativar " + usuario.getNome()));
            return "redirect:/usuarios";
        }

        usuarioService.desativar(id, logado.getEmpresaId());

        atributos.addFlashAttribute("mensagem",
                usuario.getNome() + " foi desativado e não consegue mais entrar. "
                        + "Os lançamentos antigos continuam no sistema.");
        return "redirect:/usuarios";
    }

    /**
     * #003-RF07: devolve o acesso a quem estava desativado.
     *
     * Nao tem regra nenhuma para conferir: ativar so aumenta o numero de
     * usuarios com acesso, e nunca deixa a empresa sem administrador.
     */
    @PostMapping("/{id}/ativar")
    public String ativar(@PathVariable Long id,
                         @AuthenticationPrincipal UsuarioAutenticado logado,
                         RedirectAttributes atributos) {

        Usuario usuario = buscarNaEmpresa(id, logado);
        usuarioService.ativar(id, logado.getEmpresaId());

        atributos.addFlashAttribute("mensagem", usuario.getNome() + " foi reativado.");
        return "redirect:/usuarios";
    }

    /* ==================================================================
       Regras que uma anotacao sozinha nao alcanca
       ================================================================== */

    /**
     * Regras que valem no cadastro E na edicao.
     *
     * Elas ficam aqui, e nao no UsuarioForm, porque dependem de coisas que a
     * anotacao nao enxerga: outro campo do mesmo formulario (a CNH depende do
     * perfil) ou uma consulta ao banco (CPF e login ja usados).
     *
     * @param idEmEdicao null no cadastro; na edicao, o id do proprio usuario,
     *                   para ele nao ser acusado de duplicar o proprio CPF.
     */
    private void validarRegrasDoCadastro(UsuarioForm form, BindingResult resultado,
                                         Long empresaId, Long idEmEdicao) {

        /*
         * #003-RN007: o perfil recebido precisa estar na lista que a tela
         * oferece. Nao basta montar a caixa de selecao com os perfis certos -
         * o que chega no POST e texto vindo do navegador, e qualquer pessoa
         * consegue alterar isso. Esta conferencia e o que de fato impede um
         * usuario com perfil que a empresa nao pode atribuir.
         */
        if (form.getPerfil() != null
                && !Perfil.atribuiveisPelaEmpresa().contains(form.getPerfil())) {
            resultado.rejectValue("perfil", "perfil.naoAtribuivel", "Perfil inválido.");
        }

        if (form.getCpf() != null && !ValidadorDeCpf.valido(form.getCpf())) {
            // #003-RN006: CPF valido pelos digitos verificadores.
            resultado.rejectValue("cpf", "cpf.invalido",
                    "CPF inválido. Confira os números digitados.");

        } else if (form.getCpf() != null && cpfJaUsado(empresaId,
                ValidadorDeCpf.somenteNumeros(form.getCpf()), idEmEdicao)) {
            // #003-RN004: unico DENTRO da empresa. Em outra empresa o mesmo
            // CPF pode existir sem problema nenhum.
            resultado.rejectValue("cpf", "cpf.duplicado",
                    "Já existe um funcionário com este CPF nesta empresa.");
        }

        // #003-RN005: login unico dentro da empresa.
        if (form.getLogin() != null && loginJaUsado(empresaId, form.getLogin(), idEmEdicao)) {
            resultado.rejectValue("login", "login.duplicado",
                    "Este usuário de acesso já está em uso nesta empresa.");
        }

        // #003-RN002: a habilitacao so e obrigatoria para o motorista.
        if (form.getPerfil() == Perfil.MOTORISTA) {
            if (form.getCnh() == null) {
                resultado.rejectValue("cnh", "cnh.obrigatoria",
                        "Informe o número da CNH do motorista.");
            }
            if (form.getCategoriaCnh() == null) {
                resultado.rejectValue("categoriaCnh", "categoriaCnh.obrigatoria",
                        "Informe a categoria da CNH.");
            }
            if (form.getValidadeCnh() == null) {
                resultado.rejectValue("validadeCnh", "validadeCnh.obrigatoria",
                        "Informe a validade da CNH.");
            }
        }
    }

    /**
     * Regras que so existem na edicao.
     *
     * #003-RN014: o Administrador nao rebaixa o proprio usuario. A conta de
     * quem esta mexendo na tela nao pode ser a porta que se fecha por dentro:
     * um administrador que virasse motorista por engano perderia o acesso ao
     * cadastro e dependeria de OUTRO administrador para voltar atras - e a
     * empresa pode nao ter outro naquele momento.
     *
     * #003-RN015: e a empresa nao pode ficar com menos de dois administradores
     * ativos. Rebaixar um administrador para motorista tira um do grupo tanto
     * quanto desativa-lo, entao a mesma pergunta vale nos dois lugares.
     */
    private void validarRegrasDaEdicao(UsuarioForm form, BindingResult resultado,
                                       UsuarioAutenticado logado, Usuario usuario) {

        boolean editandoASiMesmo = usuario.getId().equals(logado.getId());
        boolean estaRebaixando = form.getPerfil() != null
                && form.getPerfil() != Perfil.ADMINISTRADOR;

        if (editandoASiMesmo && estaRebaixando) {
            resultado.rejectValue("perfil", "perfil.autoRebaixamento",
                    "Você não pode alterar o seu próprio perfil de acesso. "
                            + "Peça a outro administrador da empresa.");
            return;
        }

        if (estaRebaixando && !usuarioService.podeDeixarDeSerAdministradorAtivo(
                logado.getEmpresaId(), usuario)) {
            resultado.rejectValue("perfil", "perfil.doisAdministradores",
                    mensagemDosDoisAdministradores(
                            "mudar o perfil de " + usuario.getNome()));
        }
    }

    /**
     * A mensagem da #003-RN015, usada pela edicao e pela desativacao.
     *
     * A regra manda EXPLICAR o motivo da recusa: sem isso o Administrador
     * tentaria de novo achando que o sistema falhou. A mensagem diz tambem o
     * que fazer para conseguir - cadastrar ou ativar outro administrador.
     */
    private String mensagemDosDoisAdministradores(String acaoRecusada) {
        return "A empresa precisa de pelo menos "
                + UsuarioService.MINIMO_DE_ADMINISTRADORES
                + " administradores ativos. Cadastre ou ative outro administrador"
                + " antes de " + acaoRecusada + ".";
    }

    /* ==================================================================
       Auxiliares
       ================================================================== */

    /**
     * Busca o usuario CONFERINDO A EMPRESA e responde 404 quando ele nao
     * existe para esta empresa (#003-RN009).
     *
     * POR QUE 404 E NAO 403
     * O 403 ("proibido") contaria que aquele id existe em algum lugar do
     * sistema - e daria para descobrir quantos usuarios cada cliente tem
     * testando ids em sequencia. Para esta empresa, o registro de outra
     * simplesmente nao existe.
     */
    private Usuario buscarNaEmpresa(Long id, UsuarioAutenticado logado) {
        return usuarioRepository.findByIdAndEmpresaId(id, logado.getEmpresaId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Usuário não encontrado."));
    }

    private boolean cpfJaUsado(Long empresaId, String cpf, Long idEmEdicao) {
        if (idEmEdicao == null) {
            return usuarioRepository.existsByEmpresaIdAndCpf(empresaId, cpf);
        }
        return usuarioRepository.existsByEmpresaIdAndCpfAndIdNot(empresaId, cpf, idEmEdicao);
    }

    private boolean loginJaUsado(Long empresaId, String login, Long idEmEdicao) {
        if (idEmEdicao == null) {
            return usuarioRepository.existsByEmpresaIdAndLogin(empresaId, login);
        }
        return usuarioRepository.existsByEmpresaIdAndLoginAndIdNot(empresaId, login, idEmEdicao);
    }

    /**
     * Coloca no modelo o que a tela de formulario precisa alem do proprio
     * formulario, e devolve o nome do template.
     *
     * A LISTA DE PERFIS VEM DE Perfil.atribuiveisPelaEmpresa(), NAO DE
     * Perfil.values(). O card #002 vai acrescentar o perfil OPERADOR ao enum;
     * com values(), naquele dia ele apareceria sozinho nesta caixa de selecao
     * e a RN007 seria quebrada sem ninguem perceber.
     */
    private String prepararFormulario(Model model, UsuarioAutenticado logado, String titulo) {
        model.addAttribute("logado", logado);
        model.addAttribute("tituloDaTela", titulo);
        model.addAttribute("perfis", List.copyOf(Perfil.atribuiveisPelaEmpresa()));
        return "usuarios/formulario";
    }
}
