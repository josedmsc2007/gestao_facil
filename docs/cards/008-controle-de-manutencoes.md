# Gestão Fácil - #008 - CONTROLE DE MANUTENÇÕES

Atende ao **RF08** do documento de requisitos (versão 1.3).

## Descrição

- Analista de Requisitos (PO): Leonardo Almeida Silva
- Desenvolvedor: José A. Damasceno Lopes
- Desenvolvedor: Victor Ruan
- Scrum Master: José A. Damasceno Lopes
- Analista de Qualidade (QA): Leonardo Almeida Silva

## Descrição da funcionalidade atual

Atualmente não existe registro de manutenções dos veículos. Essa
funcionalidade permitirá registrar manutenções realizadas ou agendadas,
com anexos de comprovantes.

## Requisitos funcionais

**#008-RF01 – Registro de manutenção**
O sistema deverá permitir registrar veículo, tipo de manutenção
(preventiva/corretiva), data prevista, custo, status e anexos.

**#008-RF02 – Conclusão da manutenção**
Ao concluir uma manutenção, o sistema deverá registrar a data de
conclusão.

## Regras de negócio

- **RN001** – Veículo, tipo, data prevista e status são obrigatórios.
- **RN002** – Status deve ser um entre: `AGENDADA`, `EM_ANDAMENTO`,
  `CONCLUIDA`.
- **RN003** – Ao registrar manutenção com status `EM_ANDAMENTO`, o status
  do veículo muda automaticamente para `EM_MANUTENCAO` (regra 2).
- **RN004** – Ao concluir a manutenção, o status do veículo volta
  automaticamente para `DISPONIVEL` (regra 2).
- **RN005** – Apenas o perfil Administrador registra e edita manutenções,
  e somente de veículos da própria empresa (regra 1).
- **RN006** – Os anexos seguem o mesmo padrão do card #003: PDF, JPG ou
  PNG, no máximo 5 MB por arquivo, armazenados fora da pasta pública com
  nome gerado pelo sistema, e só acessíveis a usuário autenticado da mesma
  empresa (regra 5).
- **RN007** – A data prevista pode ser retroativa, para registrar
  manutenções já realizadas; o sistema grava também a `data_registro`,
  preenchida automaticamente e não editável (regra 4).

## Critérios de aceitação

- [ ] Administrador consegue registrar uma manutenção com os campos
      obrigatórios
- [ ] Status do veículo é atualizado automaticamente conforme o status da
      manutenção
- [ ] É possível anexar arquivos (notas fiscais, orçamentos, fotos)
- [ ] Sistema recusa anexo fora de PDF, JPG e PNG
- [ ] Sistema recusa anexo acima de 5 MB
- [ ] Administrador não vê nem altera manutenções de veículos de outra
      empresa

## Impacto no modelo de dados

As tabelas `manutencao` e `anexo_manutencao` já existem no modelo de
dados. Campos previstos em `manutencao`: `veiculo_id`, `tipo`,
`data_prevista`, `data_conclusao` (nulo até concluir), `custo`, `status`,
`data_registro` e `empresa_id`.

> **Nota:** o rascunho original não tinha nenhum campo de data, mas a
> descrição fala em manutenção "agendada" — não dá para agendar algo sem
> guardar quando. Acrescentei `data_prevista` e `data_conclusao`; ajustem
> se a ideia da equipe for outra.
