# DIST-MAIL — Parallel & Distributed Email Dispatching System

**6th Semester — Parallel & Distributed Computing Project**  
**Tech:** Java 21, Spring Boot 3.3, Kafka, SQL Server, Flyway, ThreadPoolExecutor, BlockingQueue

---

## Executive Summary

DIST-MAIL is a distributed bulk email dispatch simulation platform built to demonstrate real-world parallel and distributed computing concepts.  
It combines Kafka-based distribution, bounded blocking queues, retry prioritization, and multithreaded workers to process large email batches with observability and fault tolerance.

The system is designed for:
- High-throughput dispatch
- Backpressure-safe execution
- Failure recovery via retries
- Real-time operational monitoring

---

## Problem Statement

Bulk email systems face four common challenges:
1. Producer overload (unbounded task creation)
2. Worker starvation or overload
3. Failure handling without blocking normal traffic
4. Lack of live observability for throughput and queue pressure

DIST-MAIL addresses these with bounded queues, controlled thread pools, retry prioritization, and telemetry APIs.

---

## System Architecture

```text
Frontend / API Client
        |
        v
DispatchController
        |
        v
Kafka Producer  --->  Kafka Topic (dist-mail.tasks)  --->  Kafka Consumers (concurrent)
                                                           |
                                                           v
                                                    DispatcherService
                                         +-------------------+-------------------+
                                         |                                       |
                                  LinkedBlockingQueue                    PriorityBlockingQueue
                                   (main work queue)                       (retry queue)
                                         |                                       |
                                         +--------------- RetryDrainer ----------+
                                                         |
                                                         v
                                                  Worker Thread Pool
                                                         |
                                                         v
                                         SimulatedMailSender + SQL Server Persistence
                                                         |
                                                         v
                                              TelemetryService + Dashboard APIs
```

---

## Core Parallel & Distributed Concepts Used

### 1. Producer-Consumer Pattern
- Producers publish tasks to Kafka.
- Consumers submit tasks to bounded dispatcher queues.
- Workers consume from queue and process in parallel.

### 2. Bounded Blocking Queue Backpressure
- Main queue: `LinkedBlockingQueue<>(capacity)`
- If full, producers naturally slow (`put()` blocks).
- Prevents memory explosion under load.

### 3. Thread Pool Based Parallelism
- Fixed worker pool for controlled throughput.
- Configurable thread count based on machine capacity.
- Predictable CPU scheduling and reduced thread churn.

### 4. Retry Prioritization
- Failed tasks move to `PriorityBlockingQueue`.
- High-priority retries are reprocessed first.
- Avoids starvation of critical retries.

### 5. Rate Limiting
- Token-window style limiter (lock-free counter).
- Protects downstream SMTP-like sender from burst overload.

### 6. Graceful Shutdown
- Poison-pill strategy for clean worker termination.
- Avoids abrupt interruption of in-flight operations.

---

## Data Model

### Mail Task Lifecycle
`PENDING -> QUEUED -> SENT`  
or  
`PENDING -> QUEUED -> RETRYING -> ... -> SENT/FAILED`

### Persistence
Stored in SQL Server table `mail_task` with fields like:
- `id`, `batch_id`
- `recipient`, `subject`, `body`
- `priority`, `retry_count`, `max_retries`
- `status`, `error_message`
- `created_at`, `updated_at`, `next_retry_at`

Flyway manages schema versions (`V1`, `V2`).

---

## Real-Time Telemetry for Dashboard

The backend provides live metrics for frontend charts/cards:
- Throughput (emails/sec, interval-based)
- Queue size
- Retry queue size
- Active vs configured threads
- Submitted/completed/failed counters
- Pressure score (`LOW`, `MEDIUM`, `HIGH`)
- Trend points for time-series plotting

---

## REST API Contract

Base URL: `http://localhost:8080/api/v1/dist-mail`

### Dispatch APIs
- `POST /dispatch` — dispatch custom batch
- `POST /dispatch/synthetic?totalEmails=10000` — synthetic stress batch

### Dashboard APIs
- `GET /dashboard` — raw snapshot
- `GET /dashboard/pressure` — pressure metrics only
- `GET /dashboard/trend?limit=120` — recent trend points
- `GET /dashboard/realtime?trendLimit=120` — combined payload for frontend

### Batch Analytics
- `GET /report/{batchId}` — per-batch breakdown (`queued`, `retrying`, `sent`, `failed`, `inProgress`)

### Health
- `GET /actuator/health`

---

## Configuration Highlights (`application.yml`)

Key tuning knobs:
- `distmail.thread-count`
- `distmail.consumer-concurrency`
- `distmail.queue-capacity`
- `distmail.rate-window-emails`
- `distmail.failure-rate`
- `distmail.retry-backoff`
- `distmail.smtp-delay-min-ms`
- `distmail.smtp-delay-max-ms`
- `distmail.cors.allowed-origins`

Actuator CORS is also configured for frontend health polling.

---

## Build & Run

### Prerequisites
- Docker Desktop running
- Java 21+
- Maven wrapper (`mvnw.cmd`)

### Start infrastructure
```powershell
cd "D:\PDC Project"
docker compose up -d sqlserver zookeeper kafka
```

### Build and run app
```powershell
.\mvnw.cmd clean compile
.\mvnw.cmd spring-boot:run
```

### Quick validation
- `GET http://localhost:8080/actuator/health`
- `POST http://localhost:8080/api/v1/dist-mail/dispatch/synthetic?totalEmails=10000`
- `GET http://localhost:8080/api/v1/dist-mail/dashboard/realtime?trendLimit=60`

---

## Demo Flow (For Presentation)

### Step 1: Show architecture
Explain Kafka distribution + thread pool + blocking queue + retry queue.

### Step 2: Trigger synthetic load
Dispatch `10,000` emails from API.

### Step 3: Show live dashboard
Highlight:
- Throughput curve
- Queue pressure trends
- Retry behavior
- Completion and failure counters

### Step 4: Change failure rate (optional)
Increase `distmail.failure-rate` to demonstrate retry pressure rising.

### Step 5: Explain control mechanisms
Summarize how bounded queues and thread pools protect the system.

---

## Performance & Scalability Notes

DIST-MAIL scalability comes from:
- Horizontal scaling via Kafka consumer groups
- Vertical scaling via thread count/concurrency tuning
- Controlled memory via bounded queues
- Non-blocking counters for low-overhead telemetry

For real SMTP production:
- Replace simulated sender with actual provider integration
- Add idempotency keys and dead-letter topics
- Add OpenTelemetry and centralized log aggregation

---

## Troubleshooting

### CORS errors from frontend
- Ensure backend restarted after CORS changes.
- Confirm `distmail.cors.allowed-origins` includes frontend URL.

### Kafka connection warnings
If `localhost:9092` unreachable:
```powershell
docker compose up -d zookeeper kafka
```

### SQL migration failure
Reset local DB schema when needed and restart app so Flyway reapplies migrations.

### Docker pipe error on Windows
Start Docker Desktop and wait until engine is fully running.

---

## Academic Learning Outcomes Demonstrated

This project demonstrates practical application of:
- Producer-consumer systems
- Thread pool design
- Blocking queue backpressure
- Distributed messaging with Kafka
- Fault handling with retry queues
- Real-time telemetry and operational visibility

---

## Project Tagline

**DIST-MAIL transforms bulk email dispatch into a controlled, observable, and scalable parallel-distributed pipeline.**

