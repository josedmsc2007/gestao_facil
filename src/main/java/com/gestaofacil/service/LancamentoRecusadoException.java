package com.gestaofacil.service;

/**
 * Um lancamento (saida, devolucao...) foi recusado por uma regra de negocio.
 *
 * E a mesma ideia do AnexoInvalidoException: o problema nao e de um campo do
 * formulario, e da situacao - o motorista ja esta com um veiculo, ou o
 * veiculo escolhido acabou de sair com outra pessoa. O service recusa
 * lancando esta excecao, com a mensagem ja pronta para o funcionario ler
 * (o motivo E o que fazer), e o controller so a transforma em aviso na tela.
 *
 * Nasceu no card #006, mas o nome e generico de proposito: abastecimento
 * (#007) e manutencao (#008) vao ter as suas recusas.
 */
public class LancamentoRecusadoException extends RuntimeException {

    public LancamentoRecusadoException(String mensagem) {
        super(mensagem);
    }
}
