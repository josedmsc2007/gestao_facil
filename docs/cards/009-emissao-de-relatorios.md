# Gestão Fácil - #009 - EMISSÃO DE RELATÓRIOS

Atende ao **RF09** do documento de requisitos (versão 1.3).

## Descrição

- Analista de Requisitos (PO): Leonardo Almeida Silva
- Desenvolvedor: José A. Damasceno Lopes
- Desenvolvedor: Victor Ruan
- Scrum Master: José A. Damasceno Lopes
- Analista de Qualidade (QA): Leonardo Almeida Silva

## Descrição da funcionalidade atual

Atualmente não existe geração de relatórios consolidados de uso da frota.
Essa funcionalidade permitirá gerar relatórios com filtros combináveis,
facilitando o acompanhamento e o controle de custos.

## Requisitos funcionais

**#009-RF01 – Emissão de relatórios**
O sistema deverá permitir gerar relatórios de uso da frota, com filtros
por período, veículo, centro de custo e motorista, cruzando dados de
saídas, abastecimentos e manutenções.

## Regras de negócio

- **RN001** – Os filtros devem poder ser combinados entre si.
- **RN002** – O relatório deve exibir apenas dados da empresa do usuário
  logado (isolamento multi-tenant, regra 1).
- **RN003** – Apenas o perfil Administrador acessa a tela de relatórios.

## Critérios de aceitação

- [ ] Usuário consegue gerar relatório aplicando um ou mais filtros
      combinados
- [ ] Relatório exibe apenas os dados da empresa do usuário logado
- [ ] Relatório reflete corretamente os dados cadastrados no sistema
- [ ] Motorista não consegue acessar a tela de relatórios

## Impacto no modelo de dados

Nenhuma tabela nova: o relatório é uma consulta agregada sobre `veiculo`,
`centro_custo`, `uso_veiculo`, `abastecimento` e `manutencao`.

> **Nota:** o rascunho original não dizia quem pode gerar relatórios. Como
> eles reúnem custo da frota inteira, restringi ao Administrador — mesma
> lógica de outras telas administrativas do sistema. Confirmem se o
> Motorista precisa ver algum recorte próprio (por exemplo, só os próprios
> lançamentos); se precisar, isso muda o RN003.
