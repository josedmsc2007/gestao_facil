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
digitalmente o uso do veículo, do momento da saída até a devolução, e que o
Administrador acompanhe quais veículos estão em uso e com quem.

## Requisitos funcionais

**#006-RF01 – Registro de saída**
O sistema deverá permitir ao motorista registrar a saída de um veículo,
informando o veículo, o centro de custo de destino, a data/hora de saída
e uma observação opcional.

**#006-RF02 – Registro de devolução**
O sistema deverá permitir registrar a devolução de um veículo em uso,
informando a data/hora de devolução.

**#006-RF03 – Painel de veículos em uso**
O sistema deverá mostrar ao Administrador a lista de veículos que estão em
uso neste momento, com o motorista, o centro de custo de destino e a
data/hora da saída de cada um.

**#006-RF04 – Encerramento de saída pelo Administrador**
O sistema deverá permitir ao Administrador registrar a devolução de um
veículo em uso no lugar do motorista, para os casos em que o motorista não
pode mais fazer isso sozinho (por exemplo, foi desativado com o veículo em
aberto).

## Regras de negócio

- **RN001** – Veículo, data/hora de saída e centro de custo de destino
  são obrigatórios.
- **RN002** – O motorista não pode registrar nova saída se já tiver um
  veículo em aberto (sem devolução registrada).
- **RN003** – Ao registrar a saída, o status do veículo muda
  automaticamente para `EM_USO`; ao registrar a devolução (pelo motorista ou
  pelo Administrador), volta automaticamente para `DISPONIVEL` (regra 2 — o
  status nunca é digitado).
- **RN004** – Apenas o perfil Motorista registra saída, e somente para
  veículos e centros de custo da própria empresa (regra 1). A devolução é
  registrada pelo próprio motorista; o Administrador só pode encerrar uma
  saída em aberto no lugar dele (RF04), também apenas na própria empresa.
- **RN005** – A data/hora informada pode ser retroativa. O sistema grava
  também, automaticamente e sem permitir edição, o momento em que cada
  lançamento entrou no sistema: `data_registro` para a saída e
  `data_registro_devolucao` para a devolução (regra 4).
- **RN006** – O registro de saída e devolução exige conexão com o
  sistema; não há suporte a lançamento offline nesta versão (decisão da
  equipe).
- **RN007** – A data e hora informadas, tanto na saída quanto na devolução,
  não podem estar no futuro. Além disso, a devolução não pode ser anterior
  à data da própria saída. Vale também quando o Administrador encerra a
  saída.
- **RN008** – Fica registrado quem lançou a devolução (o próprio motorista
  ou um Administrador em seu lugar), para o histórico não parecer que o
  motorista devolveu sozinho quando foi o Administrador.
- **RN009** – O painel de veículos em uso e o encerramento de saída são
  exclusivos do perfil Administrador, e somente para registros da própria
  empresa (regra 1).

## Critérios de aceitação

- [ ] Motorista consegue registrar saída de um veículo disponível
- [ ] Sistema impede nova saída se o motorista já tiver veículo em aberto
- [ ] Status do veículo é atualizado automaticamente na saída e na
      devolução
- [ ] Sistema impede que um motorista registre saída com veículo ou
      centro de custo de outra empresa
- [ ] O lançamento grava a data de registro da saída e a da devolução
      separadamente das datas informadas
- [ ] Sem conexão, a tela informa que não é possível registrar no
      momento, sem apresentar erro genérico do servidor
- [ ] Sistema recusa data/hora de saída ou devolução no futuro, e recusa
      devolução anterior à data da saída
- [ ] Administrador vê a lista de veículos em uso, com motorista, destino e
      data/hora de saída
- [ ] Administrador consegue registrar a devolução de um veículo no lugar
      do motorista, inclusive quando o motorista está desativado
- [ ] O veículo liberado pelo Administrador volta para `DISPONIVEL`
- [ ] Fica registrado quem lançou cada devolução (motorista ou
      Administrador)
- [ ] Administrador não vê nem encerra saídas de outra empresa
- [ ] Motorista não acessa o painel de veículos em uso

## Impacto no modelo de dados

A tabela `uso_veiculo` já existe no modelo de dados. Campos previstos:
`veiculo_id`, `centro_custo_id`, `usuario_id` (motorista), `data_saida`,
`data_devolucao` (nulo enquanto em aberto), `observacao`, `data_registro`
e `empresa_id`. Nenhum campo de identificador local é necessário, já que
não há lançamento offline.

Acrescentar duas colunas, ambas nulas enquanto a saída está em aberto:
`data_registro_devolucao` (momento em que a devolução foi lançada no
sistema) e `usuario_devolucao_id` (quem lançou a devolução: o motorista ou
o Administrador). Devoluções antigas, lançadas antes dessa mudança, ficam
com as duas colunas nulas.
