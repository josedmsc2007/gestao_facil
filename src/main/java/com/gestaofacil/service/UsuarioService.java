package com.gestaofacil.service;

import com.gestaofacil.controller.form.UsuarioForm;
import com.gestaofacil.model.Empresa;
import com.gestaofacil.model.Perfil;
import com.gestaofacil.model.Usuario;
import com.gestaofacil.repository.EmpresaRepository;
import com.gestaofacil.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regras de negocio ligadas ao usuario.
 *
 * O controller cuida da tela (recebe o formulario, escolhe a proxima pagina);
 * o service cuida da REGRA. Separar os dois e o que permite, mais tarde,
 * chamar a mesma regra de outro lugar sem repetir codigo.
 *
 * O PADRAO QUE SE REPETE EM TODO METODO DESTA CLASSE
 * Todos recebem o empresaId do usuario logado e buscam com
 * findByIdAndEmpresaId. Nenhum metodo aqui aceita "so o id" - e a regra 1 do
 * projeto escrita em codigo: mesmo que um id chegue forjado pela URL, a
 * consulta so encontra registro da empresa de quem esta logado.
 */
@Service
public class UsuarioService {

    /**
     * #003-RN015: quantos administradores ativos a empresa precisa ter.
     *
     * A ideia da regra: um administrador sozinho e um ponto unico de falha.
     * Se ele esquecer a senha, bloquear a conta ou sair da empresa, nao sobra
     * ninguem capaz de desbloquear, redefinir senha ou cadastrar - e o cliente
     * fica trancado do lado de fora do proprio sistema (#003-RN016).
     */
    public static final int MINIMO_DE_ADMINISTRADORES = 2;

    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;
    private final PasswordEncoder passwordEncoder;
    private final GeradorDeSenhaTemporaria geradorDeSenha;
    private final ControleDeTentativasService controleDeTentativas;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          EmpresaRepository empresaRepository,
                          PasswordEncoder passwordEncoder,
                          GeradorDeSenhaTemporaria geradorDeSenha,
                          ControleDeTentativasService controleDeTentativas) {
        this.usuarioRepository = usuarioRepository;
        this.empresaRepository = empresaRepository;
        this.passwordEncoder = passwordEncoder;
        this.geradorDeSenha = geradorDeSenha;
        this.controleDeTentativas = controleDeTentativas;
    }

    /**
     * Cadastra um funcionario na empresa do Administrador logado
     * (#003-RF01, RF03 e RF04).
     *
     * A EMPRESA NAO VEM DO FORMULARIO
     * Ela vem do parametro empresaId, que o controller tira da sessao. O
     * UsuarioForm nem sequer tem um campo de empresa - e assim que a RF03
     * ("vinculo automatico") deixa de ser uma promessa da tela e passa a ser
     * uma impossibilidade tecnica.
     *
     * @return a senha temporaria em texto legivel, para a tela mostrar UMA
     *         vez ao Administrador (#003-RN011). Ela nao fica guardada em
     *         lugar nenhum: no banco vai so o hash.
     */
    @Transactional
    public String cadastrar(UsuarioForm form, Long empresaId) {

        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new IllegalArgumentException("Empresa não encontrada."));

        String senhaTemporaria = geradorDeSenha.gerar();

        Usuario usuario = new Usuario(
                empresa,
                form.getNome(),
                form.getLogin(),
                passwordEncoder.encode(senhaTemporaria),   // #003-RN011: so o hash
                form.getPerfil());

        usuario.setCargo(form.getCargo());
        usuario.setCpf(ValidadorDeCpf.somenteNumeros(form.getCpf()));   // #003-RN006
        copiarDadosDaCnh(form, usuario);

        // Nasce ativo e obrigado a trocar a senha no primeiro acesso
        // (#001-RF07): o funcionario escolhe uma senha que so ele conhece,
        // e nem o Administrador que o cadastrou fica sabendo qual e.
        usuario.setAtivo(true);
        usuario.setSenhaTemporaria(true);

        usuarioRepository.save(usuario);

        return senhaTemporaria;
    }

    /**
     * Altera os dados cadastrais de um funcionario (#003-RF08).
     *
     * NAO MEXE em senha, em ativo nem nos campos de bloqueio: cada um desses
     * tem a sua propria acao, com o seu proprio botao e a sua propria regra.
     * Uma tela que altera tudo de uma vez e uma tela em que ninguem consegue
     * dizer o que aconteceu.
     */
    @Transactional
    public void editar(Long usuarioId, Long empresaId, UsuarioForm form) {

        Usuario usuario = buscarNaEmpresa(usuarioId, empresaId);

        usuario.setNome(form.getNome());
        usuario.setCargo(form.getCargo());
        usuario.setCpf(ValidadorDeCpf.somenteNumeros(form.getCpf()));
        usuario.setLogin(form.getLogin());
        usuario.setPerfil(form.getPerfil());
        copiarDadosDaCnh(form, usuario);

        usuarioRepository.save(usuario);
    }

    /* ==================================================================
       Etapa 2 - regras de conta
       ================================================================== */

    /**
     * Gera uma nova senha temporaria para um funcionario (#003-RF05).
     *
     * E o caminho de quem esqueceu a senha: nao existe recuperacao por e-mail
     * nem por SMS no sistema (#001-RF06). O Administrador gera outra senha e a
     * entrega ao funcionario, que sera obrigado a troca-la no primeiro acesso.
     *
     * POR QUE ISTO TAMBEM LIBERA A CONTA BLOQUEADA
     * O bloqueio da RN004 existe para conter quem fica tentando adivinhar uma
     * senha. Depois desta linha, a senha que estava sendo tentada nao existe
     * mais - manter a conta fechada nao protege nada e so faria o funcionario
     * ligar de novo para o Administrador dizendo que ainda nao entra. Quem
     * quiser apenas liberar a conta, sem trocar a senha, usa o botao
     * "Desbloquear" (#003-RF06).
     *
     * @return a nova senha em texto legivel, para ser exibida uma unica vez.
     */
    @Transactional
    public String redefinirSenha(Long usuarioId, Long empresaId) {

        Usuario usuario = buscarNaEmpresa(usuarioId, empresaId);

        String senhaTemporaria = geradorDeSenha.gerar();
        usuario.setSenha(passwordEncoder.encode(senhaTemporaria));   // #003-RN011
        usuario.setSenhaTemporaria(true);
        usuarioRepository.save(usuario);

        // Reaproveita a regra que ja existia, em vez de zerar as colunas aqui:
        // assim existe um lugar so que sabe o que e "liberar uma conta".
        controleDeTentativas.desbloquear(usuarioId, empresaId);

        return senhaTemporaria;
    }

    /**
     * Desliga o acesso de um funcionario (#003-RF07 e RN010).
     *
     * NADA E APAGADO - e a regra 3 do projeto. O registro continua na tabela,
     * so que com ativo = false: os lancamentos antigos dele seguem apontando
     * para um usuario que existe, e os relatorios continuam fechando. Um
     * delete de verdade quebraria o historico da empresa.
     *
     * Quem confere as regras de quem PODE ser desativado (#003-RN014 e RN015)
     * e o controller, que tem a mensagem para explicar a recusa na tela.
     */
    @Transactional
    public void desativar(Long usuarioId, Long empresaId) {
        Usuario usuario = buscarNaEmpresa(usuarioId, empresaId);
        usuario.setAtivo(false);
        usuarioRepository.save(usuario);
    }

    /** Devolve o acesso a um funcionario desativado (#003-RF07). */
    @Transactional
    public void ativar(Long usuarioId, Long empresaId) {
        Usuario usuario = buscarNaEmpresa(usuarioId, empresaId);
        usuario.setAtivo(true);
        usuarioRepository.save(usuario);
    }

    /**
     * A empresa continuaria com administradores suficientes se ESTE usuario
     * deixasse de ser um administrador ativo? (#003-RN015)
     *
     * A REGRA E SOBRE A ACAO, NAO SOBRE O ESTADO ATUAL
     * Repare no que este metodo NAO faz: ele nao exige que a empresa ja tenha
     * dois administradores para funcionar. Uma empresa com um administrador so
     * - como a que a carga inicial cria - continua trabalhando normalmente.
     * O que fica proibido e a ACAO que reduziria o numero: desativar aquele
     * administrador ou mudar o perfil dele para motorista.
     *
     * Duas situacoes em que a resposta e sempre "pode":
     * - o usuario nao e administrador (desativar um motorista nao muda a conta);
     * - o usuario ja esta inativo (ele ja nao entra na conta dos ativos).
     *
     * @param usuario o usuario que seria desativado ou rebaixado
     */
    @Transactional(readOnly = true)
    public boolean podeDeixarDeSerAdministradorAtivo(Long empresaId, Usuario usuario) {

        if (usuario.getPerfil() != Perfil.ADMINISTRADOR || !usuario.isAtivo()) {
            return true;
        }

        long administradoresAtivos = usuarioRepository
                .countByEmpresaIdAndPerfilAndAtivoTrue(empresaId, Perfil.ADMINISTRADOR);

        // "- 1" e a acao acontecendo: e o proprio usuario saindo da conta.
        return administradoresAtivos - 1 >= MINIMO_DE_ADMINISTRADORES;
    }

    /**
     * Define uma nova senha definitiva para o usuario (#001-RF07).
     *
     * REPARE NO PRIMEIRO COMANDO: a busca usa findByIdAndEmpresaId, e nao
     * findById. E a regra 1 do projeto em acao - mesmo aqui, onde o id vem da
     * sessao e nao da URL, a consulta confere a empresa. Se um dia alguem
     * conseguir forjar o id, ainda assim nao alcanca usuario de outra empresa.
     *
     * @Transactional: as duas alteracoes (senha e marcacao de temporaria)
     * acontecem juntas ou nenhuma acontece.
     */
    @Transactional
    public void definirNovaSenha(Long usuarioId, Long empresaId, String novaSenha) {

        Usuario usuario = buscarNaEmpresa(usuarioId, empresaId);

        // RN010: guarda o hash, nunca o texto digitado.
        usuario.setSenha(passwordEncoder.encode(novaSenha));

        // A senha deixa de ser temporaria: o usuario para de cair na tela
        // de troca obrigatoria a cada acesso.
        usuario.setSenhaTemporaria(false);

        usuarioRepository.save(usuario);
    }

    /**
     * A busca que TODO metodo desta classe usa (regra 1 do projeto).
     *
     * Nunca findById(id) sozinho: a consulta leva sempre a empresa junto. Um
     * id que nao seja daquela empresa simplesmente nao e encontrado, e quem
     * chamou recebe a excecao em vez do registro dos outros.
     */
    private Usuario buscarNaEmpresa(Long usuarioId, Long empresaId) {
        return usuarioRepository.findByIdAndEmpresaId(usuarioId, empresaId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Usuário não encontrado na empresa do usuário logado."));
    }

    /**
     * Copia os dados da habilitacao do formulario para o usuario (#003-RF02).
     *
     * Para quem NAO e motorista os tres campos ficam nulos. O motivo e a
     * RN002: a CNH so faz sentido para quem dirige, e um administrador com
     * CNH gravada apareceria mais tarde na lista de habilitacoes a vencer,
     * sem nunca ter pegado um veiculo. Se ele virar motorista um dia, a tela
     * vai exigir a CNH naquele momento.
     */
    private void copiarDadosDaCnh(UsuarioForm form, Usuario usuario) {
        if (form.getPerfil() == Perfil.MOTORISTA) {
            usuario.setCnh(form.getCnh());
            usuario.setCategoriaCnh(form.getCategoriaCnh());
            usuario.setValidadeCnh(form.getValidadeCnh());
        } else {
            usuario.setCnh(null);
            usuario.setCategoriaCnh(null);
            usuario.setValidadeCnh(null);
        }
    }
}
