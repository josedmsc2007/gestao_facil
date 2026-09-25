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

/**
 * Centro de custo: o lugar (ou contrato, ou rota) a que o uso de um veiculo
 * sera lancado (RF05 / card #005).
 *
 * ================== POR QUE NAO SE CHAMA "Obra" ==================
 * E a regra 6 do projeto. "Obra" e so o ROTULO que a Construtora Teste
 * escolheu para este conceito; ele fica em empresa.rotulo_cc_singular e
 * empresa.rotulo_cc_plural, e cada empresa cliente escolhe o seu. Uma
 * transportadora chamaria de "Rota", uma prestadora de servicos de
 * "Contrato".
 *
 * Por isso a classe, a tabela e as mensagens do codigo falam em "centro de
 * custo", generico, e QUEM diz a palavra "Obra" e o cadastro da empresa, na
 * hora de desenhar a tela. Se o nome "Obra" estivesse aqui, vender o sistema
 * para a segunda empresa exigiria mexer no codigo.
 * =================================================================
 *
 * E a base do card #006: cada saida de veiculo aponta para um centro de
 * custo, e e isso que permite o relatorio de custo por centro de custo (RF09).
 */
@Entity
@Table(name = "centro_custo")
public class CentroCusto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Empresa dona deste centro de custo (#005-RN003 e regra 1 do projeto).
     *
     * LAZY pelo mesmo motivo das outras entidades: a lista nao precisa dos
     * dados da empresa, que ja estao na sessao de quem esta logado.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    /** #005-RN001. Ex.: "Residencial Parque das Arvores". */
    @Column(nullable = false, length = 120)
    private String nome;

    /** #005-RN001. */
    @Column(nullable = false, length = 80)
    private String cidade;

    /**
     * #005-RN001. A sigla da unidade federativa, sempre com 2 letras
     * maiusculas: "SP", "MG". Guardar sempre do mesmo jeito e o que permite,
     * mais tarde, agrupar os relatorios por estado - com "sp", "SP" e "S.P."
     * misturados, o agrupamento nao fecha. Quem confere e normaliza e o
     * UnidadesFederativas.
     */
    @Column(nullable = false, length = 2)
    private String estado;

    /** Rua, numero e bairro. Opcional: nem todo centro de custo tem endereco. */
    @Column(length = 200)
    private String endereco;

    /**
     * Regra 3 do projeto e #005-RN004: nada e apagado.
     *
     * A obra que terminou vira ativo = false: ela some das telas de
     * lancamento, mas os usos, abastecimentos e manutencoes ja lancados nela
     * continuam apontando para um registro que existe, e o relatorio daquele
     * periodo continua fechando.
     *
     * Aqui o campo e um booleano, e nao um status como no veiculo, porque
     * centro de custo nao tem situacao calculada: ou esta em uso, ou nao esta.
     */
    @Column(nullable = false)
    private boolean ativo = true;

    /** Construtor vazio exigido pelo JPA. */
    public CentroCusto() {
    }

    /** Construtor com os dados que o cadastro informa (#005-RF01). */
    public CentroCusto(Empresa empresa, String nome, String cidade,
                       String estado, String endereco) {
        this.empresa = empresa;
        this.nome = nome;
        this.cidade = cidade;
        this.estado = estado;
        this.endereco = endereco;
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

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCidade() {
        return cidade;
    }

    public void setCidade(String cidade) {
        this.cidade = cidade;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getEndereco() {
        return endereco;
    }

    public void setEndereco(String endereco) {
        this.endereco = endereco;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    /** "Campinas/SP", como a lista mostra. */
    public String getCidadeComEstado() {
        return cidade + "/" + estado;
    }
}
