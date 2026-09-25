package com.gestaofacil.controller;

import com.gestaofacil.controller.form.CentroCustoForm;
import com.gestaofacil.model.CentroCusto;
import com.gestaofacil.repository.CentroCustoRepository;
import com.gestaofacil.security.UsuarioAutenticado;
import com.gestaofacil.service.CentroCustoService;
import com.gestaofacil.service.UnidadesFederativas;
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

/**
 * Cadastro, consulta e edicao de centros de custo (#005).
 *
 * QUEM PODE ENTRAR AQUI
 * Ninguem confere o perfil dentro destes metodos: o SecurityConfig ja exige
 * ROLE_ADMINISTRADOR para /centros-de-custo/** (#005-RN002), e uma regra
 * escrita num lugar so nao tem como ser esquecida num metodo novo.
 *
 * DE ONDE VEM A EMPRESA
 * Sempre de logado.getEmpresaId(), nunca da URL nem do formulario
 * (#005-RN003). Quando o id vem da URL, a busca e findByIdAndEmpresaId e a
 * resposta para quem nao e da empresa e 404 - e nao 403, que confirmaria a
 * existencia do registro em outra empresa.
 *
 * ================ A PALAVRA "OBRA" NAO APARECE AQUI ================
 * Regra 6 do projeto e #005-RN005. Todo texto que o funcionario le sai de
 * logado.getRotuloCcSingular() / getRotuloCcPlural(), que vem do cadastro da
 * empresa. Para a Construtora Teste isso da "Obra" e "Obras"; para uma
 * transportadora daria "Rota" e "Rotas", sem mexer numa linha deste arquivo.
 *
 * POR ISSO AS MENSAGENS NAO TEM GENERO. "Obra cadastrada" e "Contrato
 * cadastrado" nao cabem na mesma frase, e o sistema nao sabe o genero do
 * rotulo que a empresa escolheu. A saida foi falar do CADASTRO, que e
 * masculino e e uma palavra nossa: "Cadastro de X concluido". Se um dia
 * alguem escrever "cadastrada" numa destas mensagens, ela quebra para metade
 * dos clientes.
 * ===================================================================
 */
@Controller
@RequestMapping("/centros-de-custo")
public class CentroCustoController {

    private final CentroCustoRepository centroCustoRepository;
    private final CentroCustoService centroCustoService;

    public CentroCustoController(CentroCustoRepository centroCustoRepository,
                                 CentroCustoService centroCustoService) {
        this.centroCustoRepository = centroCustoRepository;
        this.centroCustoService = centroCustoService;
    }

    /* ==================================================================
       Consulta (#005-RF02)
       ================================================================== */

    /**
     * Lista os centros de custo da empresa, ativos e inativos.
     *
     * O inativo continua aparecendo porque e daqui que o Administrador o
     * reativa - nada e apagado (regra 3 do projeto).
     */
    @GetMapping
    public String listar(@AuthenticationPrincipal UsuarioAutenticado logado, Model model) {

        model.addAttribute("logado", logado);
        model.addAttribute("centrosDeCusto",
                centroCustoRepository.findByEmpresaIdOrderByNome(logado.getEmpresaId()));
        return "centros-de-custo/lista";
    }

    /* ==================================================================
       Cadastro (#005-RF01)
       ================================================================== */

    /** Mostra o formulario em branco. */
    @GetMapping("/novo")
    public String novo(@AuthenticationPrincipal UsuarioAutenticado logado, Model model) {
        model.addAttribute("centroCustoForm", new CentroCustoForm());
        return prepararFormulario(model, logado, "Cadastrar " + logado.getRotuloCcSingular());
    }

    /**
     * Recebe o formulario de cadastro.
     *
     * @Valid aplica as anotacoes do CentroCustoForm (obrigatorios e
     * tamanhos). O BindingResult PRECISA vir logo depois do objeto validado -
     * em outra posicao o Spring lanca excecao em vez de preencher. A unica
     * regra que uma anotacao nao alcanca e a sigla do estado, conferida em
     * validarRegras.
     */
    @PostMapping
    public String cadastrar(@AuthenticationPrincipal UsuarioAutenticado logado,
                            @Valid @ModelAttribute CentroCustoForm centroCustoForm,
                            BindingResult resultado,
                            Model model,
                            RedirectAttributes atributos) {

        validarRegras(centroCustoForm, resultado);

        if (resultado.hasErrors()) {
            return prepararFormulario(model, logado,
                    "Cadastrar " + logado.getRotuloCcSingular());
        }

        CentroCusto centroCusto =
                centroCustoService.cadastrar(centroCustoForm, logado.getEmpresaId());

        atributos.addFlashAttribute("mensagem",
                "Cadastro de " + centroCusto.getNome() + " concluído.");
        return "redirect:/centros-de-custo";
    }

    /* ==================================================================
       Edicao (#005-RF02)
       ================================================================== */

    /** Mostra o formulario preenchido com os dados de um registro da empresa. */
    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id,
                         @AuthenticationPrincipal UsuarioAutenticado logado,
                         Model model) {

        CentroCusto centroCusto = buscarNaEmpresa(id, logado);

        model.addAttribute("centroCustoForm", CentroCustoForm.deCentroCusto(centroCusto));
        return prepararFormulario(model, logado, "Editar " + logado.getRotuloCcSingular());
    }

    /** Recebe o formulario de edicao. */
    @PostMapping("/{id}")
    public String salvarEdicao(@PathVariable Long id,
                               @AuthenticationPrincipal UsuarioAutenticado logado,
                               @Valid @ModelAttribute CentroCustoForm centroCustoForm,
                               BindingResult resultado,
                               Model model,
                               RedirectAttributes atributos) {

        // Confere ANTES de qualquer outra coisa que o registro e desta
        // empresa. Sem esta linha, um formulario montado a mao alcancaria o id
        // de outra empresa mesmo com todos os campos validos.
        CentroCusto centroCusto = buscarNaEmpresa(id, logado);

        // O id manda a tela enviar o formulario de volta para o endereco certo
        // quando ela for redesenhada com erros.
        centroCustoForm.setId(id);

        validarRegras(centroCustoForm, resultado);

        if (resultado.hasErrors()) {
            return prepararFormulario(model, logado,
                    "Editar " + logado.getRotuloCcSingular());
        }

        centroCustoService.editar(id, logado.getEmpresaId(), centroCustoForm);

        atributos.addFlashAttribute("mensagem",
                "Dados de " + centroCusto.getNome() + " atualizados.");
        return "redirect:/centros-de-custo";
    }

    /* ==================================================================
       Ativacao e desativacao (#005-RF03)

       As duas sao POST, nunca link. Um link pode ser disparado por outro site
       ou por um pre-carregamento do navegador; o POST com token CSRF, nao.
       As duas comecam por buscarNaEmpresa, que responde 404 quando o id nao e
       da empresa de quem esta logado.
       ================================================================== */

    /** #005-RF03 e RN004: encerra, nunca exclui. */
    @PostMapping("/{id}/desativar")
    public String desativar(@PathVariable Long id,
                            @AuthenticationPrincipal UsuarioAutenticado logado,
                            RedirectAttributes atributos) {

        CentroCusto centroCusto = buscarNaEmpresa(id, logado);
        centroCustoService.desativar(id, logado.getEmpresaId());

        atributos.addFlashAttribute("mensagem",
                "Cadastro de " + centroCusto.getNome() + " marcado como inativo. Não"
                        + " aparece mais para novos lançamentos, e os lançamentos"
                        + " antigos continuam no sistema.");
        return "redirect:/centros-de-custo";
    }

    /** #005-RF03: devolve o registro as telas de lancamento. */
    @PostMapping("/{id}/ativar")
    public String ativar(@PathVariable Long id,
                         @AuthenticationPrincipal UsuarioAutenticado logado,
                         RedirectAttributes atributos) {

        CentroCusto centroCusto = buscarNaEmpresa(id, logado);
        centroCustoService.ativar(id, logado.getEmpresaId());

        atributos.addFlashAttribute("mensagem",
                "Cadastro de " + centroCusto.getNome() + " reativado.");
        return "redirect:/centros-de-custo";
    }

    /* ==================================================================
       Regras que uma anotacao sozinha nao alcanca
       ================================================================== */

    /**
     * A sigla do estado precisa existir (#005-RN001).
     *
     * Nao basta a tela oferecer so as 27 siglas na caixa de selecao: o que
     * chega no POST e texto vindo do navegador, e qualquer pessoa consegue
     * alterar isso. Esta conferencia e o que de fato impede um "XX" na coluna
     * estado - o mesmo raciocinio da lista de perfis do card #003.
     */
    private void validarRegras(CentroCustoForm form, BindingResult resultado) {
        if (form.getEstado() != null && !UnidadesFederativas.valida(form.getEstado())) {
            resultado.rejectValue("estado", "estado.invalido",
                    "Escolha um estado da lista.");
        }
    }

    /* ==================================================================
       Auxiliares
       ================================================================== */

    /**
     * Busca o centro de custo CONFERINDO A EMPRESA e responde 404 quando ele
     * nao existe para esta empresa (#005-RN003).
     *
     * POR QUE 404 E NAO 403
     * O 403 (proibido) contaria que aquele id existe em algum lugar do
     * sistema - e daria para descobrir quantas obras cada cliente tem
     * testando ids em sequencia. Para esta empresa, o registro de outra
     * simplesmente nao existe.
     */
    private CentroCusto buscarNaEmpresa(Long id, UsuarioAutenticado logado) {
        return centroCustoRepository.findByIdAndEmpresaId(id, logado.getEmpresaId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Centro de custo não encontrado."));
    }

    /**
     * Coloca no modelo o que a tela de formulario precisa alem do proprio
     * formulario, e devolve o nome do template.
     *
     * O titulo chega pronto de quem chamou, ja com o rotulo da empresa
     * ("Cadastrar Obra", "Editar Rota"). A lista de siglas vai para a caixa
     * de selecao do estado - e o servidor confere de novo o que voltar dela.
     */
    private String prepararFormulario(Model model, UsuarioAutenticado logado, String titulo) {
        model.addAttribute("logado", logado);
        model.addAttribute("tituloDaTela", titulo);
        model.addAttribute("estados", UnidadesFederativas.todas());
        return "centros-de-custo/formulario";
    }
}
