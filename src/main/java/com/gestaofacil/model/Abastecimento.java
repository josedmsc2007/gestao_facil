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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Um abastecimento de veiculo (RF07 / card #007).
 *
 * E o mesmo desenho do UsoVeiculo, mais simples: o abastecimento nao tem
 * "em aberto" nem "fechado". Ele nasce pronto e nao muda mais - nao ha
 * edicao nem exclusao (regra 3).
 *
 * O QUE ESTA CLASSE NAO FAZ, DE PROPOSITO
 * Nao mexe no status do veiculo. Abastecer nao tira o veiculo da rua nem o
 * devolve: um veiculo EM_USO continua EM_USO (regra 2).
 */
@Entity
@Table(name = "abastecimento")
public class Abastecimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Empresa dona do lancamento (regra 1). Mesmo motivo do UsoVeiculo. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    /** O veiculo abastecido (#007-RN003). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "veiculo_id", nullable = false)
    private Veiculo veiculo;

    /**
     * Quem lancou (#007-RN007). Vem SEMPRE de quem esta logado: o formulario
     * nao tem campo de motorista.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    /**
     * O dia do abastecimento, COMO O MOTORISTA INFORMOU (#007-RN005). So a
     * data, sem hora: e o que vem no cupom do posto. Pode ser retroativa.
     */
    @Column(name = "data", nullable = false)
    private LocalDate data;

    /** O que o painel do veiculo marcava, em km inteiros. */
    @Column(nullable = false)
    private Integer quilometragem;

    /**
     * Litros abastecidos. Tres casas decimais porque e assim que a bomba
     * mostra (45,123 L).
     *
     * BigDecimal, e nao double: double guarda 0,1 como 0,1000000000000000055
     * e a soma dos relatorios sairia com centavos errados.
     */
    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal litros;

    /** Valor pago, em reais, com 2 casas (centavos). */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    /** Opcional (#007-RN002). */
    @Column(length = 120)
    private String posto;

    /**
     * Quando o lancamento ENTROU NO SISTEMA (regra 4 e #007-RN005).
     *
     * Igual ao UsoVeiculo: quem preenche e o construtor, com o relogio do
     * servidor. Nao existe setDataRegistro, o AbastecimentoForm nao tem esse
     * campo, e updatable = false deixa a coluna de fora de todo UPDATE.
     */
    @Column(name = "data_registro", nullable = false, updatable = false)
    private LocalDateTime dataRegistro;

    /** Construtor vazio exigido pelo JPA. */
    public Abastecimento() {
    }

    /**
     * Registra um abastecimento.
     *
     * Repare no que ele NAO recebe: a data de registro, que e a hora de agora.
     */
    public Abastecimento(Empresa empresa, Veiculo veiculo, Usuario usuario, LocalDate data,
                         Integer quilometragem, BigDecimal litros, BigDecimal valor,
                         String posto) {
        this.empresa = empresa;
        this.veiculo = veiculo;
        this.usuario = usuario;
        this.data = data;
        this.quilometragem = quilometragem;
        this.litros = litros;
        this.valor = valor;
        this.posto = posto;
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

    public Usuario getUsuario() {
        return usuario;
    }

    public LocalDate getData() {
        return data;
    }

    public Integer getQuilometragem() {
        return quilometragem;
    }

    public BigDecimal getLitros() {
        return litros;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public String getPosto() {
        return posto;
    }

    public LocalDateTime getDataRegistro() {
        return dataRegistro;
    }
}
