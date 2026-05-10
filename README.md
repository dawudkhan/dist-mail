# DIST-MAIL — Parallel & Distributed Email Dispatching System

A high-throughput email dispatch pipeline built as a semester project for **Parallel and Distributed Computing**. DIST-MAIL demonstrates core PDC concepts — producer-consumer queues, bounded thread pools, distributed messaging with Apache Kafka, backpressure, fault tolerance, and real-time telemetry — all applied to a realistic bulk-email workload.

---

## Architecture Overview

```
                    ┌─────────────────────────────────────────────────┐
                    │                   Backend (Java 21)             │
                    │                                                 │
REST API  ───────►  │  DispatchOrchestrator  ──►  Kafka Topic        │
                    │         (Producer)          dist-mail.tasks     │
                    │                                   │             │
                    │                                   ▼             │
                    │                        KafkaConsumerService     │
                    │                        (concurrency = 8)        │
                    │                                   │             │
                    │                    ┌──────────────▼──────────┐  │
                    │                    │  LinkedBlockingQueue    │  │
                    │                    │  (bounded, backpressure)│  │
                    │                    └──────────────┬──────────┘  │
                    │                                   │             │
                    │                    ┌──────────────▼──────────┐  │
                    │                    │   ThreadPoolExecutor    │  │
                    │                    │   (16 worker threads)   │  │
                    │                    └──────────────┬──────────┘  │
                    │                                   │             │
                    │                       SimulatedMailSender       │
                    │                    (SMTP + failure simulation)  │
                    │                                   │             │
                    │                       PriorityBlockingQueue     │
                    │                       (retry queue, backoff)    │
                    │                                   │             │
                    │                          SQL Server 2022        │
                    │                          (task persistence)     │
                    └─────────────────────────────────────────────────┘
                                                        ▲
                    ┌─────────────────────────┐         │
                    │  Frontend (Next.js 15)  │  ───────┘
                    │  Real-time dashboard    │  (polling /dashboard)
                    └─────────────────────────┘
```

**Key patterns demonstrated:**
| Concept | Implementation |
|---|---|
| Producer-Consumer | Kafka + `LinkedBlockingQueue` |
| Bounded thread pool | `ThreadPoolExecutor` with configurable size |
| Backpressure | Blocking queue capacity limit |
| Distributed messaging | Apache Kafka (partitioned topic) |
| Fault tolerance | `PriorityBlockingQueue` retry with exponential-like backoff |
| Lock-free rate limiting | `AtomicLong`-based sliding window |
| Real-time observability | Non-blocking `AtomicInteger`/`AtomicLong` metrics |
| Graceful shutdown | Poison-pill pattern |

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend language | Java 21 |
| Backend framework | Spring Boot 3.3.5 |
| Message broker | Apache Kafka 7.6.1 |
| Database | SQL Server 2022 |
| DB migrations | Flyway |
| Frontend framework | Next.js 15 / React 19 |
| Frontend language | TypeScript 5 |
| Styling | Tailwind CSS 4 |
| Infrastructure | Docker Compose |

---

## Prerequisites

- **Docker** (Docker Desktop or Docker Engine with Compose v2)
- **Java 21+** (`java --version`)
- **Node.js 18+** (`node --version`)
- Maven is not required — the project ships a Maven wrapper (`mvnw`)

---

## Setup & Running

### 1. Start infrastructure

Spin up SQL Server, Zookeeper, and Kafka with Docker Compose:

```bash
cd backend
docker compose up -d
```

Wait about 20–30 seconds for Kafka and SQL Server to fully initialize. You can check status with:

```bash
docker compose ps
```

All three services (`sqlserver`, `zookeeper`, `kafka`) should show `Up`.

---

### 2. Run the backend

From the `backend/` directory:

```bash
# Linux / macOS
./mvnw spring-boot:run

# Windows
mvnw.cmd spring-boot:run
```

Flyway will run database migrations automatically on startup. The API will be available at `http://localhost:8080`.

Verify it's running:

```bash
curl http://localhost:8080/actuator/health
# {"status":"UP"}
```

---

### 3. Run the frontend

In a separate terminal:

```bash
cd frontend
npm install
npm run dev
```

Open `http://localhost:3000` in your browser to access the live dashboard.

---

## Usage

### Send a synthetic load batch

The fastest way to see the system in action — generates N emails internally:

```bash
curl -X POST "http://localhost:8080/api/v1/dist-mail/dispatch/synthetic?totalEmails=10000"
```

Watch the dashboard at `http://localhost:3000` for live throughput, queue depth, thread activity, and system pressure.

### Send a custom batch

```bash
curl -X POST http://localhost:8080/api/v1/dist-mail/dispatch \
  -H "Content-Type: application/json" \
  -d '{
    "mails": [
      {
        "recipient": "alice@example.com",
        "subject": "Hello",
        "body": "This is a test email.",
        "priority": 1,
        "maxRetries": 3
      },
      {
        "recipient": "bob@example.com",
        "subject": "Hello again",
        "body": "Another test.",
        "priority": 2,
        "maxRetries": 3
      }
    ]
  }'
```

The response includes a `batchId` you can use to query progress.

### Check batch progress

```bash
curl http://localhost:8080/api/v1/dist-mail/report/{batchId}
```

Returns counts broken down by status: `queued`, `sent`, `retrying`, `failed`, `inProgress`.

### Dashboard API

| Endpoint | Description |
|---|---|
| `GET /api/v1/dist-mail/dashboard` | Full metrics snapshot |
| `GET /api/v1/dist-mail/dashboard/pressure` | Pressure score (LOW / MEDIUM / HIGH) |
| `GET /api/v1/dist-mail/dashboard/trend?limit=120` | Time-series throughput points |
| `GET /api/v1/dist-mail/dashboard/realtime?trendLimit=120` | Combined payload (used by frontend) |

---

## Configuration

All tuning knobs are in `backend/src/main/resources/application.yml` under the `distmail:` namespace:

```yaml
distmail:
  thread-count: 16          # Worker thread pool size
  queue-capacity: 10000     # Bounded queue capacity (backpressure limit)
  consumer-concurrency: 8   # Parallel Kafka consumer threads
  failure-rate: 0.08        # Simulated SMTP failure rate (0.0 – 1.0)
  smtp-delay-min-ms: 5      # Minimum simulated send latency
  smtp-delay-max-ms: 20     # Maximum simulated send latency
  max-retries: 3            # Max retry attempts per task
  retry-backoff: 500ms      # Base backoff delay before retry
  rate-window: 1s           # Rate limit window duration
  rate-window-emails: 10000 # Max emails per window
```

Adjust these values to observe how the system behaves under different concurrency levels, failure rates, or queue pressures without changing any code.

---

## Mail Task Lifecycle

```
PENDING ──► QUEUED ──► SENT
                  └──► RETRYING ──► SENT
                             └──► FAILED (max retries exceeded)
```

Tasks are persisted to SQL Server at each state transition so you can inspect them even after a crash.

---

## Project Structure

```
dist-mail/
├── backend/                          # Spring Boot application
│   ├── compose.yaml                  # Docker Compose (Kafka, Zookeeper, SQL Server)
│   ├── pom.xml                       # Maven dependencies
│   └── src/main/java/com/distmail/
│       ├── api/                      # REST controllers and DTOs
│       ├── config/                   # Spring configuration beans
│       ├── domain/                   # Core domain model and enums
│       ├── kafka/                    # Producer, consumer, topic config
│       ├── service/                  # Business logic (dispatcher, metrics, rate limiter)
│       └── repository/               # Spring Data JPA repository
└── frontend/                         # Next.js dashboard
    ├── app/
    │   └── page.tsx                  # Main real-time dashboard component
    └── components/ui/                # Reusable UI components
```

---

## Stopping the System

```bash
# Stop the frontend: Ctrl+C in its terminal

# Stop the backend: Ctrl+C in its terminal

# Stop and remove Docker containers
cd backend
docker compose down

# To also remove volumes (wipes the database):
docker compose down -v
```

---

## PDC Concepts Illustrated

This project was designed to make the following parallel and distributed computing concepts observable in a running system:

- **Thread pool management** — watch active thread count in the dashboard rise and fall with load
- **Bounded queue backpressure** — queue depth metric shows how the system self-regulates under burst traffic
- **Distributed messaging** — Kafka decouples the HTTP intake rate from the processing rate, enabling horizontal scaling
- **Concurrent data structures** — `LinkedBlockingQueue`, `PriorityBlockingQueue`, and `AtomicLong` used throughout for thread safety without coarse locking
- **Fault tolerance & retry** — set a high `failure-rate` and watch tasks cycle through `RETRYING` before settling as `SENT` or `FAILED`
- **Lock-free rate limiting** — the rate limiter uses compare-and-swap operations rather than synchronized blocks
- **Real-time observability** — all metrics are collected via non-blocking atomic operations to avoid adding contention on the hot path
