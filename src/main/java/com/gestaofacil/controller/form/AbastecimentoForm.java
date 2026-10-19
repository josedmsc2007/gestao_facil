package com.gestaofacil.controller.form;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Os campos da tela de abastecimento (#007-RF01).
 *
 * O QUE ESTA CLASSE NAO TEM, DE PROPOSITO - o mesmo raciocinio do SaidaForm:
 *
 * 1) EMPRESA e MOTORISTA. Vem da sessao de quem esta logado (regra 1 e
 *    #007-RN007).
 *
 * 2) DATA DE REGISTRO (regra 4 e #007-RN005). Preenchida pelo servidor.
 *
 * Os obrigatorios (#007-RN001) sao conferidos aqui, com @NotNull, e viram
 * erro no proprio campo. "Maior que zero" e "data no futuro" (#007-RN006)
 * ficam no AbastecimentoService, como as regras de data do #006.
 *
 * LITROS E VALOR COM VIRGULA
 * Os dois sao BigDecimal, mas quem le o texto digitado e o
 * DecimalComVirgulaEditor (registrado no AbastecimentoController): ele
 * aceita "45,50" e "45.50".
 */
public class AbastecimentoForm {

    /** #007-RN003. */
    @NotNull(message = "Escolha o veículo.")
    private Long veiculoId;

    /**
     * #007-RN001 e RN005: obrigatoria, e pode ser retroativa.
     *
     * O campo da tela e um <input type="date">, que envia "2026-10-01".
     */
    @NotNull(message = "Informe a data do abastecimento.")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate data;

    @NotNull(message = "Informe a quilometragem.")
    private Integer quilometragem;

    /** Ate 3 casas, como a bomba mostra. Mesmo tamanho da coluna. */
    @NotNull(message = "Informe os litros abastecidos.")
    @Digits(integer = 7, fraction = 3,
            message = "Informe os litros com no máximo 3 casas depois da vírgula.")
    private BigDecimal litros;

    /** Reais e centavos: 2 casas, mesmo tamanho da coluna. */
    @NotNull(message = "Informe o valor pago.")
    @Digits(integer = 8, fraction = 2,
            message = "Informe o valor com no máximo 2 casas depois da vírgula.")
    private BigDecimal valor;

    /** Opcional (#007-RN002). */
    @Size(max = 120, message = "O posto deve ter no máximo 120 caracteres.")
    private String posto;

    public Long getVeiculoId() {
        return veiculoId;
    }

    public void setVeiculoId(Long veiculoId) {
        this.veiculoId = veiculoId;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public Integer getQuilometragem() {
        return quilometragem;
    }

    public void setQuilometragem(Integer quilometragem) {
        this.quilometragem = quilometragem;
    }

    public BigDecimal getLitros() {
        return litros;
    }

    public void setLitros(BigDecimal litros) {
        this.litros = litros;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public String getPosto() {
        return posto;
    }

    /** Campo em branco vira nulo, como a observacao do SaidaForm. */
    public void setPosto(String posto) {
        if (posto == null || posto.trim().isEmpty()) {
            this.posto = null;
        } else {
            this.posto = posto.trim();
        }
    }
}
