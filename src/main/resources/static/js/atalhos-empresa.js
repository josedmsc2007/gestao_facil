/* ============================================================
   Atalhos de empresa na tela /login (#001.1-RF01)
   ============================================================
   Este arquivo faz DUAS coisas, conforme a tag <script> que o carrega:

   1. GRAVAR - a tag traz data-gravar-identificador e data-gravar-nome.
      Ela so existe nas telas internas (fragmentos/atalhos-empresa.html),
      que so abrem para quem ja fez login com sucesso. Por isso a empresa
      nunca e gravada por simples visita a um endereco (#001.1-RN002).

   2. EXIBIR - a tag traz data-exibir com o id da caixa onde os atalhos
      aparecem. So a tela /login tem essa tag.

   TUDO FICA NO NAVEGADOR (#001.1-RN003)
   Os atalhos moram no localStorage, um "armario" que cada navegador tem
   para cada site. O servidor nunca le nem recebe essa lista, e nao existe
   endpoint que devolva empresas: a tela so conhece as empresas que ESTE
   navegador ja acessou com sucesso (#001.1-RN001).
   ============================================================ */

(function () {
    'use strict';

    /** Nome da "gaveta" dentro do localStorage. */
    var CHAVE = 'gestaoFacil.atalhosEmpresa';

    /** Limite de atalhos guardados, para a lista nao crescer sem fim. */
    var MAXIMO_DE_ATALHOS = 10;

    /** Mesmo formato aceito pelo LoginController: /{identificador}/login. */
    var FORMATO_IDENTIFICADOR = /^[a-z0-9-]+$/;

    /*
       Precisa ser lido AGORA: document.currentScript so aponta para esta tag
       enquanto o arquivo esta sendo executado pela primeira vez.
    */
    var tag = document.currentScript;

    /**
     * Le a lista guardada. Devolve sempre uma lista, mesmo que o
     * localStorage esteja vazio, bloqueado (aba anonima em alguns
     * navegadores) ou com conteudo estragado.
     */
    function lerAtalhos() {
        try {
            var lista = JSON.parse(localStorage.getItem(CHAVE));
            if (!Array.isArray(lista)) {
                return [];
            }
            // O localStorage pode ser editado a mao pelo usuario. So passa
            // o que tem o formato esperado.
            return lista.filter(function (atalho) {
                return atalho
                    && typeof atalho.identificador === 'string'
                    && FORMATO_IDENTIFICADOR.test(atalho.identificador)
                    && typeof atalho.nome === 'string';
            });
        } catch (erro) {
            return [];
        }
    }

    function salvarAtalhos(lista) {
        try {
            localStorage.setItem(CHAVE, JSON.stringify(lista));
        } catch (erro) {
            // Sem localStorage o sistema funciona igual, so sem atalhos.
        }
    }

    /**
     * Guarda a empresa do login. Se ela ja estava na lista, sai de onde
     * estava e volta para o topo, com o nome atualizado.
     */
    function gravar(identificador, nome) {
        if (!FORMATO_IDENTIFICADOR.test(identificador)) {
            return;
        }
        var lista = lerAtalhos().filter(function (atalho) {
            return atalho.identificador !== identificador;
        });
        lista.unshift({ identificador: identificador, nome: nome });
        salvarAtalhos(lista.slice(0, MAXIMO_DE_ATALHOS));
    }

    function remover(identificador) {
        salvarAtalhos(lerAtalhos().filter(function (atalho) {
            return atalho.identificador !== identificador;
        }));
    }

    /**
     * Monta a lista de atalhos dentro da caixa.
     *
     * ATENCAO: o texto entra com textContent, NUNCA com innerHTML. Assim um
     * nome como "<script>..." aparece como texto e nao e executado.
     */
    function exibir(caixa, enderecoBase) {
        var lista = caixa.querySelector('[data-lista]');
        var atalhos = lerAtalhos();

        lista.replaceChildren();
        caixa.hidden = (atalhos.length === 0);

        atalhos.forEach(function (atalho) {
            var item = document.createElement('li');
            item.className = 'list-group-item d-flex align-items-center p-0';

            // O atalho e um link comum para a tela de login da empresa, a
            // mesma que o funcionario abriria pelo endereco /{identificador}.
            var link = document.createElement('a');
            link.className = 'flex-grow-1 text-decoration-none text-body px-3 py-2';
            link.href = enderecoBase + atalho.identificador + '/login';

            var nome = document.createElement('span');
            nome.className = 'd-block fw-semibold';
            nome.textContent = atalho.nome;

            var identificador = document.createElement('small');
            identificador.className = 'd-block text-secondary';
            identificador.textContent = atalho.identificador;

            link.append(nome, identificador);

            // Remover mexe so no navegador, nao em dado do servidor. Por
            // isso e um botao comum, sem formulario nem POST.
            var botao = document.createElement('button');
            botao.type = 'button';
            botao.className = 'btn btn-link text-danger text-decoration-none px-3 py-2';
            botao.textContent = 'Remover';
            botao.setAttribute('aria-label', 'Remover o atalho de ' + atalho.nome);
            botao.addEventListener('click', function () {
                remover(atalho.identificador);
                exibir(caixa, enderecoBase);
            });

            item.append(link, botao);
            lista.append(item);
        });
    }

    /* --- Decide o que fazer conforme a tag que carregou o arquivo --- */

    if (tag.dataset.gravarIdentificador) {
        gravar(tag.dataset.gravarIdentificador, tag.dataset.gravarNome || '');
    }

    if (tag.dataset.exibir) {
        var caixa = document.getElementById(tag.dataset.exibir);
        if (caixa) {
            exibir(caixa, tag.dataset.enderecoBase || '/');
        }
    }
})();
