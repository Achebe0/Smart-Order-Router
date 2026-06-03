Run instructions for latency/load testing

Prerequisites
- Node.js (v14+)
- npm
- The FluxGate application running locally (default http://localhost:8080). You can start it with:
  java -jar target\FluxGate-0.0.1-SNAPSHOT.jar
  (If the jar is missing or invalid, build with the maven wrapper: .\mvnw.cmd package)

Install dependencies (from project root):

```powershell
npm install axios minimist
```

Run a simple load test (example):

```powershell
# 200 requests at concurrency 10
node .\scripts\load_test.js --url http://localhost:8080/api/prompt/optimize --concurrency 10 --requests 200 --prompt short --out .\scripts\results_c10_short.json
```

Notes
- The script posts plaintext prompts to the `POST /api/prompt/optimize` endpoint by default.
- Results include: avg (mean), p50, p95, p99, stddev, throughput (req/sec), success/failure counts.
- If you want to measure cold start latency, run a single curl POST before the JVM has started and capture the timing. For warm measurements, run a short warmup (e.g., 20 requests) before the real run.

Troubleshooting
- If building the project with `mvnw` fails inside OneDrive, consider moving the repository to a non-synced folder (e.g., C:\dev\FluxGate) or disabling OneDrive sync temporarily.

