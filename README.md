# Starling

Starling is a Java 17 multi-project workspace for two related applications backed by the same MariaDB database:

- `starling-server`: the Habbo-style r26 game server
- `starling-web`: a Javalin + Pebble CMS and public website
- `starling-core`: shared configuration, database bootstrap, and entity access

The default web theme is adapted from Lisbon presentation assets, but the web app keeps its own CMS, routing, and rendering code.

## Requirements

- JDK 17 or newer
- MariaDB reachable on `127.0.0.1:3306` or equivalent overrides supplied through config or environment variables
- Network access to Maven Central and JitPack for the first dependency resolution

## Project Layout

- `starling-core/` shared config, database, and entity code
- `starling-server/` TCP game server and handshake probe
- `starling-web/` CMS, admin UI, public theme, and integration tests
- `starling-server/dist/` packaged game server runtime created by `packageDist`
- `starling-web/dist/` packaged web runtime created by `packageDist`

## Configuration

Both applications load bundled defaults first, then optional external overrides, then environment variables.

### Shared Database Overrides

- `STARLING_DB_HOST`
- `STARLING_DB_PORT`
- `STARLING_DB_NAME`
- `STARLING_DB_USERNAME`
- `STARLING_DB_PASSWORD`
- `STARLING_DB_PARAMS`

### Game Server Config

Load order:

1. Bundled `starling-server/src/main/resources/server.properties`
2. `config/server.properties` in the current working directory, when present
3. `-Dstarling.config=<path>`
4. `STARLING_CONFIG=<path>`
5. Environment overrides

Supported server-specific environment overrides:

- `STARLING_SERVER_PORT`

Default bundled values:

```properties
server.port=30000
db.host=127.0.0.1
db.port=3306
db.name=starling
db.username=root
db.password=verysecret
db.params=useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8
```

### Web CMS Config

Load order:

1. Bundled `starling-web/src/main/resources/web.properties`
2. `config/web.properties` in the current working directory, when present
3. `-Dstarling.web.config=<path>`
4. `STARLING_WEB_CONFIG=<path>`
5. Environment overrides

Supported web-specific environment overrides:

- `STARLING_WEB_PORT`
- `STARLING_WEB_SESSION_SECRET`
- `STARLING_WEB_THEME`
- `STARLING_WEB_THEME_DIR`
- `STARLING_WEB_UPLOAD_DIR`
- `STARLING_WEB_ADMIN_EMAIL`
- `STARLING_WEB_ADMIN_PASSWORD`

Default bundled values:

```properties
web.port=8080
web.session.secret=change-me
web.theme=default
web.theme.directory=themes
web.upload.directory=uploads
web.admin.email=admin@starling.local
web.admin.password=admin123!
db.host=127.0.0.1
db.port=3306
db.name=starling
db.username=root
db.password=verysecret
db.params=useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8
```

The web app creates CMS tables on startup, ensures the upload and theme override directories exist, and seeds the first CMS admin only when `cms_admin_users` is empty.

## Build

Build everything from the repo root:

```powershell
.\gradlew.bat clean build
```

Create packaged runtimes for both applications:

```powershell
.\gradlew.bat packageDist
```

Simple packaged build command:

```powershell
cd C:\SourceControl\Starling
.\gradlew.bat packageDist
```

That produces:

- `starling-server/dist/`
- `starling-web/dist/`

## Run

### Packaged Dist Runtimes

Build the packaged `/dist/` folders from the repo root:

```powershell
cd C:\SourceControl\Starling
.\gradlew.bat packageDist
```

Run the website from its packaged folder:

```powershell
cd C:\SourceControl\Starling\starling-web\dist
.\bin\starling-web.bat
```

Run the game server from its packaged folder:

```powershell
cd C:\SourceControl\Starling\starling-server\dist
.\bin\starling-server.bat
```

If you want to start both, launch the server in one terminal and the website in another.

### Gradle Run Tasks

Run the game server from the repo root:

```powershell
.\gradlew.bat :starling-server:run
```

Run the web CMS from the repo root:

```powershell
.\gradlew.bat :starling-web:run
```

You can also start the packaged distributions directly after `packageDist`:

```powershell
.\starling-server\dist\bin\starling-server.bat
.\starling-web\dist\bin\starling-web.bat
```

## Web Routes

Canonical public routes:

- `/`
- `/news`
- `/news/:slug`
- `/page/:slug`
- `/media/:id/:filename`

Retro-compatible aliases:

- `/index`
- `/home`
- `/articles`
- `/articles/:slug`

Admin routes are served from `/admin`, including login, dashboard, pages, articles, menus, and media management.

## Tests

Run all tests:

```powershell
.\gradlew.bat test
```

Run only the web module tests:

```powershell
.\gradlew.bat :starling-web:test
```

`starling-web` includes unit coverage for slug generation, password hashing, Markdown rendering, and theme overrides, plus integration coverage for admin bootstrap, login, page/article publishing, HTMX previews, aliases, and media uploads.

## Handshake Probe

The game server module still includes a standalone client entry point for validating the r26 normal-socket handshake:

```powershell
.\gradlew.bat :starling-server:starlingClient --args="--host 127.0.0.1 --port 30000 --handshake-only"
```

Additional supported arguments:

- `--timeout-ms <value>`
- `--version <value>`
- `--client-url <value>`
- `--ext-vars-url <value>`
- `--machine-id <value>`
