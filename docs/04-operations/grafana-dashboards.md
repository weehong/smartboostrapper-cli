# Grafana Dashboards

This document describes the pre-configured Grafana dashboards for monitoring k6 load tests.

## k6 Performance Dashboard

**File**: `grafana/dashboards/k6-dashboard.json`

### Overview
A comprehensive dashboard for visualizing k6 Open Source load test results stored in Prometheus with Native Histogram support.

### Dashboard Structure

#### 1. Performance Overview Panel
**Type**: Time Series
**Metrics Displayed**:
- `k6_vus` - Virtual Users over time
- `k6_http_req_duration_seconds` - HTTP request duration quantiles
- `k6_http_reqs_total` - Requests per second
- `k6_http_req_failed_rate` - Failed request rate
- `k6_http_reqs_total` with `expected_response="false"` - Error requests per second

**Visual Features**:
- Color-coded metrics (VUs in default, errors in red, RPS in yellow)
- Dual Y-axis support for different metric scales
- Custom line styles for different metric types

#### 2. Key Performance Indicators (KPIs)
Four stat panels showing critical metrics:

| Panel | Metric | Description |
|-------|--------|-------------|
| HTTP Requests | `sum(k6_http_reqs_total)` | Total HTTP requests made |
| HTTP Request Failures | `sum(k6_http_reqs_total{expected_response="false"})` | Total failed requests |
| Peak RPS | `sum(irate(k6_http_reqs_total[$__rate_interval]))` | Peak requests per second |
| HTTP Request Duration | `histogram_quantile($quantile, rate(k6_http_req_duration_seconds[$__rate_interval]))` | Response time quantile |

#### 3. Transfer Rate Panel
**Type**: Time Series
**Metrics**:
- `k6_data_sent_total` - Data sent rate
- `k6_data_received_total` - Data received rate

**Unit**: Bytes per second

#### 4. Iterations Panel
**Type**: Time Series
**Metrics**:
- `k6_iteration_duration_seconds` - Iteration duration quantiles
- `k6_dropped_iterations_total` - Dropped iterations count

#### 5. HTTP Latency Timings Panel
**Type**: Time Series
**Metrics**: Detailed HTTP phase timings:
- `k6_http_req_blocked_seconds` - Time blocked before request
- `k6_http_req_tls_handshaking_seconds` - TLS handshake time
- `k6_http_req_sending_seconds` - Request sending time
- `k6_http_req_waiting_seconds` - Time waiting for response
- `k6_http_req_receiving_seconds` - Response receiving time
- `k6_http_req_duration_seconds` - Total request duration

### Dashboard Variables

The dashboard includes template variables for dynamic filtering:

1. **`testid`**: Filter metrics by k6 test ID
2. **`quantile`**: Select quantile value for histogram calculations (default: 0.95)

### Prometheus Queries

The dashboard uses the following key Prometheus query patterns:

#### Rate Calculations
```promql
sum(irate(k6_http_reqs_total{testid=~"$testid"}[$__rate_interval]))
```

#### Histogram Quantiles
```promql
histogram_quantile($quantile, 
  sum by (le) (
    rate(k6_http_req_duration_seconds{testid=~"$testid"}[$__rate_interval])
  )
)
```

#### Error Rate Calculation
```promql
sum(irate(k6_http_reqs_total{testid=~"$testid", expected_response="false"}[$__rate_interval]))
```

### Visualization Features

1. **Color Coding**:
   - Errors: Red with dashed lines
   - RPS: Yellow with dashed lines
   - HTTP duration metrics: Blue
   - VUs: Default palette

2. **Axis Placement**:
   - Right Y-axis: RPS and error metrics
   - Left Y-axis: VUs and duration metrics

3. **Units**:
   - Requests: `reqps` (requests per second)
   - Duration: `s` (seconds)
   - Data: `bytes`
   - VUs: `VUs` (Virtual Users)

### Usage Instructions

1. **Access the Dashboard**:
   - Navigate to http://localhost:3000
   - Login with credentials: vernon/password
   - Select the "k6 Performance" dashboard

2. **Filtering Data**:
   - Use the `testid` variable dropdown to filter by specific test runs
   - Adjust the `quantile` variable to change percentile calculations

3. **Time Range Selection**:
   - Use the time picker in the top right to adjust the time window
   - Auto-refresh options available for real-time monitoring

### Integration with k6

The dashboard is designed to work with k6 metrics exported via Prometheus remote write. Ensure k6 is configured with:

```javascript
// k6 test script configuration
export const options = {
  ext: {
    loadimpact: {
      projectID: 12345,
      name: 'Your Test Name'
    }
  }
};
```

### Customization

To modify the dashboard:

1. **Add New Panels**:
   - Click the "+" icon in Grafana
   - Select "Add new panel"
   - Configure Prometheus queries using k6 metric names

2. **Modify Existing Panels**:
   - Click panel title → "Edit"
   - Adjust queries in the "Query" tab
   - Modify visualization in the "Panel" tab

3. **Export/Import**:
   - Export: Dashboard settings → Share → Export
   - Import: Create → Import → Upload JSON

### Metric Reference

Key k6 metrics used in this dashboard:

| Metric | Type | Description |
|--------|------|-------------|
| `k6_vus` | Gauge | Current number of virtual users |
| `k6_http_reqs_total` | Counter | Total HTTP requests |
| `k6_http_req_duration_seconds` | Histogram | HTTP request duration |
| `k6_http_req_blocked_seconds` | Histogram | Time blocked before request |
| `k6_data_sent_total` | Counter | Total data sent |
| `k6_data_received_total` | Counter | Total data received |
| `k6_iteration_duration_seconds` | Histogram | Test iteration duration |
| `k6_dropped_iterations_total` | Counter | Dropped iterations |

For complete metric documentation, refer to [k6 Metrics Reference](https://k6.io/docs/using-k6/metrics/reference/).
