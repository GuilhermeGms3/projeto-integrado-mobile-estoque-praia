# Estrutura e versionamento do projeto mobile

## Repositório

- Nome: `projeto-integrado-mobile-estoque-praia`
- Aplicação: Android nativo em Kotlin com Jetpack Compose
- Visibilidade: privada
- Branch padrão: `main`

## Diretórios

| Caminho | Finalidade |
| --- | --- |
| `app/` | Código-fonte, manifest e recursos do aplicativo Android. |
| `assets/` | Logotipos, wireframes e demais arquivos visuais de origem. |
| `docs/` | Decisões, diagramas, regras de negócio e documentação acadêmica. |
| `tests/` | Plano, casos e evidências de testes complementares. |
| `.github/` | Template de Pull Request e workflow de integração contínua. |

Como todas as pastas possuem conteúdo útil, não é necessário manter arquivos `.gitkeep`.

## Git Flow simplificado

1. `main` contém a versão estável.
2. `develop` recebe e integra o trabalho aprovado.
3. Cada tarefa parte de `develop` em uma branch `feature/<nome>` ou `docs/<nome>`.
4. A branch de trabalho retorna à `develop` por Pull Request revisado.
5. A entrega consolidada segue de `develop` para `main` por outro Pull Request.
6. Não devem ser feitos commits diretos em `main` ou `develop`.

Exemplo:

```text
main
  └── develop
        └── feature/setup-arquitetura
```

## Pull Requests e proteção

As branches `main` e `develop` devem exigir Pull Request e pelo menos uma aprovação. A opção que impede o administrador de ignorar as regras também deve permanecer ativa. O template do repositório orienta descrição, validação e evidências.

## Baseline

A primeira baseline utiliza Versionamento Semântico com o identificador `v0.1.0-alpha` e a release **Baseline Inicial: Estrutura e Configuração**.

## Responsabilidade da equipe

Cada integrante deve possuir acesso ao repositório e criar sua própria branch a partir de `develop` nas próximas tarefas. Convites dependem do nome de usuário de cada integrante no GitHub.
