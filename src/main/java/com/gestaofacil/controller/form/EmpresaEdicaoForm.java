package com.gestaofacil.controller.form;

import com.gestaofacil.model.Empresa;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Os campos da empresa que o Operador pode EDITAR (#002-RF07): o nome e os
 * dois rotulos do centro de custo.
 *
 * O QUE FALTA AQUI DE PROPOSITO
 * - identificador: nao muda depois de criado (#002-RN003). Mudar quebraria o
 *   atalho que os funcionarios ja puseram na tela inicial do celular. Como o
 *   campo nao existe nesta classe, mesmo um formulario montado a mao com
 *   name="identificador" nao consegue altera-lo - o Spring nao tem onde
 *   guardar o valor.
 * - ativa: a situacao muda pelos botoes Inativar/Ativar da lista, cada um com
 *   o seu POST, como no cadastro de usuarios.
 *
 * O formulario de CADASTRO (EmpresaForm) herda desta classe e acrescenta o
 * identificador e os dois administradores iniciais.
 */
public class EmpresaEdicaoForm {

    @NotBlank(message = "Informe o nome da empresa.")
    @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres.")
    private String nome;

    /**
     * #002-RF04 e RN004: como o centro de custo se chama nas telas desta
     * empresa. Regra 6 do projeto: o sistema nunca escreve "Obra" fixo - o
     * texto sai daqui.
     */
    @NotBlank(message = "Informe o rótulo no singular.")
    @Size(max = 40, message = "O rótulo deve ter no máximo 40 caracteres.")
    private String rotuloCcSingular;

    @NotBlank(message = "Informe o rótulo no plural.")
    @Size(max = 40, message = "O rótulo deve ter no máximo 40 caracteres.")
    private String rotuloCcPlural;

    public EmpresaEdicaoForm() {
    }

    /** Monta o formulario a partir da empresa gravada, para a tela de edicao. */
    public static EmpresaEdicaoForm deEmpresa(Empresa empresa) {
        EmpresaEdicaoForm form = new EmpresaEdicaoForm();
        form.setNome(empresa.getNome());
        form.setRotuloCcSingular(empresa.getRotuloCcSingular());
        form.setRotuloCcPlural(empresa.getRotuloCcPlural());
        return form;
    }

    /**
     * Tira os espacos das pontas e transforma campo vazio em nulo - o mesmo
     * tratamento do UsuarioForm, pelo mesmo motivo: o navegador envia "" para
     * campo em branco, e um espaco no fim do identificador quebraria o
     * endereco de acesso.
     *
     * protected: a classe filha (EmpresaForm) usa nos campos dela.
     */
    protected static String limpar(String texto) {
        if (texto == null) {
            return null;
        }
        String semEspacos = texto.trim();
        return semEspacos.isEmpty() ? null : semEspacos;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = limpar(nome);
    }

    public String getRotuloCcSingular() {
        return rotuloCcSingular;
    }

    public void setRotuloCcSingular(String rotuloCcSingular) {
        this.rotuloCcSingular = limpar(rotuloCcSingular);
    }

    public String getRotuloCcPlural() {
        return rotuloCcPlural;
    }

    public void setRotuloCcPlural(String rotuloCcPlural) {
        this.rotuloCcPlural = limpar(rotuloCcPlural);
    }
}
