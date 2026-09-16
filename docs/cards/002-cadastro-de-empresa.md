# Gestão Fácil - #002 - CADASTRO DE EMPRESA

Atende ao **RF02** do documento de requisitos (versão 1.3).

## Descrição

- Analista de Requisitos (PO): Leonardo Almeida Silva
- Desenvolvedor: José A. Damasceno Lopes
- Desenvolvedor: Victor Ruan
- Analista de Qualidade (QA): Leonardo Almeida Silva

## Descrição da funcionalidade atual

Atualmente não existe forma de cadastrar uma empresa no sistema; a única empresa
existente é criada pela carga inicial. Essa funcionalidade é a base do modelo
multiempresa: permite que a equipe responsável pelo sistema cadastre uma nova
construtora cliente junto com os seus dois administradores, e entregue a
operação do dia a dia para a própria empresa.

## Requisitos funcionais

**#002-RF01 – Perfil Operador**
O sistema deverá possuir um terceiro perfil de acesso, o **Operador**,
pertencente à equipe responsável pelo sistema. É o único perfil autorizado a
cadastrar empresas e a definir os administradores de cada uma.

**#002-RF02 – Cadastro de empresa**
O Operador deverá conseguir cadastrar uma empresa informando nome, identificador
de acesso e os rótulos do centro de custo.

**#002-RF03 – Identificador de acesso**
Cada empresa possui um identificador único, usado no endereço de acesso
(`/{identificador}/login`). O sistema deve sugerir um identificador a partir do
nome digitado, permitindo ajuste manual.

**#002-RF04 – Rótulo do centro de custo**
No cadastro, define-se como o centro de custo é chamado nas telas daquela
empresa, no singular e no plural (por exemplo, "Obra" e "Obras"). Esses textos
passam a ser usados em todas as telas da empresa.

**#002-RF05 – Administradores iniciais da empresa**
No mesmo formulário, o sistema deverá cadastrar **dois** usuários com perfil
Administrador para a nova empresa, informando nome e login de cada um, e gerando
uma senha temporária para cada, exibidas uma única vez ao final do cadastro.

**#002-RF06 – Situação da empresa**
O Operador deverá conseguir marcar uma empresa como inativa. Empresa inativa não
aceita login de nenhum de seus usuários.

**#002-RF07 – Consulta e edição**
O Operador deverá conseguir listar as empresas cadastradas e editar o nome, os
rótulos e a situação de cada uma.

**#002-RF08 – Recuperação de acesso da empresa**
O Operador deverá conseguir gerar uma nova senha temporária para um
Administrador de qualquer empresa. É o caminho de socorro para quando uma
construtora perde o acesso aos dois administradores.

## Regras de negócio

- **RN001** – O nome da empresa é obrigatório.
- **RN002** – O identificador é obrigatório, único em todo o sistema, e aceita
  apenas letras minúsculas, números e hífen. Não pode conter espaços nem
  acentos, porque é usado no endereço.
- **RN003** – O identificador não pode ser alterado após a criação: mudá-lo
  quebraria o atalho já instalado no celular dos funcionários.
- **RN004** – Os rótulos do centro de custo são obrigatórios. Sugestão padrão:
  "Obra" e "Obras".
- **RN005** – O nome e o login dos dois administradores iniciais são
  obrigatórios, e os dois logins devem ser diferentes entre si.
- **RN006** – A empresa não pode ser criada com menos de dois administradores.
  Não existe perfil acima do Administrador dentro da empresa: com um único
  administrador, o esquecimento da senha ou um bloqueio por tentativas deixaria
  a construtora sem acesso ao sistema.
- **RN007** – Apenas o perfil Operador pode cadastrar, editar e inativar
  empresas. Administrador e Motorista não têm acesso a essas telas.
- **RN008** – O Operador **não** acessa os dados operacionais das empresas:
  veículos, centros de custo, saídas, abastecimentos, manutenções, motoristas e
  documentos anexados. Ele administra o cadastro das empresas e o acesso dos
  administradores, e nada além disso.
- **RN009** – Gerar nova senha temporária para um Administrador não é considerado
  acesso a dados, e por isso é permitido ao Operador (RF08).
- **RN010** – O Operador pertence a uma empresa reservada da equipe responsável
  pelo sistema, que não é uma construtora cliente e não aparece nas listagens de
  empresas.
- **RN011** – Existem três contas de Operador, uma para cada integrante da
  equipe, com as mesmas permissões. Contas nominais permitem identificar quem
  realizou cada cadastro.
- **RN012** – Cada empresa visualiza apenas os seus próprios dados. O isolamento
  vale em todas as consultas, e não apenas na exibição das telas.
- **RN013** – Empresa inativa não aceita login, mesmo com usuário e senha
  corretos.

## Critérios de aceitação

- [ ] Operador consegue cadastrar uma nova empresa
- [ ] Administrador não consegue acessar a tela de cadastro de empresas
- [ ] Motorista não consegue acessar a tela de cadastro de empresas
- [ ] Sistema impede cadastro sem o nome da empresa
- [ ] Sistema impede cadastro sem identificador
- [ ] Sistema impede identificador repetido
- [ ] Sistema impede identificador com espaço, acento ou letra maiúscula
- [ ] Sistema sugere o identificador a partir do nome digitado
- [ ] Sistema impede cadastro sem os rótulos do centro de custo
- [ ] Os dois administradores são criados junto com a empresa
- [ ] Sistema impede o cadastro com apenas um administrador
- [ ] Sistema impede que os dois administradores tenham o mesmo login
- [ ] As senhas temporárias dos dois são exibidas ao final do cadastro
- [ ] Cada um dos dois consegue entrar em `/{identificador}/login` e é obrigado
      a trocar a senha
- [ ] As telas da nova empresa exibem o rótulo cadastrado por ela
- [ ] Identificador não pode ser editado depois de criado
- [ ] Usuário de empresa inativa não consegue entrar
- [ ] Operador consegue gerar nova senha temporária para um Administrador
- [ ] Operador não consegue abrir nenhuma tela de dados de uma empresa
- [ ] A empresa reservada da equipe não aparece na listagem de empresas
- [ ] O histórico registra qual Operador cadastrou cada empresa

## Impacto no modelo de dados

O tipo `Perfil` passa a ter três valores: `OPERADOR`, `ADMINISTRADOR` e
`MOTORISTA`. Nenhuma coluna nova é necessária: o Operador é um usuário comum,
vinculado à empresa reservada da equipe.

A carga inicial passa a criar a empresa reservada e as três contas de Operador.
