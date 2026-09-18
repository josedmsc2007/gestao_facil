package com.gestaofacil.service;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;

/**
 * Sorteia a senha temporaria que o Administrador entrega ao funcionario
 * (#003-RF04 e #003-RF05).
 *
 * DUAS DECISOES QUE VALE DEFENDER NA APRESENTACAO
 *
 * 1. SecureRandom, e nao Random ou Math.random().
 *    O Random comum e previsivel: quem souber uma senha gerada consegue
 *    calcular as proximas. Para senha isso e inaceitavel. O SecureRandom usa
 *    a fonte de aleatoriedade do sistema operacional.
 *
 * 2. Um alfabeto sem caracteres ambiguos.
 *    Esta senha vai ser DITADA - por telefone, no papel, no WhatsApp. Em caixa
 *    alta o zero e a letra O se confundem, e o mesmo acontece com 1, I e l.
 *    Tirando esses cinco caracteres, ninguem perde tempo tentando descobrir o
 *    que estava escrito. Sobram 32 caracteres; com 10 sorteios dao
 *    32^10 combinacoes, mais do que suficiente para uma senha que sera
 *    trocada no primeiro acesso (#001-RF07).
 */
@Service
public class GeradorDeSenhaTemporaria {

    /** Sem as letras I e O, e sem os numeros 0 e 1 - veja o comentario acima. */
    private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    /** 10 caracteres. O minimo exigido pela tela de troca de senha e 8. */
    private static final int TAMANHO = 10;

    /**
     * Um sorteador so, criado uma vez. O SecureRandom pode ser usado por
     * varias requisicoes ao mesmo tempo sem problema.
     */
    private final SecureRandom sorteio = new SecureRandom();

    /**
     * Devolve a senha em texto legivel.
     *
     * Este e o UNICO momento em que a senha existe assim: quem chama grava
     * apenas o hash (#003-RN011) e mostra o texto uma vez na tela. Depois
     * disso nem o Administrador nem o banco conseguem recupera-la - so gerar
     * outra.
     */
    public String gerar() {
        StringBuilder senha = new StringBuilder(TAMANHO);
        for (int posicao = 0; posicao < TAMANHO; posicao++) {
            senha.append(ALFABETO.charAt(sorteio.nextInt(ALFABETO.length())));
        }
        return senha.toString();
    }
}
