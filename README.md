# Gestão Fácil — Sistema de Controle de Veículos

Sistema web para uma construtora controlar o uso da sua frota. Hoje esse
controle é feito em papel; o sistema digitaliza o registro de saída e devolução
dos veículos, os abastecimentos e as manutenções, e permite apurar o custo por
veículo e por obra. É acessado pelo navegador tanto no computador quanto no
celular, pelo mesmo endereço — no celular o funcionário adiciona o atalho à tela
inicial e usa como se fosse um aplicativo. O sistema é multiempresa: cada
empresa acessa por um endereço próprio, vê apenas os seus dados, e a
terminologia das telas é configurável (o que a construtora chama de "Obra",
outra empresa pode chamar de "Rota" ou "Contrato").

Trabalho de faculdade, desenvolvido para uma construtora real.

## Documentação

| Arquivo | Conteúdo |
|---|---|
| `docs/cards/` | Cards de desenvolvimento, um por funcionalidade |
| `PROJETO_..._v1.2.docx` | Documento oficial entregue ao cliente |
| `Modelo_de_Dados_Gestao_Facil.pdf` | Detalhamento das tabelas |
| `CLAUDE.md` | Estado atual do projeto e padrões de código |

## Tecnologias

| | |
|---|---|
| Linguagem | Java 17 |
| Framework | Spring Boot 3.3.5 |
| Build | Maven (via wrapper — não precisa instalar) |
| Banco de dados | PostgreSQL |
| Acesso a dados | Spring Data JPA / Hibernate |
| Telas | Thymeleaf + Bootstrap 5.3 |
| Segurança | Spring Security (senhas em hash BCrypt) |
| Testes | JUnit 5 + MockMvc, com banco H2 em memória |

## Como rodar numa máquina nova

### 1. Pré-requisitos

- **JDK 17 ou superior** — confira com `java -version`
- **PostgreSQL** com o **pgAdmin** — usado na porta padrão 5432

Maven **não** precisa ser instalado: o projeto traz o Maven Wrapper (`mvnw`),
que baixa a versão certa sozinho na primeira execução.

> Se você tiver mais de uma versão do PostgreSQL instalada, elas ficam em portas
> diferentes (5432, 5433...). Anote em qual delas você vai criar o banco.

### 2. Criar o banco no pgAdmin

O sistema **não** cria o banco sozinho — ele precisa existir antes.

1. Abra o pgAdmin e conecte no servidor
2. Clique com o botão direito em **Databases** → **Create** → **Database...**
3. Em *Database*, digite exatamente `gestao_facil`
4. Salve

As tabelas **não** precisam ser criadas à mão: o Hibernate está configurado com
`ddl-auto=update` e cria tudo na primeira vez que a aplicação sobe.

### 3. Configurar a sua senha

A senha do banco é diferente em cada máquina, e por isso **não** fica no
`application.properties` (que vai para o Git). Cada integrante cria o seu
arquivo local, que o Git ignora.

Na raiz do projeto, copie o modelo:

```powershell
# Windows (PowerShell)
Copy-Item application-local.properties.exemplo application-local.properties
```

```bash
# Linux / macOS
cp application-local.properties.exemplo application-local.properties
```

Abra o `application-local.properties` recém-criado e troque
`troque-pela-sua-senha` pela senha do seu usuário `postgres`. Se o seu
PostgreSQL estiver em outra porta ou com outro usuário, o próprio arquivo tem
as linhas comentadas para ajustar.

**Nunca edite a senha direto no `application.properties`** — ele é versionado e
a senha iria parar no repositório.

### 4. Subir a aplicação

```powershell
# Windows
.\mvnw.cmd spring-boot:run
```

```bash
# Linux / macOS
./mvnw spring-boot:run
```

A primeira execução demora alguns minutos, baixando as dependências. Deu certo
quando o console mostrar:

```
Started GestaoFacilApplication in X seconds
 Carga inicial concluida.
 Empresa .... Construtora Teste (identificador: construtora-teste)
```

Para parar, `Ctrl+C`.

Também é possível abrir a pasta no IntelliJ IDEA e dar Run em
`GestaoFacilApplication` — a IDE já traz o Maven embutido.

### Se der erro

| Mensagem | Causa |
|---|---|
| `FATAL: autenticação do tipo senha falhou` | Senha errada no `application-local.properties` |
| `FATAL: database "gestao_facil" does not exist` | Banco não criado, ou criado num servidor de outra porta |
| `Connection refused` / `Connection to localhost:5432 refused` | O serviço do PostgreSQL não está rodando |
| `Web server failed to start. Port 8080 was already in use` | Outra aplicação (ou uma execução anterior) ocupando a porta |

## URLs de teste

Com a aplicação rodando, em `http://localhost:8080`:

| Endereço | O que é |
|---|---|
| `/construtora-teste` | **Atalho da empresa** — é este que o funcionário adiciona à tela inicial do celular |
| `/construtora-teste/login` | Tela de login da empresa. Pede só usuário e senha; a empresa vem do endereço |
| `/login` | Tela de login sem empresa no endereço. Mostra um campo a mais, para informar a empresa |
| `/painel` | Tela inicial do Administrador *(provisória)* |
| `/lancamentos` | Tela inicial do Motorista *(provisória)* |
| `/usuarios` | Lista de funcionários, com cadastro, edição, redefinição de senha, desbloqueio e ativação *(só Administrador)* |
| `/usuarios/novo` | Cadastro de funcionário *(só Administrador)* |
| `/usuarios/{id}/anexos` | Documentos do funcionário: enviar, baixar e remover (PDF, JPG ou PNG, até 5 MB). Os arquivos ficam em `~/gestao-facil-anexos`, fora do projeto *(só Administrador)* |
| `/gestao-facil` | **Atalho da equipe do sistema**: login dos Operadores |
| `/empresas` | Empresas clientes: cadastro, edição, inativação e nova senha para administrador *(só Operador)* |
| `/empresas/nova` | Cadastro de empresa com os seus dois administradores *(só Operador)* |

As telas de `/painel` em diante só abrem depois do login. Digitar o endereço
direto sem estar logado devolve para a tela de login.

## Credenciais de teste

A aplicação cria sozinha, na primeira execução, os usuários de exemplo — sem
eles não haveria como fazer o primeiro login, já que todo cadastro exige
alguém logado.

**Empresa de demonstração** — para as telas do dia a dia:

| | |
|---|---|
| Endereço | `/construtora-teste` (Construtora Teste) |
| Usuário | `admin` |
| Senha | `admin12345` |

**Equipe do sistema** — três Operadores, um por integrante, que cadastram as
empresas clientes:

| | |
|---|---|
| Endereço | `/gestao-facil` |
| Usuários | `jose.lopes`, `victor.ruan`, `leonardo.silva` |
| Senha | `operador12345` — cada um é obrigado a trocá-la no primeiro acesso |

> **Estas credenciais servem apenas para desenvolvimento e demonstração.**
> As senhas estão em texto legível no `application.properties`, que é
> versionado — qualquer pessoa com acesso ao repositório as conhece. Antes de
> qualquer uso real, a carga inicial deve ser desligada
> (`seed.habilitado=false`). Ainda não existe outra forma de criar os
> Operadores em produção.

No banco a senha é gravada em **hash BCrypt**, nunca em texto legível. Se
consultar a tabela `usuario` no pgAdmin, a coluna `senha` mostra algo como
`$2a$10$N9qo8uLO...`.

## Estrutura de pastas

```
GESTAO_FACIL/
├── docs/                          Requisitos e cards de desenvolvimento
├── src/main/java/com/gestaofacil/
│   ├── model/                     Entidades JPA — uma classe por tabela do banco
│   ├── repository/                Interfaces de acesso ao banco (Spring Data JPA)
│   ├── service/                   Regras de negócio, separadas das telas
│   ├── controller/                Recebe as requisições e escolhe a tela a mostrar
│   │   └── form/                  Classes que representam os campos dos formulários
│   ├── security/                  Peças do Spring Security: login, perfis, bloqueio
│   └── config/                    Configuração do Spring e carga inicial de dados
├── src/main/resources/
│   ├── templates/                 Telas em Thymeleaf, uma pasta por assunto
│   ├── static/css/                Folha de estilo própria, por cima do Bootstrap
│   └── application.properties     Configuração da aplicação (sem senhas)
├── src/test/java/                 Testes automatizados, espelhando os pacotes acima
├── application-local.properties.exemplo   Modelo da configuração local de cada um
├── pom.xml                        Dependências e build do Maven
└── mvnw / mvnw.cmd                Maven Wrapper — dispensa instalar o Maven
```

## Testes

```powershell
.\mvnw.cmd test
```

São 141 testes automatizados cobrindo o login (isolamento entre empresas, senha
temporária, bloqueio por tentativas, encerramento de sessão), o cadastro de
usuários e anexos, e o cadastro de empresas com o perfil Operador. Eles
rodam num banco H2 em memória, então **não** precisam do PostgreSQL no ar nem
do `application-local.properties` configurado.
