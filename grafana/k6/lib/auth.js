/**
 * Authentication Helper Module for k6 Tests
 *
 * Provides Auth0 authentication functionality for stress tests.
 * Uses password grant flow (Resource Owner Password) for admin authentication.
 */

import http from 'k6/http';
import { Trend } from 'k6/metrics';

const authTokenTime = new Trend('auth_token_time', true);

/**
 * Auth0 configuration loaded from environment variables
 */
export const auth0Config = {
    domain: __ENV.AUTH0_DOMAIN || 'dev-buildapp.jp.auth0.com',
    audience: __ENV.AUTH0_AUDIENCE || 'https://dev-api.upmatches.com/api/v1',
    clientId: __ENV.AUTH0_CLIENT_ID || 'kvXP5j3OfuuYMt4qxzuUtFjJQCio0iym',
    clientSecret: __ENV.AUTH0_CLIENT_SECRET || '',
    adminUsername: __ENV.AUTH0_ADMIN_USERNAME || '',
    adminPassword: __ENV.AUTH0_ADMIN_PASSWORD || '',
    realm: __ENV.AUTH0_REALM || 'Username-Password-Authentication',
};

/**
 * Get access token using password grant flow (Resource Owner Password)
 *
 * This is the recommended method for admin authentication in k6 tests.
 * Uses JSON body format with realm parameter for Auth0 Database Connection.
 *
 * @param {string} username - User's email (defaults to AUTH0_ADMIN_USERNAME)
 * @param {string} password - User's password (defaults to AUTH0_ADMIN_PASSWORD)
 * @returns {string|null} Access token or null if failed
 */
export function getPasswordGrantToken(username = null, password = null) {
    const user = username || auth0Config.adminUsername;
    const pass = password || auth0Config.adminPassword;

    if (!auth0Config.domain || !auth0Config.clientId) {
        console.warn('Auth0 domain or client ID not configured');
        return null;
    }

    if (!user || !pass) {
        console.warn('Username and password are required for password grant flow');
        console.warn('Set AUTH0_ADMIN_USERNAME and AUTH0_ADMIN_PASSWORD environment variables');
        return null;
    }

    const tokenUrl = `https://${auth0Config.domain}/oauth/token`;

    // Auth0 password grant with JSON body and realm parameter
    const payload = JSON.stringify({
        grant_type: 'password',
        username: user,
        password: pass,
        audience: auth0Config.audience,
        client_id: auth0Config.clientId,
        client_secret: auth0Config.clientSecret,
        scope: 'openid profile email',
        realm: auth0Config.realm,
    });

    const params = {
        headers: {
            'Content-Type': 'application/json',
        },
    };

    console.log(`Authenticating as ${user} against ${auth0Config.domain}...`);

    const startTime = new Date();
    const response = http.post(tokenUrl, payload, params);
    authTokenTime.add(new Date() - startTime);

    if (response.status === 200) {
        try {
            const body = JSON.parse(response.body);
            console.log('Successfully obtained admin access token via password grant');
            return body.access_token;
        } catch (e) {
            console.error('Failed to parse token response');
            return null;
        }
    }

    console.error(`Failed to get password grant token: ${response.status}`);
    try {
        const errorBody = JSON.parse(response.body);
        console.error(`Error: ${errorBody.error} - ${errorBody.error_description}`);
    } catch (e) {
        console.error(`Response: ${response.body}`);
    }
    return null;
}

/**
 * Get admin access token (convenience wrapper)
 * Uses password grant flow with configured admin credentials.
 *
 * @returns {string|null} Access token or null if failed
 */
export function getAdminToken() {
    return getPasswordGrantToken(auth0Config.adminUsername, auth0Config.adminPassword);
}

/**
 * Create HTTP headers with authorization
 * @param {string|null} accessToken - Bearer token
 * @returns {Object} Headers object
 */
export function getAuthHeaders(accessToken = null) {
    const headers = {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
    };

    if (accessToken) {
        headers['Authorization'] = `Bearer ${accessToken}`;
    }

    return headers;
}

/**
 * Create headers without authorization
 * @returns {Object} Headers object
 */
export function getPublicHeaders() {
    return {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
    };
}

/**
 * Decode JWT payload (without verification)
 * @param {string} token - JWT token
 * @returns {Object|null} Decoded payload
 */
export function decodeJwtPayload(token) {
    if (!token) return null;

    try {
        const parts = token.split('.');
        if (parts.length !== 3) return null;

        // Base64 decode the payload (second part)
        const payload = parts[1];
        const decoded = atob(payload.replace(/-/g, '+').replace(/_/g, '/'));
        return JSON.parse(decoded);
    } catch (e) {
        console.error('Failed to decode JWT payload');
        return null;
    }
}

/**
 * Check if token is expired
 * @param {string} token - JWT token
 * @returns {boolean}
 */
export function isTokenExpired(token) {
    const payload = decodeJwtPayload(token);
    if (!payload || !payload.exp) return true;

    const now = Math.floor(Date.now() / 1000);
    return payload.exp < now;
}

// Base64 decode helper (k6 doesn't have atob natively in all contexts)
function atob(str) {
    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=';
    let output = '';

    str = str.replace(/=+$/, '');

    for (let bc = 0, bs, buffer, idx = 0; (buffer = str.charAt(idx++)); ) {
        bs = chars.indexOf(buffer);
        if (bs === -1) continue;
        bc = bc % 4 ? bc * 64 + bs : bs;
        if ((bc %= 4) !== 0) {
            output += String.fromCharCode(255 & (bc >> ((-2 * (bc % 4 + 1)) & 6)));
        }
    }

    return output;
}
