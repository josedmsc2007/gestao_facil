# Gestão Fácil - #005 - CADASTRO DE CENTRO DE CUSTO

Atende ao **RF05** do documento de requisitos (versão 1.3).

## Descrição

- Analista de Requisitos (PO): Leonardo Almeida Silva
- Desenvolvedor: José A. Damasceno Lopes
- Desenvolvedor: Victor Ruan
- Scrum Master: José A. Damasceno Lopes
- Analista de Qualidade (QA): Leonardo Almeida Silva

## Descrição da funcionalidade atual

Atualmente não existe cadastro de centro de custo no sistema: a tabela já
está prevista no modelo de dados, mas nada a preenche. Essa funcionalidade
permitirá cadastrar os centros de custo da empresa, para depois vincular a
eles a saída de veículos (card #006).

## Requisitos funcionais

**#005-RF01 – Cadastro de centro de custo**
O sistema deverá permitir cadastrar um centro de custo informando nome,
cidade, estado e endereço.

**#005-RF02 – Consulta e edição**
O sistema deverá permitir listar os centros de custo da empresa e editar
os seus dados.

**#005-RF03 – Ativação e desativação**
O sistema deverá permitir ao Administrador marcar um centro de custo como
inativo, em vez de excluí-lo.

## Regras de negócio

- **RN001** – Nome, cidade e estado são campos obrigatórios.
- **RN002** – Apenas o perfil Administrador cadastra, edita e
  ativa/desativa centros de custo, e somente os da própria empresa.
- **RN003** – Um centro de custo cadastrado fica vinculado à empresa que o
  cadastrou (isolamento multi-tenant, regra 1).
- **RN004** – Centro de custo não é excluído, apenas marcado inativo
  (campo `ativo`, regra 3), para não quebrar os lançamentos históricos que
  apontam para ele.
- **RN005** – Em toda tela, o centro de custo é chamado pelo rótulo
  cadastrado pela própria empresa (`empresa.rotulo_cc_singular` /
  `rotulo_cc_plural`), nunca pela palavra fixa "Obra" (regra 6 do
  projeto).
- **RN006** – O nome do centro de custo é único por empresa (duas empresas
  diferentes podem ter centros de custo com o mesmo nome, mas a mesma
  empresa não pode ter dois com o nome igual). A comparação ignora
  maiúsculas/minúsculas e espaços nas pontas, para não deixar cadastrar
  "Obra Norte" e "obra norte " como se fossem diferentes.

## Critérios de aceitação

- [ ] Administrador consegue cadastrar um novo centro de custo com todos
      os campos
- [ ] Sistema impede cadastro sem os campos obrigatórios
- [ ] Centro de custo cadastrado fica disponível para vínculo na saída de
      veículos
- [ ] Administrador consegue editar um centro de custo já cadastrado
- [ ] Administrador consegue marcar um centro de custo como inativo sem
      apagar o seu histórico
- [ ] A tela usa o rótulo cadastrado pela empresa (ex.: "Obra"/"Obras"),
      nunca o texto fixo
- [ ] Administrador não vê nem altera centros de custo de outra empresa
- [ ] Sistema impede cadastrar dois centros de custo com o mesmo nome na
      mesma empresa, mas aceita o mesmo nome em empresas diferentes

## Impacto no modelo de dados

A tabela `centro_custo` já existe no modelo de dados. Campos previstos:
`nome`, `cidade`, `estado`, `endereco`, `ativo` e `empresa_id`.

Restrição única no banco por `(empresa_id, nome)`, nos mesmos moldes da
restrição `(empresa_id, login)` do usuário (regra do card #001) e de
placa/RENAVAM do card #004 — não basta validar na tela.

> **Nota:** o rascunho original deste card chamava a funcionalidade de
> "Cadastro de obras" e usava "obra" como o nome do conceito. Renomeei
> para "centro de custo", que é o nome da tabela e do conceito genérico
> (regra 6) — "Obra" é só o rótulo que a Construtora Teste escolheu; outra
> empresa cliente pode chamar de "Rota" ou "Contrato". O texto das telas
> continua podendo dizer "Obra" para essa empresa específica, mas vindo do
> cadastro dela, nunca fixo no código ou no template.
