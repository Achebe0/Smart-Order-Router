const axios = require('axios');
const fs = require('fs');
const argv = require('minimist')(process.argv.slice(2));

function nowMs() { return Date.now(); }

function percentile(arr, p) {
  if (arr.length === 0) return 0;
  const sorted = arr.slice().sort((a,b) => a-b);
  const idx = Math.ceil((p/100) * sorted.length) - 1;
  return sorted[Math.max(0, Math.min(idx, sorted.length-1))];
}

function stddev(arr, mean) {
  if (arr.length === 0) return 0;
  const variance = arr.reduce((s, v) => s + Math.pow(v - mean, 2), 0) / arr.length;
  return Math.sqrt(variance);
}

async function worker(id, url, requests, data, headers) {
  const results = [];
  for (let i = 0; i < requests; i++) {
    const start = nowMs();
    try {
      const res = await axios.post(url, data, { headers, timeout: 60000 });
      const duration = nowMs() - start;
      results.push({ ok: true, status: res.status, duration });
    } catch (err) {
      const duration = nowMs() - start;
      let status = err.response ? err.response.status : 0;
      results.push({ ok: false, status, duration, error: err.message });
    }
  }
  return results;
}

async function main() {
  const url = argv.url || 'http://localhost:8080/api/prompt/optimize';
  const concurrency = parseInt(argv.concurrency || '10', 10);
  const total = parseInt(argv.requests || '200', 10);
  const promptType = argv.prompt || 'short';
  const out = argv.out || null;

  const short = 'Summarize the book Dune in one sentence.';
  const medium = 'Explain the main design differences between REST and GraphQL and give a short example for each. Keep it concise.';
  const long = 'Write a detailed analysis (approximately 300 words) comparing the design trade-offs of REST versus GraphQL for large-scale microservice architectures, including caching, versioning, and developer productivity considerations.';

  const dataMap = { short, medium, long };
  const data = dataMap[promptType] || medium;
  const headers = { 'Content-Type': 'text/plain' };

  const perWorker = Math.floor(total / concurrency);
  const remainder = total % concurrency;

  console.log(`URL: ${url}`);
  console.log(`Concurrency: ${concurrency}, Total requests: ${total}, per worker: ${perWorker} (+${remainder} for first workers), prompt: ${promptType}`);

  const startAll = nowMs();

  const workers = [];
  for (let i = 0; i < concurrency; i++) {
    const count = perWorker + (i < remainder ? 1 : 0);
    workers.push(worker(i, url, count, data, headers));
  }

  const allResults = (await Promise.all(workers)).flat();

  const endAll = nowMs();
  const wallTimeSec = (endAll - startAll) / 1000;

  const durations = allResults.map(r => r.duration);
  const successes = allResults.filter(r => r.ok).length;
  const failures = allResults.length - successes;

  const mean = durations.reduce((s,v) => s+v, 0) / durations.length || 0;
  const sd = stddev(durations, mean);
  const p50 = percentile(durations, 50);
  const p95 = percentile(durations, 95);
  const p99 = percentile(durations, 99);
  const throughput = allResults.length / wallTimeSec;

  const summary = {
    url, concurrency, totalRequests: allResults.length, successes, failures,
    wallTimeSec, throughput, meanMs: mean, stdMs: sd, p50Ms: p50, p95Ms: p95, p99Ms: p99,
    minMs: Math.min(...durations), maxMs: Math.max(...durations), timestamp: new Date().toISOString()
  };

  console.log('--- SUMMARY ---');
  console.log(JSON.stringify(summary, null, 2));

  if (out) {
    fs.writeFileSync(out, JSON.stringify({ summary, results: allResults }, null, 2));
    console.log('Wrote results to', out);
  }
}

main().catch(err => { console.error(err); process.exit(1); });

