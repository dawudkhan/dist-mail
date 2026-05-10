package com.distmail.service;

import com.distmail.config.DistMailProperties;
import com.distmail.domain.DashboardSnapshot;
import com.distmail.domain.MailStatus;
import com.distmail.domain.MailTask;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DispatcherService {

    private static final Logger log = LoggerFactory.getLogger(DispatcherService.class);

    private final DistMailProperties properties;
    private final SimulatedMailSender mailSender;
    private final RateLimiter rateLimiter;
    private final MetricsService metricsService;
    private final MailTaskPersistenceService persistenceService;
    private final TelemetryService telemetryService;

    private final Map<UUID, CompletableFuture<MailStatus>> futures = new ConcurrentHashMap<>();
    private BlockingQueue<MailTask> mainQueue;
    private PriorityBlockingQueue<MailTask> retryQueue;
    private ThreadPoolExecutor workerPool;
    private ExecutorService retryDrainer;
    private ScheduledExecutorService scheduler;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public DispatcherService(DistMailProperties properties, SimulatedMailSender mailSender, RateLimiter rateLimiter,
                             MetricsService metricsService, MailTaskPersistenceService persistenceService,
                             TelemetryService telemetryService) {
        this.properties = properties;
        this.mailSender = mailSender;
        this.rateLimiter = rateLimiter;
        this.metricsService = metricsService;
        this.persistenceService = persistenceService;
        this.telemetryService = telemetryService;
    }

    @PostConstruct
    void init() {
        mainQueue = new LinkedBlockingQueue<>(properties.queueCapacity());
        retryQueue = new PriorityBlockingQueue<>();
        workerPool = buildExecutor();
        retryDrainer = Executors.newSingleThreadExecutor(r -> { Thread t = new Thread(r, "retry-drainer"); t.setDaemon(true); return t; });
        scheduler = Executors.newScheduledThreadPool(2);
        running.set(true);

        startWorkers();
        startRetryDrainer();
        scheduler.scheduleAtFixedRate(rateLimiter::resetWindow, 1, Math.max(1, properties.rateWindow().toSeconds()), TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(this::logDashboard, 3, properties.dashboardIntervalSeconds(), TimeUnit.SECONDS);
    }

    public CompletableFuture<MailStatus> submitTask(MailTask task) {
        metricsService.startIfNeeded();
        task.markQueued();
        persistenceService.saveNewTask(task);
        metricsService.incrementSubmitted();

        CompletableFuture<MailStatus> future = new CompletableFuture<>();
        futures.put(task.getId(), future);
        try {
            mainQueue.put(task);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            future.complete(MailStatus.FAILED);
        }
        return future;
    }

    public List<CompletableFuture<MailStatus>> submitBatch(List<MailTask> tasks) { return tasks.stream().map(this::submitTask).toList(); }

    public DashboardSnapshot currentSnapshot() {
        return metricsService.snapshot(workerPool.getActiveCount(), properties.threadCount(), mainQueue.size(), retryQueue.size());
    }

    private ThreadPoolExecutor buildExecutor() {
        if (properties.poolMode().name().equals("CACHED")) {
            return new ThreadPoolExecutor(0, properties.threadCount() * 16, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(properties.queueCapacity()), new CallerRunsPolicy());
        }
        return new ThreadPoolExecutor(properties.threadCount(), properties.threadCount(), 30L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(properties.queueCapacity()), new CallerRunsPolicy());
    }

    private void startWorkers() { for (int i = 0; i < properties.threadCount(); i++) workerPool.execute(this::runWorkerLoop); }

    private void runWorkerLoop() {
        while (running.get()) {
            try {
                MailTask task = mainQueue.take();
                if (task.isPoisonPill()) { mainQueue.offer(MailTask.POISON_PILL); return; }
                if (!rateLimiter.tryAcquire(properties.rateWindowEmails())) { Thread.sleep(20); mainQueue.offer(task); continue; }
                processTask(task);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private void processTask(MailTask task) {
        CompletableFuture<MailStatus> future = futures.get(task.getId());
        try {
            mailSender.send(task);
            task.markSent();
            persistenceService.markSent(task);
            metricsService.incrementCompleted();
            if (future != null && !future.isDone()) future.complete(MailStatus.SENT);
        } catch (Exception ex) {
            if (task.canRetry()) {
                int retries = task.incrementRetryCount();
                Instant nextRetry = Instant.now().plus(properties.retryBackoff().multipliedBy(retries));
                task.markRetrying(nextRetry);
                persistenceService.markRetrying(task, ex.getMessage(), nextRetry);
                retryQueue.offer(task);
            } else {
                task.markFailed();
                persistenceService.markFailed(task, ex.getMessage());
                metricsService.incrementCompleted();
                metricsService.incrementFailed();
                if (future != null && !future.isDone()) future.complete(MailStatus.FAILED);
            }
        }
    }

    private void startRetryDrainer() {
        retryDrainer.submit(() -> {
            while (running.get()) {
                try {
                    MailTask next = retryQueue.poll(200, TimeUnit.MILLISECONDS);
                    if (next == null) continue;
                    if (next.getNextRetryAt() != null && next.getNextRetryAt().isAfter(Instant.now())) { retryQueue.offer(next); Thread.sleep(25); continue; }
                    mainQueue.put(next);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });
    }

    private void logDashboard() {
        DashboardSnapshot s = currentSnapshot();
        telemetryService.record(s);
        log.info("[DASHBOARD] elapsed={}s active={}/{} queue={} retry={} submitted={} completed={} failed={} throughput={} emails/sec",
            s.elapsedSeconds(), s.activeThreads(), s.configuredThreads(), s.queueSize(), s.retryQueueSize(), s.submitted(), s.completed(), s.permanentlyFailed(), String.format("%.2f", s.throughputPerSecond()));
    }

    @PreDestroy
    void shutdown() {
        running.set(false);
        for (int i = 0; i < Math.max(properties.poisonPillCount(), 1); i++) mainQueue.offer(MailTask.POISON_PILL);
        workerPool.shutdown();
        retryDrainer.shutdown();
        scheduler.shutdown();
        try { workerPool.awaitTermination(60, TimeUnit.SECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
