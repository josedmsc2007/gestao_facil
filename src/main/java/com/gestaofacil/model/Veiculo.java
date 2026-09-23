package com.gestaofacil.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Veiculo da frota de uma empresa (RF04 / card #004).
 *
 * E a base dos proximos cards: a saida e a devolucao (#006), o abastecimento
 * (#007) e a manutencao (#008) todos apontam para um veiculo.
 */
@Entity
@Table(
        name = "veiculo",
        /*
         * #004-RN001: placa e RENAVAM sao unicos DENTRO da empresa, nao no
         * sistema inteiro - exatamente como o login e o CPF em usuario.
         *
         * O motivo e concreto: se a Construtora Alfa vender um caminhao para
         * a Construtora Beta, o registro antigo continua existindo na Alfa,
         * agora INATIVO (regra 3 do projeto - nada e apagado, senao as saidas
         * e os abastecimentos antigos ficariam apontando para o vazio). Com
         * uma restricao unica sobre a placa sozinha, a Beta nunca conseguiria
         * cadastrar o caminhao que acabou de comprar.
         *
         * Quem garante isto e o BANCO, nao a tela: duas abas enviando a mesma
         * placa ao mesmo tempo passariam as duas pela conferencia do
         * controller, e e esta linha que recusa a segunda.
         */
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_veiculo_empresa_placa",
                        columnNames = {"empresa_id", "placa"}),
                @UniqueConstraint(
                        name = "uk_veiculo_empresa_renavam",
                        columnNames = {"empresa_id", "renavam"})
        }
)
public class Veiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Empresa dona do veiculo (regra 1 do projeto).
     *
     * LAZY pelo mesmo motivo de Usuario.empresa: a lista de veiculos nao
     * precisa dos dados da empresa, que ja estao na sessao de quem esta
     * logado.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    /** Nome ou modelo, como a empresa chama o veiculo. Ex.: "Caminhão Ford Cargo". */
    @Column(nullable = false, length = 120)
    private String nome;

    /** Ano do veiculo (modelo). */
    @Column(nullable = false)
    private Integer ano;

    /**
     * RENAVAM, guardado SO COM NUMEROS e sempre com 11 digitos.
     *
     * Os documentos antigos trazem 9 digitos; o padrao atual tem 11, com
     * zeros a esquerda. O FormatoDeVeiculo completa os zeros antes de gravar,
     * pelo mesmo motivo que o CPF e guardado sem pontuacao (#003-RN006):
     * guardando sempre do mesmo jeito, "0012345678 9" e "123456789" viram o
     * mesmo texto e a restricao unica la de cima funciona de verdade.
     */
    @Column(nullable = false, length = 11)
    private String renavam;

    /**
     * Placa, guardada em MAIUSCULAS e sem hifen: "ABC1D23".
     *
     * Serve tanto para o padrao antigo (ABC1234) quanto para o Mercosul
     * (ABC1D23). Normalizar tem o mesmo motivo do RENAVAM: sem isso
     * "abc-1234" e "ABC1234" seriam duas placas diferentes para o banco.
     */
    @Column(nullable = false, length = 7)
    private String placa;

    /** #004-RF01. Gravado como texto (ver TipoCombustivel). */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_combustivel", nullable = false, length = 20)
    private TipoCombustivel tipoCombustivel;

    /**
     * #004-RN002 e RN003, regra 2 do projeto: o status e CALCULADO.
     *
     * Nasce DISPONIVEL e a partir dai quem o muda sao as regras de negocio
     * (#006 e #008) ou o botao Desativar do Administrador. Repare que nao
     * existe campo "ativo" nesta entidade, diferente de usuario e de centro
     * de custo: aqui o proprio status ja diz se o veiculo saiu de circulacao,
     * e dois campos para a mesma informacao acabariam se contradizendo.
     *
     * Nao precisa de @ColumnDefault: a tabela veiculo esta nascendo neste
     * card, entao nao existem linhas antigas para receberem a coluna vazia.
     * (A armadilha do ddl-auto=update vale para coluna NOT NULL acrescentada
     * a tabela que JA tem dados - ver Empresa.ativa.)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusVeiculo status = StatusVeiculo.DISPONIVEL;

    /** Construtor vazio exigido pelo JPA. */
    public Veiculo() {
    }

    /**
     * Construtor com os dados que o cadastro informa.
     *
     * Repare no que ele NAO recebe: o status. Ele nao e um dado informado,
     * e sim o valor inicial que o proprio campo ja tem la em cima
     * (#004-RN003).
     */
    public Veiculo(Empresa empresa, String nome, Integer ano, String renavam,
                   String placa, TipoCombustivel tipoCombustivel) {
        this.empresa = empresa;
        this.nome = nome;
        this.ano = ano;
        this.renavam = renavam;
        this.placa = placa;
        this.tipoCombustivel = tipoCombustivel;
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

    public Integer getAno() {
        return ano;
    }

    public void setAno(Integer ano) {
        this.ano = ano;
    }

    public String getRenavam() {
        return renavam;
    }

    public void setRenavam(String renavam) {
        this.renavam = renavam;
    }

    public String getPlaca() {
        return placa;
    }

    public void setPlaca(String placa) {
        this.placa = placa;
    }

    public TipoCombustivel getTipoCombustivel() {
        return tipoCombustivel;
    }

    public void setTipoCombustivel(TipoCombustivel tipoCombustivel) {
        this.tipoCombustivel = tipoCombustivel;
    }

    public StatusVeiculo getStatus() {
        return status;
    }

    /**
     * Troca o status do veiculo.
     *
     * ELE E PUBLICO, MAS NAO E PARA A TELA. Quem chama este metodo e o
     * VeiculoService (ao desativar e ao reativar) e, nos proximos cards, as
     * regras de saida/devolucao (#006) e de manutencao (#008). O formulario
     * nao alcanca este campo porque o VeiculoForm nao tem status nenhum -
     * e essa a barreira, nao a visibilidade do metodo (#004-RN003).
     */
    public void setStatus(StatusVeiculo status) {
        this.status = status;
    }

    /** Atalho para a tela: o veiculo nao foi desativado? */
    public boolean isEmCirculacao() {
        return status != null && status.emCirculacao();
    }

    /** A placa como ela aparece no documento: "ABC1D23" vira "ABC-1D23". */
    public String getPlacaFormatada() {
        if (placa == null || placa.length() != 7) {
            return placa;
        }
        return placa.substring(0, 3) + "-" + placa.substring(3);
    }
}
