package com.gestaofacil.controller.form;

import com.gestaofacil.model.CentroCusto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Os campos da tela de cadastro e edicao de centro de custo (#005-RF01 e RF02).
 *
 * DUAS COISAS QUE ESTA CLASSE NAO TEM, DE PROPOSITO
 *
 * 1) EMPRESA (#005-RN003 e regra 1 do projeto). Se a tela enviasse um
 *    CentroCusto direto, bastaria acrescentar
 *    <input type="hidden" name="empresa" value="3"> pelo proprio navegador
 *    para cadastrar uma obra dentro da empresa dos outros. Aqui nao existe
 *    campo de empresa: ela vem da sessao de quem esta logado.
 *
 * 2) ATIVO (#005-RN004). Ativar e desativar sao acoes proprias, cada uma com
 *    o seu botao e o seu POST. Se "ativo" fosse um campo do formulario, uma
 *    edicao qualquer poderia desativar a obra sem ninguem perceber - e a
 *    mensagem de confirmacao diria apenas "dados atualizados".
 *
 * NAO EXISTE A PALAVRA "OBRA" AQUI, nem no nome da classe, nem nas mensagens
 * de erro (regra 6 do projeto). O nome que o funcionario le na tela vem de
 * empresa.rotulo_cc_singular, e quem o coloca la e o template.
 */
public class CentroCustoForm {

    /**
     * Preenchido apenas na edicao, para a tela saber para onde enviar o
     * formulario. Mesmo assim nao e ele que busca o registro: quem manda e o
     * id da URL, conferido junto com a empresa do logado.
     */
    private Long id;

    @NotBlank(message = "Informe o nome.")
    @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres.")
    private String nome;

    @NotBlank(message = "Informe a cidade.")
    @Size(max = 80, message = "A cidade deve ter no máximo 80 caracteres.")
    private String cidade;

    /**
     * #005-RN001. A sigla do estado, conferida contra a lista de
     * UnidadesFederativas pelo controller - a anotacao sozinha nao sabe
     * quais siglas existem.
     */
    @NotBlank(message = "Escolha o estado.")
    @Size(max = 2, message = "Use a sigla do estado, com 2 letras.")
    private String estado;

    /** Opcional (#005-RF01). */
    @Size(max = 200, message = "O endereço deve ter no máximo 200 caracteres.")
    private String endereco;

    public CentroCustoForm() {
    }

    /**
     * Monta o formulario a partir de um registro ja gravado, para a tela de
     * edicao aparecer preenchida.
     *
     * Repare no que NAO e copiado: empresa e ativo. Eles nao pertencem a esta
     * tela.
     */
    public static CentroCustoForm deCentroCusto(CentroCusto centroCusto) {
        CentroCustoForm form = new CentroCustoForm();
        form.setId(centroCusto.getId());
        form.setNome(centroCusto.getNome());
        form.setCidade(centroCusto.getCidade());
        form.setEstado(centroCusto.getEstado());
        form.setEndereco(centroCusto.getEndereco());
        return form;
    }

    /**
     * Tira os espacos das pontas e transforma campo vazio em nulo - a mesma
     * limpeza do UsuarioForm e do VeiculoForm.
     *
     * O navegador envia "" para todo campo deixado em branco. Sem isto o
     * banco guardaria "" no endereco, e um "endereco em branco" ficaria
     * diferente de "endereco nao informado".
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

    public String getCidade() {
        return cidade;
    }

    public void setCidade(String cidade) {
        this.cidade = limpar(cidade);
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = limpar(estado);
    }

    public String getEndereco() {
        return endereco;
    }

    public void setEndereco(String endereco) {
        this.endereco = limpar(endereco);
    }
}
