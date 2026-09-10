# Gestão Fácil - #001 - DESENVOLVIMENTO DA TELA DE LOGIN (FRONT E BACKEND)

Atende ao **RF01** do documento de requisitos v1.2.

## Descrição

- Analista de Requisitos (PO): Victor Ruan
- Desenvolvedor: Victor Ruan
- Desenvolvedor: José A. Damasceno Lopes
- Analista de Qualidade (QA): Leonardo Almeida Silva

## Descrição da funcionalidade atual

Atualmente o sistema não possui mecanismo de autenticação. A implementação da
tela de login permitirá que apenas usuários cadastrados (administradores e
motoristas) acessem o sistema, e que cada usuário acesse somente os dados da
empresa à qual pertence.

## Requisitos funcionais

**#001-RF01 – Autenticação de usuários**
O sistema deverá permitir que o usuário informe usuário e senha para realizar a
autenticação (sem necessidade de CPF ou CNPJ no login).

**#001-RF02 – Identificação da empresa pelo endereço**
Cada empresa possui um endereço de acesso próprio, formado pelo seu
identificador (ex.: `.../construtora-silva`). Ao acessar por esse endereço, a
empresa já fica identificada e o usuário informa apenas usuário e senha. Caso o
sistema seja acessado por um endereço sem identificação de empresa, a tela
deverá exibir um campo adicional para informá-la.

**#001-RF03 – Validação dos dados**
O sistema deverá validar as informações fornecidas antes do envio.

**#001-RF04 – Mensagem de erro**
O sistema deverá exibir mensagem informando que usuário ou senha estão
incorretos caso a autenticação falhe. A mensagem não deve indicar qual dos dois
campos está errado.

**#001-RF05 – Encerramento da sessão**
O sistema deverá permitir que o usuário encerre a sessão por meio da opção
"Sair", invalidando a sessão no servidor.

**#001-RF06 – Recuperação de senha**
O usuário que esquecer a senha deverá solicitar ao Administrador da empresa, que
gera uma senha temporária no cadastro do usuário. Não haverá envio de e-mail nem
de SMS.

**#001-RF07 – Troca obrigatória de senha temporária**
No primeiro acesso realizado com senha temporária, o sistema deverá direcionar o
usuário para a tela de definição de nova senha, bloqueando o acesso às demais
telas enquanto a troca não for concluída.

## Regras de negócio

- **RN001** – Campo de usuário obrigatório.
- **RN002** – Campo de senha obrigatório.
- **RN003** – A senha deve possuir no mínimo 8 caracteres. Validada na tela de
  definição/troca de senha, não na tela de login.
- **RN004** – Após 5 tentativas inválidas consecutivas, a conta é bloqueada por
  30 minutos.
- **RN005** – O Administrador da empresa pode desbloquear a conta imediatamente,
  sem aguardar os 30 minutos.
- **RN006** – Após o login, o sistema redireciona conforme o perfil:
  Administrador para o painel de gestão; Motorista para a tela de lançamento de
  saída, devolução e abastecimento.
- **RN007** – O login é único dentro de cada empresa. Empresas diferentes podem
  ter usuários com o mesmo login.
- **RN008** – O usuário só é autenticado na empresa à qual pertence. Credenciais
  válidas de uma empresa não autenticam no endereço de outra empresa.
- **RN009** – Usuários marcados como inativos não podem autenticar.
- **RN010** – As senhas são armazenadas com hash (BCrypt), nunca em texto
  legível.

## Critérios de aceitação

- [ ] Usuário consegue realizar login com credenciais válidas
- [ ] Sistema impede acesso de usuários não cadastrados
- [ ] Sistema apresenta mensagem de erro genérica, sem revelar qual campo está incorreto
- [ ] Sistema redireciona o usuário para a página inicial conforme o perfil
- [ ] Acesso pelo endereço da empresa não exige campo adicional de empresa
- [ ] Usuário de uma empresa não consegue autenticar no endereço de outra empresa
- [ ] Usuário inativo não consegue autenticar
- [ ] Senha temporária direciona obrigatoriamente para a troca de senha
- [ ] Senha com menos de 8 caracteres é recusada na tela de troca de senha
- [ ] Conta é bloqueada após 5 tentativas inválidas consecutivas
- [ ] Administrador consegue desbloquear a conta antes dos 30 minutos
- [ ] Após "Sair", as páginas internas não são acessíveis pelo botão voltar do navegador

## Impacto no modelo de dados

A tabela `usuario` recebe dois campos para atender a RN004 e RN005:

| Campo | Tipo | Observação |
|---|---|---|
| tentativas_invalidas | Integer | zerado a cada login bem-sucedido |
| bloqueado_ate | LocalDateTime | nulo quando a conta não está bloqueada |
