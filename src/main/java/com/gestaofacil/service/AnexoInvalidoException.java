package com.gestaofacil.service;

/**
 * O arquivo enviado nao passou nas regras de anexo (#003-RN012).
 *
 * POR QUE UMA EXCECAO PROPRIA
 * As regras do anexo nao cabem num BindingResult: nao existe "campo do
 * formulario" com erro - o problema e o arquivo inteiro. E tambem nao cabem
 * num if espalhado pelo controller, porque valem para qualquer tela que um dia
 * receba anexo (a manutencao do veiculo tera a sua, no card de manutencoes).
 *
 * Entao o service recusa lancando esta excecao, com a mensagem ja pronta para
 * o funcionario ler, e o controller so a transforma em aviso na tela. Herdar
 * de RuntimeException evita obrigar todo metodo do caminho a declarar throws.
 */
public class AnexoInvalidoException extends RuntimeException {

    public AnexoInvalidoException(String mensagem) {
        super(mensagem);
    }
}
