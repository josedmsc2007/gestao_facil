package com.gestaofacil.security;

import com.gestaofacil.service.ControleDeTentativasService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Aviso de tentativas restantes na tela de login (#001.1-RF02).
 *
 * EXISTEM DOIS CONTADORES NO SISTEMA - E ELES NAO SE CONVERSAM
 * - O do BANCO (coluna tentativas_invalidas, no ControleDeTentativasService)
 *   e o unico que BLOQUEIA a conta. So existe para usuario real.
 * - O desta classe, guardado na SESSAO do navegador, serve apenas para
 *   MOSTRAR o aviso. Ele nunca consulta o banco (#001.1-RN008).
 *
 * POR QUE NAO MOSTRAR SIMPLESMENTE O NUMERO DO BANCO
 * Porque login inventado nao tem contador no banco. Se o aviso so aparecesse
 * para usuario real, bastaria errar tres vezes para descobrir se um login
 * existe: com aviso, existe; sem aviso, nao existe. Isso derrubaria a mensagem
 * generica do #001-RF04. Por isso a contagem daqui sobe a cada falha, qualquer
 * que seja o motivo, e a tela fica identica nos dois casos (#001.1-RN005).
 *
 * UMA CONTAGEM POR EMPRESA + LOGIN DIGITADO
 * A sessao guarda um mapa, por exemplo:
 *     "construtora-teste|admin" -> 3
 *     "construtora-teste|joao"  -> 1
 * Trocar o login digitado nao aproveita a contagem do anterior (#001.1-RN007).
 */
@Component
public class AvisoDeTentativasNaSessao {

    /** #001.1-RN004: a partir de qual falha o aviso aparece. */
    public static final int FALHA_QUE_COMECA_O_AVISO = 3;

    /** Nome do mapa de contagens dentro da sessao. */
    private static final String ATRIBUTO_CONTAGENS = "contagemDeFalhasDeLogin";

    /** Nome do aviso que o LoginController retira e manda para a tela. */
    private static final String ATRIBUTO_AVISO = "avisoTentativasRestantes";

    /**
     * Chamado pelo AutenticacaoFalhaHandler a cada login recusado, exista o
     * usuario ou nao.
     */
    public void registrarFalha(HttpServletRequest request) {
        HttpSession sessao = request.getSession();
        Map<String, Integer> contagens = contagensDaSessao(sessao);

        String chave = chave(request);
        int falhas = contagens.getOrDefault(chave, 0) + 1;
        contagens.put(chave, falhas);

        // O Tomcat pode guardar a sessao em disco ou replica-la. Gravar o mapa
        // de novo avisa que ele mudou, em vez de confiar que foi percebido.
        sessao.setAttribute(ATRIBUTO_CONTAGENS, contagens);

        // #001.1-RN004: primeira e segunda falha nao deixam aviso.
        if (falhas >= FALHA_QUE_COMECA_O_AVISO) {
            int restantes = ControleDeTentativasService.TENTATIVAS_ATE_BLOQUEAR - falhas;
            sessao.setAttribute(ATRIBUTO_AVISO, Math.max(restantes, 0));
        } else {
            sessao.removeAttribute(ATRIBUTO_AVISO);
        }
    }

    /**
     * Chamado pelo AutenticacaoSucessoHandler: zera a contagem SO do login que
     * entrou (#001.1-RN006). As contagens de outros logins ficam como estao.
     */
    public void registrarSucesso(HttpServletRequest request) {
        HttpSession sessao = request.getSession(false);
        if (sessao == null) {
            return;
        }
        Map<String, Integer> contagens = contagensDaSessao(sessao);
        contagens.remove(chave(request));
        sessao.setAttribute(ATRIBUTO_CONTAGENS, contagens);
        sessao.removeAttribute(ATRIBUTO_AVISO);
    }

    /**
     * Devolve o numero de tentativas restantes para a tela mostrar, ou null
     * quando nao ha aviso.
     *
     * "Retirar" porque o aviso e APAGADO da sessao ao ser lido: vale para uma
     * exibicao so. Sem isso, recarregar a tela ou voltar a ela mais tarde
     * mostraria um aviso velho.
     */
    public Integer retirarAviso(HttpServletRequest request) {
        HttpSession sessao = request.getSession(false);
        if (sessao == null) {
            return null;
        }
        Integer restantes = (Integer) sessao.getAttribute(ATRIBUTO_AVISO);
        sessao.removeAttribute(ATRIBUTO_AVISO);
        return restantes;
    }

    /** Quantas falhas a sessao registrou para essa empresa + login. Usado nos testes. */
    public int falhasRegistradas(HttpSession sessao, String empresa, String login) {
        return contagensDaSessao(sessao).getOrDefault(empresa + "|" + login, 0);
    }

    /**
     * Monta a chave com o que o formulario enviou, com os espacos das pontas
     * removidos - o mesmo tratamento que o Spring (no usuario) e o
     * DetalhesLoginEmpresa (na empresa) dao antes de procurar no banco.
     */
    private String chave(HttpServletRequest request) {
        return aparado(request.getParameter("empresa")) + "|"
                + aparado(request.getParameter("usuario"));
    }

    private String aparado(String valor) {
        return (valor == null) ? "" : valor.trim();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Integer> contagensDaSessao(HttpSession sessao) {
        Object guardado = sessao.getAttribute(ATRIBUTO_CONTAGENS);
        if (guardado instanceof Map) {
            return (Map<String, Integer>) guardado;
        }
        // HashMap e serializavel, requisito para objetos guardados na sessao.
        return new HashMap<>();
    }
}
