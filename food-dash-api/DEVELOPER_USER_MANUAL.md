# FoodDash Developer User Manual

This guide explains how to run, verify, and troubleshoot the backend locally and with Docker.

## 1. Prerequisites

Install the following before running the backend:

1. Java 21
2. Maven Wrapper is already included in the repo
3. PostgreSQL 17 or a compatible PostgreSQL instance
4. Redis 7 or a compatible Redis instance
5. Docker Desktop, if you want to run the full stack in containers

Useful tools:

1. `psql` for PostgreSQL access
2. `redis-cli` for Redis checks
3. Swagger UI in the app for API exploration

## 2. Project Commands

Run these from the repository root:

```powershell
./mvnw clean compile
./mvnw test
./mvnw clean package -DskipTests
./mvnw spring-boot:run
```

If you want to run with a specific profile:

```powershell
$env:SPRING_PROFILES_ACTIVE="dev"
./mvnw spring-boot:run
```

## 3. Local Backend Run

### Option A: Run with local PostgreSQL and Redis

Set these environment variables before starting the app:

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/food_dash"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="postgres"
$env:REDIS_HOST="localhost"
$env:REDIS_PORT="6379"
$env:JWT_SECRET="VGhpc0lzQURldmVsb3BtZW50T25seVNlY3JldEtleVdoaWNoTXVzdEJlQ2hhbmdlZEluUHJvZA=="
```

Start the application:

```powershell
./mvnw spring-boot:run
```

The backend will be available at:

```text
http://localhost:8080
```

Swagger UI:

```text
http://localhost:8080/swagger-ui
```

### Option B: Run the app from an IDE

1. Open the project in IntelliJ IDEA or your IDE of choice.
2. Set the environment variables listed above.
3. Run `com.fooddash.FoodDashApiApplication`.

## 4. PostgreSQL Setup

### Option A: Use Docker for PostgreSQL only

```powershell
docker run --name fooddash-postgres `
  -e POSTGRES_DB=food_dash `
  -e POSTGRES_USER=postgres `
  -e POSTGRES_PASSWORD=postgres `
  -p 5432:5432 `
  -d postgres:17-alpine
```

Check the database:

```powershell
psql "postgresql://postgres:postgres@localhost:5432/food_dash"
```

### Option B: Use a local PostgreSQL installation

Create the database manually:

```sql
CREATE DATABASE food_dash;
```

Then set:

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/food_dash"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="postgres"
```

## 5. Redis Setup

### Option A: Use Docker for Redis only

```powershell
docker run --name fooddash-redis -p 6379:6379 -d redis:7-alpine
```

Check Redis:

```powershell
redis-cli -h localhost -p 6379 ping
```

Expected response:

```text
PONG
```

### Option B: Use a local Redis installation

Start Redis and set:

```powershell
$env:REDIS_HOST="localhost"
$env:REDIS_PORT="6379"
```

## 6. Full Stack Docker Run

Use Docker Compose to start the complete stack:

```powershell
docker compose up --build
```

Run in detached mode:

```powershell
docker compose up -d --build
```

Stop the stack:

```powershell
docker compose down
```

Remove volumes as well:

```powershell
docker compose down -v
```

Services started by Compose:

1. `app`
2. `postgres`
3. `redis`

## 7. Default Ports

1. Backend API: `8080`
2. PostgreSQL: `5432`
3. Redis: `6379`

## 8. Flyway Migrations

Database schema is managed by Flyway.

Relevant migrations:

1. `V1__initial_schema.sql`
2. `V2__auth_users_and_refresh_tokens.sql`
3. `V3__restaurant_management_columns.sql`
4. `V4__payments_order_state_delivery_tracking.sql`
5. `V5__observability_and_webhooks.sql`

On application startup, Flyway validates and applies the schema automatically.

## 9. Running Tests

### Unit tests

```powershell
./mvnw test
```

### Compile only

```powershell
./mvnw -DskipTests compile
```

### Package the app

```powershell
./mvnw clean package -DskipTests
```

## 10. Docker Image Build

Build the application image manually:

```powershell
docker build -t fooddash-api .
```

Run the image:

```powershell
docker run --rm -p 8080:8080 `
  -e SPRING_PROFILES_ACTIVE=prod `
  -e DB_URL="jdbc:postgresql://host.docker.internal:5432/food_dash" `
  -e DB_USERNAME="postgres" `
  -e DB_PASSWORD="postgres" `
  -e REDIS_HOST="host.docker.internal" `
  -e REDIS_PORT="6379" `
  fooddash-api
```

## 11. Useful API Checks

Health check:

```powershell
Invoke-RestMethod http://localhost:8080/api/health
```

Swagger:

```text
http://localhost:8080/swagger-ui
```

Register a user:

```powershell
Invoke-RestMethod -Method Post `
  -Uri http://localhost:8080/api/v1/auth/register `
  -ContentType "application/json" `
  -Body '{
    "email":"user@example.com",
    "password":"StrongPass123",
    "fullName":"Test User",
    "phone":"+911234567890",
    "role":"CUSTOMER"
  }'
```

## 12. Websocket Notes

The websocket endpoint is:

```text
/ws
```

Private user notifications are delivered through:

```text
/user/queue/notifications
```

The client must connect with a valid JWT access token.

## 13. Troubleshooting

### Application fails to start

Check:

1. PostgreSQL is running and the `food_dash` database exists.
2. Redis is running on the configured host and port.
3. `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` are set correctly.
4. `JWT_SECRET` is a valid Base64-encoded value.

### Flyway validation fails

Check:

1. The database schema is clean.
2. The migration version matches the codebase.
3. No manual schema changes were made outside Flyway.

### Websocket connection is rejected

Check:

1. The JWT access token is valid and not blacklisted.
2. The user is authenticated.
3. The client is sending the token during the STOMP/WebSocket handshake.

### Tests fail because Docker is unavailable

The integration tests are container-aware.

If Docker Desktop is not running, the Testcontainers-backed tests will skip or fail depending on the environment. Start Docker Desktop before running the suite if you need full integration coverage.

## 14. Recommended Developer Workflow

1. Update code in the service layer first.
2. Add or update unit tests immediately.
3. Run `./mvnw test`.
4. Run `./mvnw clean package -DskipTests`.
5. Start the full stack with `docker compose up --build`.
6. Validate the API in Swagger UI.

## 15. Notes for Contributors

1. Keep ownership checks in the service layer.
2. Keep order status changes in `OrderStateMachineService`.
3. Persist notification and audit side effects instead of logging only.
4. Prefer Flyway migrations over ad hoc schema edits.
5. Do not duplicate controllers or services when extending the platform.
