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
 * Um uso de veiculo: da saida ate a devolucao (RF06 / card #006).
 *
 * UMA LINHA SO PARA A SAIDA E A DEVOLUCAO
 * A saida cria a linha com data_devolucao NULA; a devolucao preenche essa
 * coluna na mesma linha. "Saida em aberto" e, entao, exatamente isto: uma
 * linha com data_devolucao nula. Nao existe coluna "situacao" para dizer a
 * mesma coisa - duas informacoes iguais acabariam se contradizendo, o mesmo
 * raciocinio do status do veiculo.
 *
 * O QUE ESTA CLASSE NAO TEM, DE PROPOSITO
 * Identificador gerado no celular. O rascunho do card previa lancamento
 * offline; a equipe decidiu que esta versao exige conexao (#006-RN006), entao
 * o unico identificador e o id do banco.
 */
@Entity
@Table(name = "uso_veiculo")
public class UsoVeiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Empresa dona do lancamento (regra 1 do projeto).
     *
     * Ela poderia ser descoberta pelo veiculo, mas toda tabela tem o seu
     * empresa_id: assim a consulta filtra direto por ele, sem depender de
     * join, e ninguem esquece o filtro.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    /** O veiculo que saiu (#006-RN001). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "veiculo_id", nullable = false)
    private Veiculo veiculo;

    /** Para onde o veiculo foi (#006-RN001). E a base do relatorio de custo. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "centro_custo_id", nullable = false)
    private CentroCusto centroCusto;

    /**
     * O motorista que registrou a saida. Vem SEMPRE de quem esta logado: o
     * formulario nao tem campo de motorista, entao ninguem lanca uma saida em
     * nome de outra pessoa.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    /**
     * Quando o veiculo saiu, COMO O MOTORISTA INFORMOU (#006-RN005). Pode ser
     * retroativa: quem esqueceu de lancar de manha lanca a tarde, com a hora
     * certa.
     */
    @Column(name = "data_saida", nullable = false)
    private LocalDateTime dataSaida;

    /** Quando voltou. NULA enquanto o veiculo esta na rua. */
    @Column(name = "data_devolucao")
    private LocalDateTime dataDevolucao;

    /** Opcional (#006-RF01). */
    @Column(length = 500)
    private String observacao;

    /**
     * Quando o lancamento ENTROU NO SISTEMA (regra 4 do projeto e #006-RN005).
     *
     * E diferente da data_saida de proposito: a saida aceita data retroativa,
     * e sem esta coluna nao daria para saber que um lancamento "de segunda"
     * foi digitado na sexta.
     *
     * QUEM PREENCHE E O CONSTRUTOR, com o relogio do servidor. Nao existe
     * setDataRegistro, o SaidaForm nao tem esse campo, e updatable = false
     * faz o Hibernate deixar a coluna de fora de todo UPDATE - nem a
     * devolucao mexe nela.
     */
    @Column(name = "data_registro", nullable = false, updatable = false)
    private LocalDateTime dataRegistro;

    /**
     * Quando a DEVOLUCAO entrou no sistema (regra 4 do projeto e #006-RN005).
     *
     * E para a devolucao o que a data_registro e para a saida: a
     * data_devolucao tambem aceita data retroativa, entao sem esta coluna nao
     * daria para saber quando ela foi de fato lancada.
     *
     * NULA enquanto a saida esta em aberto - e tambem nas devolucoes lancadas
     * antes de a coluna existir. Quem preenche e o registrarDevolucao, com o
     * relogio do servidor; nao ha setter nem campo de formulario.
     */
    @Column(name = "data_registro_devolucao")
    private LocalDateTime dataRegistroDevolucao;

    /**
     * Quem LANCOU a devolucao (#006-RN008): o proprio motorista ou o
     * Administrador que encerrou a saida no lugar dele.
     *
     * Sem esta coluna o historico diria que o motorista devolveu sozinho
     * mesmo quando foi o Administrador. NULA nos mesmos casos da coluna acima.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_devolucao_id")
    private Usuario usuarioDevolucao;

    /** Construtor vazio exigido pelo JPA. */
    public UsoVeiculo() {
    }

    /**
     * Registra uma saida.
     *
     * Repare no que ele NAO recebe: a data de registro (e a hora de agora) e
     * a data de devolucao (nasce nula - a saida esta em aberto).
     */
    public UsoVeiculo(Empresa empresa, Veiculo veiculo, CentroCusto centroCusto,
                      Usuario usuario, LocalDateTime dataSaida, String observacao) {
        this.empresa = empresa;
        this.veiculo = veiculo;
        this.centroCusto = centroCusto;
        this.usuario = usuario;
        this.dataSaida = dataSaida;
        this.observacao = observacao;
        this.dataRegistro = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public Veiculo getVeiculo() {
        return veiculo;
    }

    public CentroCusto getCentroCusto() {
        return centroCusto;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public LocalDateTime getDataSaida() {
        return dataSaida;
    }

    public LocalDateTime getDataDevolucao() {
        return dataDevolucao;
    }

    /**
     * Fecha a saida. E a unica mudanca que a linha sofre depois de existir.
     *
     * UM METODO SO, E NAO TRES SETTERS
     * A data informada, o momento do lancamento e quem lancou sao preenchidos
     * JUNTOS. Com setters soltos, alguem poderia gravar a data de devolucao e
     * esquecer de registrar quem foi (#006-RN008).
     *
     * Repare no que ele NAO recebe: o momento do lancamento. Como no
     * construtor, e a hora de agora, do servidor (#006-RN005).
     *
     * @param dataInformada quando o veiculo voltou; pode ser retroativa
     * @param quemDevolveu  o motorista, ou o Administrador no lugar dele
     */
    public void registrarDevolucao(LocalDateTime dataInformada, Usuario quemDevolveu) {
        this.dataDevolucao = dataInformada;
        this.usuarioDevolucao = quemDevolveu;
        this.dataRegistroDevolucao = LocalDateTime.now();
    }

    public String getObservacao() {
        return observacao;
    }

    public LocalDateTime getDataRegistro() {
        return dataRegistro;
    }

    public LocalDateTime getDataRegistroDevolucao() {
        return dataRegistroDevolucao;
    }

    public Usuario getUsuarioDevolucao() {
        return usuarioDevolucao;
    }

    /** O veiculo ainda nao foi devolvido? */
    public boolean isEmAberto() {
        return dataDevolucao == null;
    }
}
