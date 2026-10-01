# game-library-tracker

A web app for tracking the video games you own or want to play, with game data from the [RAWG API](https://rawg.io/apidocs). It has two parts: a Spring Boot REST API (this folder) and a React frontend (`frontend/`).

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

## Frontend

A React + TypeScript single-page app (Vite) lives in `frontend/`. It has four pages: Login, Register, Library (your games, with status, rating, notes and stats) and Search (find a game on RAWG and add it).

Requirements: Node.js 20+ and the backend running on port 8080.

```bash
cd frontend
npm install
npm run dev
```

Then open http://localhost:5173. The dev server proxies every `/api` request to `http://localhost:8080` (see `frontend/vite.config.ts`), so the browser only talks to one origin and the backend needs no CORS configuration in development.

How it works:

- `src/api.ts` is the only place that calls `fetch`. It adds the `Authorization: Bearer <token>` header and logs the user out when the backend answers 401.
- The token is kept in `localStorage` and shared with the pages through `AuthContext`.
- `/library` and `/search` are protected routes: without a token they redirect to `/login`.
- `src/types.ts` mirrors the backend DTOs.

`npm run build` type-checks the code and writes a production build to `frontend/dist`. `npm run lint` runs ESLint.

**Windows note:** Vite 8 bundles with Rolldown, which ships as an unsigned native binary. Windows Smart App Control blocks it ("An Application Control policy has blocked this file"). The npm scripts therefore set `NAPI_RS_FORCE_WASI=1`, which makes Rolldown use its WebAssembly build (`@rolldown/binding-wasm32-wasi`) instead. It behaves the same, only a little slower. Linting uses ESLint instead of the Vite template's default oxlint for the same reason: oxlint's native binary is blocked too and has no WebAssembly build, while ESLint is plain JavaScript.

## Tests

```bash
./mvnw test
```

The tests use an in-memory H2 database and their own test-only secrets (`src/test/resources/application-test.properties`), so they need neither PostgreSQL nor the environment variables above.
