package com.gestaofacil.service;

/**
 * Como a placa e o RENAVAM sao limpos e conferidos antes de ir para o banco
 * (#004-RN001).
 *
 * POR QUE NORMALIZAR ANTES DE GRAVAR
 * E o mesmo raciocinio do ValidadorDeCpf: a restricao unica do banco compara
 * TEXTO. Se um usuario cadastrar "abc-1234" e outro "ABC1234", o banco ve
 * duas placas diferentes e o mesmo veiculo entra duas vezes na frota. Tirando
 * a pontuacao e subindo para maiusculas SEMPRE, no cadastro e na edicao, os
 * dois viram o mesmo texto e a duplicidade e barrada.
 *
 * Igual ao ValidadorDeCpf, e uma classe de metodos estaticos: nao vai ao
 * banco e nao guarda nada.
 */
public final class FormatoDeVeiculo {

    /**
     * Placa valida nos dois padroes que circulam hoje no Brasil:
     *
     *   ABC1234 - padrao antigo: 3 letras + 4 numeros
     *   ABC1D23 - padrao Mercosul: 3 letras, 1 numero, 1 LETRA, 2 numeros
     *
     * Lendo a expressao por partes:
     *   [A-Z]{3}    exatamente tres letras
     *   [0-9]       um numero
     *   [A-Z0-9]    letra (Mercosul) ou numero (padrao antigo)
     *   [0-9]{2}    mais dois numeros
     */
    private static final String PADRAO_DA_PLACA = "^[A-Z]{3}[0-9][A-Z0-9][0-9]{2}$";

    /** Quantos digitos o RENAVAM tem hoje. */
    private static final int DIGITOS_DO_RENAVAM = 11;

    private FormatoDeVeiculo() {
    }

    /**
     * Deixa a placa do jeito que o banco guarda: maiusculas, sem hifen,
     * espaco ou ponto. "abc-1d23" vira "ABC1D23".
     */
    public static String placaNormalizada(String placa) {
        if (placa == null) {
            return "";
        }
        return placa.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
    }

    /** A placa esta num dos dois padroes? Aceita com ou sem hifen. */
    public static boolean placaValida(String placa) {
        return placaNormalizada(placa).matches(PADRAO_DA_PLACA);
    }

    /**
     * Deixa o RENAVAM do jeito que o banco guarda: so numeros e sempre com
     * 11 digitos, completando com zeros a esquerda quando faltar.
     *
     * POR QUE COMPLETAR COM ZEROS
     * O RENAVAM tem 11 digitos desde 2013, mas os documentos antigos mostram
     * so 9 - os zeros da frente nao sao impressos. O mesmo veiculo, entao,
     * pode ser digitado como "123456789" por um funcionario e "00123456789"
     * por outro. Completando sempre, os dois viram o mesmo texto e a
     * conferencia de duplicidade (#004-RN001) funciona.
     *
     * Devolve "" quando o texto nao tem numero nenhum ou tem digitos demais;
     * nesses casos quem recusa e o renavamValido, logo abaixo.
     */
    public static String renavamNormalizado(String renavam) {

        if (renavam == null) {
            return "";
        }

        String numeros = renavam.replaceAll("[^0-9]", "");

        if (numeros.isEmpty() || numeros.length() > DIGITOS_DO_RENAVAM) {
            return "";
        }

        // Ex.: "123456789" (9 digitos) vira "00123456789" (11).
        StringBuilder completo = new StringBuilder(numeros);
        while (completo.length() < DIGITOS_DO_RENAVAM) {
            completo.insert(0, '0');
        }
        return completo.toString();
    }

    /**
     * O RENAVAM informado serve?
     *
     * A conferencia e de FORMATO: tem numeros, cabe em 11 digitos e nao e um
     * numero so repetido ("00000000000"), que e o que alguem digita para
     * preencher rapido. O RENAVAM tambem tem um digito verificador, mas o
     * card nao pede a conta e ela nao cabe na primeira entrega - fica
     * registrado aqui como melhoria possivel.
     */
    public static boolean renavamValido(String renavam) {

        String numeros = renavamNormalizado(renavam);

        if (numeros.length() != DIGITOS_DO_RENAVAM) {
            return false;
        }

        // distinct() joga fora os repetidos: sobrou um caractere so quer
        // dizer que os 11 digitos eram iguais.
        return numeros.chars().distinct().count() > 1;
    }
}
