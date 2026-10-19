package com.gestaofacil.controller.form;

import java.beans.PropertyEditorSupport;
import java.math.BigDecimal;

/**
 * Ensina o Spring a ler numero com virgula nos campos BigDecimal (#007).
 *
 * O PROBLEMA
 * Sozinho, o Spring so entende "45.50". O motorista digita no padrao
 * brasileiro, "45,50", e levaria um erro de campo sem entender por que.
 *
 * COMO LE
 *   "45,50"     -> 45.50   (virgula e a casa decimal)
 *   "45.50"     -> 45.50   (so ponto: tambem e a casa decimal)
 *   "1.234,56"  -> 1234.56 (os dois: o ponto separa o milhar)
 * Qualquer outra coisa ("abc", "1,2,3") faz o Spring marcar o campo com o
 * erro typeMismatch, cuja mensagem esta no messages.properties.
 *
 * Quem o liga e o @InitBinder do AbastecimentoController - so naquele
 * controller, para nao mudar o comportamento das outras telas.
 */
public class DecimalComVirgulaEditor extends PropertyEditorSupport {

    /** Do texto digitado para o numero. */
    @Override
    public void setAsText(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            setValue(null);   // vazio vira nulo, e o @NotNull avisa
            return;
        }

        String limpo = texto.trim();
        if (limpo.contains(",")) {
            limpo = limpo.replace(".", "").replace(",", ".");
        }

        try {
            setValue(new BigDecimal(limpo));
        } catch (NumberFormatException e) {
            // IllegalArgumentException e o que o Spring transforma em
            // "typeMismatch" no campo, em vez de pagina de erro.
            throw new IllegalArgumentException("Número inválido: " + texto);
        }
    }

    /**
     * Do numero para o texto, quando a tela e redesenhada com o que foi
     * digitado: mostra com virgula, do jeito que o motorista escreveu.
     */
    @Override
    public String getAsText() {
        Object valor = getValue();
        if (valor == null) {
            return "";
        }
        return ((BigDecimal) valor).toPlainString().replace(".", ",");
    }
}
