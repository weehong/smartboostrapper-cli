# Upmatches API Stress Testing with k6

Comprehensive stress testing suite for the Upmatches API using [k6](https://k6.io/).

## Overview

This test suite provides:

- **Multiple test scenarios**: smoke, load, stress, spike, soak, and breakpoint tests
- **Full CRUD coverage**: Tests all User API endpoints
- **Authentication support**: Auth0 JWT token integration (password grant)
- **Custom metrics**: Detailed performance tracking
- **Grafana integration**: Visualize results in real-time

## Prerequisites

- [k6](https://k6.io/docs/getting-started/installation/) installed locally OR
- Docker with k6 image (`grafana/k6`)
- Running Upmatches API instance
- Auth0 credentials for authenticated endpoints

## Quick Start

### Using the Bash Script (Recommended)

```bash
cd grafana/k6

# Run smoke test
./run-tests.sh smoke

# Run load test with authentication
AUTH0_ADMIN_USERNAME=admin@example.com \
AUTH0_ADMIN_PASSWORD=your_password \
AUTH0_CLIENT_SECRET=your_secret \
./run-tests.sh load

# Run with local k6 instead of Docker
./run-tests.sh stress --local
```

### Using .env File

```bash
cp .env.example .env
nano .env  # Fill in your credentials
source .env && ./run-tests.sh load
```

### Run with Docker Directly

```bash
docker run --rm -i \
  -e BASE_URL=http://host.docker.internal:8080 \
  -e K6_SCENARIO=smoke \
  -v $(pwd)/grafana/k6:/scripts \
  grafana/k6 run /scripts/stress-test.js
```

## Test Scenarios

| Scenario | Description | Duration | Max VUs |
|----------|-------------|----------|---------|
| `smoke` | Quick validation | 1 min | 1 |
| `load` | Normal expected load | 9 min | 50 |
| `stress` | Push beyond normal | 17 min | 150 |
| `spike` | Sudden traffic spike | 5 min | 200 |
| `soak` | Extended duration test | 30 min | 30 |
| `breakpoint` | Find breaking point | 12 min | 300+ |
| `crud` | User CRUD operations | 9 min | 50 |

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `BASE_URL` | API base URL | `http://host.docker.internal:8080` |
| `API_VERSION` | API version | `v1` |
| `K6_SCENARIO` | Test scenario to run | `smoke` |
| `AUTH0_DOMAIN` | Auth0 domain (without https://) | `dev-buildapp.jp.auth0.com` |
| `AUTH0_AUDIENCE` | Auth0 API audience | `https://dev-api.upmatches.com/api/v1` |
| `AUTH0_CLIENT_ID` | Auth0 client ID | `kvXP5j3OfuuYMt4qxzuUtFjJQCio0iym` |
| `AUTH0_CLIENT_SECRET` | Auth0 client secret | - |
| `AUTH0_ADMIN_USERNAME` | Admin user email | - |
| `AUTH0_ADMIN_PASSWORD` | Admin user password | - |
| `AUTH0_REALM` | Auth0 database connection | `Username-Password-Authentication` |

## Test Files

```
grafana/k6/
├── stress-test.js           # Main stress test with all scenarios
├── test.js                  # Simple validation test
├── run-tests.sh             # Bash script runner
├── .env.example             # Environment variables template
├── scenarios/
│   └── user-crud-test.js    # Focused CRUD operations test
├── lib/
│   ├── auth.js              # Authentication helpers
│   └── data-generator.js    # Test data generation
└── README.md                # This file
```

## Custom Metrics

| Metric | Type | Description |
|--------|------|-------------|
| `errors` | Rate | Overall error rate |
| `successful_requests` | Counter | Total successful requests |
| `failed_requests` | Counter | Total failed requests |
| `user_creation_time` | Trend | User creation latency |
| `user_fetch_time` | Trend | User fetch latency |
| `auth_token_time` | Trend | Auth token acquisition time |

## Thresholds

Default performance thresholds:

- `http_req_duration`: 95th percentile < 500ms, 99th < 1000ms
- `http_req_failed`: < 5% error rate
- `user_creation_time`: 95th percentile < 1000ms
- `user_fetch_time`: 95th percentile < 300ms

## Grafana Integration

### Output to InfluxDB

```bash
k6 run --out influxdb=http://localhost:8086/k6 grafana/k6/stress-test.js
```

### JSON Output

```bash
k6 run --out json=results.json grafana/k6/stress-test.js
```

## Troubleshooting

### Connection Refused

```bash
# Make sure the API is running
curl http://localhost:8080/actuator/health

# For Docker, use host.docker.internal
docker run --rm grafana/k6 run -e BASE_URL=http://host.docker.internal:8080 ...
```

### Auth0 Token Errors

Verify your credentials:
```bash
curl -X POST https://dev-buildapp.jp.auth0.com/oauth/token \
  -H "Content-Type: application/json" \
  -d '{
    "grant_type": "password",
    "username": "your@email.com",
    "password": "your_password",
    "audience": "https://dev-api.upmatches.com/api/v1",
    "client_id": "kvXP5j3OfuuYMt4qxzuUtFjJQCio0iym",
    "client_secret": "your_secret",
    "scope": "openid profile email",
    "realm": "Username-Password-Authentication"
  }'
```

## Best Practices

1. **Start with smoke tests** before running stress tests
2. **Monitor system resources** during tests
3. **Clean up test data** after tests complete
4. **Run tests multiple times** for consistent results
