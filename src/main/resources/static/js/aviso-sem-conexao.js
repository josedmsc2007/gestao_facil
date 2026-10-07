/* ============================================================
   Aviso de "sem conexao" nas telas de lancamento (#006-RN006)
   ============================================================
   O sistema NAO tem lancamento offline: saida e devolucao so sao
   gravadas com internet. O que este arquivo faz e impedir que o
   motorista descubra isso do pior jeito - tocando em "Registrar" e
   caindo na tela de erro do navegador, sem saber se o lancamento foi
   ou nao.

   COMO A TELA USA
     - uma caixa <div id="aviso-sem-conexao" class="... d-none">
     - o formulario marcado com o atributo data-exige-conexao

   O QUE ELE FAZ
     1. mostra a caixa enquanto o celular estiver sem conexao, e a
        esconde quando a conexao volta;
     2. se o motorista tocar em "Registrar" sem conexao, cancela o
        envio - nada sai do celular, e ele continua na mesma tela, com
        o que digitou;
     3. com conexao, desliga o botao depois do primeiro toque, para um
        toque duplo nao enviar o lancamento duas vezes.

   O LIMITE DISTO
   navigator.onLine diz se o aparelho esta ligado a ALGUMA rede. "Sem
   rede" e certeza; "com rede" nao garante que o servidor responde (wi-fi
   sem internet, por exemplo). Nesse caso o envio acontece e quem avisa
   e a pagina de "sem internet" do proprio navegador. Nada e gravado
   pela metade: ou o servidor recebeu o lancamento inteiro, ou nao
   recebeu nada.
   ============================================================ */

(function () {
    'use strict';

    var aviso = document.getElementById('aviso-sem-conexao');
    var formularios = document.querySelectorAll('form[data-exige-conexao]');

    if (!aviso) {
        return;
    }

    /** Mostra a caixa quando nao ha conexao; esconde quando ha. */
    function atualizarAviso() {
        aviso.classList.toggle('d-none', navigator.onLine);
    }

    /** Liga ou desliga os botoes de envio de um formulario. */
    function desligarBotoes(formulario, desligar) {
        formulario.querySelectorAll('button[type="submit"]').forEach(function (botao) {
            botao.disabled = desligar;
        });
    }

    window.addEventListener('offline', atualizarAviso);
    window.addEventListener('online', atualizarAviso);
    atualizarAviso();

    formularios.forEach(function (formulario) {
        formulario.addEventListener('submit', function (evento) {

            if (!navigator.onLine) {
                // Cancela o envio: o navegador nem tenta falar com o servidor.
                evento.preventDefault();
                atualizarAviso();
                aviso.scrollIntoView();
                return;
            }

            desligarBotoes(formulario, true);
        });
    });

    /*
       Quem volta para esta tela pelo botao "voltar" do navegador recebe a
       pagina guardada na memoria, com o botao ainda desligado. O evento
       pageshow acontece tambem nesse caso, e religa o botao.
    */
    window.addEventListener('pageshow', function () {
        formularios.forEach(function (formulario) {
            desligarBotoes(formulario, false);
        });
    });
})();
