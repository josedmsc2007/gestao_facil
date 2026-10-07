package com.gestaofacil.controller;

import com.gestaofacil.controller.form.DevolucaoForm;
import com.gestaofacil.model.UsoVeiculo;
import com.gestaofacil.model.Veiculo;
import com.gestaofacil.repository.UsoVeiculoRepository;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Painel de veiculos em uso e encerramento de saida, as telas do
 * Administrador no card #006 (RF03 e RF04).
 *
 * QUEM PODE ENTRAR AQUI
 * So o Administrador (#006-RN009): o SecurityConfig exige ROLE_ADMINISTRADOR
 * para /veiculos-em-uso/**. As telas do Motorista ficam no
 * UsoVeiculoController, em /saidas/**.
 *
 * A DIFERENCA PARA AS TELAS DO MOTORISTA
 * La nenhuma rota tem {id}: a saida e sempre a de quem esta logado. Aqui o
 * Administrador escolhe QUAL saida encerrar, entao o id vem da URL - e por
 * isso toda rota com id comeca por buscarEmAbertoNaEmpresa, com 404 para o
 * que nao for da empresa (regra 1).
 *
 * AS DUAS TELAS
 *   /veiculos-em-uso                lista das saidas em aberto
 *   /veiculos-em-uso/{id}/encerrar  confirmacao do encerramento, com a data
 */
@Controller
@RequestMapping("/veiculos-em-uso")
public class VeiculoEmUsoController {

    private final UsoVeiculoRepository usoVeiculoRepository;
    private final UsoVeiculoService usoVeiculoService;

    public VeiculoEmUsoController(UsoVeiculoRepository usoVeiculoRepository,
                                  UsoVeiculoService usoVeiculoService) {
        this.usoVeiculoRepository = usoVeiculoRepository;
        this.usoVeiculoService = usoVeiculoService;
    }

    /** #006-RF03: as saidas em aberto da empresa de quem esta logado. */
    @GetMapping
    public String listar(@AuthenticationPrincipal UsuarioAutenticado logado, Model model) {

        model.addAttribute("logado", logado);
        model.addAttribute("usos", usoVeiculoRepository
                .findByEmpresaIdAndDataDevolucaoIsNullOrderByDataSaida(logado.getEmpresaId()));
        return "veiculos-em-uso/lista";
    }

    /**
     * #006-RF04: a tela de confirmacao. Mostra a saida que sera encerrada e o
     * campo da data de devolucao, ja com a hora de agora.
     *
     * E um passo a mais de proposito: um clique na lista NAO fecha nada. O
     * Administrador ve qual saida escolheu e confere a data antes de gravar.
     *
     * A hora vai sem segundos pelo mesmo motivo da tela do motorista: o
     * <input type="datetime-local"> trabalha em minutos. E so uma sugestao
     * editavel.
     */
    @GetMapping("/{id}/encerrar")
    public String confirmarEncerramento(@PathVariable Long id,
                                        @AuthenticationPrincipal UsuarioAutenticado logado,
                                        Model model) {

        UsoVeiculo uso = buscarEmAbertoNaEmpresa(id, logado);

        DevolucaoForm devolucaoForm = new DevolucaoForm();
        devolucaoForm.setDataDevolucao(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES));

        model.addAttribute("devolucaoForm", devolucaoForm);
        return prepararTelaDeEncerramento(model, logado, uso);
    }

    /**
     * #006-RF04: o Administrador registra a devolucao no lugar do motorista.
     *
     * Reaproveita o DevolucaoForm: o unico dado que vem da tela e a data da
     * devolucao. Quem encerrou vem da sessao (logado.getId()), nunca do
     * formulario.
     *
     * As regras de data sao do UsoVeiculoService, as mesmas da devolucao do
     * motorista. Aqui a recusa so vira o aviso vermelho na propria tela.
     */
    @PostMapping("/{id}/encerrar")
    public String encerrar(@PathVariable Long id,
                           @AuthenticationPrincipal UsuarioAutenticado logado,
                           @Valid @ModelAttribute DevolucaoForm devolucaoForm,
                           BindingResult resultado,
                           Model model,
                           RedirectAttributes atributos) {

        UsoVeiculo uso = buscarEmAbertoNaEmpresa(id, logado);

        if (resultado.hasErrors()) {
            return prepararTelaDeEncerramento(model, logado, uso);
        }

        Veiculo veiculo = uso.getVeiculo();

        try {
            usoVeiculoService.encerrarSaida(
                    id, devolucaoForm, logado.getId(), logado.getEmpresaId());

        } catch (LancamentoRecusadoException recusa) {
            model.addAttribute("erro", recusa.getMessage());
            return prepararTelaDeEncerramento(model, logado, uso);
        }

        atributos.addFlashAttribute("mensagem",
                "Saída encerrada: " + veiculo.getNome() + " ("
                        + veiculo.getPlacaFormatada() + ") está disponível de novo.");
        return "redirect:/veiculos-em-uso";
    }

    /**
     * Busca a saida CONFERINDO A EMPRESA (regra 1) e responde 404 quando ela
     * nao existe para esta empresa. E 404 tambem para a saida ja fechada: nao
     * ha o que encerrar nela.
     */
    private UsoVeiculo buscarEmAbertoNaEmpresa(Long id, UsuarioAutenticado logado) {
        return usoVeiculoRepository.findByIdAndEmpresaId(id, logado.getEmpresaId())
                .filter(UsoVeiculo::isEmAberto)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Saída não encontrada."));
    }

    private String prepararTelaDeEncerramento(Model model, UsuarioAutenticado logado,
                                              UsoVeiculo uso) {
        model.addAttribute("logado", logado);
        model.addAttribute("uso", uso);
        return "veiculos-em-uso/encerrar";
    }
}
