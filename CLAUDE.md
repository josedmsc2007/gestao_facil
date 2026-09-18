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

**Pronto — cards #001 (login) e #001.1 (melhorias do login), com 57 testes:**

- autenticação com isolamento por empresa, bloqueio por tentativas e logout
- telas de login, troca obrigatória de senha e contas bloqueadas
- aviso de tentativas restantes a partir da 3ª falha (#001.1-RF02)
- atalhos de empresa na tela `/login` (#001.1-RF01)
- entidades `Empresa` e `Usuario` com seus repositórios
- carga inicial (`config/CargaInicial`): empresa `construtora-teste`,
  usuário `admin`, senha `admin12345`

**Ainda não existe:**

- as outras sete entidades e seus CRUDs (veículo, centro de custo, uso,
  abastecimento, manutenção, anexos)
- upload e download de anexos
- relatórios (RF09)
- geração de senha temporária pelo Administrador (#001-RF06) — o campo
  `senha_temporaria` e todo o fluxo de troca já existem, falta a tela que
  gera a senha, que virá junto com o CRUD de usuários
- cadastro de empresa (RF02) — hoje só o seed cria empresa

**Provisório, será substituído pelos próximos cards:** as telas `/painel`
(administrador) e `/lancamentos` (motorista), que existem só para o
redirecionamento por perfil ter destino, e `/usuarios/bloqueados`, que migra
para o CRUD de usuários.

## Regras que o código precisa garantir

1. **Isolamento por empresa (multi-tenant).** Toda consulta filtra pela empresa
   do usuário logado. Não basta esconder o botão na tela: buscar por id sempre
   confere se o registro pertence à empresa do usuário. Acessar
   `/veiculos/57` de outra empresa deve falhar.
2. **Status do veículo é calculado, nunca digitado.** Saída sem devolução →
   EM_USO; devolução → DISPONIVEL; manutenção EM_ANDAMENTO → EM_MANUTENCAO.
   Só o Administrador altera para INATIVO manualmente.
3. **Nada é apagado.** Veículo, usuário e centro de custo usam o campo `ativo`.
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

O card #001 acrescentou três colunas em `usuario`:

| Coluna | Para quê | Origem |
|---|---|---|
| `tentativas_invalidas` | RN004 — erros consecutivos; zera ao acertar | previsto no card |
| `bloqueado_ate` | RN004/RN005 — nulo quando a conta está liberada | previsto no card |
| `senha_temporaria` | RF07 — força a troca no primeiro acesso | decidido na implementação |

O par `(empresa_id, login)` tem restrição única no banco, não só na tela (RN007).

**Confiram `Empresa` e `Usuario` contra o PDF.** Como ele não é legível por
extração automática, os campos das duas entidades foram deduzidos do
`requisitos.md` e do card #001, e podem estar incompletos.

## Convenções

- Pacote base `com.gestaofacil`, subpacotes `model`, `repository`, `service`,
  `controller`, e mais dois criados no card #001: `config` (configuração do
  Spring) e `security` (peças do Spring Security)
- Objetos de formulário em `controller/form/` — nunca receba uma `@Entity`
  direto do formulário
- Nomes de tabela e coluna em snake_case; classes e atributos Java em camelCase
- Templates Thymeleaf em `src/main/resources/templates/<entidade>/`
- Textos de tela em português

## Padrões estabelecidos no card #001

Os próximos CRUDs devem seguir estes padrões.

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

URLs internas **não** levam a empresa (`/painel`, `/usuarios/bloqueados`): o
isolamento vem da sessão, não do endereço. Mapeamentos exatos têm prioridade
sobre o atalho `/{identificador}`, então uma rota nova como `/veiculos` não
conflita.

Em `SecurityConfig`, `anyRequest().authenticated()` já protege qualquer rota
nova. Acrescente uma regra só quando a tela for de um perfil específico:

```java
.requestMatchers("/veiculos/**").hasRole("ADMINISTRADOR")
```

CSRF está ligado. Toda ação que altera dados é `POST` com `th:action` — nunca
um link.

### Telas

**Não existe layout base ainda.** Existem apenas os fragmentos
`templates/fragmentos/barra.html` (barra do topo com o botão Sair, que também
grava o atalho de empresa) e `fragmentos/atalhos-empresa.html`. A barra entra
assim nas telas internas:

```html
<nav th:replace="~{fragmentos/barra :: barra(${logado})}"></nav>
```

O `<head>` com o CDN do Bootstrap está **repetido** nas cinco telas. JavaScript
próprio vai em `static/js/`, rota já liberada no `SecurityConfig`. Antes do
próximo CRUD, decidam se criam um layout base — a duplicação vai se multiplicar.

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
empresa: um usuário da empresa A não alcança registro da empresa B.

## Como rodar

Cada integrante copia `application-local.properties.exemplo` para
`application-local.properties` (ignorado pelo Git) e põe ali a senha do
PostgreSQL. O banco `gestao_facil` precisa existir antes.

```
.\mvnw.cmd spring-boot:run     # http://localhost:8080/construtora-teste
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
