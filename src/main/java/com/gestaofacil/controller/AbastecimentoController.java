package com.gestaofacil.controller;

import com.gestaofacil.controller.form.AbastecimentoForm;
import com.gestaofacil.controller.form.DecimalComVirgulaEditor;
import com.gestaofacil.model.StatusVeiculo;
import com.gestaofacil.model.Veiculo;
import com.gestaofacil.repository.VeiculoRepository;
import com.gestaofacil.security.UsuarioAutenticado;
import com.gestaofacil.service.AbastecimentoService;
import com.gestaofacil.service.LancamentoRecusadoException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Registro de abastecimento, tela do Motorista (#007).
 *
 * QUEM PODE ENTRAR AQUI
 * So o Motorista (#007-RN004): o SecurityConfig exige ROLE_MOTORISTA para
 * /abastecimentos/**, como faz com /saidas/**.
 *
 * DE ONDE VEM A EMPRESA E O MOTORISTA
 * Sempre de "logado", nunca da URL nem do formulario. Nenhuma rota tem {id}.
 *
 * AS DUAS ROTAS
 *   GET  /abastecimentos/novo  formulario
 *   POST /abastecimentos       grava e volta para /lancamentos
 */
@Controller
public class AbastecimentoController {

    private final VeiculoRepository veiculoRepository;
    private final AbastecimentoService abastecimentoService;

    public AbastecimentoController(VeiculoRepository veiculoRepository,
                                   AbastecimentoService abastecimentoService) {
        this.veiculoRepository = veiculoRepository;
        this.abastecimentoService = abastecimentoService;
    }

    /**
     * Faz os campos BigDecimal (litros e valor) aceitarem virgula. Vale so
     * para as rotas deste controller.
     */
    @InitBinder
    public void aceitarVirgula(WebDataBinder binder) {
        binder.registerCustomEditor(BigDecimal.class, new DecimalComVirgulaEditor());
    }

    /** Mostra o formulario, com a data de hoje ja preenchida. */
    @GetMapping("/abastecimentos/novo")
    public String novo(@AuthenticationPrincipal UsuarioAutenticado logado, Model model) {

        // So uma SUGESTAO editavel: a data informada (#007-RN005). A data de
        // registro e outra coisa e nao passa pela tela.
        AbastecimentoForm abastecimentoForm = new AbastecimentoForm();
        abastecimentoForm.setData(LocalDate.now());

        model.addAttribute("abastecimentoForm", abastecimentoForm);
        return prepararFormulario(model, logado);
    }

    /**
     * Recebe o formulario.
     *
     * @Valid confere os obrigatorios (#007-RN001). As demais regras sao do
     * AbastecimentoService; aqui a recusa so vira o aviso vermelho no alto
     * do formulario.
     */
    @PostMapping("/abastecimentos")
    public String registrar(@AuthenticationPrincipal UsuarioAutenticado logado,
                            @Valid @ModelAttribute AbastecimentoForm abastecimentoForm,
                            BindingResult resultado,
                            Model model,
                            RedirectAttributes atributos) {

        if (resultado.hasErrors()) {
            return prepararFormulario(model, logado);
        }

        // Regra 1: veiculo de outra empresa (ou que nao existe) responde 404,
        // igual a saida do #006. A caixa de selecao so tem os da empresa,
        // entao so chega aqui um id forjado.
        Veiculo veiculo = veiculoRepository
                .findByIdAndEmpresaId(abastecimentoForm.getVeiculoId(), logado.getEmpresaId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Veículo não encontrado."));

        try {
            abastecimentoService.registrar(abastecimentoForm, logado.getId(),
                    logado.getEmpresaId());

        } catch (LancamentoRecusadoException recusa) {
            model.addAttribute("erro", recusa.getMessage());
            return prepararFormulario(model, logado);
        }

        atributos.addFlashAttribute("mensagem",
                "Abastecimento registrado: " + veiculo.getNome() + " ("
                        + veiculo.getPlacaFormatada() + ").");
        return "redirect:/lancamentos";
    }

    /**
     * Coloca no modelo a caixa de selecao de veiculos e devolve o template.
     *
     * A lista ja sai filtrada do banco: todos os veiculos da empresa, menos
     * os INATIVOS. O EM_USO entra, porque e o que o motorista esta usando.
     */
    private String prepararFormulario(Model model, UsuarioAutenticado logado) {
        model.addAttribute("logado", logado);
        model.addAttribute("veiculos", veiculoRepository.findByEmpresaIdAndStatusNotOrderByNome(
                logado.getEmpresaId(), StatusVeiculo.INATIVO));
        return "abastecimentos/abastecimento";
    }
}
