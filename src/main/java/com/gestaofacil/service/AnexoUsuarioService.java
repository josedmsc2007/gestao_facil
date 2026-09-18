package com.gestaofacil.service;

import com.gestaofacil.model.AnexoUsuario;
import com.gestaofacil.model.Empresa;
import com.gestaofacil.model.Usuario;
import com.gestaofacil.repository.AnexoUsuarioRepository;
import com.gestaofacil.repository.EmpresaRepository;
import com.gestaofacil.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * As regras dos anexos do funcionario (#003-RF09).
 *
 * Esta classe decide o que pode entrar; quem mexe no disco e o
 * ArmazenamentoDeAnexos. Como em todo service do projeto, o empresaId do
 * usuario logado entra em cada metodo e vai junto para a consulta.
 */
@Service
public class AnexoUsuarioService {

    /** #003-RN012: 5 MB por arquivo, em bytes. */
    public static final long TAMANHO_MAXIMO_EM_BYTES = 5L * 1024 * 1024;

    /**
     * #003-RN012: os unicos tipos aceitos, ligando a extensao ao tipo que sera
     * gravado na ficha do anexo.
     *
     * O TIPO VEM DESTA TABELA, NAO DO NAVEGADOR
     * O navegador manda junto um "content type", mas ele e apenas uma
     * informacao do lado de fora: da para altera-la com qualquer ferramenta, e
     * navegadores diferentes mandam valores diferentes para o mesmo .jpg. Por
     * isso a conferencia olha a EXTENSAO e o tipo gravado sai daqui. Assim o
     * campo tipo_conteudo so pode conter um destes tres valores, e o download
     * nunca devolve um arquivo dizendo ser outra coisa.
     */
    private static final Map<String, String> TIPOS_ACEITOS = Map.of(
            ".pdf", "application/pdf",
            ".jpg", "image/jpeg",
            ".jpeg", "image/jpeg",
            ".png", "image/png");

    private static final String MENSAGEM_TIPO =
            "Só é possível anexar arquivos PDF, JPG ou PNG.";

    private final AnexoUsuarioRepository anexoRepository;
    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;
    private final ArmazenamentoDeAnexos armazenamento;

    public AnexoUsuarioService(AnexoUsuarioRepository anexoRepository,
                               UsuarioRepository usuarioRepository,
                               EmpresaRepository empresaRepository,
                               ArmazenamentoDeAnexos armazenamento) {
        this.anexoRepository = anexoRepository;
        this.usuarioRepository = usuarioRepository;
        this.empresaRepository = empresaRepository;
        this.armazenamento = armazenamento;
    }

    /** Os anexos de um funcionario, do mais novo para o mais antigo. */
    @Transactional(readOnly = true)
    public List<AnexoUsuario> listar(Long empresaId, Long usuarioId) {
        return anexoRepository.findByEmpresaIdAndUsuarioIdOrderByDataRegistroDesc(
                empresaId, usuarioId);
    }

    /**
     * Recebe um arquivo e o anexa ao cadastro do funcionario.
     *
     * A ORDEM IMPORTA: primeiro grava o arquivo, depois a ficha no banco. Se o
     * disco falhar (pasta sem permissao, por exemplo), a excecao sobe antes de
     * existir qualquer linha no banco, e nao fica registro apontando para um
     * arquivo que nunca chegou a existir.
     *
     * @throws AnexoInvalidoException quando o arquivo nao passa na RN012
     */
    @Transactional
    public AnexoUsuario anexar(MultipartFile arquivo, Long usuarioId, Long empresaId) {

        Usuario dono = usuarioRepository.findByIdAndEmpresaId(usuarioId, empresaId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Usuário não encontrado na empresa do usuário logado."));

        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new IllegalArgumentException("Empresa não encontrada."));

        String extensao = conferirRegrasDoArquivo(arquivo);

        String nomeArmazenado = armazenamento.gravar(arquivo, empresaId, extensao);

        AnexoUsuario anexo = new AnexoUsuario(
                empresa,
                dono,
                arquivo.getOriginalFilename(),
                nomeArmazenado,
                TIPOS_ACEITOS.get(extensao),
                arquivo.getSize());

        return anexoRepository.save(anexo);
    }

    /**
     * Busca um anexo conferindo a empresa (#003-RN013).
     *
     * E o metodo que o download usa. Um anexo de outra empresa simplesmente
     * nao e encontrado, e o controller responde 404.
     */
    @Transactional(readOnly = true)
    public AnexoUsuario buscarNaEmpresa(Long anexoId, Long empresaId) {
        return anexoRepository.findByIdAndEmpresaId(anexoId, empresaId)
                .orElseThrow(() -> new IllegalArgumentException("Anexo não encontrado."));
    }

    /**
     * Remove o anexo: a ficha do banco e o arquivo do disco.
     *
     * POR QUE AQUI A EXCLUSAO E DE VERDADE
     * A regra 3 do projeto diz que nada e apagado - mas ela existe para
     * proteger os registros que os LANCAMENTOS apontam: apagar um veiculo ou um
     * funcionario quebraria o historico e os relatorios. Um anexo nao e
     * apontado por nada, e o motivo mais comum de remove-lo e ter enviado o
     * documento errado. Guardar para sempre o documento pessoal que alguem
     * pediu para tirar seria o contrario do desejavel.
     *
     * A ORDEM AQUI E A INVERSA DO ANEXAR, E TAMBEM DE PROPOSITO: apaga-se
     * primeiro a linha do banco. Se o arquivo nao puder ser apagado, sobra um
     * arquivo orfao na pasta - que nao faz mal a ninguem. Na ordem contraria,
     * sobraria uma linha na tela apontando para um arquivo que nao existe, e o
     * download daria erro.
     */
    @Transactional
    public void remover(Long anexoId, Long empresaId) {
        AnexoUsuario anexo = buscarNaEmpresa(anexoId, empresaId);
        String nomeArmazenado = anexo.getNomeArmazenado();

        anexoRepository.delete(anexo);
        armazenamento.apagar(empresaId, nomeArmazenado);
    }

    /**
     * #003-RN012: tipo e tamanho.
     *
     * @return a extensao aceita (com o ponto), que o chamador usa para gravar
     *         o arquivo e para descobrir o tipo de conteudo
     */
    private String conferirRegrasDoArquivo(MultipartFile arquivo) {

        if (arquivo == null || arquivo.isEmpty()) {
            throw new AnexoInvalidoException("Escolha um arquivo para anexar.");
        }

        if (arquivo.getSize() > TAMANHO_MAXIMO_EM_BYTES) {
            throw new AnexoInvalidoException(
                    "O arquivo tem mais de 5 MB. Envie um arquivo menor "
                            + "ou tire uma foto com qualidade mais baixa.");
        }

        String nome = arquivo.getOriginalFilename();
        if (nome == null || !nome.contains(".")) {
            throw new AnexoInvalidoException(MENSAGEM_TIPO);
        }

        // toLowerCase com Locale.ROOT: sem isso, num computador configurado em
        // turco, "I".toLowerCase() vira outra letra e ".JPG" deixaria de bater.
        String extensao = nome.substring(nome.lastIndexOf('.')).toLowerCase(Locale.ROOT);

        if (!TIPOS_ACEITOS.containsKey(extensao)) {
            throw new AnexoInvalidoException(MENSAGEM_TIPO);
        }

        return extensao;
    }
}
