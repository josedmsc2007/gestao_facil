package com.gestaofacil.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Documento anexado ao cadastro de um funcionario (#003-RF09).
 *
 * O QUE FICA AQUI E O QUE FICA NO DISCO
 * Esta tabela guarda apenas a FICHA do arquivo: como ele se chamava, que tipo
 * de arquivo e, quanto pesa e com que nome foi gravado. O conteudo em si mora
 * numa pasta fora do projeto (#003-RN013), cuidada pelo ArmazenamentoDeAnexos.
 *
 * Guardar o conteudo no banco tambem seria possivel, mas deixaria o backup do
 * banco gigante e cada consulta mais pesada - e o modelo de dados do projeto
 * ja previa a tabela deste jeito.
 *
 * ATENCAO: os campos abaixo foram deduzidos do card #003 e do documento de
 * requisitos, porque o Modelo_de_Dados_Gestao_Facil.pdf usa fontes embutidas e
 * nao permite extrair o texto. Confiram contra o PDF.
 */
@Entity
@Table(name = "anexo_usuario")
public class AnexoUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Empresa dona do anexo.
     *
     * POR QUE ESTA COLUNA EXISTE SE O USUARIO JA TEM EMPRESA
     * Pela regra 1 do projeto: toda tabela, menos empresa, tem empresa_id.
     * Na pratica ela permite que a consulta do download filtre pela empresa
     * SEM precisar passar pelo usuario - quanto mais curto o caminho ate o
     * filtro, menor a chance de alguem escrever uma consulta que esqueca dele.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    /** Funcionario a quem o documento pertence. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    /**
     * O nome que o arquivo tinha no computador de quem enviou, como
     * "CNH frente.jpg". Serve so para exibir na tela e para sugerir o nome no
     * download - o arquivo NAO e gravado com ele (#003-RN013).
     */
    @Column(name = "nome_original", nullable = false, length = 255)
    private String nomeOriginal;

    /**
     * O nome com que o arquivo foi gravado na pasta: um UUID mais a extensao,
     * como "3f2b1a90-....pdf".
     *
     * TRES PROBLEMAS QUE ISTO RESOLVE DE UMA VEZ
     * 1. dois funcionarios enviando "cnh.jpg" nao sobrescrevem um ao outro;
     * 2. o nome no disco nao conta nada sobre a pessoa;
     * 3. um nome enviado como "../../application.properties" nao tem como
     *    escapar da pasta, porque o nome enviado simplesmente nao e usado.
     */
    @Column(name = "nome_armazenado", nullable = false, length = 80)
    private String nomeArmazenado;

    /**
     * O tipo do arquivo (application/pdf, image/jpeg ou image/png). E este
     * valor, e nao o que o navegador disser depois, que o download devolve.
     */
    @Column(name = "tipo_conteudo", nullable = false, length = 100)
    private String tipoConteudo;

    /** Tamanho em bytes. Guardado para a tela mostrar sem abrir o arquivo. */
    @Column(nullable = false)
    private long tamanho;

    /**
     * Quando o anexo entrou no sistema - regra 4 do projeto.
     *
     * Quem preenche e o sistema, no momento do envio, e nao existe tela que
     * altere este campo.
     */
    @Column(name = "data_registro", nullable = false)
    private LocalDateTime dataRegistro;

    /** Construtor vazio exigido pelo JPA. */
    public AnexoUsuario() {
    }

    public AnexoUsuario(Empresa empresa, Usuario usuario, String nomeOriginal,
                        String nomeArmazenado, String tipoConteudo, long tamanho) {
        this.empresa = empresa;
        this.usuario = usuario;
        this.nomeOriginal = nomeOriginal;
        this.nomeArmazenado = nomeArmazenado;
        this.tipoConteudo = tipoConteudo;
        this.tamanho = tamanho;
        this.dataRegistro = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public String getNomeOriginal() {
        return nomeOriginal;
    }

    public void setNomeOriginal(String nomeOriginal) {
        this.nomeOriginal = nomeOriginal;
    }

    public String getNomeArmazenado() {
        return nomeArmazenado;
    }

    public void setNomeArmazenado(String nomeArmazenado) {
        this.nomeArmazenado = nomeArmazenado;
    }

    public String getTipoConteudo() {
        return tipoConteudo;
    }

    public void setTipoConteudo(String tipoConteudo) {
        this.tipoConteudo = tipoConteudo;
    }

    public long getTamanho() {
        return tamanho;
    }

    public void setTamanho(long tamanho) {
        this.tamanho = tamanho;
    }

    public LocalDateTime getDataRegistro() {
        return dataRegistro;
    }

    public void setDataRegistro(LocalDateTime dataRegistro) {
        this.dataRegistro = dataRegistro;
    }
}
