package com.gestaofacil.model;

import java.util.List;

/**
 * Tipo de combustivel do veiculo (#004-RF01).
 *
 * POR QUE UM ENUM, E NAO UM CAMPO DE TEXTO LIVRE
 * Com texto livre a mesma frota acabaria com "Diesel", "diesel", "Disel" e
 * "óleo diesel" - e o relatorio de custo por combustivel (RF09) nao teria
 * como somar. Com o enum a tela vira uma caixa de selecao e so existem os
 * valores desta lista.
 *
 * Assim como o Perfil, e gravado como TEXTO no banco
 * (@Enumerated(EnumType.STRING) em Veiculo): a coluna fica legivel no pgAdmin
 * e reordenar o enum nao troca o combustivel de ninguem.
 */
public enum TipoCombustivel {

    GASOLINA("Gasolina"),
    ETANOL("Etanol"),

    /** Gasolina ou etanol, no mesmo tanque - a maioria dos carros de passeio. */
    FLEX("Flex (gasolina/etanol)"),

    DIESEL("Diesel"),

    /** Diesel de baixo enxofre, exigido pelos caminhoes mais novos. */
    DIESEL_S10("Diesel S-10"),

    /** Gas natural veicular. */
    GNV("GNV"),

    ELETRICO("Elétrico");

    private final String rotulo;

    TipoCombustivel(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }

    /**
     * Os tipos oferecidos na tela, na ordem em que aparecem.
     *
     * Aqui values() PODE ser usado: diferente do Perfil, nao existe tipo de
     * combustivel "interno" que precise ficar de fora da caixa de selecao.
     * O metodo existe so para a tela nao depender da forma como o enum e
     * percorrido.
     */
    public static List<TipoCombustivel> paraSelecao() {
        return List.of(values());
    }
}
