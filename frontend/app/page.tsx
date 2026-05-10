"use client";

import { useEffect, useMemo, useState } from "react";
import {
  FaBolt,
  FaChartLine,
  FaCheckCircle,
  FaClock,
  FaEnvelope,
  FaExclamationTriangle,
  FaHeartbeat,
  FaLayerGroup,
  FaPlay,
  FaRocket,
  FaServer,
  FaSyncAlt,
  FaTimesCircle,
} from "react-icons/fa";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";

type DashboardSnapshot = {
  elapsedSeconds: number;
  activeThreads: number;
  configuredThreads: number;
  queueSize: number;
  retryQueueSize: number;
  submitted: number;
  completed: number;
  permanentlyFailed: number;
  throughputPerSecond: number;
  capturedAt: string;
};

type DispatchResponse = {
  batchId: string;
  queued: number;
  snapshot: DashboardSnapshot;
};

type BatchReport = {
  batchId: string;
  submitted: number;
  completed: number;
  permanentlyFailed: number;
  inProgress: number;
};

type HealthResponse = {
  status: string;
};

type RecipientRow = {
  id: number;
  recipient: string;
};

const API_BASE_DEFAULT = "http://localhost:8080/api/v1/dist-mail";
const HEALTH_URL_DEFAULT = "http://localhost:8080/actuator/health";
const DASHBOARD_INTERVAL_MS = 3000;
const HISTORY_MAX = 30;

function toNum(value: string, fallback: number) {
  const parsed = Number.parseInt(value, 10);
  return Number.isFinite(parsed) ? parsed : fallback;
}

export default function Home() {
  const [apiBase, setApiBase] = useState(API_BASE_DEFAULT);
  const [polling, setPolling] = useState(true);
  const healthUrl = useMemo(() => {
    try {
      const parsed = new URL(apiBase);
      return `${parsed.origin}/actuator/health`;
    } catch {
      return HEALTH_URL_DEFAULT;
    }
  }, [apiBase]);

  const [health, setHealth] = useState<HealthResponse | null>(null);
  const [healthError, setHealthError] = useState<string | null>(null);

  const [dashboard, setDashboard] = useState<DashboardSnapshot | null>(null);
  const [history, setHistory] = useState<DashboardSnapshot[]>([]);
  const [dashboardError, setDashboardError] = useState<string | null>(null);
  const [lastUpdated, setLastUpdated] = useState<string | null>(null);

  const [recipientRows, setRecipientRows] = useState<RecipientRow[]>([
    { id: 1, recipient: "alice@example.com" },
    { id: 2, recipient: "bob@example.com" },
  ]);
  const [subject, setSubject] = useState("Hello from DIST-MAIL");
  const [body, setBody] = useState("Welcome to the distributed mail dispatcher.");
  const [priority, setPriority] = useState("10");
  const [maxRetries, setMaxRetries] = useState("3");
  const [dispatchResult, setDispatchResult] = useState<DispatchResponse | null>(null);
  const [dispatching, setDispatching] = useState(false);
  const [dispatchError, setDispatchError] = useState<string | null>(null);

  const [syntheticTotal, setSyntheticTotal] = useState("10000");
  const [syntheticRunning, setSyntheticRunning] = useState(false);
  const [syntheticActivePreset, setSyntheticActivePreset] = useState<number | "custom" | null>(null);
  const [syntheticMessage, setSyntheticMessage] = useState<string | null>(null);

  const [reportBatchId, setReportBatchId] = useState("");
  const [reportLoading, setReportLoading] = useState(false);
  const [reportError, setReportError] = useState<string | null>(null);
  const [report, setReport] = useState<BatchReport | null>(null);

  const completionRate = useMemo(() => {
    if (!dashboard || dashboard.submitted === 0) {
      return 0;
    }
    return Math.min(100, (dashboard.completed / dashboard.submitted) * 100);
  }, [dashboard]);

  useEffect(() => {
    let cancelled = false;

    async function fetchHealthAndDashboard() {
      try {
        const [healthRes, dashboardRes] = await Promise.all([
          fetch(healthUrl, { cache: "no-store" }),
          fetch(`${apiBase}/dashboard`, { cache: "no-store" }),
        ]);

        if (!healthRes.ok) {
          throw new Error(`Health request failed with ${healthRes.status}`);
        }

        const healthData = (await healthRes.json()) as HealthResponse;

        if (!dashboardRes.ok) {
          throw new Error(`Dashboard request failed with ${dashboardRes.status}`);
        }

        const dashboardData = (await dashboardRes.json()) as DashboardSnapshot;

        if (cancelled) {
          return;
        }

        setHealth(healthData);
        setHealthError(null);
        setDashboard(dashboardData);
        setDashboardError(null);
        setLastUpdated(new Date().toLocaleTimeString());
        setHistory((prev) => [...prev.slice(-(HISTORY_MAX - 1)), dashboardData]);
      } catch (error) {
        if (cancelled) {
          return;
        }
        const message = error instanceof Error ? error.message : "Unknown fetch error";
        setHealthError(message);
        setDashboardError(message);
      }
    }

    void fetchHealthAndDashboard();

    if (!polling) {
      return () => {
        cancelled = true;
      };
    }

    const intervalId = window.setInterval(() => {
      if (!document.hidden) {
        void fetchHealthAndDashboard();
      }
    }, DASHBOARD_INTERVAL_MS);

    return () => {
      cancelled = true;
      window.clearInterval(intervalId);
    };
  }, [apiBase, healthUrl, polling]);

  function addRecipientRow() {
    setRecipientRows((prev) => [...prev, { id: Date.now(), recipient: "" }]);
  }

  function updateRecipientRow(id: number, value: string) {
    setRecipientRows((prev) => prev.map((row) => (row.id === id ? { ...row, recipient: value } : row)));
  }

  function removeRecipientRow(id: number) {
    setRecipientRows((prev) => (prev.length > 1 ? prev.filter((row) => row.id !== id) : prev));
  }

  async function handleDispatch() {
    setDispatching(true);
    setDispatchError(null);

    const recipients = recipientRows
      .map((row) => row.recipient.trim())
      .filter((value) => value.length > 0);

    if (recipients.length === 0) {
      setDispatchError("Add at least one recipient before dispatching.");
      setDispatching(false);
      return;
    }

    try {
      const payload = {
        mails: recipients.map((recipient) => ({
          recipient,
          subject,
          body,
          priority: toNum(priority, 10),
          maxRetries: toNum(maxRetries, 3),
        })),
      };

      const res = await fetch(`${apiBase}/dispatch`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });

      if (!res.ok) {
        throw new Error(`Dispatch request failed with ${res.status}`);
      }

      const data = (await res.json()) as DispatchResponse;
      setDispatchResult(data);
      setReportBatchId(data.batchId);
      setSyntheticMessage(`Custom batch queued with ID ${data.batchId}`);
    } catch (error) {
      setDispatchError(error instanceof Error ? error.message : "Unknown dispatch error");
    } finally {
      setDispatching(false);
    }
  }

  async function handleSyntheticDispatch(totalEmails: number, source: number | "custom") {
    setSyntheticRunning(true);
    setSyntheticActivePreset(source);
    setSyntheticMessage(null);
    try {
      const res = await fetch(`${apiBase}/dispatch/synthetic?totalEmails=${totalEmails}`, {
        method: "POST",
      });
      if (!res.ok) {
        throw new Error(`Synthetic dispatch failed with ${res.status}`);
      }

      const text = await res.text();
      setSyntheticMessage(text || `Synthetic dispatch started for ${totalEmails} emails.`);
    } catch (error) {
      setSyntheticMessage(error instanceof Error ? error.message : "Unknown synthetic error");
    } finally {
      setSyntheticRunning(false);
      setSyntheticActivePreset(null);
    }
  }

  async function handleFetchReport() {
    if (!reportBatchId.trim()) {
      setReportError("Enter a batch ID to fetch report.");
      return;
    }
    setReportLoading(true);
    setReportError(null);

    try {
      const res = await fetch(`${apiBase}/report/${reportBatchId.trim()}`);
      if (!res.ok) {
        throw new Error(`Report request failed with ${res.status}`);
      }
      const data = (await res.json()) as BatchReport;
      setReport(data);
      setReportBatchId(data.batchId);
    } catch (error) {
      setReportError(error instanceof Error ? error.message : "Unknown report error");
    } finally {
      setReportLoading(false);
    }
  }

  const lastThroughput = history.at(-1)?.throughputPerSecond ?? 0;
  const maxThroughput = Math.max(...history.map((h) => h.throughputPerSecond), 1);
  const isHealthUp = health?.status?.toUpperCase() === "UP";

  return (
    <div className="min-h-screen bg-[radial-gradient(circle_at_10%_20%,#cffafe_0%,#f8fafc_40%,#e2e8f0_100%)] px-3 py-5 text-slate-900 sm:px-4 md:px-8 md:py-6">
      <div className="mx-auto max-w-7xl space-y-5 md:space-y-6">
        <Card className="overflow-hidden border-none bg-gradient-to-r from-slate-900 via-cyan-900 to-sky-900 text-white shadow-xl">
          <CardContent className="p-4 sm:p-6 md:p-8">
            <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
              <div className="space-y-2">
                <Badge className="border-cyan-200/30 bg-cyan-400/20 text-cyan-100">
                  <FaRocket className="mr-1.5" /> DIST-MAIL Control Tower
                </Badge>
                <h1 className="text-xl font-bold tracking-tight sm:text-2xl md:text-4xl">Parallel Email Dispatch Dashboard</h1>
                <p className="max-w-2xl text-sm text-cyan-100/90 md:text-base">
                  Real-time operational dashboard for dispatch, synthetic load, health, and batch reports.
                </p>
              </div>
              <div className="grid w-full grid-cols-1 gap-2 text-sm sm:grid-cols-2 lg:w-[430px]">
                <InfoPill icon={<FaServer />} label="Endpoint" value={apiBase} />
                <InfoPill icon={<FaSyncAlt />} label="Polling" value={polling ? "ON" : "OFF"} />
                <InfoPill icon={<FaClock />} label="Last update" value={lastUpdated ?? "-"} />
                <InfoPill icon={<FaChartLine />} label="Throughput" value={`${lastThroughput.toFixed(2)} eps`} />
              </div>
            </div>
          </CardContent>
        </Card>

        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-5">
          <StatCard title="Submitted" value={dashboard?.submitted ?? 0} icon={<FaEnvelope />} tone="sky" />
          <StatCard title="Completed" value={dashboard?.completed ?? 0} icon={<FaBolt />} tone="emerald" />
          <StatCard title="Failed" value={dashboard?.permanentlyFailed ?? 0} icon={<FaExclamationTriangle />} tone="rose" />
          <StatCard title="Active Threads" value={dashboard?.activeThreads ?? 0} icon={<FaLayerGroup />} tone="violet" />
          <StatCard title="Health" value={isHealthUp ? "UP" : "DOWN"} icon={isHealthUp ? <FaCheckCircle /> : <FaTimesCircle />} tone={isHealthUp ? "emerald" : "rose"} />
        </div>

        <div className="grid gap-6 xl:grid-cols-3">
          <Card className="xl:col-span-2">
            <CardHeader>
              <CardTitle>Live Throughput and Queue Pressure</CardTitle>
              <CardDescription>
                Polling `/dashboard` every 3 seconds. Polling pauses automatically when tab is hidden.
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="grid gap-3 sm:grid-cols-3">
                <MiniMetric label="Queue size" value={dashboard?.queueSize ?? 0} />
                <MiniMetric label="Retry queue" value={dashboard?.retryQueueSize ?? 0} />
                <MiniMetric label="Configured threads" value={dashboard?.configuredThreads ?? 0} />
              </div>
              <div className="rounded-xl border border-slate-200 bg-slate-50 p-4">
                <div className="mb-2 flex items-center justify-between text-xs text-slate-500">
                  <span>Throughput trend (eps)</span>
                  <span>max {maxThroughput.toFixed(2)}</span>
                </div>
                <div className="flex h-36 items-end gap-1 overflow-hidden rounded">
                  {history.length === 0 ? (
                    <div className="flex h-full w-full items-center justify-center text-xs text-slate-500">Waiting for live data...</div>
                  ) : (
                    history.map((point, i) => {
                      const pct = Math.max(3, (point.throughputPerSecond / maxThroughput) * 100);
                      return (
                        <div
                          key={`${point.capturedAt}-${i}`}
                          className="w-full rounded-t bg-gradient-to-t from-cyan-500 to-sky-300"
                          style={{ height: `${pct}%` }}
                          title={`${new Date(point.capturedAt).toLocaleTimeString()} - ${point.throughputPerSecond.toFixed(2)} eps`}
                        />
                      );
                    })
                  )}
                </div>
              </div>
              {dashboardError ? <ErrorText message={dashboardError} /> : null}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Control Center</CardTitle>
              <CardDescription>Endpoint, health probe, and polling controls.</CardDescription>
            </CardHeader>
            <CardContent className="space-y-3">
              <label className="space-y-1 text-sm">
                <span className="font-medium text-slate-700">API base URL</span>
                <Input value={apiBase} onChange={(e) => setApiBase(e.target.value)} />
              </label>
              <p className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-xs text-slate-600">Health URL: {healthUrl}</p>
              <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
                <Button variant="outline" className="w-full" onClick={() => setApiBase(API_BASE_DEFAULT)}>
                  Reset URL
                </Button>
                <Button
                  variant={polling ? "destructive" : "secondary"}
                  className="w-full"
                  onClick={() => setPolling((prev) => !prev)}
                >
                  {polling ? "Pause Polling" : "Resume Polling"}
                </Button>
              </div>
              <div className="h-2 overflow-hidden rounded-full bg-slate-200">
                <div className="h-full bg-gradient-to-r from-cyan-500 to-emerald-400" style={{ width: `${completionRate}%` }} />
              </div>
              <p className="text-xs text-slate-500">Completion progress: {completionRate.toFixed(1)}%</p>
              {healthError ? <ErrorText message={healthError} /> : null}
            </CardContent>
          </Card>
        </div>

        <div className="grid gap-6 xl:grid-cols-2">
          <Card>
            <CardHeader>
              <CardTitle>Batch Composer</CardTitle>
              <CardDescription>`POST /dispatch` with multiple recipients and shared mail template.</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <div className="flex items-center justify-between">
                  <p className="text-sm font-medium text-slate-700">Recipients</p>
                  <Button variant="outline" className="h-8 px-3 text-xs" onClick={addRecipientRow}>
                    Add Recipient
                  </Button>
                </div>
                <div className="space-y-2">
                  {recipientRows.map((row, index) => (
                    <div key={row.id} className="grid grid-cols-1 gap-2 sm:grid-cols-[1fr_auto]">
                      <Input
                        placeholder={`recipient-${index + 1}@example.com`}
                        value={row.recipient}
                        onChange={(e) => updateRecipientRow(row.id, e.target.value)}
                      />
                      <Button
                        variant="destructive"
                        className="h-10 w-full sm:w-10 sm:px-0"
                        onClick={() => removeRecipientRow(row.id)}
                        disabled={recipientRows.length === 1}
                        aria-label="Remove recipient"
                      >
                        x
                      </Button>
                    </div>
                  ))}
                </div>
              </div>

              <label className="space-y-1 text-sm">
                <span className="font-medium text-slate-700">Subject</span>
                <Input value={subject} onChange={(e) => setSubject(e.target.value)} />
              </label>
              <label className="space-y-1 text-sm">
                <span className="font-medium text-slate-700">Body</span>
                <Textarea value={body} onChange={(e) => setBody(e.target.value)} />
              </label>
              <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                <label className="space-y-1 text-sm">
                  <span className="font-medium text-slate-700">Priority</span>
                  <Input type="number" value={priority} onChange={(e) => setPriority(e.target.value)} />
                </label>
                <label className="space-y-1 text-sm">
                  <span className="font-medium text-slate-700">Max retries</span>
                  <Input type="number" value={maxRetries} onChange={(e) => setMaxRetries(e.target.value)} />
                </label>
              </div>
              <Button disabled={dispatching} onClick={handleDispatch} className="w-full gap-2 sm:w-auto">
                <FaPlay /> {dispatching ? "Dispatching..." : "Dispatch Custom Batch"}
              </Button>
              {dispatchError ? <ErrorText message={dispatchError} /> : null}
              {dispatchResult ? (
                <p className="rounded-xl border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-700">
                  Batch <strong>{dispatchResult.batchId}</strong> queued: {dispatchResult.queued}
                </p>
              ) : null}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Synthetic Load Panel</CardTitle>
              <CardDescription>`POST /dispatch/synthetic` with quick presets and custom value.</CardDescription>
            </CardHeader>
            <CardContent className="space-y-3">
              <label className="space-y-1 text-sm">
                <span className="font-medium text-slate-700">Total emails</span>
                <Input type="number" value={syntheticTotal} onChange={(e) => setSyntheticTotal(e.target.value)} />
              </label>
              <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
                <Button
                  variant="secondary"
                  className="h-11 w-full px-5"
                  disabled={syntheticRunning}
                  onClick={() => void handleSyntheticDispatch(1000, 1000)}
                >
                  {syntheticActivePreset === 1000 ? "Running..." : "1k"}
                </Button>
                <Button
                  variant="secondary"
                  className="h-11 w-full px-5"
                  disabled={syntheticRunning}
                  onClick={() => void handleSyntheticDispatch(5000, 5000)}
                >
                  {syntheticActivePreset === 5000 ? "Running..." : "5k"}
                </Button>
                <Button
                  variant="secondary"
                  className="h-11 w-full px-5"
                  disabled={syntheticRunning}
                  onClick={() => void handleSyntheticDispatch(10000, 10000)}
                >
                  {syntheticActivePreset === 10000 ? "Running..." : "10k"}
                </Button>
                <Button
                  className="h-11 w-full px-5"
                  disabled={syntheticRunning}
                  onClick={() => void handleSyntheticDispatch(toNum(syntheticTotal, 10000), "custom")}
                >
                  {syntheticActivePreset === "custom" ? "Running..." : "Run"}
                </Button>
              </div>
              <Button
                variant="outline"
                className="h-11 w-full gap-2 px-5"
                disabled={syntheticRunning}
                onClick={() => void handleSyntheticDispatch(toNum(syntheticTotal, 10000), "custom")}
              >
                <FaHeartbeat /> {syntheticRunning ? "Running..." : "Run Custom Synthetic"}
              </Button>
              {syntheticMessage ? <p className="text-sm text-slate-700">{syntheticMessage}</p> : null}
            </CardContent>
          </Card>
        </div>

        <Card>
          <CardHeader>
            <CardTitle>Batch Report Lookup</CardTitle>
            <CardDescription>`GET /report/{"{batchId}"}` to inspect per-batch totals.</CardDescription>
          </CardHeader>
          <CardContent className="space-y-3">
            <div className="grid grid-cols-1 gap-3 md:grid-cols-[1fr_auto]">
              <Input
                placeholder="Paste batchId"
                value={reportBatchId}
                onChange={(e) => {
                  setReportBatchId(e.target.value);
                  setReportError(null);
                }}
              />
              <Button className="w-full md:w-auto" disabled={reportLoading} onClick={handleFetchReport}>
                {reportLoading ? "Loading..." : "Fetch Report"}
              </Button>
            </div>
            {reportError ? <ErrorText message={reportError} /> : null}
            {report ? (
              <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
                <MiniMetric label="Submitted" value={report.submitted} />
                <MiniMetric label="Completed" value={report.completed} />
                <MiniMetric label="Permanently failed" value={report.permanentlyFailed} />
                <MiniMetric label="In progress" value={report.inProgress} />
              </div>
            ) : null}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}

function ErrorText({ message }: { message: string }) {
  return <p className="rounded-lg border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">{message}</p>;
}

function StatCard({
  title,
  value,
  icon,
  tone,
}: {
  title: string;
  value: number | string;
  icon: React.ReactNode;
  tone: "sky" | "emerald" | "rose" | "violet";
}) {
  const tones = {
    sky: "from-sky-500/15 to-cyan-500/15 text-sky-700",
    emerald: "from-emerald-500/15 to-teal-500/15 text-emerald-700",
    rose: "from-rose-500/15 to-red-500/15 text-rose-700",
    violet: "from-violet-500/15 to-fuchsia-500/15 text-violet-700",
  } as const;

  const displayValue = typeof value === "number" ? value.toLocaleString() : value;

  return (
    <Card className={`bg-gradient-to-br ${tones[tone]}`}>
      <CardContent className="flex items-center justify-between p-4 sm:p-5">
        <div>
          <p className="text-sm text-slate-600">{title}</p>
          <p className="text-2xl font-bold tracking-tight text-slate-900 sm:text-3xl">{displayValue}</p>
        </div>
        <div className="rounded-xl bg-white/70 p-3 text-xl">{icon}</div>
      </CardContent>
    </Card>
  );
}

function MiniMetric({ label, value }: { label: string; value: number | null | undefined }) {
  return (
    <div className="rounded-xl border border-slate-200 bg-white p-3">
      <p className="text-xs uppercase tracking-wide text-slate-500">{label}</p>
      <p className="mt-1 text-xl font-semibold text-slate-900">{(value ?? 0).toLocaleString()}</p>
    </div>
  );
}

function InfoPill({
  icon,
  label,
  value,
}: {
  icon: React.ReactNode;
  label: string;
  value: string;
}) {
  return (
    <div className="rounded-xl border border-white/20 bg-white/10 p-2 backdrop-blur">
      <p className="flex items-center gap-1 text-[11px] uppercase tracking-wide text-cyan-100/80">
        {icon} {label}
      </p>
      <p className="truncate text-xs text-white">{value}</p>
    </div>
  );
}
