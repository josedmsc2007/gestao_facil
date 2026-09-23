# Gestão Fácil — contexto do projeto

Sistema de controle de veículos para uma construtora real, desenvolvido como
trabalho de faculdade por um grupo **iniciante em programação** que já teve
contato com Java.

Os requisitos completos estão em `docs/requisitos.md` (versão 1.2). Leia esse
arquivo antes de implementar qualquer tela. Cada funcionalidade tem um card em
`docs/cards/`. Os `.docx` e o `.pdf` na raiz são as versões entregues ao cliente
e ao professor — não os edite.

## Stack

- Java 17 + Spring Boot (Maven, com wrapper `mvnw`)
- Spring Data JPA + PostgreSQL (interface de administracao: pgAdmin)
- Thymeleaf + Bootstrap 5.3 por CDN (telas responsivas, servidas pelo Spring Boot)
- Spring Security (autenticação e perfis)
- H2 em memória **só nos testes** (`src/test/resources/application.properties`)
- Projeto único: não há API REST separada nem app mobile separado
- No celular, o sistema é adicionado à tela inicial (PWA simples)

## Estado atual

**Pronto: cards #001, #001.1, #003, #002 e #004, com 163 testes.** O #002 foi
feito depois do #003, fora da ordem dos números.

- **#001 e #001.1, login:** autenticação com isolamento por empresa, bloqueio
  por tentativas, logout, troca obrigatória de senha, aviso de tentativas
  restantes e atalhos de empresa na tela `/login`
- **#003, usuários** (`/usuarios`, só Administrador): cadastro e edição com
  CPF validado, CNH obrigatória para Motorista, senha temporária exibida uma
  vez, redefinição de senha, desbloqueio, ativação e desativação, e a regra dos
  dois administradores ativos (#003-RN015). Anexos do funcionário
  (`/usuarios/{id}/anexos`): PDF, JPG e PNG de até 5 MB, gravados fora do
  projeto (`gestao-facil.anexos.diretorio`). A antiga `/usuarios/bloqueados`
  virou o botão Desbloquear da lista
- **#002, empresas** (`/empresas`, só Operador): perfil `OPERADOR`; cadastro
  de empresa já com os dois administradores; sugestão do identificador a partir
  do nome; edição de nome e rótulos (o identificador não muda); inativação, que
  barra o login de todos os usuários da empresa; nova senha para Administrador
  (`/empresas/{id}/administradores`)
- **#004, veículos** (`/veiculos`, só Administrador): cadastro e edição de
  nome, ano, placa, RENAVAM e tipo de combustível; desativação e reativação.
  O status **não aparece no formulário**: o veículo nasce `DISPONIVEL` e o
  `VeiculoForm` não tem esse campo (regra 2). Placa e RENAVAM são únicos por
  empresa e normalizados antes de gravar (`service/FormatoDeVeiculo`): placa
  em maiúsculas sem hífen, RENAVAM só com números e sempre com 11 dígitos,
  completando os zeros à esquerda dos documentos antigos
- **Carga inicial** (`config/CargaInicial`), em duas etapas independentes:
  - empresa reservada `gestao-facil` com três Operadores (`jose.lopes`,
    `victor.ruan`, `leonardo.silva`, senha `operador12345`, com troca
    obrigatória no primeiro acesso)
  - empresa `construtora-teste` com o usuário `admin`, senha `admin12345`.
    Ela nasce com **um** administrador só, exceção que a tela do #002 não
    permite

**Ainda não existe:**

- as entidades e CRUDs de centro de custo, uso, abastecimento, manutenção e
  anexo de manutenção
- as transições de status do veículo para `EM_USO` (#006) e `EM_MANUTENCAO`
  (#008). Hoje só existem `DISPONIVEL` e `INATIVO` na prática
- relatórios (RF09)
- forma de criar os Operadores em produção: hoje só a carga inicial os cria,
  e ela é desligada em produção (`seed.habilitado=false`)
- derrubar a sessão de quem já está logado quando a empresa é inativada. A
  inativação barra o **próximo** login e não a sessão aberta

**Provisório, será substituído pelos próximos cards:** as telas `/painel`
(administrador) e `/lancamentos` (motorista), que existem só para o
redirecionamento por perfil ter destino.

**Divergências a resolver com o PO:**

- `docs/requisitos.md` (v1.2) ainda diz que "qualquer Administrador pode
  cadastrar empresa". Os cards #002 e #003 citam a versão 1.3, em que só o
  Operador cadastra. O código segue os cards.
- o #004 não diz o que fazer ao desativar um veículo `EM_USO` ou
  `EM_MANUTENCAO`. O código **recusa** e explica o motivo na tela
  (`VeiculoService.podeSerDesativado`): desativar apagaria um status calculado,
  e a devolução do #006 devolveria o veículo "inativo" para `DISPONIVEL`
  sozinha.

## Regras que o código precisa garantir

1. **Isolamento por empresa (multi-tenant).** Toda consulta filtra pela empresa
   do usuário logado. Não basta esconder o botão na tela: buscar por id sempre
   confere se o registro pertence à empresa do usuário. Acessar
   `/veiculos/57` de outra empresa deve falhar. **Única exceção:** as telas
   `/empresas` do Operador, que por definição mexem em outras empresas. Elas
   têm um limite próprio: nunca alcançam a empresa reservada nem dado
   operacional (ver "O perfil Operador" abaixo).
2. **Status do veículo é calculado, nunca digitado.** Saída sem devolução →
   EM_USO; devolução → DISPONIVEL; manutenção EM_ANDAMENTO → EM_MANUTENCAO.
   Só o Administrador altera para INATIVO manualmente.
3. **Nada é apagado.** Veículo, usuário e centro de custo usam o campo `ativo`;
   empresa usa `ativa`.
   Exclusão física quebraria os lançamentos históricos e os relatórios.
4. **Data informada ≠ data de registro.** Os lançamentos aceitam data
   retroativa, então cada um guarda também `data_registro`, preenchido pelo
   sistema e não editável.
5. **Anexos nunca em pasta pública.** Arquivos vão para fora do projeto, com
   nome UUID, servidos por endpoint que confere login e empresa.
6. **Centro de custo é genérico.** A tabela é `centro_custo`; o nome exibido
   vem de `empresa.rotulo_cc_singular` / `rotulo_cc_plural`. Nunca escreva
   "Obra" fixo em template ou mensagem — use o rótulo da empresa.

## Modelo de dados

Nove tabelas: `empresa`, `usuario`, `centro_custo`, `veiculo`, `uso_veiculo`,
`abastecimento`, `manutencao`, `anexo_usuario`, `anexo_manutencao`.

Toda tabela, exceto `empresa`, tem `empresa_id`. O detalhamento dos campos está
em `Modelo_de_Dados_Gestao_Facil.pdf` (atenção: o PDF usa fontes embutidas e
não é legível por extração automática de texto).

Existem hoje `empresa`, `usuario`, `anexo_usuario` e `veiculo`.

`veiculo` nasceu no #004 com `empresa_id`, `nome`, `ano`, `renavam`, `placa`,
`tipo_combustivel` e `status`. Ela **não tem** o campo `ativo` das outras: o
valor `INATIVO` do próprio `status` faz esse papel, e dois campos para a mesma
informação acabariam se contradizendo.

Colunas acrescentadas pelos cards às tabelas que já existiam:

| Tabela | Coluna | Para quê | Card |
|---|---|---|---|
| `usuario` | `tentativas_invalidas`, `bloqueado_ate` | bloqueio por tentativas (RN004/RN005) | #001 |
| `usuario` | `senha_temporaria` | força a troca no primeiro acesso (RF07); decidido na implementação | #001 |
| `usuario` | `cargo`, `cpf` | cadastro do funcionário; CPF só com números | #003 |
| `usuario` | `cnh`, `categoria_cnh`, `validade_cnh` | habilitação, obrigatória só para Motorista | #003 |
| `empresa` | `ativa` | empresa inativa não aceita login (#002-RN013) | #002 |
| `empresa` | `data_cadastro`, `cadastrada_por_id` | histórico: qual Operador cadastrou e quando. Nulos nas empresas da carga | #002 |

As três colunas de `empresa` **não estavam no card #002**, que diz "nenhuma
coluna nova é necessária". Sem elas, porém, não haveria como atender a RF06
(inativar) nem o critério do histórico. Confirmem com o PO.

Restrições únicas no banco, não só na tela: `(empresa_id, login)` e
`(empresa_id, cpf)` em `usuario`; `(empresa_id, placa)` e
`(empresa_id, renavam)` em `veiculo`; `identificador` em `empresa`.

`usuario.perfil` aceita `OPERADOR`, `ADMINISTRADOR` e `MOTORISTA`;
`veiculo.status` aceita `DISPONIVEL`, `EM_USO`, `EM_MANUTENCAO` e `INATIVO`;
`veiculo.tipo_combustivel` aceita os valores de `TipoCombustivel`. Todos
gravados como texto.

**Confiram `Empresa` e `Usuario` contra o PDF.** Como ele não é legível por
extração automática, os campos das duas entidades foram deduzidos do
`requisitos.md` e dos cards, e podem estar incompletos.

## Convenções

- Pacote base `com.gestaofacil`, subpacotes `model`, `repository`, `service`,
  `controller`, e mais dois criados no card #001: `config` (configuração do
  Spring) e `security` (peças do Spring Security)
- Objetos de formulário em `controller/form/` — nunca receba uma `@Entity`
  direto do formulário
- Nomes de tabela e coluna em snake_case; classes e atributos Java em camelCase
- Templates Thymeleaf em `src/main/resources/templates/<entidade>/`
- Textos de tela em português

## Padrões estabelecidos

Criados nos cards #001, #003 e #002. Os próximos CRUDs devem segui-los. O CRUD
de referência é o de usuários (`UsuarioController`, `UsuarioService`,
`UsuarioForm`, `templates/usuarios/`); o de veículos (#004) é o mesmo desenho,
menor, e serve de segundo exemplo.

**Campo-chave que o banco compara é normalizado antes de gravar**, sempre por
uma classe de métodos estáticos em `service/`: `ValidadorDeCpf` (CPF só com
números) e `FormatoDeVeiculo` (placa em maiúsculas sem hífen, RENAVAM com 11
dígitos). Sem isso `abc-1d23` e `ABC1D23` seriam duas placas diferentes para a
restrição única, e o mesmo veículo entraria duas vezes. A mesma função
normaliza na conferência de duplicidade e na gravação.

### A empresa na autenticação

O login não tem campo de empresa visível. A empresa vem do endereço:

- `/{identificador}` — atalho que o funcionário põe na tela inicial do celular
- `/{identificador}/login` — tela de login; o controller põe o identificador
  num campo escondido chamado `empresa`
- `/login` — caminho de exceção (endereço sem empresa); o mesmo campo aparece
  visível

O formulário sempre envia para `POST /login` com três campos: `empresa`,
`usuario`, `senha`. `security/DetalhesLoginEmpresa` lê o campo `empresa` e
`security/AutenticacaoPorEmpresaProvider` faz a verificação, sempre buscando o
usuário com `findByEmpresaIdAndLogin`. Não existe busca por login sozinho — o
isolamento é estrutural.

Login aceito passa pelo `security/AutenticacaoSucessoHandler`, que substituiu
o `.defaultSuccessUrl("/", true)`: manda todo mundo para `/` do mesmo jeito e
ainda zera o aviso de tentativas.

**Armadilha registrada:** o provider é um componente e o Spring Security já o
adota sozinho. Nunca acrescente `.authenticationProvider(...)` no
`SecurityConfig` — ele passa a rodar duas vezes por tentativa e o bloqueio da
RN004 dispara na terceira em vez da quinta.

### Dois contadores de tentativas (#001.1-RF02)

- **Banco** (`tentativas_invalidas`, `ControleDeTentativasService`): o único
  que **bloqueia** a conta. Só existe para usuário real.
- **Sessão** (`security/AvisoDeTentativasNaSessao`): só **exibe** o aviso,
  contado por empresa + login digitado, subindo em toda falha, exista o login
  ou não. Nunca troque o aviso pelo número do banco: login inventado ficaria
  sem aviso, e isso revelaria quais logins existem (#001-RF04).

O `AutenticacaoFalhaHandler` registra a falha, o `AutenticacaoSucessoHandler`
zera a contagem daquele login e o `LoginController` retira o aviso da sessão
(vale para uma exibição só).

### Atalhos de empresa (#001.1-RF01)

A tela `/login` mostra as empresas em que **este navegador** já fez login com
sucesso, guardadas no `localStorage` por `static/js/atalhos-empresa.js`.

- **Gravar:** o fragmento `fragmentos/atalhos-empresa :: gravar(logado)` está
  na barra do topo e em `trocar-senha.html`. Como só tela interna o inclui, a
  empresa nunca é gravada por simples visita. Tela interna sem a barra precisa
  incluí-lo.
- **Exibir e remover:** só no navegador; o servidor manda a caixa vazia.
- **Proibido:** endpoint, busca, lista ou sugestão de empresas na tela de login.
  Ela é pública e exporia os clientes do sistema. O servidor nunca lê nem
  recebe a lista de atalhos.
- O script monta a tela com `textContent`, nunca `innerHTML`.

### O usuário logado

O principal da sessão é `security/UsuarioAutenticado`. Ele **copia** os valores
em vez de guardar a entidade `Usuario`, porque fica na sessão depois de a
consulta terminar (`open-in-view=false`).

```java
@GetMapping("/veiculos")
public String listar(@AuthenticationPrincipal UsuarioAutenticado logado, Model model) {
    model.addAttribute("veiculos",
            veiculoRepository.findByEmpresaIdAndAtivoTrue(logado.getEmpresaId()));
    model.addAttribute("logado", logado);   // a barra do topo precisa dele
    return "veiculos/lista";
}
```

Ele oferece `getId()`, `getNome()`, `getPerfil()`, `isSenhaTemporaria()`,
`getEmpresaId()`, `getEmpresaIdentificador()`, `getEmpresaNome()`,
`getRotuloCcSingular()` e `getRotuloCcPlural()` — os dois últimos atendem à
regra 6.

**`getEmpresaId()` é a origem do filtro de toda consulta.** Nas rotas com id
(`/veiculos/{id}`), use sempre `findByIdAndEmpresaId`, e responda **404** quando
vier vazio — 403 confirmaria que aquele id existe em algum lugar.

### Rotas e segurança

URLs internas **não** levam a empresa (`/painel`, `/usuarios`): o isolamento
vem da sessão, não do endereço. Mapeamentos exatos têm prioridade sobre o
atalho `/{identificador}`, então uma rota nova como `/veiculos` não conflita.
Mesmo assim, **acrescente o nome de toda rota nova de primeiro nível em
`EmpresaService.IDENTIFICADORES_PROIBIDOS`**: sem isso, uma empresa poderia ser
cadastrada com esse identificador e o atalho dela abriria a sua tela.

`SecurityConfig` é **fechado por padrão** desde o #002: a última linha é
`anyRequest().hasAnyRole("ADMINISTRADOR", "MOTORISTA")`. Toda rota nova já
nasce protegida contra visitante **e** contra o Operador (#002-RN008), sem
linha nova. Acrescente uma regra só quando a tela for de um perfil só:

```java
.requestMatchers("/veiculos/**").hasRole("ADMINISTRADOR")
```

Tela que o Operador também precise abrir vai na lista explícita de
`.authenticated()` do `SecurityConfig`, hoje com `/`, `/trocar-senha`, `/error`
e `/gestao-facil`.

CSRF está ligado. Toda ação que altera dados é `POST` com `th:action` — nunca
um link.

### O perfil Operador (#002)

- É um usuário comum com `perfil = OPERADOR`, que pertence à empresa reservada
  `Empresa.IDENTIFICADOR_DA_EQUIPE` (`gestao-facil`) e entra por
  `/gestao-facil/login`. Depois do login, o `InicioController` o manda para
  `/empresas`.
- Ele **não** é atribuível por tela nenhuma da empresa. A caixa de perfis vem
  de `Perfil.atribuiveisPelaEmpresa()`, nunca de `Perfil.values()`, e o
  `UsuarioController` confere de novo no POST.
- A empresa reservada fica fora de todas as telas do Operador: listagem e
  busca por id usam `findByIdentificadorNot...` / `findByIdAndIdentificadorNot`,
  com 404, o mesmo papel do `findByIdAndEmpresaId`.
- Das empresas clientes o Operador vê o cadastro e os **administradores**
  ativos. Motorista nem sai do banco: o perfil vai dentro da consulta.
- `InicioController` decide o destino com um `switch` sem `default` sobre o
  `Perfil`. Perfil novo no enum não compila até ganhar um destino ali.

### Telas

O `<head>` de todas as telas está em `fragmentos/layout.html`, criado no #003.
Um layout completo, com corpo embrulhado, foi descartado de propósito: o
motivo está no comentário do arquivo. Toda tela interna começa assim:

```html
<head th:replace="~{fragmentos/layout :: cabeca('Usuários')}"></head>
...
<nav th:replace="~{fragmentos/barra :: barra(${logado})}"></nav>
```

A barra (`fragmentos/barra.html`) tem o botão Sair e grava o atalho de empresa.
JavaScript próprio vai em `static/js/`, rota já liberada no `SecurityConfig`.

Senha gerada pelo sistema (cadastro de usuário, redefinição, cadastro de
empresa) aparece **uma vez**, por flash attribute, numa caixa `alert-warning`
na tela para onde o POST redireciona. No banco fica só o hash, e a senha
sai de `GeradorDeSenhaTemporaria`.

Um formulário por finalidade: se edição e cadastro têm campos diferentes, são
duas classes (`EmpresaEdicaoForm` e `EmpresaForm`, esta herdando da outra). O
campo que não pode mudar (identificador, empresa) **não existe** na classe da
edição. Não basta estar escondido na tela.

Telas pensadas para motorista no celular: campos `form-control-lg`, botões
grandes, poucos elementos. Três detalhes que já resolveram problemas reais e
devem ser mantidos: `autocapitalize="none"` em campos de texto que não são nome
próprio (o teclado do celular põe maiúscula e o login falha), `100dvh` em vez de
`100vh` (a barra do navegador empurra o botão para fora da tela) e fonte de 16px
nos campos (abaixo disso o iPhone dá zoom sozinho).

### Mensagens na tela

Três formatos, todos com `alert` do Bootstrap:

| Situação | Como |
|---|---|
| Erro vindo de um redirecionamento | parâmetro na URL: `th:if="${param.erro != null}"` |
| Erro de validação de campo | `BindingResult` + `th:errors="*{campo}"` e `is-invalid` |
| Confirmação após ação | `RedirectAttributes.addFlashAttribute("mensagem", ...)` |
| Ação recusada por regra de negócio | `addFlashAttribute("erro", ...)`, explicando o motivo e o que fazer |

Mensagem de login incorreto é **genérica** (#001-RF04) e vive numa constante no
provider — nunca diga qual campo está errado. A única exceção deliberada é a
conta bloqueada, que precisa se identificar para o funcionário entender por que
a senha certa parou de funcionar.

### Testes

Em `src/test/java`, espelhando o pacote da classe testada. Padrão adotado:
`@SpringBootTest` + `@AutoConfigureMockMvc` + `@Transactional`, cada teste
criando os próprios dados num `@BeforeEach` — nunca dependendo do seed nem de
outro teste. Rodam em H2, então não precisam de PostgreSQL no ar.

Todo CRUD novo precisa de pelo menos um teste que prove o isolamento por
empresa: um usuário da empresa A não alcança registro da empresa B. E, desde o
#002, um que prove que o Operador leva 403 na tela nova.

### Armadilhas que já custaram tempo

- **Coluna `NOT NULL` nova em tabela que já tem dados** (`ddl-auto=update`):
  o PostgreSQL recusa o `alter table` e o Hibernate só escreve o erro no
  console e segue em frente. O sistema quebra depois, longe da causa. Use
  `@ColumnDefault(...)` (ver `Empresa.ativa`). No H2 dos testes o problema não
  aparece, porque lá o banco nasce vazio.
- **O `application.properties` dos testes substitui o principal**, porque tem o
  mesmo nome. Propriedade nova que um teste precise tem de ir nele ou no
  `@SpringBootTest(properties = ...)`.
- **Teste que liga a carga inicial usa banco próprio** (`CargaInicialTests`,
  com outra `spring.datasource.url`). Sem isso, o que a carga grava fica no H2
  compartilhado e esbarra nos dados dos outros testes.
- **Carga inicial:** cada etapa confere sozinha o que já existe. Uma
  conferência única do tipo "a empresa de teste existe? então pare" impediria
  que dados novos, como os Operadores, chegassem a bancos já criados.
- **`.param(...)` repetido no MockMvc:** quando um método auxiliar já envia o
  campo e o teste manda outro valor com o mesmo nome, o Spring fica com o
  **primeiro** e o teste passa sem testar nada. Monte o POST inteiro à mão
  quando quiser variar um campo que o auxiliar já preenche (aconteceu no
  `anoNoFuturoERecusado`, do #004).

## Como rodar

Cada integrante copia `application-local.properties.exemplo` para
`application-local.properties` (ignorado pelo Git) e põe ali a senha do
PostgreSQL. O banco `gestao_facil` precisa existir antes.

```
.\mvnw.cmd spring-boot:run     # http://localhost:8080/construtora-teste  (admin)
                               # http://localhost:8080/gestao-facil       (Operadores)
.\mvnw.cmd test
```

## Como ajudar este grupo

Eles são iniciantes e precisam **defender o código individualmente** na
apresentação, além de manter o sistema depois da faculdade.

- Explique o que o código faz e por quê, não apenas entregue o arquivo pronto.
- Prefira a solução simples e legível à solução esperta.
- Quando eles pedirem um CRUD já implementado antes, aponte o padrão existente
  no projeto e deixe que eles escrevam, revisando depois.
- Aponte quando algo violar uma das regras acima, mesmo que não tenham
  perguntado.

## Manter este arquivo atualizado

**Ao final de qualquer tarefa que mude o sistema, atualize este arquivo antes
de encerrar**, sem esperar que peçam. Vale para código, para o modelo de dados
e para decisões que mudem como algo funciona.

O que precisa ficar em dia:

- a seção **Estado atual**: o que passou a existir e o que saiu da lista do que
  ainda falta
- os **padrões estabelecidos**, quando a tarefa criar um jeito novo de fazer
  algo que os próximos cards devem seguir
- as **armadilhas** que custaram tempo, para ninguém repetir
- o **modelo de dados**, quando uma coluna ou tabela for criada
- a contagem de testes, quando mudar

Este é um arquivo de **contexto**, não um changelog: reescreva as seções
existentes em vez de empilhar histórico, e mantenha o texto enxuto. O histórico
de quem fez o quê fica nos commits do Git.

Documentação que aponta caminho de arquivo é código disfarçado: se um arquivo
mudar de lugar, corrija a referência no mesmo commit que o moveu.
