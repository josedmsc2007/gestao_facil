package com.gestaofacil.model;

/**
 * Situacao em que o veiculo esta agora (#004-RN002).
 *
 * ================== O STATUS NUNCA E DIGITADO ==================
 * E a regra 2 do projeto e a #004-RN003. Nao existe campo de status na tela
 * de cadastro nem na de edicao: o VeiculoForm nao tem esse campo, entao a
 * tela nao tem como enviar um. Quem muda cada valor:
 *
 *   DISPONIVEL     - o sistema, ao cadastrar o veiculo (nasce assim) e
 *                    quando o motorista devolve (card #006);
 *   EM_USO         - o sistema, quando ha uma saida sem devolucao (#006);
 *   EM_MANUTENCAO  - o sistema, quando existe manutencao em andamento (#008);
 *   INATIVO        - o Administrador, pelo botao Desativar (#004-RF03).
 *
 * INATIVO e o unico que uma pessoa escolhe, e mesmo assim por um botao
 * proprio, nunca por uma caixa de selecao no formulario. Ele faz na tabela
 * veiculo o papel que o campo "ativo" faz em usuario e centro de custo: o
 * veiculo vendido ou baixado sai de circulacao sem ser apagado (regra 3).
 * ===============================================================
 */
public enum StatusVeiculo {

    /** Parado na garagem, pronto para sair. */
    DISPONIVEL("Disponível"),

    /** Saiu com um motorista e ainda nao foi devolvido (#006). */
    EM_USO("Em uso"),

    /** Existe uma manutencao em andamento para ele (#008). */
    EM_MANUTENCAO("Em manutenção"),

    /** Vendido, baixado ou fora de uso (#004-RF03 e RN005). */
    INATIVO("Inativo");

    /** Como o status aparece nas telas, ja em portugues. */
    private final String rotulo;

    StatusVeiculo(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }

    /**
     * O veiculo esta em circulacao? (ou seja: nao foi desativado)
     *
     * Existe para a tela nao precisar escrever "status != INATIVO" em varios
     * lugares - se um dia aparecer outro status de fora de circulacao, muda
     * aqui e todas as telas acompanham.
     */
    public boolean emCirculacao() {
        return this != INATIVO;
    }
}
