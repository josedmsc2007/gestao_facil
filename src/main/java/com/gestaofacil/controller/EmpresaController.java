package com.gestaofacil.controller;

import com.gestaofacil.controller.form.EmpresaEdicaoForm;
import com.gestaofacil.controller.form.EmpresaForm;
import com.gestaofacil.model.Empresa;
import com.gestaofacil.model.Perfil;
import com.gestaofacil.model.Usuario;
import com.gestaofacil.repository.EmpresaRepository;
import com.gestaofacil.repository.UsuarioRepository;
import com.gestaofacil.security.UsuarioAutenticado;
import com.gestaofacil.service.EmpresaService;
import com.gestaofacil.service.EmpresaService.AdministradorCriado;
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

import java.util.List;

/**
 * Cadastro de empresa (#002) - as telas do Operador.
 *
 * QUEM PODE ENTRAR AQUI
 * So o perfil OPERADOR (#002-RN007). Quem garante e a linha
 * .requestMatchers("/empresas/**").hasRole("OPERADOR") do SecurityConfig,
 * pelo mesmo motivo do UsuarioController: uma regra escrita num lugar so nao
 * tem como ser esquecida num metodo novo.
 *
 * O OPERADOR E DIFERENTE DE TODO O RESTO DO SISTEMA
 * Nas outras telas a empresa vem de logado.getEmpresaId() e ninguem enxerga
 * fora dela. Aqui o trabalho e justamente mexer em OUTRAS empresas - so que
 * apenas no cadastro delas e no acesso dos administradores. O limite do
 * Operador nao e "a empresa dele", e sim "o que ele pode ver":
 * - nunca a empresa reservada da equipe (#002-RN010): toda busca por id usa
 *   findByIdAndIdentificadorNot, e responde 404 para ela;
 * - nunca dado operacional (#002-RN008): nenhum metodo daqui le veiculo,
 *   lancamento, motorista ou anexo, e o SecurityConfig barra as outras telas.
 */
@Controller
@RequestMapping("/empresas")
public class EmpresaController {

    private final EmpresaRepository empresaRepository;
    private final UsuarioRepository usuarioRepository;
    private final EmpresaService empresaService;

    public EmpresaController(EmpresaRepository empresaRepository,
                             UsuarioRepository usuarioRepository,
                             EmpresaService empresaService) {
        this.empresaRepository = empresaRepository;
        this.usuarioRepository = usuarioRepository;
        this.empresaService = empresaService;
    }

    /* ==================================================================
       Consulta (#002-RF07)
       ================================================================== */

    /** Lista as empresas clientes - a reservada da equipe fica de fora. */
    @GetMapping
    public String listar(@AuthenticationPrincipal UsuarioAutenticado logado, Model model) {
        model.addAttribute("logado", logado);
        model.addAttribute("empresas",
                empresaRepository.findByIdentificadorNotOrderByNome(Empresa.IDENTIFICADOR_DA_EQUIPE));
        return "empresas/lista";
    }

    /* ==================================================================
       Cadastro (#002-RF02 a RF05)
       ================================================================== */

    @GetMapping("/nova")
    public String nova(@AuthenticationPrincipal UsuarioAutenticado logado, Model model) {
        model.addAttribute("empresaForm", new EmpresaForm());
        model.addAttribute("logado", logado);
        return "empresas/formulario";
    }

    @PostMapping
    public String cadastrar(@AuthenticationPrincipal UsuarioAutenticado logado,
                            @Valid @ModelAttribute EmpresaForm empresaForm,
                            BindingResult resultado,
                            Model model,
                            RedirectAttributes atributos) {

        validarRegrasDoCadastro(empresaForm, resultado);

        if (resultado.hasErrors()) {
            model.addAttribute("logado", logado);
            return "empresas/formulario";
        }

        List<AdministradorCriado> administradores =
                empresaService.cadastrar(empresaForm, logado.getId(), logado.getEmpresaId());

        /*
         * #002-RF05: as senhas aparecem UMA vez, pelo mesmo mecanismo do
         * cadastro de usuarios - flash attribute, que some depois que a
         * proxima tela e desenhada. No banco ficou so o hash.
         */
        atributos.addFlashAttribute("mensagem",
                "Empresa " + empresaForm.getNome() + " cadastrada.");
        atributos.addFlashAttribute("identificadorCriado", empresaForm.getIdentificador());
        atributos.addFlashAttribute("administradoresCriados", administradores);

        return "redirect:/empresas";
    }

    /* ==================================================================
       Edicao (#002-RF07)
       ================================================================== */

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id,
                         @AuthenticationPrincipal UsuarioAutenticado logado,
                         Model model) {
        Empresa empresa = buscarCliente(id);

        model.addAttribute("empresaEdicaoForm", EmpresaEdicaoForm.deEmpresa(empresa));
        return prepararEdicao(model, logado, empresa);
    }

    /**
     * Recebe a edicao. O identificador nao esta no EmpresaEdicaoForm
     * (#002-RN003): mesmo que alguem o acrescente ao formulario pelo
     * navegador, nao ha campo para recebe-lo.
     */
    @PostMapping("/{id}")
    public String salvarEdicao(@PathVariable Long id,
                               @AuthenticationPrincipal UsuarioAutenticado logado,
                               @Valid @ModelAttribute EmpresaEdicaoForm empresaEdicaoForm,
                               BindingResult resultado,
                               Model model,
                               RedirectAttributes atributos) {

        Empresa empresa = buscarCliente(id);

        if (resultado.hasErrors()) {
            return prepararEdicao(model, logado, empresa);
        }

        empresaService.editar(id, empresaEdicaoForm);

        atributos.addFlashAttribute("mensagem",
                "Dados de " + empresaEdicaoForm.getNome() + " atualizados.");
        return "redirect:/empresas";
    }

    /* ==================================================================
       Situacao (#002-RF06). POST, nunca link: muda dados.
       ================================================================== */

    @PostMapping("/{id}/inativar")
    public String inativar(@PathVariable Long id, RedirectAttributes atributos) {
        Empresa empresa = buscarCliente(id);
        empresaService.inativar(id);

        atributos.addFlashAttribute("mensagem", empresa.getNome()
                + " foi inativada. Nenhum usuário dela consegue entrar até ser reativada.");
        return "redirect:/empresas";
    }

    @PostMapping("/{id}/ativar")
    public String ativar(@PathVariable Long id, RedirectAttributes atributos) {
        Empresa empresa = buscarCliente(id);
        empresaService.ativar(id);

        atributos.addFlashAttribute("mensagem", empresa.getNome() + " foi reativada.");
        return "redirect:/empresas";
    }

    /* ==================================================================
       Recuperacao de acesso (#002-RF08 e RN009)
       ================================================================== */

    /**
     * Os administradores ativos da empresa - e so eles. Os motoristas nem
     * saem do banco (#002-RN008): o perfil vai dentro da consulta.
     */
    @GetMapping("/{id}/administradores")
    public String administradores(@PathVariable Long id,
                                  @AuthenticationPrincipal UsuarioAutenticado logado,
                                  Model model) {
        Empresa empresa = buscarCliente(id);

        model.addAttribute("logado", logado);
        model.addAttribute("empresa", empresa);
        model.addAttribute("administradores", usuarioRepository
                .findByEmpresaIdAndPerfilAndAtivoTrueOrderByNome(empresa.getId(), Perfil.ADMINISTRADOR));
        return "empresas/administradores";
    }

    @PostMapping("/{id}/administradores/{usuarioId}/redefinir-senha")
    public String redefinirSenha(@PathVariable Long id,
                                 @PathVariable Long usuarioId,
                                 RedirectAttributes atributos) {

        Empresa empresa = buscarCliente(id);
        Usuario administrador = buscarAdministrador(empresa, usuarioId);

        String senhaTemporaria = empresaService.redefinirSenhaDeAdministrador(id, usuarioId);

        atributos.addFlashAttribute("mensagem",
                "Nova senha gerada para " + administrador.getNome() + ".");
        atributos.addFlashAttribute("loginDaSenha", administrador.getLogin());
        atributos.addFlashAttribute("senhaTemporaria", senhaTemporaria);
        return "redirect:/empresas/" + id + "/administradores";
    }

    /* ==================================================================
       Regras que uma anotacao sozinha nao alcanca
       ================================================================== */

    private void validarRegrasDoCadastro(EmpresaForm form, BindingResult resultado) {

        /*
         * #002-RF03: identificador em branco ganha uma sugestao a partir do
         * nome. Normalmente o JavaScript da tela ja preencheu enquanto o
         * Operador digitava; isto cobre quem apagou a sugestao ou esta com o
         * JavaScript desligado. A sugestao volta para a tela para ser
         * CONFERIDA - nunca e gravada sem o Operador ver, porque e o endereco
         * que ele vai entregar ao cliente.
         */
        if (form.getIdentificador() == null) {
            String sugestao = EmpresaService.sugerirIdentificador(form.getNome());
            if (sugestao.isEmpty()) {
                resultado.rejectValue("identificador", "identificador.obrigatorio",
                        "Informe o identificador de acesso.");
            } else {
                form.setIdentificador(sugestao);
                resultado.rejectValue("identificador", "identificador.sugerido",
                        "Sugerimos um identificador a partir do nome. Confira e salve de novo.");
            }

        } else if (EmpresaService.IDENTIFICADORES_PROIBIDOS.contains(form.getIdentificador())) {
            // #002-RN002: nomes de rotas do sistema quebrariam o endereco.
            resultado.rejectValue("identificador", "identificador.reservado",
                    "Este identificador é reservado pelo sistema. Escolha outro.");

        } else if (empresaRepository.existsByIdentificador(form.getIdentificador())) {
            // #002-RN002: unico no sistema INTEIRO - ao contrario do login,
            // que e unico so dentro da empresa. E ele que diz de qual
            // empresa e o endereco. O banco tambem garante (unique = true).
            resultado.rejectValue("identificador", "identificador.duplicado",
                    "Já existe uma empresa com este identificador.");
        }

        // #002-RN005: os dois logins nao podem ser iguais - o banco recusaria
        // o segundo (uk_usuario_empresa_login), e a empresa nasceria com um
        // administrador so. Nao ha o que conferir contra outras empresas: a
        // empresa e nova, entao ainda nao tem usuario nenhum.
        if (form.getLoginAdministrador1() != null
                && form.getLoginAdministrador1().equals(form.getLoginAdministrador2())) {
            resultado.rejectValue("loginAdministrador2", "login.repetido",
                    "Os dois administradores precisam de usuários de acesso diferentes.");
        }
    }

    /* ==================================================================
       Auxiliares
       ================================================================== */

    /**
     * Busca uma empresa CLIENTE e responde 404 para qualquer outro id -
     * inclusive o da empresa reservada da equipe (#002-RN010).
     */
    private Empresa buscarCliente(Long id) {
        return empresaRepository.findByIdAndIdentificadorNot(id, Empresa.IDENTIFICADOR_DA_EQUIPE)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Empresa não encontrada."));
    }

    /**
     * Busca um administrador ATIVO da empresa, e responde 404 para qualquer
     * outro - motorista, inativo ou usuario de outra empresa. O mesmo 404 para
     * todos os casos: a resposta nao revela se o id existe (#002-RN008).
     */
    private Usuario buscarAdministrador(Empresa empresa, Long usuarioId) {
        return usuarioRepository.findByIdAndEmpresaId(usuarioId, empresa.getId())
                .filter(usuario -> usuario.getPerfil() == Perfil.ADMINISTRADOR)
                .filter(Usuario::isAtivo)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Administrador não encontrado."));
    }

    private String prepararEdicao(Model model, UsuarioAutenticado logado, Empresa empresa) {
        model.addAttribute("logado", logado);
        model.addAttribute("empresa", empresa);
        return "empresas/editar";
    }
}
