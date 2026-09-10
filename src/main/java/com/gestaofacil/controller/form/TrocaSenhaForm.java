package com.gestaofacil.controller.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Os campos da tela de troca de senha.
 *
 * POR QUE UMA CLASSE SO PARA ISSO
 * Esta classe existe para NAO usar a entidade Usuario no formulario. Se a
 * tela enviasse um Usuario, alguem poderia acrescentar campos escondidos no
 * HTML - perfil=ADMINISTRADOR, por exemplo - e o Spring preencheria sozinho.
 * Aqui so existem os dois campos que a tela pode mesmo enviar.
 *
 * As anotacoes fazem a validacao no servidor. A tela tambem valida no
 * navegador (atributos required e minlength), mas validacao de navegador se
 * contorna com qualquer ferramenta - a que vale e esta.
 */
public class TrocaSenhaForm {

    /** RN003: minimo de 8 caracteres, conferido aqui e nao na tela de login. */
    @NotBlank(message = "Informe a nova senha.")
    @Size(min = 8, message = "A senha deve ter pelo menos 8 caracteres.")
    private String novaSenha;

    /**
     * Repeticao da senha. Nao e exigida pelo card, mas evita um problema
     * real: como esta tela e obrigatoria no primeiro acesso, um erro de
     * digitacao deixaria o funcionario preso do lado de fora, dependendo do
     * Administrador para gerar outra senha temporaria.
     */
    @NotBlank(message = "Repita a nova senha.")
    private String confirmacaoSenha;

    public String getNovaSenha() {
        return novaSenha;
    }

    public void setNovaSenha(String novaSenha) {
        this.novaSenha = novaSenha;
    }

    public String getConfirmacaoSenha() {
        return confirmacaoSenha;
    }

    public void setConfirmacaoSenha(String confirmacaoSenha) {
        this.confirmacaoSenha = confirmacaoSenha;
    }
}
