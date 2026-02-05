# System Parameter Library – Dockerized Test Setup

This repository provides a Docker Compose–based test environment that allows
the System Parameter Library to be tested on any machine without relying on
local JDBC URLs or local database setup.

---

## What’s Included

The setup consists of two containers:

1. **MySQL database container (`db`)**
2. **Library test runner container (`lib`)** that builds the project and runs `mvn test`

This ensures that all tests are executed in a reproducible and environment-independent way.

---

## Project Files

### `docker-compose.yml`
Defines two services:
- `db` – MySQL 8.0 database
- `lib` – Maven + JDK 17 container that executes library tests

### `Dockerfile`
Builds a Maven/JDK 17 environment and runs `mvn test` inside the container.

### `docker/mysql/init/01_schema.sql`
Database initialization script.
All SQL files in this directory are executed automatically on the first startup
of the MySQL container to create required tables.

---

## How It Works

Inside Docker Compose, **`localhost` refers to the container itself**, not your host machine.
Therefore, services communicate using the Docker Compose service name.

- Database host: `db`
- Database port: `3306`

The following environment variables are passed to the test container:

- `JDBC_URL`
- `JDBC_USER`
- `JDBC_PASS`

These variables are used by integration tests and database-related components.

---

## Running Tests (Recommended)

To run all tests using Docker Compose:

```bash
docker compose up --build --exit-code-from lib
