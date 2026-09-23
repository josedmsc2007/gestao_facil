# Gestão Fácil - #004 - CADASTRO DE VEÍCULOS

Atende ao **RF04** do documento de requisitos (versão 1.3).

## Descrição

- Analista de Requisitos (PO): Leonardo Almeida Silva
- Desenvolvedor: José A. Damasceno Lopes
- Desenvolvedor: Victor Ruan
- Scrum Master: José A. Damasceno Lopes
- Analista de Qualidade (QA): Leonardo Almeida Silva

## Descrição da funcionalidade atual

Atualmente não existe cadastro de veículos no sistema. Essa funcionalidade
permitirá ao Administrador cadastrar os veículos da frota da sua empresa,
cada um com o status controlado pelo próprio sistema (regra 2 do projeto),
servindo de base para as saídas, abastecimentos e manutenções dos próximos
cards.

## Requisitos funcionais

**#004-RF01 – Cadastro de veículos**
O sistema deverá permitir cadastrar veículos informando nome/modelo, ano,
RENAVAM, placa e tipo de combustível.

**#004-RF02 – Consulta e edição**
O sistema deverá permitir listar os veículos da empresa e editar os seus
dados cadastrais.

**#004-RF03 – Ativação e desativação**
O sistema deverá permitir ao Administrador marcar um veículo como inativo
(vendido, baixado, fora de uso), em vez de excluí-lo, preservando o
histórico de saídas, abastecimentos e manutenções que apontam para ele.

## Regras de negócio

- **RN001** – Placa e RENAVAM são obrigatórios e únicos **dentro da
  empresa**, não no sistema inteiro — mesma lógica do CPF e do login no
  card #003: se um veículo for vendido de uma construtora cliente para
  outra, a segunda precisa conseguir cadastrá-lo mesmo que o registro
  antigo, agora inativo, continue existindo na primeira (regra 3).
- **RN002** – O status do veículo é um entre: `DISPONIVEL`, `EM_USO`,
  `EM_MANUTENCAO` ou `INATIVO` (regra 2 do projeto).
- **RN003** – O status **nunca é digitado**, nem mesmo no cadastro: todo
  veículo novo entra automaticamente como `DISPONIVEL`. As transições para
  `EM_USO` e `EM_MANUTENCAO` são calculadas pelos cards #006 e #008; só o
  Administrador altera manualmente para `INATIVO`, ao desativar o veículo.
- **RN004** – Apenas o perfil Administrador cadastra, edita e
  ativa/desativa veículos, e somente os da própria empresa.
- **RN005** – Veículo não é excluído, apenas marcado inativo (regra 3),
  para não quebrar os lançamentos históricos de uso, abastecimento e
  manutenção.

## Critérios de aceitação

- [ ] Administrador consegue cadastrar um novo veículo com todos os campos
- [ ] Sistema impede cadastro de placa ou RENAVAM duplicados dentro da
      mesma empresa
- [ ] Sistema aceita a mesma placa ou RENAVAM em empresas diferentes
- [ ] Veículo aparece com status "disponível" logo após o cadastro, sem
      que o campo apareça editável na tela de cadastro
- [ ] Administrador consegue editar os dados de um veículo já cadastrado
- [ ] Administrador consegue marcar um veículo como inativo sem apagar o
      seu histórico
- [ ] Administrador não vê nem altera veículos de outra empresa

## Impacto no modelo de dados

A tabela `veiculo` já existe no modelo de dados. Campos previstos: `nome`,
`ano`, `renavam`, `placa`, `tipo_combustivel`, `status` (enum) e
`empresa_id`. Não há campo `ativo` separado para esta tabela: o valor
`INATIVO` do próprio `status` cumpre esse papel, diferente de `usuario` e
`centro_custo`, que têm um booleano à parte por não possuírem um status
calculado.

> **Nota:** o rascunho original deste card listava "status" entre os
> campos informados no cadastro, mas isso contraria a regra 2
> (status é sempre calculado) e a própria regra de negócio RN003 do
> rascunho, que já dizia que o veículo nasce "disponível". O campo foi
> retirado da tela de cadastro por esse motivo.
