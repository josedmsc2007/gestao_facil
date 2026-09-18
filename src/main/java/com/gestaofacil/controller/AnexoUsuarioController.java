package com.gestaofacil.controller;

import com.gestaofacil.model.AnexoUsuario;
import com.gestaofacil.model.Usuario;
import com.gestaofacil.repository.UsuarioRepository;
import com.gestaofacil.security.UsuarioAutenticado;
import com.gestaofacil.service.AnexoInvalidoException;
import com.gestaofacil.service.AnexoUsuarioService;
import com.gestaofacil.service.ArmazenamentoDeAnexos;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Anexos do funcionario (#003-RF09): listar, enviar, baixar e remover.
 *
 * POR QUE UM CONTROLLER SEPARADO DO UsuarioController
 * Anexo e outro assunto (arquivo, disco, download) e o UsuarioController ja
 * e grande. Os enderecos, porem, continuam debaixo de /usuarios/{id}: o
 * anexo so existe dentro de um funcionario, e assim a regra
 * .requestMatchers("/usuarios/**").hasRole("ADMINISTRADOR") do SecurityConfig
 * ja protege tudo daqui sem nenhuma linha nova (#003-RN008).
 *
 * AS DUAS CONFERENCIAS DE CADA METODO
 * 1. o funcionario da URL e da empresa de quem esta logado (buscarUsuario);
 * 2. o anexo da URL e daquele funcionario, naquela empresa (buscarAnexo).
 * Qualquer uma que falhe responde 404, pelo mesmo motivo do UsuarioController:
 * o 403 confirmaria que aquele id existe em outra empresa.
 */
@Controller
@RequestMapping("/usuarios/{usuarioId}/anexos")
public class AnexoUsuarioController {

    private final UsuarioRepository usuarioRepository;
    private final AnexoUsuarioService anexoService;
    private final ArmazenamentoDeAnexos armazenamento;

    public AnexoUsuarioController(UsuarioRepository usuarioRepository,
                                  AnexoUsuarioService anexoService,
                                  ArmazenamentoDeAnexos armazenamento) {
        this.usuarioRepository = usuarioRepository;
        this.anexoService = anexoService;
        this.armazenamento = armazenamento;
    }

    /** A tela com os documentos do funcionario e o formulario de envio. */
    @GetMapping
    public String listar(@PathVariable Long usuarioId,
                         @AuthenticationPrincipal UsuarioAutenticado logado,
                         Model model) {

        Usuario usuario = buscarUsuario(usuarioId, logado);

        model.addAttribute("logado", logado);
        model.addAttribute("usuario", usuario);
        model.addAttribute("anexos", anexoService.listar(logado.getEmpresaId(), usuarioId));
        return "usuarios/anexos";
    }

    /**
     * Recebe o arquivo enviado.
     *
     * MultipartFile e como o Spring entrega um arquivo vindo de um formulario
     * com enctype="multipart/form-data". O nome "arquivo" precisa ser igual ao
     * name do <input type="file"> da tela.
     *
     * required = false: sem isso, enviar o formulario sem escolher arquivo
     * daria uma pagina de erro do Spring. Deixando passar, quem recusa e o
     * service, com a mensagem "Escolha um arquivo para anexar."
     */
    @PostMapping
    public String enviar(@PathVariable Long usuarioId,
                         @RequestParam(name = "arquivo", required = false) MultipartFile arquivo,
                         @AuthenticationPrincipal UsuarioAutenticado logado,
                         RedirectAttributes atributos) {

        buscarUsuario(usuarioId, logado);

        try {
            AnexoUsuario anexo = anexoService.anexar(arquivo, usuarioId, logado.getEmpresaId());
            atributos.addFlashAttribute("mensagem",
                    "Arquivo " + anexo.getNomeOriginal() + " anexado.");

        } catch (AnexoInvalidoException recusa) {
            // #003-RN012: tipo ou tamanho fora da regra. A mensagem ja vem
            // pronta do service; aqui ela so vira aviso na tela.
            atributos.addFlashAttribute("erro", recusa.getMessage());
        }

        return "redirect:/usuarios/" + usuarioId + "/anexos";
    }

    /**
     * Devolve o arquivo para o navegador (#003-RN013).
     *
     * E O UNICO CAMINHO ATE O ARQUIVO
     * A pasta dos anexos fica fora do projeto, entao nao existe URL que leve
     * direto a ela. Todo download passa por este metodo, que so chega aqui
     * depois do Spring Security conferir o login e o perfil, e que confere a
     * empresa antes de ler qualquer byte.
     *
     * O QUE VAI NA RESPOSTA
     * - Content-Type: o tipo gravado no banco no momento do envio (um dos tres
     *   da RN012), e nao um tipo adivinhado agora;
     * - Content-Disposition "attachment": manda o navegador BAIXAR o arquivo
     *   com o nome original, em vez de tentar abri-lo dentro do sistema.
     *   O StandardCharsets.UTF_8 preserva acentos no nome ("Habilitação.pdf").
     */
    @GetMapping("/{anexoId}")
    public ResponseEntity<Resource> baixar(@PathVariable Long usuarioId,
                                           @PathVariable Long anexoId,
                                           @AuthenticationPrincipal UsuarioAutenticado logado) {

        AnexoUsuario anexo = buscarAnexo(anexoId, usuarioId, logado);

        Path caminho = armazenamento.caminhoDe(logado.getEmpresaId(), anexo.getNomeArmazenado());

        // A ficha existe mas o arquivo sumiu da pasta (apagado a mao, backup
        // restaurado pela metade). Para quem clicou, e o mesmo que nao existir.
        if (!Files.exists(caminho)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Arquivo não encontrado.");
        }

        ContentDisposition disposicao = ContentDisposition.attachment()
                .filename(anexo.getNomeOriginal(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(anexo.getTipoConteudo()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposicao.toString())
                .body(new FileSystemResource(caminho));
    }

    /**
     * Remove o anexo. E POST, nunca link, como toda acao que altera dados.
     *
     * Por que aqui a remocao e de verdade, e nao por campo "ativo", esta
     * explicado no AnexoUsuarioService.remover.
     */
    @PostMapping("/{anexoId}/remover")
    public String remover(@PathVariable Long usuarioId,
                          @PathVariable Long anexoId,
                          @AuthenticationPrincipal UsuarioAutenticado logado,
                          RedirectAttributes atributos) {

        AnexoUsuario anexo = buscarAnexo(anexoId, usuarioId, logado);

        anexoService.remover(anexoId, usuarioId, logado.getEmpresaId());

        atributos.addFlashAttribute("mensagem",
                "Arquivo " + anexo.getNomeOriginal() + " removido.");
        return "redirect:/usuarios/" + usuarioId + "/anexos";
    }

    /* ==================================================================
       Auxiliares - as duas conferencias, sempre com a empresa do logado
       ================================================================== */

    private Usuario buscarUsuario(Long usuarioId, UsuarioAutenticado logado) {
        return usuarioRepository.findByIdAndEmpresaId(usuarioId, logado.getEmpresaId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Usuário não encontrado."));
    }

    private AnexoUsuario buscarAnexo(Long anexoId, Long usuarioId, UsuarioAutenticado logado) {
        return anexoService.buscarNaEmpresa(anexoId, usuarioId, logado.getEmpresaId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Anexo não encontrado."));
    }
}
