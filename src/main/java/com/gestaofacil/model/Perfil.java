package com.gestaofacil.model;

import java.util.List;

/**
 * Perfis de acesso previstos no documento de requisitos (secao 2).
 *
 * E um enum, e nao uma tabela, porque os perfis sao fixos: nao existe tela
 * para a empresa cadastrar perfis novos. Guardar como texto no banco
 * (ver @Enumerated(EnumType.STRING) em Usuario) deixa a coluna legivel:
 * aparece "ADMINISTRADOR" e nao um numero.
 */
public enum Perfil {

    /** Cadastra empresa, veiculos, centros de custo e usuarios; ve relatorios. */
    ADMINISTRADOR("Administrador"),

    /** Registra saida, devolucao e abastecimento dos veiculos que utiliza. */
    MOTORISTA("Motorista");

    /** Como o perfil aparece nas telas, ja em portugues e sem caixa alta. */
    private final String rotulo;

    Perfil(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }

    /**
     * Os perfis que o Administrador da empresa pode atribuir a um usuario
     * (#003-RN007).
     *
     * ================== NAO TROQUE ISTO POR values() ==================
     * A tentacao e escrever "return List.of(values())", que hoje daria no
     * mesmo. Mas o card #002 acrescenta o perfil OPERADOR, usado so pela
     * equipe do sistema - e naquele dia o values() faria o OPERADOR aparecer
     * sozinho na caixa de selecao da tela de usuarios, sem ninguem ter
     * pedido, e a RN007 seria quebrada em silencio.
     *
     * Por isso a lista e escrita a mao: acrescentar um perfil ao enum NAO o
     * torna atribuivel. Quem quiser oferecer um perfil novo na tela precisa
     * vir aqui de proposito.
     * ==================================================================
     */
    public static List<Perfil> atribuiveisPelaEmpresa() {
        return List.of(ADMINISTRADOR, MOTORISTA);
    }
}
