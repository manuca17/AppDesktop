# AppDesktop — Taça Lab (Administração)

Aplicação **desktop JavaFX** de administração do sistema **Taça Lab**. Destina-se à
**artesã/administrador** e permite gerir o atelier de cerâmica a partir do computador:
catálogo, projetos personalizados, encomendas e reuniões, com suporte a fotos e chat.

Autentica-se e consome dados da API REST do backend
**[proj2](https://github.com/manuca17/proj2)** (Spring Boot).

## Ecossistema Taça Lab

| Componente | Repositório | Stack |
|------------|-------------|-------|
| **App Desktop (admin)** (este repo) | [AppDesktop](https://github.com/manuca17/AppDesktop) | JavaFX |
| **Backend / API REST** | [proj2](https://github.com/manuca17/proj2) | Spring Boot + PostgreSQL |
| **Portal Web** | [Sistemadeinformaocermica](https://github.com/manuca17/Sistemadeinformaocermica) | React + Vite |

## Stack

- **Java 17** + **JavaFX 17** (controls + FXML)
- **ControlsFX**, **Ikonli** e **BootstrapFX** para UI
- **Jackson** para (des)serialização JSON da API
- **Maven** (com wrapper `mvnw`)
- `java.net.http.HttpClient` para comunicação com o backend

## Requisitos

- **JDK 17+**
- O backend [proj2](https://github.com/manuca17/proj2) a correr (por omissão em `http://localhost:8080`)

## Como correr

```bat
mvnw.cmd clean javafx:run
```

(No Linux/macOS: `./mvnw clean javafx:run`.)

## Testes

```bat
mvnw.cmd test
```

## Configuração da API

Por omissão a app liga-se a:

- Login de admin/artesã → `http://localhost:8080/api/artesas`
- Utilizadores/clientes → `http://localhost:8080/api/utilizadores`

Estes URLs podem ser alterados por **propriedade de sistema** ou **variável de ambiente**:

| Alvo | Propriedade (`-D`) | Variável de ambiente |
|------|--------------------|----------------------|
| Base de utilizadores | `appdesktop.api.base-url` | `APPDESKTOP_API_BASE_URL` |
| Base de artesãs (admin) | `appdesktop.api.artesa-base-url` | `APPDESKTOP_API_ARTESA_BASE_URL` |

Exemplo:

```bat
mvnw.cmd javafx:run -Dappdesktop.api.artesa-base-url=http://192.168.1.10:8080/api/artesas
```

## Funcionalidades

- **Login de administrador** validado contra a API (`/api/artesas/login`).
- Após autenticação, abre o **painel de administração** com navegação lateral:
  - **Dashboard** — visão geral
  - **Catálogo** — gestão de artigos (com fotos)
  - **Projetos** — projetos personalizados dos clientes
  - **Encomendas** — gestão de encomendas
  - **Reuniões** — marcação e gestão de reuniões
- Suporte a **fotos** de artigos e **chat** por projeto, através dos serviços da API.

## Estrutura

```
src/main/java/com/example/appdesktop/
  HelloApplication.java        Arranque JavaFX (carrega o ecrã de login)
  Launcher.java                Launcher (entry point sem módulo)
  LoginController.java         Login de admin contra a API
  Admin*Controller.java        Controladores das páginas de administração
  AdminPage / AdminPageNavigator   Navegação entre páginas do painel
  models/                      Modelos do domínio (Utilizador, Artesa, Projeto, ...)
  services/                    Serviços HTTP para a API (Utilizador, Catálogo,
                               Encomenda, Chat, Reunião, Upload, Pagamento, ...)
src/main/resources/com/example/appdesktop/
  login-view.fxml              Ecrã de login
  admin-*-view.fxml            Ecrãs do painel de administração
```

> Nota: existe também `ClientPortalDataService` com dados de exemplo (mock) usados durante
> o desenvolvimento de algumas vistas.
