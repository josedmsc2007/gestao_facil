package com.gestaofacil.controller;

import com.gestaofacil.controller.form.VeiculoForm;
import com.gestaofacil.model.TipoCombustivel;
import com.gestaofacil.model.Veiculo;
import com.gestaofacil.repository.VeiculoRepository;
import com.gestaofacil.security.UsuarioAutenticado;
import com.gestaofacil.service.FormatoDeVeiculo;
import com.gestaofacil.service.VeiculoService;
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

import java.time.Year;
import java.util.List;

/**
 * Cadastro, consulta e edicao de veiculos (#004).
 *
 * QUEM PODE ENTRAR AQUI
 * Ninguem confere o perfil dentro destes metodos: o SecurityConfig ja exige
 * ROLE_ADMINISTRADOR para /veiculos/** (#004-RN004), e uma regra escrita num
 * lugar so nao tem como ser esquecida num metodo novo.
 *
 * DE ONDE VEM A EMPRESA
 * Sempre de logado.getEmpresaId(), nunca da URL nem do formulario. Quando o
 * id do veiculo vem da URL, a busca e findByIdAndEmpresaId e a resposta para
 * quem nao e da empresa e 404 - e nao 403, que confirmaria a existencia do
 * registro em outra empresa.
 *
 * O QUE ESTE CONTROLLER NAO FAZ
 * Nao recebe status de lugar nenhum (#004-RN003). Nao existe metodo para
 * "mudar o status": existem Desativar e Ativar, cada um com a sua regra. As
 * transicoes para EM_USO e EM_MANUTENCAO virao dos cards #006 e #008, pelo
 * service, longe de qualquer formulario.
 */
@Controller
@RequestMapping("/veiculos")
public class VeiculoController {

    /**
     * Quantos anos a frente do ano atual o cadastro aceita (#004-RF01).
     *
     * E 1 porque a industria lanca o modelo do ano seguinte ainda neste ano:
     * em outubro de 2026 a concessionaria ja entrega um caminhao 2027. Mais
     * do que isso e quase sempre erro de digitacao.
     */
    private static final int ANOS_DE_FOLGA_NO_FUTURO = 1;

    private final VeiculoRepository veiculoRepository;
    private final VeiculoService veiculoService;

    public VeiculoController(VeiculoRepository veiculoRepository,
                             VeiculoService veiculoService) {
        this.veiculoRepository = veiculoRepository;
        this.veiculoService = veiculoService;
    }

    /* ==================================================================
       Consulta (#004-RF02)
       ================================================================== */

    /**
     * Lista os veiculos da empresa, em circulacao e inativos.
     *
     * O inativo continua aparecendo porque e daqui que o Administrador o
     * reativa - nada e apagado (regra 3 do projeto).
     */
    @GetMapping
    public String listar(@AuthenticationPrincipal UsuarioAutenticado logado, Model model) {

        model.addAttribute("logado", logado);
        model.addAttribute("veiculos",
                veiculoRepository.findByEmpresaIdOrderByNome(logado.getEmpresaId()));
        return "veiculos/lista";
    }

    /* ==================================================================
       Cadastro (#004-RF01)
       ================================================================== */

    /** Mostra o formulario em branco. */
    @GetMapping("/novo")
    public String novo(@AuthenticationPrincipal UsuarioAutenticado logado, Model model) {
        model.addAttribute("veiculoForm", new VeiculoForm());
        return prepararFormulario(model, logado, "Novo veículo");
    }

    /**
     * Recebe o formulario de cadastro.
     *
     * @Valid aplica as anotacoes do VeiculoForm (obrigatorios e tamanhos). O
     * BindingResult PRECISA vir logo depois do objeto validado - em outra
     * posicao o Spring lanca excecao em vez de preencher. As regras que uma
     * anotacao nao alcanca ficam em validarRegras.
     */
    @PostMapping
    public String cadastrar(@AuthenticationPrincipal UsuarioAutenticado logado,
                            @Valid @ModelAttribute VeiculoForm veiculoForm,
                            BindingResult resultado,
                            Model model,
                            RedirectAttributes atributos) {

        validarRegras(veiculoForm, resultado, logado.getEmpresaId(), null);

        if (resultado.hasErrors()) {
            return prepararFormulario(model, logado, "Novo veículo");
        }

        Veiculo veiculo = veiculoService.cadastrar(veiculoForm, logado.getEmpresaId());

        // #004-RN003: a mensagem diz o status em que o veiculo nasceu. E o
        // jeito de o Administrador entender que o sistema cuida disso - ele
        // nao vai procurar um campo que a tela "esqueceu" de mostrar.
        atributos.addFlashAttribute("mensagem",
                "Veículo " + veiculo.getNome() + " (" + veiculo.getPlacaFormatada()
                        + ") cadastrado como " + veiculo.getStatus().getRotulo() + ".");
        return "redirect:/veiculos";
    }

    /* ==================================================================
       Edicao (#004-RF02)
       ================================================================== */

    /** Mostra o formulario preenchido com os dados de um veiculo da empresa. */
    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id,
                         @AuthenticationPrincipal UsuarioAutenticado logado,
                         Model model) {

        Veiculo veiculo = buscarNaEmpresa(id, logado);

        model.addAttribute("veiculoForm", VeiculoForm.deVeiculo(veiculo));
        return prepararFormulario(model, logado, "Editar veículo");
    }

    /** Recebe o formulario de edicao. */
    @PostMapping("/{id}")
    public String salvarEdicao(@PathVariable Long id,
                               @AuthenticationPrincipal UsuarioAutenticado logado,
                               @Valid @ModelAttribute VeiculoForm veiculoForm,
                               BindingResult resultado,
                               Model model,
                               RedirectAttributes atributos) {

        // Confere ANTES de qualquer outra coisa que o veiculo e desta empresa.
        // Sem esta linha, um formulario montado a mao alcancaria o id de outra
        // empresa mesmo com todos os campos validos.
        Veiculo veiculo = buscarNaEmpresa(id, logado);

        // O id manda a tela enviar o formulario de volta para o endereco certo
        // quando ela for redesenhada com erros.
        veiculoForm.setId(id);

        validarRegras(veiculoForm, resultado, logado.getEmpresaId(), id);

        if (resultado.hasErrors()) {
            return prepararFormulario(model, logado, "Editar veículo");
        }

        veiculoService.editar(id, logado.getEmpresaId(), veiculoForm);

        atributos.addFlashAttribute("mensagem",
                "Dados de " + veiculo.getNome() + " atualizados.");
        return "redirect:/veiculos";
    }

    /* ==================================================================
       Ativacao e desativacao (#004-RF03)

       As duas sao POST, nunca link. Um link pode ser disparado por outro site
       ou por um pre-carregamento do navegador; o POST com token CSRF, nao.
       As duas comecam por buscarNaEmpresa, que responde 404 quando o id nao e
       da empresa de quem esta logado.
       ================================================================== */

    /** #004-RF03 e RN005: tira de circulacao, nunca exclui. */
    @PostMapping("/{id}/desativar")
    public String desativar(@PathVariable Long id,
                            @AuthenticationPrincipal UsuarioAutenticado logado,
                            RedirectAttributes atributos) {

        Veiculo veiculo = buscarNaEmpresa(id, logado);

        // O veiculo na rua ou na oficina nao pode ser desativado: ver o
        // porque em VeiculoService.podeSerDesativado. A mensagem explica o
        // motivo E o que fazer, como manda o padrao de acao recusada.
        if (!veiculoService.podeSerDesativado(veiculo)) {
            atributos.addFlashAttribute("erro",
                    "O veículo " + veiculo.getNome() + " está "
                            + veiculo.getStatus().getRotulo().toLowerCase()
                            + " e não pode ser desativado agora. Registre a devolução"
                            + " ou encerre a manutenção antes de tirá-lo de circulação.");
            return "redirect:/veiculos";
        }

        veiculoService.desativar(id, logado.getEmpresaId());

        atributos.addFlashAttribute("mensagem",
                veiculo.getNome() + " foi marcado como inativo e não aparece mais para"
                        + " saída. Os lançamentos antigos continuam no sistema.");
        return "redirect:/veiculos";
    }

    /**
     * #004-RF03: devolve o veiculo a circulacao, como DISPONIVEL.
     *
     * Nao tem regra nenhuma para conferir: reativar so aumenta o numero de
     * veiculos disponiveis.
     */
    @PostMapping("/{id}/ativar")
    public String ativar(@PathVariable Long id,
                         @AuthenticationPrincipal UsuarioAutenticado logado,
                         RedirectAttributes atributos) {

        Veiculo veiculo = buscarNaEmpresa(id, logado);
        veiculoService.ativar(id, logado.getEmpresaId());

        atributos.addFlashAttribute("mensagem",
                veiculo.getNome() + " voltou para a frota e está disponível.");
        return "redirect:/veiculos";
    }

    /* ==================================================================
       Regras que uma anotacao sozinha nao alcanca
       ================================================================== */

    /**
     * Regras que valem no cadastro E na edicao.
     *
     * Elas ficam aqui, e nao no VeiculoForm, porque dependem de coisas que a
     * anotacao nao enxerga: o ano de hoje ou uma consulta ao banco (placa e
     * RENAVAM ja usados NESTA empresa).
     *
     * @param idEmEdicao null no cadastro; na edicao, o id do proprio veiculo,
     *                   para ele nao ser acusado de duplicar a propria placa.
     */
    private void validarRegras(VeiculoForm form, BindingResult resultado,
                               Long empresaId, Long idEmEdicao) {

        // O piso (1900) esta no @Min do formulario; o teto depende do ano em
        // que o sistema estiver rodando, entao e conferido aqui.
        int anoMaximo = Year.now().getValue() + ANOS_DE_FOLGA_NO_FUTURO;
        if (form.getAno() != null && form.getAno() > anoMaximo) {
            resultado.rejectValue("ano", "ano.futuro",
                    "O ano não pode ser maior que " + anoMaximo + ".");
        }

        if (form.getPlaca() != null && !FormatoDeVeiculo.placaValida(form.getPlaca())) {
            resultado.rejectValue("placa", "placa.invalida",
                    "Placa inválida. Use ABC1234 ou ABC1D23.");

        } else if (form.getPlaca() != null && placaJaUsada(empresaId,
                FormatoDeVeiculo.placaNormalizada(form.getPlaca()), idEmEdicao)) {
            // #004-RN001: unica DENTRO da empresa. Em outra empresa a mesma
            // placa pode existir sem problema nenhum.
            resultado.rejectValue("placa", "placa.duplicada",
                    "Já existe um veículo com esta placa nesta empresa.");
        }

        if (form.getRenavam() != null && !FormatoDeVeiculo.renavamValido(form.getRenavam())) {
            resultado.rejectValue("renavam", "renavam.invalido",
                    "RENAVAM inválido. Informe os números do documento (até 11 dígitos).");

        } else if (form.getRenavam() != null && renavamJaUsado(empresaId,
                FormatoDeVeiculo.renavamNormalizado(form.getRenavam()), idEmEdicao)) {
            resultado.rejectValue("renavam", "renavam.duplicado",
                    "Já existe um veículo com este RENAVAM nesta empresa.");
        }
    }

    /* ==================================================================
       Auxiliares
       ================================================================== */

    /**
     * Busca o veiculo CONFERINDO A EMPRESA e responde 404 quando ele nao
     * existe para esta empresa (regra 1 do projeto).
     *
     * POR QUE 404 E NAO 403
     * O 403 (proibido) contaria que aquele id existe em algum lugar do
     * sistema - e daria para descobrir quantos veiculos cada cliente tem
     * testando ids em sequencia. Para esta empresa, o registro de outra
     * simplesmente nao existe.
     */
    private Veiculo buscarNaEmpresa(Long id, UsuarioAutenticado logado) {
        return veiculoRepository.findByIdAndEmpresaId(id, logado.getEmpresaId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Veículo não encontrado."));
    }

    private boolean placaJaUsada(Long empresaId, String placa, Long idEmEdicao) {
        if (idEmEdicao == null) {
            return veiculoRepository.existsByEmpresaIdAndPlaca(empresaId, placa);
        }
        return veiculoRepository.existsByEmpresaIdAndPlacaAndIdNot(empresaId, placa, idEmEdicao);
    }

    private boolean renavamJaUsado(Long empresaId, String renavam, Long idEmEdicao) {
        if (idEmEdicao == null) {
            return veiculoRepository.existsByEmpresaIdAndRenavam(empresaId, renavam);
        }
        return veiculoRepository.existsByEmpresaIdAndRenavamAndIdNot(
                empresaId, renavam, idEmEdicao);
    }

    /**
     * Coloca no modelo o que a tela de formulario precisa alem do proprio
     * formulario, e devolve o nome do template.
     *
     * Repare que NAO vai um "status" para a tela: o formulario nao tem esse
     * campo (#004-RN003).
     */
    private String prepararFormulario(Model model, UsuarioAutenticado logado, String titulo) {
        model.addAttribute("logado", logado);
        model.addAttribute("tituloDaTela", titulo);
        model.addAttribute("combustiveis", List.copyOf(TipoCombustivel.paraSelecao()));
        return "veiculos/formulario";
    }
}
