import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Counter, Trend } from 'k6/metrics';
import { randomString, randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

// =============================================================================
// CUSTOM METRICS
// =============================================================================
const errorRate = new Rate('errors');
const successfulRequests = new Counter('successful_requests');
const failedRequests = new Counter('failed_requests');
const userCreationTime = new Trend('user_creation_time', true);
const userFetchTime = new Trend('user_fetch_time', true);
const authTokenTime = new Trend('auth_token_time', true);

// =============================================================================
// CONFIGURATION - Set via environment variables or defaults
// =============================================================================
const BASE_URL = __ENV.BASE_URL || 'http://host.docker.internal:8080';
const API_VERSION = __ENV.API_VERSION || 'v1';

// Auth0 Configuration
const AUTH0_DOMAIN = __ENV.AUTH0_DOMAIN || 'dev-buildapp.jp.auth0.com';
const AUTH0_AUDIENCE = __ENV.AUTH0_AUDIENCE || 'https://dev-api.upmatches.com/api/v1';
const AUTH0_CLIENT_ID = __ENV.AUTH0_CLIENT_ID || 'kvXP5j3OfuuYMt4qxzuUtFjJQCio0iym';
const AUTH0_CLIENT_SECRET = __ENV.AUTH0_CLIENT_SECRET || '';
const AUTH0_ADMIN_USERNAME = __ENV.AUTH0_ADMIN_USERNAME || '';
const AUTH0_ADMIN_PASSWORD = __ENV.AUTH0_ADMIN_PASSWORD || '';
const AUTH0_REALM = __ENV.AUTH0_REALM || 'Username-Password-Authentication';

// =============================================================================
// TEST SCENARIOS - Select via K6_SCENARIO environment variable
// =============================================================================
const scenarios = {
    // Smoke test: Quick validation that the system works
    smoke: {
        executor: 'constant-vus',
        vus: 1,
        duration: '1m',
        gracefulStop: '30s',
    },
    // Load test: Normal expected load
    load: {
        executor: 'ramping-vus',
        startVUs: 0,
        stages: [
            { duration: '2m', target: 50 },   // Ramp up to 50 users
            { duration: '5m', target: 50 },   // Stay at 50 users
            { duration: '2m', target: 0 },    // Ramp down to 0
        ],
        gracefulRampDown: '30s',
    },
    // Stress test: Push system beyond normal load
    stress: {
        executor: 'ramping-vus',
        startVUs: 0,
        stages: [
            { duration: '2m', target: 50 },   // Ramp up to normal load
            { duration: '3m', target: 50 },   // Stay at normal load
            { duration: '2m', target: 100 },  // Ramp up to stress level
            { duration: '3m', target: 100 },  // Stay at stress level
            { duration: '2m', target: 150 },  // Push beyond stress
            { duration: '3m', target: 150 },  // Stay at peak
            { duration: '2m', target: 0 },    // Ramp down
        ],
        gracefulRampDown: '30s',
    },
    // Spike test: Sudden traffic spike
    spike: {
        executor: 'ramping-vus',
        startVUs: 0,
        stages: [
            { duration: '30s', target: 10 },  // Warm up
            { duration: '1m', target: 10 },   // Normal load
            { duration: '10s', target: 200 }, // Spike!
            { duration: '2m', target: 200 },  // Stay at spike
            { duration: '10s', target: 10 },  // Scale down
            { duration: '1m', target: 10 },   // Recovery
            { duration: '30s', target: 0 },   // Ramp down
        ],
        gracefulRampDown: '30s',
    },
    // Soak test: Extended duration to find memory leaks
    soak: {
        executor: 'constant-vus',
        vus: 30,
        duration: '30m',
        gracefulStop: '1m',
    },
    // Breakpoint test: Find the breaking point
    breakpoint: {
        executor: 'ramping-arrival-rate',
        startRate: 10,
        timeUnit: '1s',
        preAllocatedVUs: 50,
        maxVUs: 500,
        stages: [
            { duration: '2m', target: 50 },
            { duration: '2m', target: 100 },
            { duration: '2m', target: 150 },
            { duration: '2m', target: 200 },
            { duration: '2m', target: 250 },
            { duration: '2m', target: 300 },
        ],
    },
};

// Select scenario based on environment variable
const selectedScenario = __ENV.K6_SCENARIO || 'smoke';

export const options = {
    scenarios: {
        [selectedScenario]: scenarios[selectedScenario] || scenarios.smoke,
    },
    thresholds: {
        http_req_duration: ['p(95)<500', 'p(99)<1000'],  // 95% < 500ms, 99% < 1s
        http_req_failed: ['rate<0.05'],                   // Error rate < 5%
        errors: ['rate<0.1'],                             // Custom error rate < 10%
        user_creation_time: ['p(95)<1000'],               // User creation < 1s
        user_fetch_time: ['p(95)<300'],                   // User fetch < 300ms
    },
};

// =============================================================================
// HELPER FUNCTIONS
// =============================================================================

// Get Auth0 access token using password grant flow (for admin authentication)
function getAccessToken() {
    if (!AUTH0_ADMIN_USERNAME || !AUTH0_ADMIN_PASSWORD) {
        console.warn('Auth0 admin credentials not configured. Skipping authentication.');
        console.warn('Set AUTH0_ADMIN_USERNAME and AUTH0_ADMIN_PASSWORD environment variables.');
        return null;
    }

    if (!AUTH0_DOMAIN || !AUTH0_CLIENT_ID) {
        console.warn('Auth0 domain or client ID not configured.');
        return null;
    }

    const tokenUrl = `https://${AUTH0_DOMAIN}/oauth/token`;
    const payload = {
        grant_type: 'password',
        username: AUTH0_ADMIN_USERNAME,
        password: AUTH0_ADMIN_PASSWORD,
        audience: AUTH0_AUDIENCE,
        client_id: AUTH0_CLIENT_ID,
        client_secret: AUTH0_CLIENT_SECRET,
        scope: 'openid profile email',
        realm: AUTH0_REALM,
    };

    const params = {
        headers: {
            'Content-Type': 'application/json',
        },
    };

    console.log(`Authenticating as ${AUTH0_ADMIN_USERNAME}...`);

    const startTime = new Date();
    const response = http.post(tokenUrl, JSON.stringify(payload), params);
    authTokenTime.add(new Date() - startTime);

    if (response.status === 200) {
        const body = JSON.parse(response.body);
        console.log('Successfully obtained admin access token via password grant');
        return body.access_token;
    }

    console.error(`Password grant failed: ${response.status}`);
    try {
        const errorBody = JSON.parse(response.body);
        console.error(`Error: ${errorBody.error} - ${errorBody.error_description}`);
    } catch (e) {
        console.error(`Response: ${response.body}`);
    }
    return null;
}

// Generate random user data
function generateUserData() {
    const timestamp = Date.now();
    const random = randomString(8);
    return {
        auth0Id: `auth0|k6_${timestamp}_${random}`,
        name: `K6 Test User ${random}`,
        email: `k6_${timestamp}_${random}@test.upmatches.com`,
        gender: ['MALE', 'FEMALE', 'OTHER'][randomIntBetween(0, 2)],
        telegramId: `@k6user_${random}`,
        skillId: randomIntBetween(1, 5),
    };
}

// Create HTTP headers with optional auth
function getHeaders(accessToken = null) {
    const headers = {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
    };
    if (accessToken) {
        headers['Authorization'] = `Bearer ${accessToken}`;
    }
    return headers;
}

// =============================================================================
// API ENDPOINT FUNCTIONS
// =============================================================================

function healthCheck() {
    const response = http.get(`${BASE_URL}/actuator/health`);
    check(response, {
        'health check status is 200': (r) => r.status === 200,
        'health status is UP': (r) => {
            try {
                return JSON.parse(r.body).status === 'UP';
            } catch (e) {
                return false;
            }
        },
    });
    return response.status === 200;
}

function createUser(headers) {
    const userData = generateUserData();
    const startTime = new Date();

    const response = http.post(
        `${BASE_URL}/api/${API_VERSION}/users`,
        JSON.stringify(userData),
        { headers }
    );

    userCreationTime.add(new Date() - startTime);

    const success = check(response, {
        'create user status is 201': (r) => r.status === 201,
        'create user returns uuid': (r) => {
            try {
                const body = JSON.parse(r.body);
                return body.uuid !== undefined;
            } catch (e) {
                return false;
            }
        },
    });

    if (success) {
        successfulRequests.add(1);
        errorRate.add(0);
        try {
            return JSON.parse(response.body);
        } catch (e) {
            return null;
        }
    } else {
        failedRequests.add(1);
        errorRate.add(1);
        return null;
    }
}

function getUserByUuid(uuid, headers) {
    const startTime = new Date();
    const response = http.get(`${BASE_URL}/api/${API_VERSION}/users/${uuid}`, { headers });
    userFetchTime.add(new Date() - startTime);

    const success = check(response, {
        'get user by uuid status is 200': (r) => r.status === 200,
        'get user returns correct uuid': (r) => {
            try {
                return JSON.parse(r.body).uuid === uuid;
            } catch (e) {
                return false;
            }
        },
    });

    if (success) {
        successfulRequests.add(1);
        errorRate.add(0);
    } else {
        failedRequests.add(1);
        errorRate.add(1);
    }

    return response;
}

function getAllUsers(headers, page = 0, size = 20) {
    const startTime = new Date();
    const response = http.get(
        `${BASE_URL}/api/${API_VERSION}/users?page=${page}&size=${size}&sortBy=createdAt&sortDirection=DESC`,
        { headers }
    );
    userFetchTime.add(new Date() - startTime);

    const success = check(response, {
        'get all users status is 200': (r) => r.status === 200,
        'get all users returns paginated content': (r) => {
            try {
                const body = JSON.parse(r.body);
                return body.content !== undefined && body.page !== undefined;
            } catch (e) {
                return false;
            }
        },
    });

    if (success) {
        successfulRequests.add(1);
        errorRate.add(0);
    } else {
        failedRequests.add(1);
        errorRate.add(1);
    }

    return response;
}

function getUserByAuth0Id(auth0Id, headers) {
    const encodedAuth0Id = encodeURIComponent(auth0Id);
    const response = http.get(
        `${BASE_URL}/api/${API_VERSION}/users/auth0/${encodedAuth0Id}`,
        { headers }
    );

    const success = check(response, {
        'get user by auth0Id status is 200': (r) => r.status === 200,
    });

    if (success) {
        successfulRequests.add(1);
        errorRate.add(0);
    } else {
        failedRequests.add(1);
        errorRate.add(1);
    }

    return response;
}

function getUserByEmail(email, headers) {
    const response = http.get(
        `${BASE_URL}/api/${API_VERSION}/users/email?email=${encodeURIComponent(email)}`,
        { headers }
    );

    const success = check(response, {
        'get user by email status is 200': (r) => r.status === 200,
    });

    if (success) {
        successfulRequests.add(1);
        errorRate.add(0);
    } else {
        failedRequests.add(1);
        errorRate.add(1);
    }

    return response;
}

function updateUser(uuid, userData, headers) {
    const response = http.put(
        `${BASE_URL}/api/${API_VERSION}/users/${uuid}`,
        JSON.stringify(userData),
        { headers }
    );

    const success = check(response, {
        'update user status is 200': (r) => r.status === 200,
    });

    if (success) {
        successfulRequests.add(1);
        errorRate.add(0);
    } else {
        failedRequests.add(1);
        errorRate.add(1);
    }

    return response;
}

function softDeleteUser(uuid, headers) {
    const response = http.patch(
        `${BASE_URL}/api/${API_VERSION}/users/${uuid}/soft-delete`,
        null,
        { headers }
    );

    const success = check(response, {
        'soft delete user status is 204': (r) => r.status === 204,
    });

    if (success) {
        successfulRequests.add(1);
        errorRate.add(0);
    } else {
        failedRequests.add(1);
        errorRate.add(1);
    }

    return response;
}

function deleteUser(uuid, headers) {
    const response = http.del(
        `${BASE_URL}/api/${API_VERSION}/users/${uuid}`,
        null,
        { headers }
    );

    const success = check(response, {
        'delete user status is 204': (r) => r.status === 204,
    });

    if (success) {
        successfulRequests.add(1);
        errorRate.add(0);
    } else {
        failedRequests.add(1);
        errorRate.add(1);
    }

    return response;
}

// =============================================================================
// SETUP - Runs once before the test
// =============================================================================
export function setup() {
    console.log(`Starting ${selectedScenario} test against ${BASE_URL}`);
    console.log(`API Version: ${API_VERSION}`);

    // Verify system is healthy
    const isHealthy = healthCheck();
    if (!isHealthy) {
        console.error('System health check failed! Aborting test.');
        return { healthy: false };
    }

    // Get access token for authenticated requests
    const accessToken = getAccessToken();

    return {
        healthy: true,
        accessToken: accessToken,
        startTime: new Date().toISOString(),
    };
}

// =============================================================================
// MAIN TEST FUNCTION - Runs for each VU iteration
// =============================================================================
export default function (data) {
    if (!data.healthy) {
        console.error('Skipping iteration - system not healthy');
        return;
    }

    const headers = getHeaders(data.accessToken);
    const headersNoAuth = getHeaders();

    // Simulate realistic user behavior with random delays
    const thinkTime = randomIntBetween(1, 3);

    group('Health Check', function () {
        healthCheck();
        sleep(0.5);
    });

    group('User CRUD Operations', function () {
        // Create a new user
        const createdUser = createUser(headersNoAuth);
        sleep(thinkTime);

        if (createdUser && createdUser.uuid) {
            // Read the created user by UUID
            getUserByUuid(createdUser.uuid, headersNoAuth);
            sleep(thinkTime / 2);

            // Read by Auth0 ID
            if (createdUser.auth0Id) {
                getUserByAuth0Id(createdUser.auth0Id, headersNoAuth);
                sleep(thinkTime / 2);
            }

            // Read by Email
            if (createdUser.email) {
                getUserByEmail(createdUser.email, headersNoAuth);
                sleep(thinkTime / 2);
            }

            // Update the user
            const updatedData = {
                ...createdUser,
                name: `Updated ${createdUser.name}`,
                gender: 'OTHER',
            };
            updateUser(createdUser.uuid, updatedData, headersNoAuth);
            sleep(thinkTime / 2);

            // Soft delete the user (requires admin token)
            if (data.accessToken) {
                softDeleteUser(createdUser.uuid, headers);
                sleep(thinkTime / 2);
            }

            // Hard delete the user (cleanup, requires admin token)
            if (data.accessToken) {
                deleteUser(createdUser.uuid, headers);
            }
        }
    });

    // Only run admin operations if we have a token
    if (data.accessToken) {
        group('Admin Operations', function () {
            // Get paginated user list
            getAllUsers(headers, 0, 10);
            sleep(thinkTime);

            // Get second page
            getAllUsers(headers, 1, 10);
            sleep(thinkTime / 2);
        });
    }

    sleep(thinkTime);
}

// =============================================================================
// TEARDOWN - Runs once after all VUs finish
// =============================================================================
export function teardown(data) {
    console.log(`Test completed. Started at: ${data.startTime}`);
    console.log(`Finished at: ${new Date().toISOString()}`);
}
