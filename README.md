# Starling

Starling is split into four JVM microservices:

- `gateway`
- `identity-service`
- `room-service`
- `navigator-service`

The Habbo client connects only to `gateway`. The backend services communicate internally over gRPC.

## Quick Start

1. Make sure Java 17 and MariaDB are available.
2. Build and package everything:

```powershell
.\gradlew.bat clean build packageDist
```

3. Update the packaged config files under [`dist`](/C:/SourceControl/Starling/dist) if your DB host, port, username, or password differ from the defaults.
4. Start the whole stack:

```powershell
.\dist\start-all.bat
```

On Unix-like systems, use:

```bash
./gradlew clean build packageDist
./dist/start-all
```

## Prerequisites

- Java 17
- MariaDB running and reachable from this machine
- A database user that matches the default config, or updated config files before startup

Default database settings in the packaged configs:

- host: `127.0.0.1`
- port: `3306`
- database: `starling`
- username: `root`
- password: `verysecret`

MariaDB itself must already be running before the services start.

## Compile

If you only want to compile and run tests:

```powershell
.\gradlew.bat build
```

If you want a runnable packaged output under [`dist`](/C:/SourceControl/Starling/dist):

```powershell
.\gradlew.bat clean build packageDist
```

On Unix-like systems:

```bash
./gradlew build
./gradlew clean build packageDist
```

`packageDist` compiles every module, runs the test suite, and writes packaged service folders into [`dist`](/C:/SourceControl/Starling/dist).

## Packaged Output

After `packageDist`, the repo root contains:

- [`dist/gateway`](/C:/SourceControl/Starling/dist/gateway)
- [`dist/identity-service`](/C:/SourceControl/Starling/dist/identity-service)
- [`dist/room-service`](/C:/SourceControl/Starling/dist/room-service)
- [`dist/navigator-service`](/C:/SourceControl/Starling/dist/navigator-service)

Each service folder contains:

- `bin/`
- `lib/`
- `config/`
- `logs/`
- `start.bat`
- `start`

## Run Everything

Windows:

```powershell
.\dist\start-all.bat
```

Unix-like systems:

```bash
./dist/start-all
```

## Run Services Individually

Recommended startup order:

1. `identity-service`
2. `room-service`
3. `navigator-service`
4. `gateway`

Windows examples:

```powershell
.\dist\identity-service\start.bat
.\dist\room-service\start.bat
.\dist\navigator-service\start.bat
.\dist\gateway\start.bat
```

Unix-like systems:

```bash
./dist/identity-service/start
./dist/room-service/start
./dist/navigator-service/start
./dist/gateway/start
```

## Default Ports

- `gateway`: `30000`
- `identity-service`: `50051`
- `room-service`: `50052`
- `navigator-service`: `50053`

Health ports:

- `gateway`: `18080`
- `identity-service`: `18081`
- `room-service`: `18082`
- `navigator-service`: `18083`

## Config Files

Packaged config files live here:

- [`dist/gateway/config/server.properties`](/C:/SourceControl/Starling/dist/gateway/config/server.properties)
- [`dist/identity-service/config/identity-service.properties`](/C:/SourceControl/Starling/dist/identity-service/config/identity-service.properties)
- [`dist/room-service/config/room-service.properties`](/C:/SourceControl/Starling/dist/room-service/config/room-service.properties)
- [`dist/navigator-service/config/navigator-service.properties`](/C:/SourceControl/Starling/dist/navigator-service/config/navigator-service.properties)

The packaged `start` wrappers set these environment variables automatically:

- `STARLING_GATEWAY_CONFIG`
- `STARLING_IDENTITY_CONFIG`
- `STARLING_ROOM_CONFIG`
- `STARLING_NAVIGATOR_CONFIG`

If you want to point a service at a different config file, set the matching environment variable before launching it.

## Notes

- `gateway` is the only service the Habbo client should connect to.
- `navigator-service` depends on `room-service`.
- Logs are written under each service's `dist/<service>/logs` folder.
