package com.gestaofacil.service;

/**
 * Confere se um CPF e valido pelos digitos verificadores (#003-RN006).
 *
 * POR QUE CONFERIR OS DIGITOS, E NAO SO CONTAR 11 NUMEROS
 * Um CPF nao e um numero qualquer: os dois ultimos digitos sao calculados a
 * partir dos nove primeiros. Isso permite descobrir na hora do cadastro um
 * erro de digitacao - dois numeros trocados de lugar quase sempre quebram a
 * conta. Sem essa conferencia, o CPF errado so apareceria meses depois, num
 * relatorio, sem ninguem saber qual era o certo.
 *
 * ATENCAO ao que este validador NAO faz: ele diz que o numero e bem formado,
 * nao que a pessoa existe. Consultar a Receita Federal e outro assunto, fora
 * do escopo do projeto.
 *
 * E uma classe de metodos estaticos (nao e um @Service) porque nao depende de
 * nada: nao vai ao banco, nao guarda estado. Chama-se
 * ValidadorDeCpf.valido(...) de qualquer lugar.
 */
public final class ValidadorDeCpf {

    /** Construtor privado: ninguem precisa criar um ValidadorDeCpf. */
    private ValidadorDeCpf() {
    }

    /**
     * Devolve so os numeros do texto: "123.456.789-09" vira "12345678909".
     *
     * O "[^0-9]" quer dizer "todo caractere que NAO e um numero de 0 a 9":
     * pontos, tracos e espacos. Como o CPF e guardado so com numeros
     * (#003-RN006), toda entrada passa por aqui antes de ser comparada.
     */
    public static String somenteNumeros(String cpf) {
        if (cpf == null) {
            return "";
        }
        return cpf.replaceAll("[^0-9]", "");
    }

    /**
     * O CPF informado e valido?
     *
     * Aceita com ou sem mascara: a primeira coisa que o metodo faz e tirar a
     * pontuacao.
     */
    public static boolean valido(String cpf) {

        String numeros = somenteNumeros(cpf);

        // Todo CPF tem exatamente 11 digitos.
        if (numeros.length() != 11) {
            return false;
        }

        /*
         * Casos como 111.111.111-11 e 000.000.000-00 passam na conta dos
         * digitos verificadores por coincidencia matematica, mas nao sao CPFs
         * de verdade. Sao tambem o que alguem digita para "preencher rapido",
         * entao e importante recusa-los.
         *
         * distinct() joga fora os repetidos: se sobrou um caractere so, os 11
         * eram iguais.
         */
        if (numeros.chars().distinct().count() == 1) {
            return false;
        }

        // O 10o digito confere os 9 primeiros; o 11o confere os 10 primeiros.
        int decimoDigito = Character.getNumericValue(numeros.charAt(9));
        int decimoPrimeiroDigito = Character.getNumericValue(numeros.charAt(10));

        return calcularDigito(numeros, 9) == decimoDigito
                && calcularDigito(numeros, 10) == decimoPrimeiroDigito;
    }

    /**
     * Calcula um digito verificador.
     *
     * A REGRA, EM PALAVRAS
     * Multiplique cada digito por um peso que comeca em (quantidade + 1) e vai
     * caindo de um em um. Some tudo, divida por 11 e olhe o RESTO:
     * resto 0 ou 1 -> o digito e 0; caso contrario -> o digito e 11 - resto.
     *
     * Exemplo com o CPF 111.444.777-35, conferindo o primeiro digito (o 3):
     * pesos 10, 9, 8, 7, 6, 5, 4, 3, 2 sobre os digitos 1, 1, 1, 4, 4, 4, 7,
     * 7, 7 -> soma 162 -> 162 % 11 = 8 -> 11 - 8 = 3, que e exatamente o
     * primeiro digito verificador desse CPF. Vale fazer a conta no papel uma
     * vez: e o melhor jeito de defender este metodo na apresentacao.
     *
     * @param numeros            o CPF ja sem pontuacao
     * @param quantidadeDeDigitos quantos digitos entram na conta (9 ou 10)
     */
    private static int calcularDigito(String numeros, int quantidadeDeDigitos) {

        int soma = 0;
        int peso = quantidadeDeDigitos + 1;

        for (int posicao = 0; posicao < quantidadeDeDigitos; posicao++) {
            soma += Character.getNumericValue(numeros.charAt(posicao)) * peso;
            peso--;
        }

        int resto = soma % 11;
        return (resto < 2) ? 0 : 11 - resto;
    }
}
