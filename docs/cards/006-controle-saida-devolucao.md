# Gestão Fácil - #006 - CONTROLE DE SAÍDA E DEVOLUÇÃO DE VEÍCULOS

Atende ao **RF06** do documento de requisitos (versão 1.3).

## Descrição

- Analista de Requisitos (PO): Leonardo Almeida Silva
- Desenvolvedor: José A. Damasceno Lopes
- Desenvolvedor: Victor Ruan
- Scrum Master: José A. Damasceno Lopes
- Analista de Qualidade (QA): Leonardo Almeida Silva

## Descrição da funcionalidade atual

Atualmente o controle de retirada e devolução de veículos é feito de forma
manuscrita. Essa funcionalidade permitirá que o motorista registre
digitalmente o uso do veículo, do momento da saída até a devolução.

## Requisitos funcionais

**#006-RF01 – Registro de saída**
O sistema deverá permitir ao motorista registrar a saída de um veículo,
informando o veículo, o centro de custo de destino, a data/hora de saída
e uma observação opcional.

**#006-RF02 – Registro de devolução**
O sistema deverá permitir registrar a devolução de um veículo em uso,
informando a data/hora de devolução.

## Regras de negócio

- **RN001** – Veículo, data/hora de saída e centro de custo de destino
  são obrigatórios.
- **RN002** – O motorista não pode registrar nova saída se já tiver um
  veículo em aberto (sem devolução registrada).
- **RN003** – Ao registrar a saída, o status do veículo muda
  automaticamente para `EM_USO`; ao registrar a devolução, volta
  automaticamente para `DISPONIVEL` (regra 2 — o status nunca é digitado).
- **RN004** – Apenas o perfil Motorista registra saída e devolução, e
  somente para veículos e centros de custo da própria empresa (regra 1).
- **RN005** – A data/hora informada pode ser retroativa; o sistema grava
  também a `data_registro`, preenchida automaticamente pelo sistema e não
  editável (regra 4).
- **RN006** – O registro de saída e devolução exige conexão com o
  sistema; não há suporte a lançamento offline nesta versão (decisão da
  equipe — ver nota abaixo).

## Critérios de aceitação

- [ ] Motorista consegue registrar saída de um veículo disponível
- [ ] Sistema impede nova saída se o motorista já tiver veículo em aberto
- [ ] Status do veículo é atualizado automaticamente na saída e na
      devolução
- [ ] Sistema impede que um motorista registre saída com veículo ou
      centro de custo de outra empresa
- [ ] O lançamento grava a data de registro separadamente da data
      informada
- [ ] Sem conexão, a tela informa que não é possível registrar no
      momento, sem apresentar erro genérico do servidor

## Impacto no modelo de dados

A tabela `uso_veiculo` já existe no modelo de dados. Campos previstos:
`veiculo_id`, `centro_custo_id`, `usuario_id` (motorista), `data_saida`,
`data_devolucao` (nulo enquanto em aberto), `observacao`, `data_registro`
e `empresa_id`. Nenhum campo de identificador local é necessário, já que
não há lançamento offline.

> **Nota:** o rascunho original previa lançamentos offline com
> identificador gerado no celular e sincronização por ordem de data/hora.
> A equipe decidiu não implementar isso nesta versão — o card #006 exige
> conexão, como as demais telas do sistema (Opção A das alternativas
> registradas antes).
