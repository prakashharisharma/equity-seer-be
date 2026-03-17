# equity-seer-be

Multi-module **Gradle** project (Java 21, Spring Boot, Postgres/JPA).

## Modules

- `api`: Spring Boot REST app (Boot Jar)
- `worker`: Spring Boot worker app (Boot Jar)
- `service`: business services
- `data`: JPA entities + repositories (Postgres)
- `external`: outbound integrations/clients (placeholder)
- `config`: shared configuration/properties

## Run Postgres locally

```bash
docker compose up -d
```

## Run the apps

API (port **8080**):

```bash
./gradlew :api:bootRun
```

Worker (port **8081**):

```bash
./gradlew :worker:bootRun
```

## Test endpoints

Create user:

```bash
curl -X POST localhost:8080/users -H 'content-type: application/json' -d '{"name":"Alice"}'
```

List users:

```bash
curl localhost:8080/users
```

