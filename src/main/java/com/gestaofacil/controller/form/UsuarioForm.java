package com.gestaofacil.controller.form;

import com.gestaofacil.model.Perfil;
import com.gestaofacil.model.Usuario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Os campos da tela de cadastro e edicao de usuarios (#003-RF01 e #003-RF08).
 *
 * POR QUE NAO RECEBER UM Usuario DIRETO DO FORMULARIO
 * E a regra do projeto, e aqui ela fica evidente: a entidade Usuario tem os
 * campos senha, ativo, tentativasInvalidas, bloqueadoAte e empresa. Se a tela
 * enviasse um Usuario, bastaria acrescentar
 * <input type="hidden" name="empresa" value="3"> no HTML - pelo navegador
 * mesmo, com o botao direito - para cadastrar um usuario dentro da empresa
 * dos outros. O Spring preencheria o campo sem reclamar.
 *
 * Nesta classe simplesmente NAO EXISTE campo de empresa (#003-RF03), nem de
 * senha, nem de bloqueio. O que a tela nao pode enviar, ela nao envia.
 */
public class UsuarioForm {

    /**
     * Preenchido apenas na edicao, para a tela saber para onde enviar o
     * formulario. Mesmo assim ele nao e usado para buscar o usuario: quem
     * manda e o id da URL, conferido junto com a empresa do logado.
     */
    private Long id;

    @NotBlank(message = "Informe o nome do funcionário.")
    @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres.")
    private String nome;

    @Size(max = 80, message = "O cargo deve ter no máximo 80 caracteres.")
    private String cargo;

    /**
     * #003-RN001 e RN006. Aceita com ou sem mascara ("123.456.789-09"): o
     * controller confere os digitos verificadores com o ValidadorDeCpf e o
     * service grava so os numeros.
     */
    @NotBlank(message = "Informe o CPF.")
    @Size(max = 14, message = "CPF inválido.")
    private String cpf;

    /**
     * #003-RN005: unico dentro da empresa.
     *
     * O @Pattern so aceita letras minusculas, numeros, ponto, hifen e
     * sublinhado. A restricao nasceu de um problema real do card #001: o
     * teclado do celular poe maiuscula na primeira letra, e um login gravado
     * como "Joao" nunca mais entra quando o motorista digita "joao". Sem
     * maiuscula no cadastro, o problema deixa de existir.
     */
    @NotBlank(message = "Informe o usuário de acesso.")
    @Size(max = 60, message = "O usuário deve ter no máximo 60 caracteres.")
    @Pattern(regexp = "^[a-z0-9._-]+$",
            message = "Use apenas letras minúsculas, números, ponto, hífen ou sublinhado.")
    private String login;

    /** #003-RN001. A lista oferecida vem de Perfil.atribuiveisPelaEmpresa(). */
    @NotNull(message = "Escolha o perfil de acesso.")
    private Perfil perfil;

    /* --- Habilitacao: obrigatoria so para o MOTORISTA (#003-RN002).
           Quem cobra isso e o controller, porque a regra depende do perfil
           escolhido e uma anotacao sozinha nao enxerga outro campo. --- */

    @Size(max = 11, message = "A CNH deve ter no máximo 11 números.")
    private String cnh;

    @Size(max = 5, message = "A categoria deve ter no máximo 5 caracteres.")
    private String categoriaCnh;

    /**
     * @DateTimeFormat ensina o Spring a ler a data no formato que o
     * <input type="date"> envia (aaaa-mm-dd) e a devolve-la no mesmo formato
     * quando a tela for redesenhada.
     */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate validadeCnh;

    public UsuarioForm() {
    }

    /**
     * Monta o formulario a partir de um usuario ja gravado, para a tela de
     * edicao aparecer preenchida.
     *
     * Repare no que NAO e copiado: senha, empresa, ativo e os campos de
     * bloqueio. Eles nao pertencem a esta tela.
     */
    public static UsuarioForm deUsuario(Usuario usuario) {
        UsuarioForm form = new UsuarioForm();
        form.setId(usuario.getId());
        form.setNome(usuario.getNome());
        form.setCargo(usuario.getCargo());
        form.setCpf(usuario.getCpf());
        form.setLogin(usuario.getLogin());
        form.setPerfil(usuario.getPerfil());
        form.setCnh(usuario.getCnh());
        form.setCategoriaCnh(usuario.getCategoriaCnh());
        form.setValidadeCnh(usuario.getValidadeCnh());
        return form;
    }

    /**
     * Tira os espacos das pontas e transforma campo vazio em nulo.
     *
     * POR QUE ISSO IMPORTA
     * O navegador envia "" (texto vazio) para todo campo que o usuario deixou
     * em branco. Sem esta limpeza, o banco guardaria "" em vez de nulo, e um
     * "cargo em branco" ficaria diferente de "cargo nao informado". Um espaco
     * digitado sem querer no fim do login tambem quebraria a comparacao na
     * hora de entrar no sistema.
     */
    private static String limpar(String texto) {
        if (texto == null) {
            return null;
        }
        String semEspacos = texto.trim();
        return semEspacos.isEmpty() ? null : semEspacos;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = limpar(nome);
    }

    public String getCargo() {
        return cargo;
    }

    public void setCargo(String cargo) {
        this.cargo = limpar(cargo);
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = limpar(cpf);
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = limpar(login);
    }

    public Perfil getPerfil() {
        return perfil;
    }

    public void setPerfil(Perfil perfil) {
        this.perfil = perfil;
    }

    public String getCnh() {
        return cnh;
    }

    public void setCnh(String cnh) {
        this.cnh = limpar(cnh);
    }

    public String getCategoriaCnh() {
        return categoriaCnh;
    }

    public void setCategoriaCnh(String categoriaCnh) {
        this.categoriaCnh = limpar(categoriaCnh);
    }

    public LocalDate getValidadeCnh() {
        return validadeCnh;
    }

    public void setValidadeCnh(LocalDate validadeCnh) {
        this.validadeCnh = validadeCnh;
    }
}
