# game-library-tracker

A Spring Boot REST API for tracking the video games you own or want to play, with game data from the [RAWG API](https://rawg.io/apidocs).

## Running locally

Requirements: JDK 17+, and PostgreSQL on `localhost:5432` with a `gametracker` database (user/password `postgres`/`postgres`).

Two environment variables are **required**. The app refuses to start if either is missing:

| Variable | What it is |
|---|---|
| `JWT_SECRET` | Base64-encoded key of at least 256 bits, used to sign login tokens |
| `RAWG_API_KEY` | Your RAWG API key (free at https://rawg.io/apidocs) |

Generate a `JWT_SECRET` with `openssl rand -base64 32`, or in PowerShell:

```powershell
$b = New-Object byte[] 32; [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); [Convert]::ToBase64String($b)
```

Then start the app on port 8080:

```bash
./mvnw spring-boot:run
```

## Tests

```bash
./mvnw test
```

The tests use an in-memory H2 database and their own test-only secrets (`src/test/resources/application-test.properties`), so they need neither PostgreSQL nor the environment variables above.
