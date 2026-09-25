package com.gestaofacil.service;

import java.util.List;

/**
 * As siglas dos estados brasileiros, para o campo "estado" do centro de custo
 * (#005-RN001).
 *
 * POR QUE UMA LISTA FECHADA, E NAO UM CAMPO DE TEXTO DE DUAS LETRAS
 * Pelo mesmo motivo do TipoCombustivel do card #004: com texto livre, um
 * "SP", um "sp" e um "XX" digitado por engano convivem na mesma tabela, e o
 * dia em que alguem quiser somar os custos por estado a conta nao fecha.
 * Com a lista, a tela vira uma caixa de selecao e o servidor recusa o que
 * nao estiver aqui.
 *
 * E uma lista de textos, e nao um enum, porque a sigla E o valor guardado no
 * banco: um enum so acrescentaria uma traducao no meio do caminho, sem
 * ganho nenhum - nao ha rotulo em portugues para exibir, como havia no
 * Perfil ("ADMINISTRADOR" -> "Administrador").
 *
 * Igual ao ValidadorDeCpf e ao FormatoDeVeiculo, e uma classe de metodos
 * estaticos: nao vai ao banco e nao guarda nada.
 */
public final class UnidadesFederativas {

    /** As 26 unidades federativas mais o Distrito Federal, em ordem. */
    private static final List<String> SIGLAS = List.of(
            "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO",
            "MA", "MT", "MS", "MG", "PA", "PB", "PR", "PE", "PI",
            "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO");

    private UnidadesFederativas() {
    }

    /** A lista que a caixa de selecao da tela mostra. */
    public static List<String> todas() {
        return SIGLAS;
    }

    /**
     * Deixa a sigla do jeito que o banco guarda: sem espacos e em maiusculas.
     * "sp " vira "SP".
     */
    public static String normalizada(String estado) {
        if (estado == null) {
            return "";
        }
        return estado.trim().toUpperCase();
    }

    /**
     * A sigla informada existe?
     *
     * ISTO E CONFERIDO NO SERVIDOR DE PROPOSITO. Nao basta a tela oferecer
     * so as 27 siglas na caixa de selecao: o que chega no POST e texto vindo
     * do navegador, e qualquer pessoa consegue alterar isso - e o mesmo
     * raciocinio da lista de perfis do card #003.
     */
    public static boolean valida(String estado) {
        return SIGLAS.contains(normalizada(estado));
    }
}
