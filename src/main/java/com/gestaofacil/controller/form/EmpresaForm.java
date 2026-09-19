package com.gestaofacil.controller.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * O formulario de CADASTRO de empresa (#002-RF02 a RF05).
 *
 * Herda nome e rotulos do EmpresaEdicaoForm e acrescenta o que so existe na
 * criacao: o identificador, que depois nao muda mais (#002-RN003), e os dois
 * administradores iniciais (#002-RF05).
 *
 * POR QUE DOIS ADMINISTRADORES EM CAMPOS FIXOS, E NAO UMA LISTA
 * A RN006 exige exatamente o minimo de dois. Com campos fixos e todos
 * obrigatorios, "cadastrar com um administrador so" e simplesmente um
 * formulario com campos em branco - e a validacao comum ja recusa. Uma lista
 * de tamanho variavel pediria JavaScript para acrescentar linhas e uma regra
 * a mais para contar quantas vieram.
 *
 * Os administradores nao tem CPF aqui porque o card pede so nome e login
 * (#002-RF05). Eles completam o proprio cadastro depois, na tela de usuarios
 * da empresa, que exige o CPF ao salvar (#003-RN001).
 */
public class EmpresaForm extends EmpresaEdicaoForm {

    /**
     * #002-RN002: minusculas, numeros e hifen - vai no endereco
     * (/construtora-silva/login), onde espaco e acento nao funcionam.
     *
     * Uma letra maiuscula e RECUSADA, e nao convertida em silencio: o
     * Operador precisa ver exatamente o endereco que vai entregar ao cliente.
     *
     * Nao tem @NotBlank: quando ele vem vazio, o controller sugere um a partir
     * do nome (#002-RF03) em vez de so reclamar.
     */
    @Size(max = 60, message = "O identificador deve ter no máximo 60 caracteres.")
    @Pattern(regexp = "^[a-z0-9-]+$",
            message = "Use apenas letras minúsculas, números e hífen, sem espaço nem acento.")
    private String identificador;

    /* --- Os dois administradores iniciais (#002-RF05 e RN005). O login
           segue a mesma regra do UsuarioForm, pelo mesmo motivo: sem
           maiuscula, o teclado do celular nao atrapalha o acesso. --- */

    @NotBlank(message = "Informe o nome do primeiro administrador.")
    @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres.")
    private String nomeAdministrador1;

    @NotBlank(message = "Informe o usuário de acesso do primeiro administrador.")
    @Size(max = 60, message = "O usuário deve ter no máximo 60 caracteres.")
    @Pattern(regexp = "^[a-z0-9._-]+$",
            message = "Use apenas letras minúsculas, números, ponto, hífen ou sublinhado.")
    private String loginAdministrador1;

    @NotBlank(message = "Informe o nome do segundo administrador.")
    @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres.")
    private String nomeAdministrador2;

    @NotBlank(message = "Informe o usuário de acesso do segundo administrador.")
    @Size(max = 60, message = "O usuário deve ter no máximo 60 caracteres.")
    @Pattern(regexp = "^[a-z0-9._-]+$",
            message = "Use apenas letras minúsculas, números, ponto, hífen ou sublinhado.")
    private String loginAdministrador2;

    /** #002-RN004: o formulario em branco ja vem com a sugestao padrao. */
    public EmpresaForm() {
        setRotuloCcSingular("Obra");
        setRotuloCcPlural("Obras");
    }

    public String getIdentificador() {
        return identificador;
    }

    public void setIdentificador(String identificador) {
        this.identificador = limpar(identificador);
    }

    public String getNomeAdministrador1() {
        return nomeAdministrador1;
    }

    public void setNomeAdministrador1(String nomeAdministrador1) {
        this.nomeAdministrador1 = limpar(nomeAdministrador1);
    }

    public String getLoginAdministrador1() {
        return loginAdministrador1;
    }

    public void setLoginAdministrador1(String loginAdministrador1) {
        this.loginAdministrador1 = limpar(loginAdministrador1);
    }

    public String getNomeAdministrador2() {
        return nomeAdministrador2;
    }

    public void setNomeAdministrador2(String nomeAdministrador2) {
        this.nomeAdministrador2 = limpar(nomeAdministrador2);
    }

    public String getLoginAdministrador2() {
        return loginAdministrador2;
    }

    public void setLoginAdministrador2(String loginAdministrador2) {
        this.loginAdministrador2 = limpar(loginAdministrador2);
    }
}
