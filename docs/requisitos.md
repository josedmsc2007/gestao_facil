# Sistema de Controle de Veículos — "Gestão Fácil"

Versão 1.2 — MVP. Esta é a versão em markdown dos requisitos, mantida para
leitura por ferramentas e controle de versão. O documento oficial entregue ao
cliente é o `.docx` na raiz da pasta.

## 1. Visão geral

Uma construtora controla hoje, em papel, a retirada, devolução, abastecimento e
manutenção dos seus veículos. O sistema digitaliza esse controle e permite
apurar custo por veículo e por obra.

O sistema é acessado pelo navegador, em computador e celular, a partir de um
mesmo endereço. No celular, o funcionário adiciona o sistema à tela inicial e
usa por um ícone, como se fosse um aplicativo — sem loja de aplicativos.

O sistema é multiempresa (multi-tenant): cada empresa vê apenas os seus dados,
e a terminologia das telas é configurável por empresa.

## 2. Perfis de acesso

| Perfil | Descrição | Permissões |
|---|---|---|
| Administrador | Responsável pela empresa no sistema | Cadastra empresa, veículos, centros de custo e usuários; lança todas as operações; acessa relatórios; redefine senha dos usuários |
| Motorista | Funcionário que usa os veículos (inclui mestres de obra) | Registra saída, devolução e abastecimento dos veículos que utiliza |

## 3. Requisitos funcionais

### RF01 — Login e autenticação

Acesso com usuário e senha. Sem CPF ou CNPJ no login.

- Cada empresa tem um endereço próprio, formado pelo seu identificador
  (ex.: `/construtora-silva`). É esse endereço que vira o atalho na tela
  inicial do celular.
- Como a empresa é identificada pelo endereço, o usuário digita apenas
  usuário e senha — nenhum campo adicional.
- O login é único **dentro de cada empresa**.
- Recuperação de senha feita pelo Administrador, que gera uma senha
  temporária. Não há envio de e-mail nem SMS.
- No primeiro acesso com senha temporária, o sistema obriga a troca.
- Se o sistema for acessado por endereço sem empresa, a tela exibe um campo
  "empresa" como caminho de exceção.

### RF02 — Cadastro de empresa

- Nome da empresa
- Identificador único (usado no endereço de acesso)
- Rótulo do centro de custo, singular e plural (ex.: "Obra" / "Obras")
- Funcionários vinculados
- Qualquer usuário Administrador pode cadastrar empresa; não há aprovação.

### RF03 — Cadastro de usuários

- Nome, cargo, CPF
- Anexos (documentos do funcionário)
- Perfil de acesso (administrador / motorista)
- Senhas armazenadas com hash, nunca em texto legível.
- Anexos ficam fora da área pública e só abrem para usuário autenticado da
  mesma empresa.

### RF04 — Cadastro de veículos

- Nome, ano, RENAVAM, placa, tipo de combustível
- Status: disponível / em uso / em manutenção
- O status é **controlado pelo sistema**: saída sem devolução → "em uso";
  devolução → "disponível"; manutenção em andamento → "em manutenção".
- Alteração manual do status só pelo Administrador, em casos excepcionais.

### RF05 — Cadastro de centros de custo (exibido como "Obras")

- Nome (obrigatório)
- Código (opcional)
- Cidade, estado, endereço (opcionais)
- Situação: ativo / inativo
- A entidade "Obra" da v1.0 foi generalizada para "Centro de Custo", para
  atender empresas de outros ramos (rotas, clientes, contratos, filiais).
  Para a construtora o rótulo cadastrado é "Obra", e é o que aparece nas telas.

### RF06 — Controle de saída e devolução

- Veículo utilizado
- Data e hora da saída (editável)
- Centro de custo de destino
- Observação (finalidade do uso)
- Data e hora da devolução (editável; em branco = veículo em uso)
- **Não há registro de quilometragem na saída e na devolução** — decisão do
  cliente: o controle é do veículo, não de quanto cada funcionário rodou.

### RF07 — Controle de abastecimento

- Data (editável)
- Quilometragem
- Litros abastecidos
- Valor gasto (obrigatório)
- Posto (opcional)
- Centro de custo, preenchido automaticamente a partir da saída em aberto do
  veículo e confirmável pelo usuário. É esse campo que permite atribuir o
  custo de combustível à obra correta.

### RF08 — Controle de manutenções

- Veículo
- Tipo: preventiva / corretiva
- Status: agendada / em andamento / concluída
- Data prevista e data de conclusão
- Quilometragem na data do serviço (opcional)
- Custo
- Observações
- Anexos (notas fiscais, orçamentos, fotos), com o mesmo acesso restrito do RF03

### RF09 — Emissão de relatórios

Relatórios consolidados com filtros opcionais e combináveis:

- Por período
- Por veículo
- Por centro de custo
- Por usuário/motorista
- Por manutenções

Detalhes:

- O relatório é apresentado em tela; a geração de PDF é feita pela função de
  impressão do navegador (Ctrl+P), sem geração de arquivo pelo servidor.
- As telas de relatório têm layout de impressão (`@media print`) que oculta
  menus, botões e filtros.
- Os relatórios apresentam custo de combustível e de manutenção por centro de
  custo. **Não** apresentam quilometragem percorrida por centro de custo.

## 4. Requisitos não funcionais

- Web e mobile a partir do mesmo endereço, com telas adaptadas ao aparelho.
- No celular, adicionável à tela inicial, sem loja de aplicativos.
- **Somente online.** O requisito de modo offline da v1.0 foi retirado com
  aprovação do cliente. Compensação: lançamentos aceitam data e hora
  retroativas (RF06 e RF07).
- Sem tratamento de concorrência entre motoristas para o mesmo veículo.
- Multiempresa: cada empresa vê apenas os seus dados.
- O isolamento entre empresas vale em **todas as consultas**, não apenas na
  exibição das telas.
- Acesso por HTTPS.
- Terminologia das telas configurável por empresa.
- Cadastros não são excluídos definitivamente: são marcados como inativos.

## 5. Histórico de versões

| Versão | Alterações |
|---|---|
| 1.0 | Versão inicial elaborada com o cliente |
| 1.1 | Senha pelo Administrador; cadastro de empresa sem aprovação; status automático; data/hora editáveis; relatórios pelo navegador; remoção do modo offline |
| 1.2 | "Obra" generalizada para "Centro de Custo" com rótulo por empresa; centro de custo no abastecimento; identificação da empresa no acesso; datas na manutenção; quilometragem mantida apenas no abastecimento |
