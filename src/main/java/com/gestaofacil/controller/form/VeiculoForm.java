package com.gestaofacil.controller.form;

import com.gestaofacil.model.TipoCombustivel;
import com.gestaofacil.model.Veiculo;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Os campos da tela de cadastro e edicao de veiculos (#004-RF01 e RF02).
 *
 * DUAS COISAS QUE ESTA CLASSE NAO TEM, DE PROPOSITO
 *
 * 1) EMPRESA (regra 1 do projeto). Se a tela enviasse um Veiculo direto,
 *    bastaria acrescentar <input type="hidden" name="empresa" value="3"> pelo
 *    proprio navegador para cadastrar um veiculo na frota dos outros. Aqui
 *    nao existe campo de empresa: ela vem da sessao de quem esta logado.
 *
 * 2) STATUS (regra 2 do projeto e #004-RN003). O status e calculado, nunca
 *    digitado. Como o campo nao existe nesta classe, o Spring nao tem onde
 *    colocar um "status=EM_USO" enviado a mao - o valor e simplesmente
 *    ignorado. A barreira e essa, e nao o fato de a caixa de selecao nao
 *    aparecer na tela: o que a tela esconde, o navegador envia assim mesmo.
 *
 * O rascunho original do card no Trello listava "status" entre os campos do
 * cadastro; ele foi retirado por contrariar a RN003 do proprio rascunho, que
 * ja dizia que o veiculo nasce disponivel.
 */
public class VeiculoForm {

    /**
     * Preenchido apenas na edicao, para a tela saber para onde enviar o
     * formulario. Mesmo assim nao e ele que busca o veiculo: quem manda e o
     * id da URL, conferido junto com a empresa do logado.
     */
    private Long id;

    @NotBlank(message = "Informe o nome ou modelo do veículo.")
    @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres.")
    private String nome;

    /**
     * O limite de cima nao cabe numa anotacao: ele depende do ano em que o
     * sistema estiver rodando. Quem confere "ano no futuro" e o controller.
     * Aqui fica so o piso, que nao muda.
     */
    @NotNull(message = "Informe o ano do veículo.")
    @Min(value = 1900, message = "Informe um ano a partir de 1900.")
    private Integer ano;

    /**
     * #004-RN001. Aceita com ou sem pontuacao: o controller confere o formato
     * com o FormatoDeVeiculo e o service grava so os 11 digitos.
     */
    @NotBlank(message = "Informe o RENAVAM.")
    @Size(max = 14, message = "RENAVAM inválido.")
    private String renavam;

    /** #004-RN001. Aceita "ABC1234", "abc-1234" e o padrao Mercosul "ABC1D23". */
    @NotBlank(message = "Informe a placa.")
    @Size(max = 8, message = "Placa inválida.")
    private String placa;

    @NotNull(message = "Escolha o tipo de combustível.")
    private TipoCombustivel tipoCombustivel;

    public VeiculoForm() {
    }

    /**
     * Monta o formulario a partir de um veiculo ja gravado, para a tela de
     * edicao aparecer preenchida.
     *
     * Repare no que NAO e copiado: empresa e status. Eles nao pertencem a
     * esta tela.
     */
    public static VeiculoForm deVeiculo(Veiculo veiculo) {
        VeiculoForm form = new VeiculoForm();
        form.setId(veiculo.getId());
        form.setNome(veiculo.getNome());
        form.setAno(veiculo.getAno());
        form.setRenavam(veiculo.getRenavam());
        form.setPlaca(veiculo.getPlacaFormatada());
        form.setTipoCombustivel(veiculo.getTipoCombustivel());
        return form;
    }

    /**
     * Tira os espacos das pontas e transforma campo vazio em nulo - mesma
     * limpeza do UsuarioForm.
     *
     * O navegador envia "" para todo campo deixado em branco. Sem isto o
     * @NotBlank ate reclamaria, mas um espaco digitado sem querer no fim da
     * placa passaria e quebraria a comparacao com a placa ja cadastrada.
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

    public Integer getAno() {
        return ano;
    }

    public void setAno(Integer ano) {
        this.ano = ano;
    }

    public String getRenavam() {
        return renavam;
    }

    public void setRenavam(String renavam) {
        this.renavam = limpar(renavam);
    }

    public String getPlaca() {
        return placa;
    }

    public void setPlaca(String placa) {
        this.placa = limpar(placa);
    }

    public TipoCombustivel getTipoCombustivel() {
        return tipoCombustivel;
    }

    public void setTipoCombustivel(TipoCombustivel tipoCombustivel) {
        this.tipoCombustivel = tipoCombustivel;
    }
}
