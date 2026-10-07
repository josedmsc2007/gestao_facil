package com.gestaofacil.controller.form;

import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * O unico campo da tela de devolucao (#006-RF02): quando o veiculo voltou.
 *
 * E uma classe separada do SaidaForm porque sao duas finalidades (padrao do
 * projeto: um formulario por finalidade). Na devolucao o veiculo, o destino
 * e a data de saida NAO podem mudar - e a forma de garantir isso e eles nao
 * existirem aqui. Esconder os campos na tela nao bastaria.
 *
 * Tambem nao existe "id da saida": a saida que sera fechada e a que esta em
 * aberto para o motorista logado, e pela #006-RN002 so ha uma. Sem id no
 * formulario, nao ha como devolver o veiculo de outra pessoa.
 */
public class DevolucaoForm {

    /** Mesmo formato do SaidaForm: o texto do <input type="datetime-local">. */
    @NotNull(message = "Informe a data e a hora da devolução.")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime dataDevolucao;

    public LocalDateTime getDataDevolucao() {
        return dataDevolucao;
    }

    public void setDataDevolucao(LocalDateTime dataDevolucao) {
        this.dataDevolucao = dataDevolucao;
    }
}
