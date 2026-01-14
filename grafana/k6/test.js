import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  // Ramp up from 1 to 20 users over 30s, stay there for 1m, then ramp down
  stages: [
    { duration: '30s', target: 20 },
    { duration: '1m', target: 20 },
    { duration: '10s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'], // 95% of requests must complete below 500ms
  },
};

export default function () {
  // 1. Base URL for your Spring Boot app running on the host
  const BASE_URL = 'http://host.docker.internal:8080';

  // 2. Define the endpoint to test. 
  // CHANGE THIS to your actual API endpoint (e.g., '/api/v1/matches')
  const ENDPOINT = '/actuator/health'; 
  // const ENDPOINT = '/api/matches'; 

  const res = http.get(`${BASE_URL}${ENDPOINT}`);

  // 3. Verify the response
  check(res, {
    'status is 200': (r) => r.status === 200,
  });

  sleep(1);
}