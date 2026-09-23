# Gestão Fácil - #007 - CONTROLE DE ABASTECIMENTO

Atende ao **RF07** do documento de requisitos (versão 1.3).

## Descrição

- Analista de Requisitos (PO): Leonardo Almeida Silva
- Desenvolvedor: José A. Damasceno Lopes
- Desenvolvedor: Victor Ruan
- Scrum Master: José A. Damasceno Lopes
- Analista de Qualidade (QA): Leonardo Almeida Silva

## Descrição da funcionalidade atual

Atualmente não existe registro de abastecimento dos veículos. Essa
funcionalidade permitirá registrar cada abastecimento realizado,
viabilizando o controle de custos por veículo.

## Requisitos funcionais

**#007-RF01 – Controle de abastecimento**
O sistema deverá permitir registrar data, quilometragem, litros
abastecidos, valor gasto e posto (opcional), vinculados a um veículo.

## Regras de negócio

- **RN001** – Data, quilometragem, litros e valor gasto são obrigatórios.
- **RN002** – O campo "posto" é opcional.
- **RN003** – O abastecimento deve ser vinculado a um veículo existente e
  ativo da própria empresa (regra 1).
- **RN004** – Apenas o perfil Motorista registra abastecimento.
- **RN005** – A data informada pode ser retroativa; o sistema grava também
  a `data_registro`, preenchida automaticamente e não editável (regra 4).

## Critérios de aceitação

- [ ] Motorista consegue registrar um abastecimento com os campos
      obrigatórios preenchidos
- [ ] Sistema impede envio sem os campos obrigatórios
- [ ] Abastecimento fica vinculado corretamente ao veículo
- [ ] Sistema impede vincular o abastecimento a um veículo de outra
      empresa
- [ ] O lançamento grava a data de registro separadamente da data
      informada

## Impacto no modelo de dados

A tabela `abastecimento` já existe no modelo de dados. Campos previstos:
`veiculo_id`, `data`, `quilometragem`, `litros`, `valor`, `posto`
(opcional), `data_registro` e `empresa_id`.

> **Nota:** assumi que só o Motorista registra abastecimento, por ser
> quem está com o veículo na hora de abastecer — nenhum outro card diz o
> contrário, mas vale a equipe confirmar se o Administrador também
> precisa lançar em nome de outro motorista.
