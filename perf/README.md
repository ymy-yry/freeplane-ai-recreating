# Performance Baseline

This directory contains lightweight backend performance scripts for the
Freeplane REST API.

## k6 baseline

Script:

```text
perf/k6-rest-baseline.js
```

What it measures:

- `GET /api/map/current`
- `POST /api/nodes/search`
- `POST /api/nodes/create`

Setup behavior:

- Creates a temporary map
- Reads the current root node
- Creates one seed node for search

Default thresholds:

- `map/current` p95 `< 500ms`
- `nodes/search` p95 `< 800ms`
- `nodes/create` p95 `< 1000ms`
- failure rate `< 1%`

Run example:

```powershell
k6 run .\perf\k6-rest-baseline.js
```

If your server is not on the default port:

```powershell
$env:BASE_URL='http://127.0.0.1:6299'
k6 run .\perf\k6-rest-baseline.js
```

Prerequisites:

- Start Freeplane with the AI plugin and REST server enabled
- Ensure port `6299` is reachable
- Open at least one map or allow the script to create one
