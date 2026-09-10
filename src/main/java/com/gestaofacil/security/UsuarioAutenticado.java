package com.gestaofacil.security;

import com.gestaofacil.model.Empresa;
import com.gestaofacil.model.Perfil;
import com.gestaofacil.model.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * O usuario logado, do jeito que o Spring Security entende.
 *
 * O Spring nao conhece a nossa classe Usuario: ele so sabe trabalhar com a
 * interface UserDetails. Esta classe e a "tradutora" entre as duas.
 *
 * POR QUE ELA COPIA OS VALORES EM VEZ DE GUARDAR O Usuario
 * --------------------------------------------------------
 * Este objeto fica guardado na SESSAO, ou seja, sobrevive depois que a
 * consulta ao banco terminou. Como o campo Usuario.empresa e LAZY e o projeto
 * usa open-in-view=false, chamar usuario.getEmpresa().getNome() mais tarde
 * estouraria com LazyInitializationException.
 *
 * Copiando os valores no construtor, o objeto fica independente do banco.
 * De quebra, o hash da senha NAO entra aqui - nao ha motivo para ele ficar
 * passeando pela sessao depois que a conferencia ja foi feita.
 */
public class UsuarioAutenticado implements UserDetails {

    /* --- dados do usuario --- */
    private final Long id;
    private final String login;
    private final String nome;
    private final Perfil perfil;
    private final boolean ativo;
    private final boolean senhaTemporaria;

    /* --- dados da empresa: todo o resto do sistema precisa deles para
           filtrar as consultas (regra 1 do projeto) e para montar os rotulos
           das telas (regra 6) --- */
    private final Long empresaId;
    private final String empresaIdentificador;
    private final String empresaNome;
    private final String rotuloCcSingular;
    private final String rotuloCcPlural;

    public UsuarioAutenticado(Usuario usuario, Empresa empresa) {
        this.id = usuario.getId();
        this.login = usuario.getLogin();
        this.nome = usuario.getNome();
        this.perfil = usuario.getPerfil();
        this.ativo = usuario.isAtivo();
        this.senhaTemporaria = usuario.isSenhaTemporaria();

        this.empresaId = empresa.getId();
        this.empresaIdentificador = empresa.getIdentificador();
        this.empresaNome = empresa.getNome();
        this.rotuloCcSingular = empresa.getRotuloCcSingular();
        this.rotuloCcPlural = empresa.getRotuloCcPlural();
    }

    /** Construtor interno, usado so pelo metodo comSenhaJaTrocada(). */
    private UsuarioAutenticado(UsuarioAutenticado original, boolean senhaTemporaria) {
        this.id = original.id;
        this.login = original.login;
        this.nome = original.nome;
        this.perfil = original.perfil;
        this.ativo = original.ativo;
        this.senhaTemporaria = senhaTemporaria;

        this.empresaId = original.empresaId;
        this.empresaIdentificador = original.empresaIdentificador;
        this.empresaNome = original.empresaNome;
        this.rotuloCcSingular = original.rotuloCcSingular;
        this.rotuloCcPlural = original.rotuloCcPlural;
    }

    /**
     * Devolve uma copia deste usuario com a senha ja marcada como definitiva.
     *
     * POR QUE ISSO E NECESSARIO
     * Este objeto foi montado no momento do login e ficou guardado na sessao.
     * Quando o usuario troca a senha temporaria, o banco e atualizado, mas a
     * copia da sessao continuaria dizendo senhaTemporaria = true - e ele seria
     * mandado de volta para a tela de troca a cada clique, para sempre.
     *
     * O TrocaSenhaController usa este metodo para colocar a versao corrigida
     * na sessao logo depois de gravar no banco.
     */
    public UsuarioAutenticado comSenhaJaTrocada() {
        return new UsuarioAutenticado(this, false);
    }

    /**
     * As "permissoes" do usuario. O Spring exige o prefixo ROLE_ para poder
     * usar hasRole("ADMINISTRADOR") nas regras de acesso.
     *
     * Cada usuario tem exatamente um perfil, entao a lista sempre tem 1 item.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + perfil.name()));
    }

    /**
     * Devolve null de proposito: quem confere a senha e o
     * AutenticacaoPorEmpresaProvider, comparando com o hash direto do banco.
     * O hash nunca precisa entrar neste objeto.
     */
    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return login;
    }

    /** RN009: usuario inativo nao autentica. */
    @Override
    public boolean isEnabled() {
        return ativo;
    }

    /* Os tres metodos abaixo sao exigidos pela interface. O bloqueio por
       tentativas invalidas (RN004) e conferido no provider, na Etapa 3. */

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /* --- getters usados pelas telas e pelos controllers --- */

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public Perfil getPerfil() {
        return perfil;
    }

    /** RF07: enquanto for true, o usuario so pode ver a tela de troca de senha. */
    public boolean isSenhaTemporaria() {
        return senhaTemporaria;
    }

    public Long getEmpresaId() {
        return empresaId;
    }

    public String getEmpresaIdentificador() {
        return empresaIdentificador;
    }

    public String getEmpresaNome() {
        return empresaNome;
    }

    public String getRotuloCcSingular() {
        return rotuloCcSingular;
    }

    public String getRotuloCcPlural() {
        return rotuloCcPlural;
    }
}
