package com.gestaofacil.model;

import java.util.List;

/**
 * Perfis de acesso previstos no documento de requisitos.
 *
 * E um enum porque os perfis sao fixos: nao existe tela
 * para a empresa cadastrar perfis novos.
 */
public enum Perfil {

    /**
     * Equipe responsavel pelo sistema. Cadastra as empresas
     * clientes e os administradores iniciais de cada uma, e gera nova senha
     * para um administrador que perdeu o acesso.
     *
     * NAO ve dados operacionais de empresa nenhuma e mora na
     * empresa reservada da equipe.
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


    public static List<Perfil> atribuiveisPelaEmpresa() {
        return List.of(ADMINISTRADOR, MOTORISTA);
    }
}
