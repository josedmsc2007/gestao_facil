package com.gestaofacil.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Funcionario que acessa o sistema (RF03).
 *
 * Cada usuario pertence a uma empresa e so enxerga os dados dela.
 */
@Entity
@Table(
        name = "usuario",
        /*
         * RN007: o login e unico DENTRO de cada empresa. Duas empresas
         * diferentes podem ter um usuario "joao". Por isso a restricao unica
         * e sobre o par (empresa_id, login), e nao sobre o login sozinho.
         * Quem garante isso e o banco, nao a tela.
         */
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_usuario_empresa_login",
                        columnNames = {"empresa_id", "login"}),

                /*
                 * #003-RN004: o CPF segue a mesma ideia do login - e unico
                 * DENTRO da empresa, nao no sistema inteiro. A mesma pessoa
                 * pode ser motorista em duas construtoras que usam o Gestao
                 * Facil, e nenhuma das duas precisa saber da outra.
                 *
                 * Quem realmente garante isso e esta linha, nao a tela: duas
                 * abas do navegador enviando o mesmo CPF ao mesmo tempo
                 * passariam pela conferencia do controller, mas o banco
                 * recusa a segunda.
                 *
                 * Linhas com cpf nulo nao atrapalham: tanto o PostgreSQL
                 * quanto o H2 permitem varios nulos numa coluna unica. E por
                 * isso que o usuario "admin" da carga inicial, que nasceu sem
                 * CPF, continua funcionando.
                 */
                @UniqueConstraint(
                        name = "uk_usuario_empresa_cpf",
                        columnNames = {"empresa_id", "cpf"})
        }
)
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Empresa dona deste usuario.
     *
     * @ManyToOne = muitos usuarios para uma empresa. No banco isso vira a
     * coluna empresa_id com chave estrangeira para empresa(id).
     *
     * FetchType.LAZY: o JPA so vai ao banco buscar a empresa quando alguem
     * chamar getEmpresa(). Evita carregar dados que a tela nem vai usar.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    /** Nome completo do funcionario. */
    @Column(nullable = false, length = 120)
    private String nome;

    /** Nome usado para entrar no sistema. Ex.: "admin". */
    @Column(nullable = false, length = 60)
    private String login;

    /**
     * RN010: aqui fica o HASH BCrypt da senha, nunca a senha digitada.
     * O hash sempre tem 60 caracteres, por isso length = 60 e suficiente.
     * O nome do campo e "senha" para nao dar a entender que guarda texto puro:
     * quem grava e quem confere e sempre o PasswordEncoder.
     */
    @Column(nullable = false, length = 60)
    private String senha;

    /** Cargo do funcionario na empresa. Ex.: "Mestre de obras". Opcional. */
    @Column(length = 80)
    private String cargo;

    /**
     * CPF, guardado SO COM NUMEROS (#003-RN006): "12345678909", nunca
     * "123.456.789-09". Guardar sempre do mesmo jeito e o que permite
     * comparar dois CPFs e confiar na restricao unica la de cima - com a
     * mascara, o mesmo CPF entraria duas vezes escrito de formas diferentes.
     * Quem tira os pontos e o tracinho e o ValidadorDeCpf.
     *
     * A coluna aceita nulo porque o "admin" da carga inicial nasceu sem CPF,
     * antes deste card. A tela de cadastro (#003-RN001) o exige de todo
     * usuario novo.
     */
    @Column(length = 11)
    private String cpf;

    /**
     * Numero de registro da CNH (#003-RF02).
     *
     * #003-RN003: NAO e o CPF. A CNH mostra o CPF no documento, mas tem um
     * numero proprio, com 11 digitos, que muda a cada renovacao. Por isso os
     * dois vivem em colunas separadas.
     *
     * Obrigatorio apenas para o perfil MOTORISTA (#003-RN002); para o
     * Administrador fica nulo.
     */
    @Column(length = 11)
    private String cnh;

    /** Categoria da habilitacao: A, B, C, D, E ou combinacoes como "AB". */
    @Column(name = "categoria_cnh", length = 5)
    private String categoriaCnh;

    /**
     * Validade da habilitacao. E um LocalDate (data sem hora) porque o que
     * importa e o dia do vencimento - base para o alerta de CNH vencida que
     * o documento de requisitos preve para mais adiante.
     */
    @Column(name = "validade_cnh")
    private LocalDate validadeCnh;

    /**
     * Perfil de acesso (secao 2 dos requisitos).
     *
     * EnumType.STRING grava o texto "ADMINISTRADOR" / "MOTORISTA" na coluna.
     * Se fosse EnumType.ORDINAL o banco guardaria 0 e 1, e bastaria alguem
     * reordenar o enum para todos os usuarios trocarem de perfil sozinhos.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Perfil perfil;

    /**
     * RN009 e regra 3 do projeto: nada e apagado.
     * Desligar um funcionario e marcar ativo = false; assim os lancamentos
     * antigos dele continuam existindo nos relatorios.
     */
    @Column(nullable = false)
    private boolean ativo = true;

    /**
     * RF07: true quando a senha atual foi gerada pelo Administrador.
     * Enquanto estiver true, o login deve levar direto para a tela de troca
     * de senha. (A tela e a proxima tarefa; o campo ja fica preparado.)
     */
    @Column(name = "senha_temporaria", nullable = false)
    private boolean senhaTemporaria = false;

    /** RN004: conta quantas senhas erradas seguidas. Zera ao acertar. */
    @Column(name = "tentativas_invalidas", nullable = false)
    private int tentativasInvalidas = 0;

    /**
     * RN004/RN005: ate quando a conta esta bloqueada.
     * Nulo = conta liberada. O Administrador desbloqueia colocando nulo aqui.
     */
    @Column(name = "bloqueado_ate")
    private LocalDateTime bloqueadoAte;

    /** Construtor vazio exigido pelo JPA. */
    public Usuario() {
    }

    /** Construtor de conveniencia, usado pela carga inicial. */
    public Usuario(Empresa empresa, String nome, String login,
                   String senha, Perfil perfil) {
        this.empresa = empresa;
        this.nome = nome;
        this.login = login;
        this.senha = senha;
        this.perfil = perfil;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public String getCargo() {
        return cargo;
    }

    public void setCargo(String cargo) {
        this.cargo = cargo;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getCnh() {
        return cnh;
    }

    public void setCnh(String cnh) {
        this.cnh = cnh;
    }

    public String getCategoriaCnh() {
        return categoriaCnh;
    }

    public void setCategoriaCnh(String categoriaCnh) {
        this.categoriaCnh = categoriaCnh;
    }

    public LocalDate getValidadeCnh() {
        return validadeCnh;
    }

    public void setValidadeCnh(LocalDate validadeCnh) {
        this.validadeCnh = validadeCnh;
    }

    public Perfil getPerfil() {
        return perfil;
    }

    public void setPerfil(Perfil perfil) {
        this.perfil = perfil;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public boolean isSenhaTemporaria() {
        return senhaTemporaria;
    }

    public void setSenhaTemporaria(boolean senhaTemporaria) {
        this.senhaTemporaria = senhaTemporaria;
    }

    public int getTentativasInvalidas() {
        return tentativasInvalidas;
    }

    public void setTentativasInvalidas(int tentativasInvalidas) {
        this.tentativasInvalidas = tentativasInvalidas;
    }

    public LocalDateTime getBloqueadoAte() {
        return bloqueadoAte;
    }

    public void setBloqueadoAte(LocalDateTime bloqueadoAte) {
        this.bloqueadoAte = bloqueadoAte;
    }
}
