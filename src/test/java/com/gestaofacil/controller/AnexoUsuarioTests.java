package com.gestaofacil.controller;

import com.gestaofacil.model.AnexoUsuario;
import com.gestaofacil.model.Empresa;
import com.gestaofacil.model.Perfil;
import com.gestaofacil.model.Usuario;
import com.gestaofacil.repository.AnexoUsuarioRepository;
import com.gestaofacil.repository.EmpresaRepository;
import com.gestaofacil.repository.UsuarioRepository;
import com.gestaofacil.security.UsuarioAutenticado;
import com.gestaofacil.service.AnexoUsuarioService;
import com.gestaofacil.service.ArmazenamentoDeAnexos;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Card #003, etapa 3: anexos do funcionario (RF09, RN012 e RN013).
 *
 * OS ARQUIVOS SAO DE MENTIRA, O DISCO E DE VERDADE
 * MockMultipartFile simula o arquivo que o navegador enviaria - basta um nome,
 * um tipo e alguns bytes. Mas a gravacao acontece de fato, na pasta temporaria
 * definida em src/test/resources/application.properties. Por isso o
 * @AfterEach apaga essa pasta: o @Transactional desfaz o banco sozinho, mas
 * nao sabe nada de arquivo.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AnexoUsuarioTests {

    private static final byte[] CONTEUDO_PDF = "%PDF-1.4 documento de teste".getBytes();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmpresaRepository empresaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private AnexoUsuarioRepository anexoRepository;

    @Autowired
    private ArmazenamentoDeAnexos armazenamento;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Empresa alfa;
    private Empresa beta;
    private Usuario adminAlfa;
    private Usuario motoristaAlfa;
    private Usuario outroMotoristaAlfa;
    private Usuario adminBeta;

    @BeforeEach
    void prepararDados() {
        alfa = empresaRepository.save(new Empresa("Alfa", "alfa-dos-anexos", "Obra", "Obras"));
        beta = empresaRepository.save(new Empresa("Beta", "beta-dos-anexos", "Obra", "Obras"));

        adminAlfa = criar(alfa, "chefe", Perfil.ADMINISTRADOR);
        motoristaAlfa = criar(alfa, "motorista", Perfil.MOTORISTA);
        outroMotoristaAlfa = criar(alfa, "motorista.dois", Perfil.MOTORISTA);
        adminBeta = criar(beta, "chefe.beta", Perfil.ADMINISTRADOR);
    }

    @AfterEach
    void apagarArquivosGravados() throws IOException {
        FileSystemUtils.deleteRecursively(armazenamento.getDiretorioBase());
    }

    /* ==================================================================
       Envio e RN012
       ================================================================== */

    @Test
    @DisplayName("#003-RF09: administrador anexa um PDF e ele aparece na lista")
    void anexaPdf() throws Exception {
        mockMvc.perform(envio(motoristaAlfa, arquivo("CNH frente.pdf", CONTEUDO_PDF))
                        .with(comoUsuario(adminAlfa, alfa)))
                .andExpect(redirectedUrl(telaDeAnexos(motoristaAlfa)))
                .andExpect(flash().attributeExists("mensagem"));

        List<AnexoUsuario> anexos = anexosDe(motoristaAlfa);
        assertEquals(1, anexos.size());

        AnexoUsuario anexo = anexos.get(0);
        assertEquals("CNH frente.pdf", anexo.getNomeOriginal());
        assertEquals("application/pdf", anexo.getTipoConteudo());

        // #003-RN013: o nome no disco e sorteado, nunca o enviado.
        assertNotEquals("CNH frente.pdf", anexo.getNomeArmazenado());
        assertTrue(Files.exists(armazenamento.caminhoDe(alfa.getId(), anexo.getNomeArmazenado())));

        mockMvc.perform(get(telaDeAnexos(motoristaAlfa)).with(comoUsuario(adminAlfa, alfa)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("CNH frente.pdf")));
    }

    @Test
    @DisplayName("#003-RN012: aceita JPG e PNG, inclusive com extensao maiuscula")
    void aceitaImagens() throws Exception {
        mockMvc.perform(envio(motoristaAlfa, arquivo("foto.JPG", new byte[]{1, 2, 3}))
                .with(comoUsuario(adminAlfa, alfa)));
        mockMvc.perform(envio(motoristaAlfa, arquivo("exame.png", new byte[]{1, 2, 3}))
                .with(comoUsuario(adminAlfa, alfa)));

        assertEquals(2, anexosDe(motoristaAlfa).size());
    }

    @Test
    @DisplayName("#003-RN012: recusa anexo fora de PDF, JPG e PNG")
    void recusaTipoNaoAceito() throws Exception {
        mockMvc.perform(envio(motoristaAlfa, arquivo("planilha.xlsx", new byte[]{1, 2, 3}))
                        .with(comoUsuario(adminAlfa, alfa)))
                .andExpect(redirectedUrl(telaDeAnexos(motoristaAlfa)))
                .andExpect(flash().attribute("erro", containsString("PDF, JPG ou PNG")));

        assertTrue(anexosDe(motoristaAlfa).isEmpty());
    }

    /**
     * O tipo que o navegador declara nao conta: um .exe enviado dizendo ser
     * "application/pdf" continua sendo recusado, porque a regra olha a
     * extensao e o tipo gravado sai da tabela do service.
     */
    @Test
    @DisplayName("#003-RN012: nao confia no tipo declarado pelo navegador")
    void naoConfiaNoTipoDoNavegador() throws Exception {
        MockMultipartFile disfarcado = new MockMultipartFile(
                "arquivo", "programa.exe", "application/pdf", new byte[]{1, 2, 3});

        mockMvc.perform(envio(motoristaAlfa, disfarcado).with(comoUsuario(adminAlfa, alfa)))
                .andExpect(flash().attributeExists("erro"));

        assertTrue(anexosDe(motoristaAlfa).isEmpty());
    }

    @Test
    @DisplayName("#003-RN012: recusa anexo acima de 5 MB")
    void recusaArquivoGrande() throws Exception {
        byte[] grande = new byte[(int) AnexoUsuarioService.TAMANHO_MAXIMO_EM_BYTES + 1];

        mockMvc.perform(envio(motoristaAlfa, arquivo("scan.pdf", grande))
                        .with(comoUsuario(adminAlfa, alfa)))
                .andExpect(flash().attribute("erro", containsString("5 MB")));

        assertTrue(anexosDe(motoristaAlfa).isEmpty());
    }

    @Test
    @DisplayName("#003-RN012: aceita arquivo com exatamente 5 MB")
    void aceitaArquivoNoLimite() throws Exception {
        byte[] noLimite = new byte[(int) AnexoUsuarioService.TAMANHO_MAXIMO_EM_BYTES];

        mockMvc.perform(envio(motoristaAlfa, arquivo("scan.pdf", noLimite))
                        .with(comoUsuario(adminAlfa, alfa)))
                .andExpect(flash().attributeExists("mensagem"));

        assertEquals(1, anexosDe(motoristaAlfa).size());
    }

    @Test
    @DisplayName("Enviar sem escolher arquivo mostra aviso, e nao pagina de erro")
    void recusaEnvioVazio() throws Exception {
        mockMvc.perform(multipart(telaDeAnexos(motoristaAlfa)).with(csrf())
                        .with(comoUsuario(adminAlfa, alfa)))
                .andExpect(redirectedUrl(telaDeAnexos(motoristaAlfa)))
                .andExpect(flash().attribute("erro", containsString("Escolha um arquivo")));
    }

    /* ==================================================================
       Download e RN013
       ================================================================== */

    @Test
    @DisplayName("#003-RN013: o download devolve o arquivo com o nome e o tipo originais")
    void baixaOArquivo() throws Exception {
        AnexoUsuario anexo = anexar(motoristaAlfa, "Habilitação.pdf");

        byte[] recebido = mockMvc.perform(get(download(motoristaAlfa, anexo))
                        .with(comoUsuario(adminAlfa, alfa)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andReturn().getResponse().getContentAsByteArray();

        assertArrayEquals(CONTEUDO_PDF, recebido);
    }

    @Test
    @DisplayName("#003-RN013: anexo nao e acessivel por link direto sem login")
    void downloadSemLoginVaiParaOLogin() throws Exception {
        AnexoUsuario anexo = anexar(motoristaAlfa, "cnh.pdf");

        mockMvc.perform(get(download(motoristaAlfa, anexo)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    /** O criterio de isolamento que todo CRUD novo precisa provar. */
    @Test
    @DisplayName("#003-RN013: administrador de outra empresa nao baixa o anexo (404)")
    void outraEmpresaNaoBaixa() throws Exception {
        AnexoUsuario anexo = anexar(motoristaAlfa, "cnh.pdf");

        mockMvc.perform(get(download(motoristaAlfa, anexo)).with(comoUsuario(adminBeta, beta)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("#003-RN009: administrador de outra empresa nao ve nem envia anexos (404)")
    void outraEmpresaNaoVeNemEnvia() throws Exception {
        mockMvc.perform(get(telaDeAnexos(motoristaAlfa)).with(comoUsuario(adminBeta, beta)))
                .andExpect(status().isNotFound());

        mockMvc.perform(envio(motoristaAlfa, arquivo("intruso.pdf", CONTEUDO_PDF))
                        .with(comoUsuario(adminBeta, beta)))
                .andExpect(status().isNotFound());

        assertTrue(anexosDe(motoristaAlfa).isEmpty());
    }

    @Test
    @DisplayName("#003-RN013: o endereco de um funcionario nao abre o anexo de outro")
    void anexoDeOutroFuncionarioNaoAbre() throws Exception {
        AnexoUsuario doMotorista = anexar(motoristaAlfa, "cnh.pdf");

        // Mesmo id de anexo, mas pendurado no endereco de outro funcionario.
        mockMvc.perform(get(download(outroMotoristaAlfa, doMotorista))
                        .with(comoUsuario(adminAlfa, alfa)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("#003-RN008: motorista nao abre anexos, nem os proprios")
    void motoristaNaoAbreAnexos() throws Exception {
        AnexoUsuario anexo = anexar(motoristaAlfa, "cnh.pdf");

        mockMvc.perform(get(download(motoristaAlfa, anexo))
                        .with(comoUsuario(motoristaAlfa, alfa)))
                .andExpect(status().isForbidden());
    }

    /* ==================================================================
       Remocao
       ================================================================== */

    @Test
    @DisplayName("#003-RF09: remover apaga a ficha e o arquivo do disco")
    void removeAnexo() throws Exception {
        AnexoUsuario anexo = anexar(motoristaAlfa, "errado.pdf");
        Path noDisco = armazenamento.caminhoDe(alfa.getId(), anexo.getNomeArmazenado());

        mockMvc.perform(remocao(motoristaAlfa, anexo).with(comoUsuario(adminAlfa, alfa)))
                .andExpect(redirectedUrl(telaDeAnexos(motoristaAlfa)))
                .andExpect(flash().attributeExists("mensagem"));

        assertTrue(anexosDe(motoristaAlfa).isEmpty());
        assertFalse(Files.exists(noDisco));
    }

    @Test
    @DisplayName("#003-RN013: administrador de outra empresa nao remove o anexo (404)")
    void outraEmpresaNaoRemove() throws Exception {
        AnexoUsuario anexo = anexar(motoristaAlfa, "cnh.pdf");

        mockMvc.perform(remocao(motoristaAlfa, anexo).with(comoUsuario(adminBeta, beta)))
                .andExpect(status().isNotFound());

        assertEquals(1, anexosDe(motoristaAlfa).size());
    }

    /* ==================================================================
       Auxiliares
       ================================================================== */

    /** Anexa pela propria tela, como o administrador faria. */
    private AnexoUsuario anexar(Usuario dono, String nome) throws Exception {
        mockMvc.perform(envio(dono, arquivo(nome, CONTEUDO_PDF)).with(comoUsuario(adminAlfa, alfa)))
                .andExpect(flash().attributeExists("mensagem"));
        return anexosDe(dono).get(0);
    }

    private MockMultipartFile arquivo(String nome, byte[] conteudo) {
        // "arquivo" e o name do <input type="file"> da tela.
        return new MockMultipartFile("arquivo", nome, "application/octet-stream", conteudo);
    }

    private MockHttpServletRequestBuilder envio(Usuario dono, MockMultipartFile arquivo) {
        return multipart(telaDeAnexos(dono)).file(arquivo).with(csrf());
    }

    private MockHttpServletRequestBuilder remocao(Usuario dono, AnexoUsuario anexo) {
        return post(download(dono, anexo) + "/remover").with(csrf());
    }

    private String telaDeAnexos(Usuario dono) {
        return "/usuarios/" + dono.getId() + "/anexos";
    }

    private String download(Usuario dono, AnexoUsuario anexo) {
        return telaDeAnexos(dono) + "/" + anexo.getId();
    }

    private List<AnexoUsuario> anexosDe(Usuario dono) {
        return anexoRepository.findByEmpresaIdAndUsuarioIdOrderByDataRegistroDesc(
                dono.getEmpresa().getId(), dono.getId());
    }

    private RequestPostProcessor comoUsuario(Usuario usuario, Empresa empresa) {
        return user(new UsuarioAutenticado(usuario, empresa));
    }

    private Usuario criar(Empresa empresa, String login, Perfil perfil) {
        Usuario usuario = new Usuario(empresa, "Funcionário " + login, login,
                passwordEncoder.encode("senha-qualquer-123"), perfil);
        return usuarioRepository.save(usuario);
    }
}
