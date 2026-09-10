package com.gestaofacil.controller;

import com.gestaofacil.model.Empresa;
import com.gestaofacil.repository.EmpresaRepository;
import com.gestaofacil.security.UsuarioAutenticado;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Optional;

/**
 * Mostra a tela de login. Atende ao #001-RF02.
 *
 * ATENCAO: aqui so existem metodos GET, que MOSTRAM a tela. O envio do
 * formulario (POST /login) e tratado pelo proprio Spring Security, configurado
 * no SecurityConfig. Nao existe - e nao deve existir - um metodo aqui para
 * receber usuario e senha.
 *
 * O IDENTIFICADOR NA URL
 * As rotas usam {identificador:[a-z0-9-]+}, ou seja, so aceitam letras
 * minusculas, numeros e hifen. Essa restricao existe para o atalho da empresa
 * (/construtora-teste) nao engolir enderecos como /favicon.ico, que tem ponto.
 */
@Controller
public class LoginController {

    private final EmpresaRepository empresaRepository;

    public LoginController(EmpresaRepository empresaRepository) {
        this.empresaRepository = empresaRepository;
    }

    /**
     * Atalho da empresa: /construtora-teste
     *
     * E este o endereco que o funcionario adiciona a tela inicial do celular
     * (RF01). Ele apenas encaminha para a tela de login da empresa.
     */
    @GetMapping("/{identificador:[a-z0-9-]+}")
    public String atalhoDaEmpresa(@PathVariable String identificador,
                                  @AuthenticationPrincipal UsuarioAutenticado logado) {
        if (logado != null) {
            return "redirect:/";
        }
        return "redirect:/" + identificador + "/login";
    }

    /**
     * Caminho normal: /construtora-teste/login
     *
     * A empresa vem do endereco, entao a tela nao mostra campo de empresa -
     * o funcionario digita so usuario e senha.
     */
    @GetMapping("/{identificador:[a-z0-9-]+}/login")
    public String loginDaEmpresa(@PathVariable String identificador,
                                 @AuthenticationPrincipal UsuarioAutenticado logado,
                                 Model model) {

        // Ja esta logado? Nao faz sentido ver a tela de login de novo.
        if (logado != null) {
            return "redirect:/";
        }

        Optional<Empresa> empresa = empresaRepository.findByIdentificador(identificador);

        if (empresa.isEmpty()) {
            // Endereco errado ou empresa que nao existe: cai no caminho de
            // excecao, com o campo de empresa visivel para corrigir.
            model.addAttribute("empresaConhecida", false);
            model.addAttribute("identificadorEmpresa", identificador);
            model.addAttribute("empresaNaoEncontrada", true);
            return "login/login";
        }

        model.addAttribute("empresaConhecida", true);
        model.addAttribute("identificadorEmpresa", identificador);
        model.addAttribute("nomeEmpresa", empresa.get().getNome());
        return "login/login";
    }

    /**
     * Caminho de excecao: /login, sem empresa no endereco (RF01).
     *
     * Acontece quando alguem acessa a raiz do sistema, ou quando a sessao
     * expira numa tela interna. Aqui a tela mostra o campo "empresa".
     */
    @GetMapping("/login")
    public String loginSemEmpresa(@AuthenticationPrincipal UsuarioAutenticado logado,
                                  Model model) {
        if (logado != null) {
            return "redirect:/";
        }
        model.addAttribute("empresaConhecida", false);
        model.addAttribute("identificadorEmpresa", "");
        return "login/login";
    }
}
