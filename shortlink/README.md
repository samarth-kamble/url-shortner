# ShortLink

ShortLink is an enterprise-grade, distributed URL shortening and real-time click-analytics platform built with Java 25, Spring Boot 4, PostgreSQL, Redis, Apache Kafka, Prometheus, Grafana, and Docker Compose.

---

## 🚀 Project Status

- **Current Status**: **100% Complete — All 4 Phases & 15 Days Finished**
- **Test Suite**: **80 / 80 Tests Passing (100% Pass Rate)**
- **Architecture**: Multi-tier distributed system with L1/L2 caching, distributed rate limiting, Kafka click event streaming, MaxMind GeoIP enrichment, time-series analytics, consistent-hash database sharding, and full Prometheus & Grafana observability.

## 🏛 Architecture Overview

```text
                                            User Requests / k6 Load
                                                       │
                                                       ▼
                                     ┌───────────────────────────────────┐
                                     │     Nginx / Gateway / Client      │
                                     └─────────────────┬─────────────────┘
                                                       │
                                                       ▼
 ┌─────────────────────────────────────────────────────────────────────────────────────────────────────────────┐
 │                                           ShortLink Engine (:8080)                                          │
 │                                                                                                             │
 │  ┌─────────────────────────────────┐   ┌─────────────────────────────────┐   ┌───────────────────────────┐  │
 │  │      Distributed Rate Limiter   │   │     Two-Tier Caching Engine     │   │    URL Validation & SSRF  │  │
 │  │   (Redis + Lua Token Bucket)    │   │  (L1 Caffeine + L2 Redis Cache) │   │     DNS Rebind Defense    │  │
 │  └─────────────────────────────────┘   └────────────────┬────────────────┘   └───────────────────────────┘  │
 │                                                         │                                                   │
 │               ┌─────────────────────────────────────────┴─────────────────────────────────────────┐         │
 │               ▼                                                                                   ▼         │
 │  ┌─────────────────────────┐                                                         ┌───────────────────┐  │
 │  │   Kafka Click Producer  │                                                         │ Shard Router Ring │  │
 │  │  (Async Click Tracking) │                                                         │ (Consistent Hash) │  │
 │  └────────────┬────────────┘                                                         └─────────┬─────────┘  │
 └───────────────┼────────────────────────────────────────────────────────────────────────────────┼────────────┘
                 │                                                                                │
                 ▼                                                                                ▼
    ┌─────────────────────────┐                                                    ┌──────────────┴───────────┐
    │   Apache Kafka KRaft    │                                                    │                          │
    │         (:9094)         │                                                    ▼                          ▼
    └────────────┬────────────┘                                           ┌─────────────────┐        ┌─────────────────┐
                 │                                                        │   PostgreSQL    │        │   PostgreSQL    │
                 ▼                                                        │ Shard 0 (:5432) │        │ Shard 1 (:5434) │
    ┌─────────────────────────┐                                           └─────────────────┘        └─────────────────┘
    │  Batch Click Consumer   │
    │  + MaxMind GeoIP Lookup │
    │  + Time-Series Storage  │
    └─────────────────────────┘
                 ▲
                 │ Pull Metrics every 5s
    ┌────────────┴────────────┐             PromQL              ┌─────────────────┐
    │   Prometheus (:9090)    │ ──────────────────────────────► │ Grafana (:3000) │ (Pre-provisioned Dashboards)
    └─────────────────────────┘                                 └─────────────────┘
```

### Modular System Architecture Across All 4 Phases

1. **Phase 1: Modular Monolith Foundation (Days 1–5)**
   - **Twitter Snowflake 64-bit ID Generator**: Generates 4,096 unique IDs per millisecond per worker node with clock-drift protection.
   - **Base62 URL Encoding**: Produces short, collision-free 7-character aliases.
   - **Enterprise Security**: Strict SSRF defense rejecting private CIDRs, loopbacks, and link-local addresses.
   - **Data Layer**: Flyway automated migrations, PostgreSQL partial indexes, soft deletes, and JPA repositories.
   - **Production Packaging**: Multi-stage Eclipse Temurin 25 Alpine Dockerfile running as non-root user.

- **Two-Tier Cache-Aside Architecture**:
  - **L1 In-Memory Near Cache**: Caffeine cache serving hot redirects in under 1 millisecond.
  - **L2 Distributed Cache**: Redis cluster caching with negative caching (anti-penetration) and stampede mitigation.
- **Distributed Atomic Rate Limiter**: High-throughput Redis + Lua Token Bucket implementing sliding refills, `X-RateLimit-*` headers, and RFC 7807 `429 Too Many Requests`.
- **Concurrency**: Native Java 25 Virtual Threads with Micrometer Actuator instrumentation.

3. **Phase 3: Event-Driven Click Streaming & Real-Time Analytics (Days 9–12)**
   - **Asynchronous Click Ingestion**: Fire-and-forget Kafka producer decoupling click tracking from redirects (0ms overhead added to user latency).
   - **Batch Analytics Consumer**: Consumes batches of up to 500 click events with idempotent PostgreSQL upserts.
   - **MaxMind GeoLite2 Geolocation**: Automatic IP-to-Country and IP-to-City enrichment with fallback for private/local subnets.
   - **Time-Series Query Engine**: Rolling analytics windows (`1h`, `24h`, `7d`, `30d`, `all`) with country, city, and daily trend breakdowns.

4. **Phase 4: Horizontal Scaling, Observability & Grand Finale (Days 13–15)**
   - **Consistent-Hash Database Sharding**: MurmurHash3 consistent hashing ring across 100 virtual nodes per physical shard, isolating write spikes and scaling storage linearly across independent PostgreSQL instances.
   - **Enterprise Telemetry**: Micrometer Prometheus registry exporting P50/P95/P99 latency histograms, cache hit ratios, and rate-limit counters at `/actuator/prometheus`.
   - **Automated Grafana Provisioning**: Self-healing Grafana deployment with auto-configured Prometheus datasources and a comprehensive 14-panel system dashboard.
   - **Capstone Stress Testing**: High-concurrency k6 benchmark validating 200 Virtual Users across combined write, redirect, and analytics workloads.

## 📊 Performance Benchmarks Across Phases

All benchmarks executed via automated `k6` load tests running inside Docker Compose:

| Metric                     | Phase 1 Baseline (Direct DB, 50 VUs) | Phase 2 Stress (L1/L2 Cache, 200 VUs) | Phase 4 Capstone (Sharding + Kafka + Metrics, 200 VUs)           |
| :------------------------- | :----------------------------------- | :------------------------------------ | :--------------------------------------------------------------- |
| **Sustained Throughput**   | 2,353 req/s                          | 10,920 req/s                          | **1,000 – 1,844 req/s** _(Mixed writes + redirects + analytics)_ |
| **Total Requests Handled** | 58,998 (25s)                         | 396,815 (35s)                         | **37,164 requests / 68,702 checks (35s)**                        |
| **Redirect Latency (Min)** | 253 µs                               | 204 µs                                | **712 µs**                                                       |
| **Redirect Latency (P90)** | 2.10 ms                              | 18.40 ms                              | **49.93 ms**                                                     |
| **Redirect Latency (P95)** | 2.95 ms                              | 27.29 ms                              | **64.89 ms**                                                     |
| **HTTP Failure Rate**      | 0.00%                                | 0.00%                                 | **0.00% (0 errors across all scenarios)**                        |
| **Check Success Rate**     | 100.00%                              | 100.00%                               | **100.00% (68,702 / 68,702 checks succeeded)**                   |
| **Automated Tests**        | 40 / 40 passed                       | 44 / 44 passed                        | **80 / 80 passed (100% suite health)**                           |

---

## 📺 Complete 15-Episode YouTube Series Guide

Full word-for-word scripts, screen recordings, architectural slides, and terminal walkthroughs are documented in [`docs/video/`](docs/video/):

| Episode        | Title & Topic                                                | Markdown Script                                                                                                                 |
| :------------- | :----------------------------------------------------------- | :------------------------------------------------------------------------------------------------------------------------------ |
| **Episode 1**  | Project Skeleton, Architecture & Docker PostgreSQL           | [Episode_01_Project_Skeleton_and_Docker_Postgres.md](docs/video/Episode_01_Project_Skeleton_and_Docker_Postgres.md)             |
| **Episode 2**  | Twitter Snowflake 64-bit Distributed ID Generator & Base62   | [Episode_02_Snowflake_ID_Generator_and_Base62.md](docs/video/Episode_02_Snowflake_ID_Generator_and_Base62.md)                   |
| **Episode 3**  | Flyway Schema Migrations, Partial Indexing & JPA Entities    | [Episode_03_Flyway_Database_Migration_and_JPA.md](docs/video/Episode_03_Flyway_Database_Migration_and_JPA.md)                   |
| **Episode 4**  | Core REST APIs, HTTP 302 Redirect Engine & SSRF Defense      | [Episode_04_REST_APIs_Redirect_Engine_and_Postman.md](docs/video/Episode_04_REST_APIs_Redirect_Engine_and_Postman.md)           |
| **Episode 5**  | Production Multi-Stage Dockerfile & Baseline k6 Benchmarks   | [Episode_05_Production_Docker_and_k6_Benchmarks.md](docs/video/Episode_05_Production_Docker_and_k6_Benchmarks.md)               |
| **Episode 6**  | Two-Tier Caching: Caffeine L1 Near-Cache + Redis L2 Cluster  | [Episode_06_Redis_Caffeine_Caching.md](docs/video/Episode_06_Redis_Caffeine_Caching.md)                                         |
| **Episode 7**  | Distributed Rate Limiting with Atomic Redis Lua Token Bucket | [Episode_07_Distributed_Rate_Limiting_Redis_Lua.md](docs/video/Episode_07_Distributed_Rate_Limiting_Redis_Lua.md)               |
| **Episode 8**  | Phase 2 Extreme Load Benchmarks & Virtual Thread Concurrency | [Episode_08_Phase2_Extreme_k6_Benchmarks.md](docs/video/Episode_08_Phase2_Extreme_k6_Benchmarks.md)                             |
| **Episode 9**  | Asynchronous Click Event Streaming with Apache Kafka KRaft   | [Episode_09_Apache_Kafka_Click_Streaming.md](docs/video/Episode_09_Apache_Kafka_Click_Streaming.md)                             |
| **Episode 10** | Kafka Batch Click Consumer & Aggregated Analytics REST API   | [Episode_10_Kafka_Batch_Consumer_and_Analytics_API.md](docs/video/Episode_10_Kafka_Batch_Consumer_and_Analytics_API.md)         |
| **Episode 11** | MaxMind GeoIP Geolocation Enrichment & Location Analytics    | [Episode_11_GeoIP_Enrichment_and_Location_Analytics.md](docs/video/Episode_11_GeoIP_Enrichment_and_Location_Analytics.md)       |
| **Episode 12** | Time-Series Analytics Windows & Rolling Click Trends         | [Episode_12_Time_Series_Analytics_and_Click_Windows.md](docs/video/Episode_12_Time_Series_Analytics_and_Click_Windows.md)       |
| **Episode 13** | Database Sharding & MurmurHash3 Consistent Hashing           | [Episode_13_Database_Sharding_and_Consistent_Hashing.md](docs/video/Episode_13_Database_Sharding_and_Consistent_Hashing.md)     |
| **Episode 14** | Prometheus Telemetry & Automated Grafana Provisioning        | [Episode_14_Prometheus_and_Grafana_Observability_Stack.md](docs/video/Episode_14_Prometheus_and_Grafana_Observability_Stack.md) |
| **Episode 15** | Capstone Load Benchmarks & Complete System Polish (Finale)   | [Episode_15_Phase4_Capstone_and_Extreme_Benchmarks.md](docs/video/Episode_15_Phase4_Capstone_and_Extreme_Benchmarks.md)         |
| **Episode 16** | Nginx L7 Load Balancer & Zero-Downtime Automatic Failover    | [Episode_16_Nginx_Load_Balancer_and_Zero_Downtime_Failover.md](docs/video/Episode_16_Nginx_Load_Balancer_and_Zero_Downtime_Failover.md) |
| **Episode 17** | Distributed User Authentication, Stateless JWT & Multi-Tenancy | [Episode_17_Distributed_Authentication_JWT_and_MultiTenancy.md](docs/video/Episode_17_Distributed_Authentication_JWT_and_MultiTenancy.md) |

---

## 🛠 Quickstart: Run the Full Stack with Docker Compose

### 1. Launch All Services

Start the entire distributed infrastructure with a single command:

```bash
docker compose up -d --build
```

This starts:

- **`shortlink-nginx`** (`:8080`, `:80`): High-Performance Nginx L7 Load Balancer with `least_conn` & automatic failover
- **`shortlink-app-1`** (`8080` internal): Spring Boot 4 Application Node 1 (`SHORTLINK_NODE_ID: 1`)
- **`shortlink-app-2`** (`8080` internal): Spring Boot 4 Application Node 2 (`SHORTLINK_NODE_ID: 2`)
- **`shortlink-postgres`** (`:5432`): Primary Database / Shard 0
- **`shortlink-postgres-shard1`** (`:5434`): Database Shard 1
- **`shortlink-redis`** (`:6379`): Redis 7 Cache & Rate Limiter
- **`shortlink-kafka`** (`:9094`): Apache Kafka KRaft Event Broker
- **`shortlink-prometheus`** (`:9090`): Prometheus Telemetry Scraper (scraping both app nodes)
- **`shortlink-grafana`** (`:3000`): Grafana Observability Dashboard (`admin` / `admin`)

Verify all services:

```bash
docker compose ps
```

### 2. Verify Load Balancing & High Availability Failover

Inspect live traffic distribution across both application nodes:

```bash
for i in {1..6}; do curl -i -s http://localhost:8080/api/v1/ping | grep "X-Upstream-Server"; done
```

Simulate an instant node failure and verify zero-downtime failover:

```bash
# Stop Node 1
docker stop shortlink-app-1

# Request still succeeds seamlessly with 200 OK via Node 2!
curl -i http://localhost:8080/api/v1/ping

# Bring Node 1 back online
docker start shortlink-app-1
```

## 📡 API cURL Cheatsheet

### 1. Create a Short Link (Distributed Sharded Write)

```bash
curl -i -X POST http://localhost:8080/api/v1/links \
  -H "Content-Type: application/json" \
  -d '{"longUrl": "https://spring.io/projects/spring-boot"}'
```

_Response (`201 Created`):_

```json
{
  "shortCode": "0000aBc",
  "shortUrl": "http://localhost:8080/0000aBc",
  "longUrl": "https://spring.io/projects/spring-boot",
  "createdAt": "2026-11-13T10:00:00Z"
}
```

### 2. Follow Redirect (Sub-Millisecond L1/L2 Cached HTTP 302)

```bash
curl -i http://localhost:8080/0000aBc
```

_Response (`302 Found` with `Location: https://spring.io/projects/spring-boot`)_

### 3. Query Real-Time Click Analytics & GeoIP Breakdown

```bash
curl -s "http://localhost:8080/api/v1/links/0000aBc/analytics?window=24h" | jq .
```

_Response (`200 OK`):_

```json
{
  "shortCode": "0000aBc",
  "totalClicks": 1284,
  "uniqueVisitors": 950,
  "topCountries": { "United States": 820, "Germany": 230, "India": 150 },
  "topCities": { "New York": 410, "Berlin": 180, "Bengaluru": 95 },
  "clicksOverTime": [
    { "timestamp": "2026-11-13T09:00:00Z", "clicks": 540 },
    { "timestamp": "2026-11-13T10:00:00Z", "clicks": 744 }
  ]
}
```

### 4. Inspect Prometheus Metrics

```bash
curl -s http://localhost:8080/actuator/prometheus | grep "http_server_requests_seconds"
```

### 5. Access Grafana Dashboard

Navigate to `http://localhost:3000` in your browser (Credentials: `admin` / `admin`) to view the pre-provisioned **ShortLink System Overview** dashboard!

## 🧪 Running Automated Tests

Run the full automated test suite (including Unit, Slice, Integration, Sharding, Kafka, and Testcontainers tests):

```bash
./mvnw clean test
```

Run the Capstone Load Benchmark:

```bash
docker run --rm -i --network shortlink_default -e TARGET_URL=http://shortlink-app:8080 grafana/k6 run - < k6/phase4-capstone-benchmark.js
```

---

## 📜 License

Licensed under the Apache License, Version 2.0.
