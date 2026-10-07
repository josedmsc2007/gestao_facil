package com.gestaofacil.controller.form;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * Os campos da tela de saida de veiculo (#006-RF01).
 *
 * O QUE ESTA CLASSE NAO TEM, DE PROPOSITO
 *
 * 1) EMPRESA e MOTORISTA. Os dois vem da sessao de quem esta logado (regra 1
 *    do projeto e #006-RN004). Se fossem campos, bastaria um
 *    <input type="hidden"> a mais para lancar a saida em outra empresa ou em
 *    nome de outro motorista.
 *
 * 2) STATUS DO VEICULO (regra 2 e #006-RN003). Quem muda o status e o
 *    service, como consequencia da saida.
 *
 * 3) DATA DE REGISTRO (regra 4 e #006-RN005). E a hora em que o lancamento
 *    entrou no sistema, preenchida pelo servidor. Mesmo que alguem envie
 *    "dataRegistro=..." a mao, o Spring nao tem onde guardar o valor.
 *
 * O veiculo e o centro de custo chegam como ID, e nao como objeto: sao so o
 * numero da opcao escolhida na caixa de selecao. Quem transforma o numero em
 * registro e o service, sempre conferindo a empresa.
 */
public class SaidaForm {

    /** #006-RN001. */
    @NotNull(message = "Escolha o veículo.")
    private Long veiculoId;

    /**
     * #006-RN001. A mensagem nao diz "a obra" nem "o contrato": o rotulo e
     * da empresa e o sistema nao conhece o genero dele (regra 6).
     */
    @NotNull(message = "Escolha o destino.")
    private Long centroCustoId;

    /**
     * #006-RN001 e RN005: obrigatoria, e pode ser retroativa.
     *
     * O campo da tela e um <input type="datetime-local">, que envia o texto
     * "2026-10-01T14:30". O @DateTimeFormat ensina o Spring a ler esse
     * formato - e a escrever nele quando a tela e redesenhada.
     */
    @NotNull(message = "Informe a data e a hora da saída.")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime dataSaida;

    /** Opcional (#006-RF01). */
    @Size(max = 500, message = "A observação deve ter no máximo 500 caracteres.")
    private String observacao;

    public Long getVeiculoId() {
        return veiculoId;
    }

    public void setVeiculoId(Long veiculoId) {
        this.veiculoId = veiculoId;
    }

    public Long getCentroCustoId() {
        return centroCustoId;
    }

    public void setCentroCustoId(Long centroCustoId) {
        this.centroCustoId = centroCustoId;
    }

    public LocalDateTime getDataSaida() {
        return dataSaida;
    }

    public void setDataSaida(LocalDateTime dataSaida) {
        this.dataSaida = dataSaida;
    }

    public String getObservacao() {
        return observacao;
    }

    /**
     * Campo em branco vira nulo - a mesma limpeza dos outros formularios,
     * para "observacao em branco" nao ficar diferente de "nao informada".
     */
    public void setObservacao(String observacao) {
        if (observacao == null || observacao.trim().isEmpty()) {
            this.observacao = null;
        } else {
            this.observacao = observacao.trim();
        }
    }
}
