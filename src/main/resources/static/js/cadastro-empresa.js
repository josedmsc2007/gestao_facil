/*
 * #002-RF03 - sugere o identificador de acesso a partir do nome da empresa.
 *
 * Enquanto o Operador digita "Construtora São João", o campo identificador
 * vai sendo preenchido com "construtora-sao-joao".
 *
 * A SUGESTAO PARA QUANDO O OPERADOR MEXE NO CAMPO
 * Se ele editar o identificador a mao, a partir dai o nome nao sobrescreve
 * mais o que ele escreveu. Se ele apagar o campo inteiro, a sugestao volta.
 *
 * A MESMA REGRA EXISTE EM JAVA, em EmpresaService.sugerirIdentificador,
 * usada quando o formulario chega com o identificador vazio. Se mudar uma,
 * mude a outra.
 */
(function () {
    'use strict';

    var nome = document.getElementById('nome');
    var identificador = document.getElementById('identificador');
    if (!nome || !identificador) {
        return;
    }

    function sugerir(texto) {
        return texto
            // separa cada letra do seu acento ("ã" vira "a" + "~")...
            .normalize('NFD')
            // ...e apaga os acentos que ficaram soltos.
            .replace(/[̀-ͯ]/g, '')
            .toLowerCase()
            // cada sequencia que nao e letra ou numero vira um hifen
            .replace(/[^a-z0-9]+/g, '-')
            // sem hifen nas pontas
            .replace(/^-+|-+$/g, '')
            .substring(0, 60)
            .replace(/-+$/, '');
    }

    // Se a tela voltou do servidor com um identificador ja preenchido (por
    // erro em outro campo), ele e respeitado: ninguem quer ver o proprio
    // ajuste desfeito.
    var ajustadoAMao = identificador.value !== ''
        && identificador.value !== sugerir(nome.value);

    identificador.addEventListener('input', function () {
        ajustadoAMao = identificador.value !== '';
    });

    nome.addEventListener('input', function () {
        if (!ajustadoAMao) {
            identificador.value = sugerir(nome.value);
        }
    });
})();
