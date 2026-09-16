# Gestão Fácil - #003 - CADASTRO DE USUÁRIOS

Atende ao **RF03** do documento de requisitos (versão 1.3).

## Descrição

- Analista de Requisitos (PO): Leonardo Almeida Silva
- Desenvolvedor: José A. Damasceno Lopes
- Desenvolvedor: Victor Ruan
- Analista de Qualidade (QA): Leonardo Almeida Silva

## Descrição da funcionalidade atual

Atualmente não existe cadastro de funcionários no sistema; os usuários
existentes vêm da carga inicial. Essa funcionalidade permitirá ao Administrador
cadastrar os funcionários da sua empresa, definindo o perfil de acesso de cada
um, e é o que dá sustentação prática às regras de login entregues no card #001.

## Requisitos funcionais

**#003-RF01 – Cadastro de usuários**
O sistema deverá permitir cadastrar funcionários informando nome, cargo, CPF,
login, perfil de acesso e anexos (documentos).

**#003-RF02 – Dados de habilitação do motorista**
Para usuários com perfil Motorista, o sistema deverá registrar também o número
da CNH, a categoria e a data de validade.

**#003-RF03 – Vínculo automático com a empresa**
O usuário é criado automaticamente na empresa do Administrador que está logado.
Não existe campo para escolher a empresa.

**#003-RF04 – Senha temporária no cadastro**
Ao cadastrar um usuário, o sistema gera uma senha temporária, exibida uma única
vez para que o Administrador a repasse ao funcionário. No primeiro acesso o
funcionário é obrigado a trocá-la (card #001, #001-RF07).

**#003-RF05 – Redefinição de senha**
A partir da tela de usuários, o Administrador deverá conseguir gerar uma nova
senha temporária para um funcionário que esqueceu a senha (card #001,
#001-RF06).

**#003-RF06 – Desbloqueio de conta**
A partir da tela de usuários, o Administrador deverá conseguir desbloquear a
conta de um funcionário bloqueada por tentativas inválidas (card #001,
#001-RN005).

**#003-RF07 – Ativação e desativação**
O sistema deverá permitir marcar um usuário como inativo, em vez de excluí-lo.
Usuário inativo não consegue autenticar, mas permanece nos registros históricos.

**#003-RF08 – Consulta e edição**
O sistema deverá permitir listar os usuários da empresa e editar os seus dados.

**#003-RF09 – Anexos do funcionário**
O sistema deverá permitir anexar documentos ao cadastro do funcionário, além de
listá-los, baixá-los e removê-los.

## Regras de negócio

- **RN001** – Nome, CPF, login e perfil de acesso são obrigatórios.
- **RN002** – Para o perfil Motorista, o número da CNH, a categoria e a validade
  também são obrigatórios. Para o perfil Administrador, são dispensáveis.
- **RN003** – O número da CNH e o CPF são dados distintos: a CNH exibe o CPF no
  documento, mas o seu número de registro é próprio e muda a cada renovação. Os
  dois são gravados em campos separados.
- **RN004** – O CPF é único **dentro da empresa**, não no sistema inteiro.
- **RN005** – O login é único **dentro da empresa** (card #001, #001-RN007).
- **RN006** – O CPF deve ser válido, verificado pelos dígitos verificadores, e é
  armazenado apenas com os números.
- **RN007** – O Administrador só pode criar usuários com perfil Administrador ou
  Motorista. O perfil Operador não é atribuível por nenhuma tela da empresa.
- **RN008** – Apenas o perfil Administrador pode cadastrar, editar, desativar,
  desbloquear e redefinir a senha de usuários.
- **RN009** – O Administrador só enxerga e altera usuários da sua própria
  empresa.
- **RN010** – Usuários não são excluídos, apenas desativados, para não quebrar os
  lançamentos históricos que apontam para eles.
- **RN011** – A senha é armazenada com hash (BCrypt), nunca em texto legível. A
  senha temporária gerada é exibida uma única vez e não pode ser consultada
  depois.
- **RN012** – Os anexos aceitos são PDF, JPG e PNG, com no máximo 5 MB por
  arquivo.
- **RN013** – Os anexos são armazenados fora da área pública do sistema, com nome
  gerado pelo sistema, e só podem ser abertos por usuário autenticado da mesma
  empresa do registro.
- **RN014** – O Administrador não pode desativar nem rebaixar o próprio usuário.
- **RN015** – O sistema deve impedir qualquer ação que deixe a empresa com menos
  de **dois** Administradores ativos, seja desativando um usuário, seja alterando
  o perfil dele para Motorista. A mensagem deve explicar o motivo da recusa.
- **RN016** – Um Administrador desbloqueia a conta e redefine a senha do outro.
  É essa regra que garante que a empresa nunca fique sem acesso ao sistema.

## Critérios de aceitação

- [ ] Administrador consegue cadastrar um novo usuário com todos os campos
- [ ] Sistema impede cadastro sem nome, CPF, login ou perfil
- [ ] Sistema exige CNH, categoria e validade ao cadastrar um Motorista
- [ ] Sistema não exige CNH ao cadastrar um Administrador
- [ ] Sistema impede CPF duplicado dentro da mesma empresa
- [ ] Sistema aceita o mesmo CPF em empresas diferentes
- [ ] Sistema recusa CPF inválido (dígitos verificadores)
- [ ] Sistema impede login duplicado dentro da mesma empresa
- [ ] Sistema aceita o mesmo login em empresas diferentes
- [ ] Sistema impede cadastro sem definir o perfil de acesso
- [ ] O perfil Operador não aparece como opção em nenhuma tela da empresa
- [ ] A senha temporária é exibida ao final do cadastro
- [ ] Usuário cadastrado consegue fazer login e é obrigado a trocar a senha
- [ ] Usuário cadastrado como Motorista cai na tela de lançamentos
- [ ] Usuário cadastrado como Administrador cai no painel
- [ ] Administrador consegue gerar nova senha temporária para um funcionário
- [ ] Administrador consegue desbloquear uma conta bloqueada
- [ ] Usuário desativado não consegue autenticar
- [ ] Administrador não consegue desativar o próprio usuário
- [ ] Sistema impede desativar um Administrador quando restariam menos de dois ativos
- [ ] Sistema impede mudar o perfil de um Administrador para Motorista quando
      restariam menos de dois ativos
- [ ] Um Administrador consegue desbloquear e redefinir a senha do outro
- [ ] Administrador não vê usuários de outra empresa
- [ ] Sistema recusa anexo fora de PDF, JPG e PNG
- [ ] Sistema recusa anexo acima de 5 MB
- [ ] Anexo enviado só abre para usuário autenticado da mesma empresa
- [ ] Anexo não é acessível por link direto sem login

## Impacto no modelo de dados

A tabela `usuario` recebe três campos:

| Campo | Tipo | Observação |
|---|---|---|
| cnh | String(11) | Número de registro da CNH. Obrigatório para o perfil Motorista |
| categoria_cnh | String(5) | A, B, C, D, E ou combinações |
| validade_cnh | LocalDate | Base para um futuro alerta de habilitação vencida |

A tabela `anexo_usuario` já existe no modelo de dados e não sofre alteração.
