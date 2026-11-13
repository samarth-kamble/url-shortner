# ShortLink

ShortLink is a production-grade, distributed URL shortening and click-analytics platform built with Spring Boot, PostgreSQL, Redis, Kafka, and Docker.

## Project Status

- **Current Phase**: Phase 2 Complete - Distributed Caching, Rate Limiting & Telemetry
- **Milestone**: Day 8 - Extreme Load Benchmarking, Java 25 Virtual Threads & Micrometer Observability
- **Status**: **Phase 2 Complete**. Full multi-tier caching (`L1 Caffeine -> L2 Redis -> Postgres`), atomic Redis+Lua Token Bucket rate limiting, Java 25 Virtual Threads, Micrometer metrics, and 44/44 tests passing.

## Architecture

![ShortLink Big Picture Architecture](docs/images/architecture.png)

ShortLink is designed as a modular monolith transitioning to an event-driven distributed architecture:

1. **Modular Monolith Foundation (Phase 1 Complete)**:
   - `com.shortlink.api`: REST endpoints (`POST /api/v1/links`, `GET /{shortCode}`, `GET /api/v1/links/{shortCode}`, `DELETE /api/v1/links/{shortCode}`, `/api/v1/ping`)
   - `com.shortlink.service`: Core URL shortening, SSRF defense, vanity alias validation, and HTTP 302 redirect engine
   - `com.shortlink.idgen`: Twitter Snowflake 64-bit ID generator with Base62 encoding and backward clock-drift protection
   - `com.shortlink.domain`: JPA entities, Flyway migrations, and derived repository queries
   - `com.shortlink.config`: Infrastructure, database, cache, and security configurations
1. **Distributed Caching & Protection (Phase 2 Complete)**:
   - `com.shortlink.cache`: Multi-tier Cache-Aside (`L1 Caffeine` < 0.1ms + `L2 Redis` < 1ms) with negative caching, striped stampede protection, and fail-open fallback
   - `com.shortlink.ratelimit`: Atomic Redis + Lua Token Bucket rate limiter (`WRITE` & `READ` tiers) with `X-RateLimit-*` headers and `429 Too Many Requests`
   - Java 25 Virtual Threads + Micrometer Actuator telemetry (`cache.gets`, `shortlink.ratelimit.requests`)
1. **Upcoming Event-Driven & Sharded Components (Phase 3 & Phase 4)**:
   - Async click analytics: Apache Kafka event streaming with GeoIP / User-Agent enrichment
   - Scalable data layer: Consistent-hash sharding across multiple PostgreSQL instances
   - Observability: Prometheus + Grafana dashboards


## Benchmarks: Phase 1 vs. Phase 2

Measured via automated `k6` load tests running in Docker:

  | Metric | Phase 1 Baseline (Direct Postgres, 50 VUs, 10ms pacing) | Phase 2 Extreme Stress (L1+L2 Cache + Rate Limit + Virtual Threads, 200 VUs, ZERO pacing) |
  | :--- | :--- | :--- |
  | **Sustained Throughput** | **2,353 req/s** | **10,920 req/s** _(4.6x throughput under 4x concurrency)_ |
  | **Total Requests Handled** | 58,998 (in 25s) | **396,815** (in 35s) |
  | **L1 Cache Hit Ratio** | N/A (100% DB reads) | **100.00%** _(0 DB hits across 396k redirects)_ |
  | **Min Latency** | 253.08 µs | **204.50 µs** |
  | **p95 Latency** | 2.95 ms | **27.29 ms** _(at full 200-VU saturation with Redis Lua checks)_ |
  | **HTTP Failure Rate** | **0.00%** | **0.00%** (0 out of 396,815 failed) |
  | **Automated Test Suite** | 40 / 40 passed | **44 / 44 passed (100%)** |

## Video Recording Guides (YouTube Series)

Detailed presenter walkthroughs, spoken video scripts, and code explanations mapped to each commit are located in [`docs/video/`](docs/video/):

- **[Phase 1 Series Overview](docs/video/00_Phase1_Playlist_Overview.md)**
- **[Episode 1: Project Skeleton & Docker Postgres](docs/video/Episode_01_Project_Skeleton_and_Docker_Postgres.md)**
- **[Episode 2: Twitter Snowflake 64-bit ID Generator & Base62](docs/video/Episode_02_Snowflake_ID_Generator_and_Base62.md)**
- **[Episode 3: Flyway Schema Migrations, Partial Indexing & JPA](docs/video/Episode_03_Flyway_Database_Migration_and_JPA.md)**
- **[Episode 4: Core REST APIs, Redirect Engine & SSRF Defense](docs/video/Episode_04_REST_APIs_Redirect_Engine_and_Postman.md)**
- **[Episode 5: Production Docker Packaging & k6 Benchmarks](docs/video/Episode_05_Production_Docker_and_k6_Benchmarks.md)**
- **[Episode 6: Multi-Tier Caching (L1 Caffeine + L2 Redis)](docs/video/Episode_06_Redis_Caffeine_Caching.md)**
- **[Episode 7: Distributed Rate Limiting (Redis + Lua)](docs/video/Episode_07_Distributed_Rate_Limiting_Redis_Lua.md)**
- **[Episode 8: Phase 2 Extreme k6 Benchmarks & Virtual Threads](docs/video/Episode_08_Phase2_Extreme_k6_Benchmarks.md)**

## Prerequisites

- Java 25 (or compatible JDK)
- Docker & Docker Compose
- Maven (or included `./mvnw` wrapper)

## Getting Started

### 1. Single-Command Startup (Production Docker Stack)

Run the entire application and PostgreSQL database with a single command:

```bash
docker compose up --build -d
```

Verify container status:

```bash
docker compose ps
```

Verify application health:

```bash
curl -i http://localhost:8080/actuator/health
```

### 2. Run k6 Load Benchmarks

Run the Phase 1 baseline 50-VU redirect load test:

```bash
docker run --rm -i --network shortlink_default -e TARGET_URL=http://app:8080 grafana/k6:latest run - < k6/baseline-redirect-test.js
```

Run the Phase 2 extreme zero-pacing 200-VU stress test:

```bash
docker run --rm -i --network shortlink_default -e TARGET_URL=http://app:8080 grafana/k6:latest run - < k6/extreme-cached-stress-test.js
```

Inspect live Micrometer L1 Cache & Rate-Limit metrics:

```bash
curl -s "http://localhost:8080/actuator/metrics/cache.gets?tag=cache:shortlink.l1.cache&tag=result:hit"
curl -s "http://localhost:8080/actuator/metrics/shortlink.ratelimit.requests?tag=outcome:allowed"
```

### 3. Local Development (Optional)

Start PostgreSQL:

```bash
docker compose up -d postgres
```

Build and run the Spring Boot application locally:

```bash
./mvnw clean compile
./mvnw spring-boot:run
```

### 4. Run Test Suite

```bash
./mvnw test
```
