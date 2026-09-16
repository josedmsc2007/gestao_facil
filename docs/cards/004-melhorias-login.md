# Gestão Fácil - #004 - MELHORIAS DA TELA DE LOGIN

Melhorias sobre o card **#001**, levantadas após os testes da sprint 1.

## Descrição

- Analista de Requisitos (PO): a definir
- Desenvolvedor: a definir
- Desenvolvedor: a definir
- Analista de Qualidade (QA): Leonardo Almeida Silva

## Descrição da funcionalidade atual

A tela de login está entregue e funcional (card #001). Os testes e o uso
mostraram três pontos de melhoria: informar a empresa na tela de exceção é
trabalhoso, o usuário não percebe que está se aproximando do bloqueio, e o
bloqueio aplicado ao Administrador pode deixar a empresa sem ninguém capaz de
desbloquear os demais usuários.

## Requisitos funcionais

**#004-RF01 – Facilitar a informação da empresa**
Na tela de login sem empresa no endereço (`/login`), o sistema deverá oferecer
as empresas já acessadas naquele navegador como atalho, para que o usuário
escolha em vez de digitar o identificador.

**#004-RF02 – Aviso de tentativas restantes**
A partir da terceira tentativa inválida consecutiva, a tela de login deverá
informar quantas tentativas restam antes do bloqueio da conta.

**#004-RF03 – Tratamento do bloqueio para o perfil Administrador**
O sistema deverá evitar a situação em que o único Administrador da empresa fica
bloqueado e, com isso, nenhum usuário pode ser desbloqueado. A solução adotada
será definida entre as opções registradas no final deste card.

## Regras de negócio

- **RN001** – A tela de login **não pode** listar, sugerir por busca ou revelar
  de qualquer forma as empresas cadastradas no sistema. Apenas empresas já
  acessadas naquele navegador podem ser oferecidas.
- **RN002** – Os atalhos de empresa ficam guardados no próprio navegador do
  usuário, nunca no servidor, e podem ser apagados por ele.
- **RN003** – O aviso de tentativas restantes aparece somente a partir da
  terceira falha consecutiva. As duas primeiras falhas exibem apenas a mensagem
  genérica, para não assustar quem apenas errou a digitação.
- **RN004** – A contagem exibida na tela é feita na sessão do navegador,
  **separada por login digitado**, e não pelo contador do usuário no banco.
  Isso faz a tela se comportar exatamente igual para um login existente e para
  um login inventado, preservando a regra da mensagem genérica (#001-RF04), e
  faz o aviso acompanhar o login que a pessoa está de fato tentando.
- **RN005** – A contagem exibida é reiniciada apenas após um login
  bem-sucedido daquele login. Errar o nome do usuário não reinicia contagem
  alguma: sem usuário encontrado, não existe acerto a reconhecer.
- **RN006** – Trocar o login digitado no meio das tentativas não aproveita a
  contagem do login anterior. Cada login digitado tem a sua própria contagem
  dentro da sessão.
- **RN007** – O contador gravado no banco continua sendo o que efetivamente
  bloqueia a conta (#001-RN004). O aviso é informativo e pode divergir do
  contador real em situações de borda, como a pessoa trocar de navegador no
  meio das tentativas. Em caso de divergência, quem vale é o banco.
- **RN008** – O aviso não informa o nome nem o perfil do usuário, apenas o
  número de tentativas restantes.

## Critérios de aceitação

- [ ] Empresa já acessada aparece como atalho na tela de login sem empresa
- [ ] Empresa nunca acessada naquele navegador não aparece em lugar nenhum
- [ ] Não existe busca, lista ou sugestão que revele empresas cadastradas
- [ ] O usuário consegue remover um atalho de empresa do seu navegador
- [ ] A primeira e a segunda falhas exibem apenas a mensagem genérica
- [ ] A partir da terceira falha, a tela informa quantas tentativas restam
- [ ] O comportamento da tela é idêntico para um login existente e um inventado
- [ ] Errar o nome do usuário também faz a contagem exibida avançar
- [ ] A contagem exibida é reiniciada somente após um login bem-sucedido
- [ ] Trocar o login digitado no meio das tentativas reinicia a contagem exibida
- [ ] Um bloqueio real acontece na quinta tentativa mesmo que o aviso da tela,
      por ter sido reiniciado, esteja mostrando um número diferente
- [ ] A conta continua bloqueando na quinta tentativa, como no card #001
- [ ] A solução escolhida para o #004-RF03 impede que a empresa fique sem
      ninguém capaz de desbloquear usuários

## Pontos que dependem de decisão

### 1. Busca de empresas na tela de login

O pedido original era uma busca (lupa) que encontrasse as empresas cadastradas.
A tela `/login` é pública, então uma busca assim publicaria a lista de clientes
do sistema: qualquer visitante poderia descobrir quais empresas o utilizam, o
que serve tanto para um concorrente quanto para quem queira escolher um alvo.
Isso também contraria a razão de existir da mensagem de erro genérica do
#001-RF04, que é justamente não revelar o que existe.

Por isso este card propõe, no lugar, os atalhos por navegador. Se o cliente
ainda assim quiser a busca aberta, a decisão precisa ser registrada por escrito,
com o risco declarado.

### 2. Como tratar o bloqueio do Administrador

O problema levantado é real: se o único Administrador da empresa for bloqueado,
ninguém consegue desbloquear os demais usuários enquanto o prazo não vencer.
Duas observações que reduzem a gravidade: o bloqueio expira sozinho em 30
minutos, e o usuário bloqueado também se libera sozinho no mesmo prazo. Ou seja,
o impasse é incômodo, mas tem hora para acabar.

Três caminhos possíveis:

| Opção | Como funciona | Avaliação |
|---|---|---|
| **A. Atraso progressivo** (recomendada) | O Administrador não é bloqueado, mas passa a esperar um intervalo crescente entre tentativas após a quinta falha | Mantém a proteção contra tentativa automatizada e elimina o impasse |
| **B. Exigir dois administradores** | A empresa precisa ter ao menos dois Administradores ativos; um desbloqueia o outro | É como as empresas resolvem isso na prática, mas depende do cadastro de usuários (card #003) |
| **C. Administrador sem bloqueio** | O perfil Administrador simplesmente não é bloqueado | Resolve o impasse, mas deixa sem proteção justamente a conta de maior poder do sistema. Não recomendada |

## Dependências

O #004-RF03 depende do card **#003 (cadastro de usuários)** caso a opção
escolhida seja a **B**, já que hoje não é possível criar um segundo
Administrador pela interface.

## Impacto no modelo de dados

Nenhum campo novo é necessário para os requisitos RF01 e RF02. O RF03 pode
exigir um campo de controle de espera, dependendo da opção escolhida.
