# Gestão Fácil — contexto do projeto

Sistema de controle de veículos para uma construtora real, desenvolvido como
trabalho de faculdade por um grupo **iniciante em programação** que já teve
contato com Java.

Os requisitos completos estão em `docs/requisitos.md` (versão 1.2). Leia esse
arquivo antes de implementar qualquer tela. Os `.docx` e o `.pdf` na raiz são
as versões entregues ao cliente e ao professor — não os edite.

## Stack

- Java 17 + Spring Boot (Maven)
- Spring Data JPA + PostgreSQL (interface de administracao: pgAdmin)
- Thymeleaf + Bootstrap (telas responsivas, servidas pelo próprio Spring Boot)
- Spring Security (autenticação e perfis)
- Projeto único: não há API REST separada nem app mobile separado
- No celular, o sistema é adicionado à tela inicial (PWA simples)

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
em `Modelo_de_Dados_Gestao_Facil.pdf`.

## Convenções

- Pacote base `com.gestaofacil`, subpacotes `model`, `repository`, `service`,
  `controller`
- Nomes de tabela e coluna em snake_case; classes e atributos Java em camelCase
- Templates Thymeleaf em `src/main/resources/templates/<entidade>/`
- Textos de tela em português

## Como ajudar este grupo

Eles são iniciantes e precisam **defender o código individualmente** na
apresentação, além de manter o sistema depois da faculdade.

- Explique o que o código faz e por quê, não apenas entregue o arquivo pronto.
- Prefira a solução simples e legível à solução esperta.
- Quando eles pedirem um CRUD já implementado antes, aponte o padrão existente
  no projeto e deixe que eles escrevam, revisando depois.
- Aponte quando algo violar uma das regras acima, mesmo que não tenham
  perguntado.
