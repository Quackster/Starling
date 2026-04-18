# Starling

Starling is a Java 17 server for a Habbo-style r26 protocol implementation. It listens on a TCP socket, performs the legacy handshake and crypto negotiation, loads navigator and room state from MariaDB, seeds the schema on startup, and exposes a small client probe for validating the handshake flow.

**Most of this has been almost entirely written by AI as a demonstration**

## Requirements

- JDK 17 or newer
- MariaDB on `127.0.0.1:3306` or equivalent connection details supplied through config or environment variables
- Network access to Maven Central and JitPack for the first dependency resolution

## Screenshots

<img width="1577" height="1291" alt="image" src="https://github.com/user-attachments/assets/528a3d9b-b536-483e-9062-8b160a09548b" />

## Project Layout

- `src/main/java` server and client sources
- `src/main/resources` bundled configuration and SQL bootstrap resources
- `src/test/java` unit and integration tests
- `sql/seed.sql` seed data for the default admin user and navigator categories
- `dist/` packaged runtime output created by `packageDist`
- `logs/` runtime log output

## Configuration

Bundled defaults live in `src/main/resources/server.properties`.

At runtime configuration is loaded in this order:

1. Bundled `server.properties`
2. `config/server.properties`
3. `-Dstarling.config=<path>`
4. Environment variables

Supported environment overrides:

- `STARLING_SERVER_PORT`
- `STARLING_DB_HOST`
- `STARLING_DB_PORT`
- `STARLING_DB_NAME`
- `STARLING_DB_USERNAME`
- `STARLING_DB_PASSWORD`
- `STARLING_DB_PARAMS`

The default local configuration expects:

```properties
server.port=30000
db.host=127.0.0.1
db.port=3306
db.name=starling
db.username=root
db.password=verysecret
db.params=useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8
```

## Build

Use the Gradle wrapper from the repo root:

```powershell
.\gradlew.bat clean build
```

The main application jar is written to `build/libs/`.

If you want an installable runtime layout with `bin/`, `lib/`, and a copied config file:

```powershell
.\gradlew.bat packageDist
```

That writes the packaged output to `dist/`.

## Run

Start the server with:

```powershell
.\gradlew.bat run
```

The server will:

- ensure the configured database exists
- create or migrate the schema
- seed default data
- reset room occupancy counters
- start the Netty listener on `server.port`

You can also run the built distribution directly after `packageDist`:

```powershell
.\dist\bin\starling-server.bat
```

## Tests

Run all tests with:

```powershell
.\gradlew.bat test
```

`DatabaseIntegrationTest` requires a reachable MariaDB instance using the configured credentials. The test suite creates and drops its own temporary database.

## Handshake Probe

There is a standalone client entry point for validating the r26 normal-socket handshake:

```powershell
.\gradlew.bat starlingClient --args="--host 127.0.0.1 --port 30000 --handshake-only"
```

Additional supported arguments:

- `--timeout-ms <value>`
- `--version <value>`
- `--client-url <value>`
- `--ext-vars-url <value>`
- `--machine-id <value>`

## Notes

- The Gradle project has been flattened to the repository root. Build and run commands now execute from the root folder.
- Runtime logs are written to `logs/starling.log`.
