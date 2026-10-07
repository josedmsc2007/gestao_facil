package com.gestaofacil.controller;

import com.gestaofacil.controller.form.DevolucaoForm;
import com.gestaofacil.controller.form.SaidaForm;
import com.gestaofacil.model.StatusVeiculo;
import com.gestaofacil.model.UsoVeiculo;
import com.gestaofacil.model.Veiculo;
import com.gestaofacil.repository.CentroCustoRepository;
import com.gestaofacil.repository.UsoVeiculoRepository;
import com.gestaofacil.repository.VeiculoRepository;
import com.gestaofacil.security.UsuarioAutenticado;
import com.gestaofacil.service.LancamentoRecusadoException;
import com.gestaofacil.service.UsoVeiculoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * Saida e devolucao de veiculos, as telas do Motorista (#006).
 *
 * QUEM PODE ENTRAR AQUI
 * So o Motorista (#006-RN004): o SecurityConfig exige ROLE_MOTORISTA para
 * /lancamentos e para /saidas/**. Ninguem confere o perfil dentro destes
 * metodos.
 *
 * DE ONDE VEM A EMPRESA E O MOTORISTA
 * Sempre de "logado" - logado.getEmpresaId() e logado.getId() - nunca da URL
 * nem do formulario. Repare que NENHUMA rota daqui tem {id}: a saida que o
 * motorista ve e devolve e sempre a dele, encontrada pela sessao. Nao existe
 * endereco que um motorista possa alterar para alcancar a saida de outro.
 *
 * AS TRES TELAS
 *   /lancamentos       tela inicial: mostra se ha veiculo em aberto e leva
 *                      para a saida ou para a devolucao
 *   /saidas/nova       formulario de saida
 *   /saidas/devolucao  formulario de devolucao
 */
@Controller
public class UsoVeiculoController {

    private final UsoVeiculoRepository usoVeiculoRepository;
    private final VeiculoRepository veiculoRepository;
    private final CentroCustoRepository centroCustoRepository;
    private final UsoVeiculoService usoVeiculoService;

    public UsoVeiculoController(UsoVeiculoRepository usoVeiculoRepository,
                                VeiculoRepository veiculoRepository,
                                CentroCustoRepository centroCustoRepository,
                                UsoVeiculoService usoVeiculoService) {
        this.usoVeiculoRepository = usoVeiculoRepository;
        this.veiculoRepository = veiculoRepository;
        this.centroCustoRepository = centroCustoRepository;
        this.usoVeiculoService = usoVeiculoService;
    }

    /* ==================================================================
       Tela inicial do Motorista
       ================================================================== */

    /**
     * Para onde o InicioController manda o Motorista depois do login.
     *
     * A tela precisa de uma informacao so: existe saida em aberto? Com ela o
     * template mostra o veiculo que esta com o motorista e o botao de
     * devolucao, ou entao o botao de nova saida. "usoEmAberto" vai nulo
     * quando nao ha nenhuma.
     */
    @GetMapping("/lancamentos")
    public String inicio(@AuthenticationPrincipal UsuarioAutenticado logado, Model model) {
        model.addAttribute("logado", logado);
        model.addAttribute("usoEmAberto", buscarUsoEmAberto(logado).orElse(null));
        return "saidas/inicio";
    }

    /* ==================================================================
       Saida (#006-RF01)
       ================================================================== */

    /** Mostra o formulario de saida, com a data e a hora de agora ja preenchidas. */
    @GetMapping("/saidas/nova")
    public String novaSaida(@AuthenticationPrincipal UsuarioAutenticado logado,
                            Model model,
                            RedirectAttributes atributos) {

        // #006-RN002: quem ja esta com um veiculo nem ve o formulario - vai
        // direto para a devolucao, que e o que ele precisa fazer.
        if (buscarUsoEmAberto(logado).isPresent()) {
            atributos.addFlashAttribute("erro",
                    "Você já está com um veículo em aberto. Registre a devolução dele"
                            + " antes de registrar uma nova saída.");
            return "redirect:/saidas/devolucao";
        }

        SaidaForm saidaForm = new SaidaForm();
        saidaForm.setDataSaida(agoraSemSegundos());

        model.addAttribute("saidaForm", saidaForm);
        return prepararFormularioDeSaida(model, logado);
    }

    /**
     * Recebe o formulario de saida.
     *
     * @Valid confere os obrigatorios (#006-RN001). As demais regras sao do
     * UsoVeiculoService, que recusa com LancamentoRecusadoException; aqui a
     * recusa so vira o aviso vermelho no alto do formulario.
     */
    @PostMapping("/saidas")
    public String registrarSaida(@AuthenticationPrincipal UsuarioAutenticado logado,
                                 @Valid @ModelAttribute SaidaForm saidaForm,
                                 BindingResult resultado,
                                 Model model,
                                 RedirectAttributes atributos) {

        if (resultado.hasErrors()) {
            return prepararFormularioDeSaida(model, logado);
        }

        // #006-RN004 e regra 1: veiculo ou centro de custo de outra empresa
        // respondem 404, como em toda busca por id do sistema. A caixa de
        // selecao so tem os da empresa, entao so chega aqui um id forjado.
        Veiculo veiculo = veiculoRepository
                .findByIdAndEmpresaId(saidaForm.getVeiculoId(), logado.getEmpresaId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Veículo não encontrado."));

        centroCustoRepository
                .findByIdAndEmpresaId(saidaForm.getCentroCustoId(), logado.getEmpresaId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Centro de custo não encontrado."));

        try {
            usoVeiculoService.registrarSaida(saidaForm, logado.getId(), logado.getEmpresaId());

        } catch (LancamentoRecusadoException recusa) {
            model.addAttribute("erro", recusa.getMessage());
            return prepararFormularioDeSaida(model, logado);
        }

        atributos.addFlashAttribute("mensagem",
                "Saída registrada: " + veiculo.getNome() + " (" + veiculo.getPlacaFormatada()
                        + "). Lembre-se de registrar a devolução quando voltar.");
        return "redirect:/lancamentos";
    }

    /* ==================================================================
       Devolucao (#006-RF02)
       ================================================================== */

    /** Mostra o veiculo que esta com o motorista e o campo da data de devolucao. */
    @GetMapping("/saidas/devolucao")
    public String novaDevolucao(@AuthenticationPrincipal UsuarioAutenticado logado,
                                Model model,
                                RedirectAttributes atributos) {

        Optional<UsoVeiculo> usoEmAberto = buscarUsoEmAberto(logado);

        if (usoEmAberto.isEmpty()) {
            atributos.addFlashAttribute("erro",
                    "Você não tem veículo em aberto para devolver.");
            return "redirect:/lancamentos";
        }

        DevolucaoForm devolucaoForm = new DevolucaoForm();
        devolucaoForm.setDataDevolucao(agoraSemSegundos());

        model.addAttribute("devolucaoForm", devolucaoForm);
        return prepararFormularioDeDevolucao(model, logado, usoEmAberto.get());
    }

    /** Recebe o formulario de devolucao. */
    @PostMapping("/saidas/devolucao")
    public String registrarDevolucao(@AuthenticationPrincipal UsuarioAutenticado logado,
                                     @Valid @ModelAttribute DevolucaoForm devolucaoForm,
                                     BindingResult resultado,
                                     Model model,
                                     RedirectAttributes atributos) {

        Optional<UsoVeiculo> usoEmAberto = buscarUsoEmAberto(logado);

        if (usoEmAberto.isEmpty()) {
            atributos.addFlashAttribute("erro",
                    "Você não tem veículo em aberto para devolver.");
            return "redirect:/lancamentos";
        }

        if (resultado.hasErrors()) {
            return prepararFormularioDeDevolucao(model, logado, usoEmAberto.get());
        }

        Veiculo veiculo = usoEmAberto.get().getVeiculo();

        try {
            usoVeiculoService.registrarDevolucao(
                    devolucaoForm, logado.getId(), logado.getEmpresaId());

        } catch (LancamentoRecusadoException recusa) {
            model.addAttribute("erro", recusa.getMessage());
            return prepararFormularioDeDevolucao(model, logado, usoEmAberto.get());
        }

        atributos.addFlashAttribute("mensagem",
                "Devolução registrada: " + veiculo.getNome() + " ("
                        + veiculo.getPlacaFormatada() + ") está disponível de novo.");
        return "redirect:/lancamentos";
    }

    /* ==================================================================
       Auxiliares
       ================================================================== */

    /** A saida em aberto de quem esta logado, se houver (#006-RN002). */
    private Optional<UsoVeiculo> buscarUsoEmAberto(UsuarioAutenticado logado) {
        return usoVeiculoRepository.findByUsuarioIdAndEmpresaIdAndDataDevolucaoIsNull(
                logado.getId(), logado.getEmpresaId());
    }

    /**
     * A hora de agora, sem segundos, para preencher o campo de data da tela.
     *
     * Sem segundos porque o <input type="datetime-local"> trabalha em
     * minutos: com "14:30:27" alguns navegadores passam a mostrar (e a
     * exigir) os segundos. Cortar tambem garante que o valor sugerido nunca
     * esta no futuro.
     *
     * Isto e so uma SUGESTAO editavel - a data informada (#006-RN005). A data
     * de registro e outra coisa e nao passa pela tela.
     */
    private LocalDateTime agoraSemSegundos() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
    }

    /**
     * Coloca no modelo as duas caixas de selecao da saida e devolve o nome do
     * template.
     *
     * AS DUAS LISTAS JA SAEM FILTRADAS DO BANCO: so veiculos DISPONIVEIS e so
     * centros de custo ATIVOS, os dois da empresa de quem esta logado. O que
     * nao pode ser escolhido nem chega a tela.
     */
    private String prepararFormularioDeSaida(Model model, UsuarioAutenticado logado) {
        model.addAttribute("logado", logado);
        model.addAttribute("veiculos", veiculoRepository.findByEmpresaIdAndStatusOrderByNome(
                logado.getEmpresaId(), StatusVeiculo.DISPONIVEL));
        model.addAttribute("centrosDeCusto", centroCustoRepository
                .findByEmpresaIdAndAtivoTrueOrderByNome(logado.getEmpresaId()));
        return "saidas/saida";
    }

    private String prepararFormularioDeDevolucao(Model model, UsuarioAutenticado logado,
                                                 UsoVeiculo usoEmAberto) {
        model.addAttribute("logado", logado);
        model.addAttribute("usoEmAberto", usoEmAberto);
        return "saidas/devolucao";
    }
}
