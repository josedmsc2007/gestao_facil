package com.gestaofacil.model;

/**
 * Perfis de acesso previstos no documento de requisitos (secao 2).
 *
 * E um enum, e nao uma tabela, porque os dois perfis sao fixos: nao existe
 * tela para a empresa cadastrar perfis novos. Guardar como texto no banco
 * (ver @Enumerated(EnumType.STRING) em Usuario) deixa a coluna legivel:
 * aparece "ADMINISTRADOR" e nao um numero.
 */
public enum Perfil {

    /** Cadastra empresa, veiculos, centros de custo e usuarios; ve relatorios. */
    ADMINISTRADOR,

    /** Registra saida, devolucao e abastecimento dos veiculos que utiliza. */
    MOTORISTA
}
