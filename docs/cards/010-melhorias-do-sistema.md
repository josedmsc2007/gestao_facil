# Gestão Fácil - #010 - MELHORIAS DO SISTEMA

Card aberto: reúne melhorias pedidas pela equipe depois que os cards
principais ficaram prontos. Novos itens entram aqui conforme forem surgindo.

## Descrição

- Analista de Requisitos (PO): Leonardo Almeida Silva
- Desenvolvedor: José A. Damasceno Lopes
- Desenvolvedor: Victor Ruan
- Scrum Master: José A. Damasceno Lopes
- Analista de Qualidade (QA): Leonardo Almeida Silva

## Descrição da funcionalidade atual

Este card não cria uma funcionalidade nova: ele melhora telas que já existem.

**Cadastro de centro de custo (card #005):** hoje o estado é escolhido numa
caixa de seleção e a cidade é um campo de texto livre. Como a cidade pode ser
digitada de qualquer jeito, o sistema aceita nomes errados, com erro de
digitação ou que nem existem (por exemplo, "dbdfgdfgdf"), e o mesmo município
pode ficar escrito de várias formas. Isso atrapalha a leitura da listagem e,
mais tarde, os relatórios por local.

**Listagens (cards #002, #003, #004, #005 e #006):** hoje as listas mostram
todos os registros de uma vez, sem nenhuma forma de procurar. Conforme a
empresa cadastra mais usuários, veículos e centros de custo, achar um registro
específico fica lento.

**Campos de senha (cards #001 e #003):** hoje a senha digitada aparece só como
bolinhas, sem como conferir o que foi escrito. No celular, onde digitar é mais
difícil, isso causa erros de digitação, principalmente na troca obrigatória de
senha, em que a senha é escrita duas vezes.

**Desativação de usuário (cards #003 e #006):** hoje o Administrador consegue
desativar um Motorista que está com um veículo em uso. O veículo continua
`EM_USO`, preso a uma pessoa que não consegue mais entrar no sistema, até que o
Administrador lembre de encerrar a saída pelo painel de veículos em uso.

## Requisitos funcionais

**#010-RF01 – Estado com busca no cadastro de centro de custo**
O campo estado deverá ter uma barra de busca: ao digitar, a lista mostra
apenas os estados que combinam com o texto, pelo nome ou pela sigla.

**#010-RF02 – Cidade escolhida numa lista filtrada pelo estado**
O campo cidade deverá aparecer depois do estado e deixar de ser texto livre.
Ele mostrará apenas os municípios brasileiros do estado escolhido, com uma
barra de busca para achar o município digitando parte do nome.

**#010-RF03 – Barra de pesquisa nas listagens**
As telas de listagem que podem ter muitos registros deverão ter uma barra de
pesquisa. O usuário digita parte do texto e a lista mostra apenas os registros
que combinam, sem diferença entre letras maiúsculas e minúsculas.

**#010-RF04 – Mostrar e ocultar a senha**
Os campos de senha deverão ter um ícone de olho. Ao clicar nele, a senha
digitada passa a aparecer em texto; ao clicar de novo, ela volta a ficar
oculta.

**#010-RF05 – Desativação bloqueada com saída em aberto**
O sistema não deverá permitir desativar um Motorista que tenha uma saída de
veículo em aberto (sem devolução registrada).

## Regras de negócio

- **RN001** – A ordem dos campos passa a ser Estado e depois Cidade. A cidade
  fica desabilitada enquanto nenhum estado for escolhido. Ao trocar o estado,
  a cidade escolhida é limpa.
- **RN002** – A cidade precisa ser um município da lista do estado escolhido.
  O sistema também confere isso ao salvar e recusa qualquer outro valor, mesmo
  que ele seja enviado fora da tela.
- **RN003** – A busca de estado e de cidade ignora maiúsculas, minúsculas e
  acentos (ex.: "cotriguacu" encontra "Cotriguaçu").
- **RN004** – O que fica gravado é o nome oficial do município, como consta na
  lista, e não o que o usuário digitou na busca.
- **RN005** – Centros de custo já cadastrados com uma cidade fora da lista
  continuam aparecendo normalmente na listagem. Ao editá-los, o Administrador
  precisa escolher a cidade na lista para conseguir salvar.
- **RN006** – A lista de municípios fica dentro do sistema: o uso da tela não
  depende de consultar um serviço de terceiros.
- **RN007** – A barra de pesquisa aparece nas listagens de usuários, veículos,
  centros de custo, empresas (perfil Operador) e veículos em uso (painel do
  Administrador). Telas com poucos registros por natureza não precisam dela.
- **RN008** – A pesquisa ignora maiúsculas, minúsculas e acentos, e procura o
  texto em todas as colunas de texto que aparecem na linha da listagem (por
  exemplo, em veículos: nome, placa e RENAVAM). "obra norte", "OBRA NORTE" e
  "Obra Norte" mostram o mesmo resultado.
- **RN009** – A lista é filtrada enquanto o usuário digita. Apagar o texto
  mostra todos os registros de novo. Se nada combinar, a tela mostra uma
  mensagem de que nenhum registro foi encontrado.
- **RN010** – A pesquisa só muda o que aparece na tela: ela mostra apenas
  registros que o usuário já pode ver naquela listagem, da própria empresa, e
  não altera nenhum dado.
- **RN011** – O ícone de olho aparece em todos os campos de senha do sistema:
  a senha da tela de login e, na troca obrigatória de senha, a nova senha e a
  confirmação.
- **RN012** – A senha sempre começa oculta. Cada clique no olho alterna entre
  mostrar e ocultar, e o ícone muda para indicar o estado atual.
- **RN013** – O olho é um botão dentro do campo, grande o bastante para o toque
  no celular, com texto alternativo ("Mostrar senha" e "Ocultar senha") para
  quem usa leitor de tela.
- **RN014** – Mostrar a senha só muda a exibição no navegador de quem está
  digitando. Nada é enviado ao servidor nem gravado por causa disso.
- **RN015** – A recusa vale para qualquer desativação de Motorista feita pela
  tela de usuários. A mensagem explica o motivo e orienta o caminho: encerrar a
  saída em "Veículos em uso" e só depois desativar o funcionário.
- **RN016** – Motorista sem saída em aberto continua podendo ser desativado
  normalmente. Depois que a saída é fechada, seja pela devolução do motorista
  ou pelo encerramento do Administrador, a desativação passa a ser permitida.
- **RN017** – Segue o mesmo princípio já usado para o veículo (#004): o sistema
  não desativa o que está em uso, porque o status do veículo só muda por um
  fato real, a devolução, e nunca pela desativação de quem está com ele.

## Critérios de aceitação

- [ ] A tela mostra o estado primeiro e a cidade depois
- [ ] A cidade fica desabilitada até um estado ser escolhido
- [ ] Ao escolher um estado, a lista de cidades mostra só os municípios dele
- [ ] A barra de busca do estado encontra por nome e por sigla
- [ ] A barra de busca da cidade encontra mesmo sem acento e sem respeitar
      maiúsculas
- [ ] Não é possível salvar uma cidade que não esteja na lista
- [ ] Não é possível salvar uma cidade de outro estado
- [ ] Trocar o estado limpa a cidade escolhida
- [ ] A cidade gravada é o nome oficial do município
- [ ] Um centro de custo antigo com cidade fora da lista aparece na
      listagem, e só salva a edição depois de a cidade ser escolhida na lista
- [ ] As listagens de usuários, veículos, centros de custo, empresas e
      veículos em uso têm uma barra de pesquisa
- [ ] Pesquisar em maiúsculas, em minúsculas ou misturando mostra o mesmo
      resultado
- [ ] A pesquisa encontra mesmo sem acento
- [ ] A lista é filtrada enquanto se digita, e apagar o texto mostra tudo
- [ ] Sem resultado, a tela mostra a mensagem de que nada foi encontrado
- [ ] A pesquisa nunca mostra registros de outra empresa
- [ ] A tela de login tem o ícone de olho no campo de senha
- [ ] A tela de troca de senha tem o ícone de olho na nova senha e na
      confirmação
- [ ] A senha começa oculta, e clicar no olho mostra o que foi digitado
- [ ] Clicar de novo no olho oculta a senha
- [ ] O botão do olho funciona com o toque no celular e tem texto alternativo
- [ ] Mostrar a senha não impede o login nem a troca de senha de funcionarem
- [ ] Sistema recusa desativar um Motorista com saída em aberto, explicando o
      motivo na tela
- [ ] A mensagem orienta a encerrar a saída em "Veículos em uso" primeiro
- [ ] Depois de a saída ser fechada, o Motorista pode ser desativado
- [ ] Motorista sem saída em aberto continua sendo desativado normalmente
- [ ] O veículo continua `EM_USO` enquanto a recusa estiver valendo

## Impacto no modelo de dados

Nenhuma coluna muda: `cidade` e `estado` continuam como texto na tabela
`centro_custo`. A lista de municípios brasileiros (fonte: IBGE) passa a ser um
arquivo de dados do sistema, não uma tabela nova.

A pesquisa nas listagens (RF03), o ícone de olho nas senhas (RF04) e a recusa
de desativação (RF05) não alteram o modelo de dados.
