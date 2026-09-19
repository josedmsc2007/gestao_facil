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

    /**
     * Equipe responsavel pelo sistema (#002-RF01). Cadastra as empresas
     * clientes e os administradores iniciais de cada uma, e gera nova senha
     * para um administrador que perdeu o acesso (#002-RF08).
     *
     * NAO ve dados operacionais de empresa nenhuma (#002-RN008) e mora na
     * empresa reservada da equipe (#002-RN010). Nao e atribuivel por nenhuma
     * tela da empresa - veja atribuiveisPelaEmpresa(), abaixo.
     */
    OPERADOR("Operador"),

    /** Cadastra veiculos, centros de custo e usuarios da empresa; ve relatorios. */
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
     * A tentacao e escrever "return List.of(values())". Mas values() inclui
     * o OPERADOR, usado so pela equipe do sistema (#002): ele apareceria
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
