package com.gestaofacil.model;

/**
 * O STATUS NUNCA E DIGITADO
 * E a regra do projeto. Nao existe campo de status na tela
 * de cadastro nem na de edicao.
 *
 *   DISPONIVEL     - o sistema, ao cadastrar o veiculo (nasce assim)
 *   EM_USO         - o sistema, quando ha uma saida sem devolucao;
 *   EM_MANUTENCAO  - o sistema, quando existe manutencao em andamento;
 *   INATIVO        - o Administrador, pelo botao Desativar.
 *
 * INATIVO e o unico que uma pessoa escolhe, e mesmo assim por um botao
 * proprio, nunca por uma caixa de selecao no formulario. Ele faz na tabela
 * veiculo o papel que o campo "ativo" faz em usuario e centro de custo: o
 * veiculo vendido ou baixado sai de circulacao sem ser apagado.
 */
public enum StatusVeiculo {

    /** Parado na garagem, pronto para sair. */
    DISPONIVEL("Disponível"),

    /** Saiu com um motorista e ainda nao foi devolvido. */
    EM_USO("Em uso"),

    /** Existe uma manutencao em andamento para ele. */
    EM_MANUTENCAO("Em manutenção"),

    /** Vendido, baixado ou fora de uso. */
    INATIVO("Inativo");

    /** Como o status aparece nas telas. */
    private final String rotulo;

    StatusVeiculo(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }


    public boolean emCirculacao() {
        return this != INATIVO;
    }
}
