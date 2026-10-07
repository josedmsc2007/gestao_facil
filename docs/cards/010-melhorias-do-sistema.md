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

## Impacto no modelo de dados

Nenhuma coluna muda: `cidade` e `estado` continuam como texto na tabela
`centro_custo`. A lista de municípios brasileiros (fonte: IBGE) passa a ser um
arquivo de dados do sistema, não uma tabela nova.

A pesquisa nas listagens (RF03) não altera o modelo de dados.
