/**
 * Test Data Generator Module for k6 Tests
 *
 * Provides functions to generate realistic test data for API stress testing.
 */

import { randomString, randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

// Gender options matching the API enum
export const GENDERS = ['MALE', 'FEMALE', 'OTHER'];

// Role options matching the API enum
export const ROLES = ['ROLE_USER', 'ROLE_ADMINISTRATOR'];

// Skill level IDs (assuming 1-5 range)
export const SKILL_LEVELS = [1, 2, 3, 4, 5];

// First names for realistic user generation
const FIRST_NAMES = [
    'James', 'Mary', 'John', 'Patricia', 'Robert', 'Jennifer',
    'Michael', 'Linda', 'William', 'Elizabeth', 'David', 'Barbara',
    'Richard', 'Susan', 'Joseph', 'Jessica', 'Thomas', 'Sarah',
    'Charles', 'Karen', 'Christopher', 'Lisa', 'Daniel', 'Nancy',
];

// Last names for realistic user generation
const LAST_NAMES = [
    'Smith', 'Johnson', 'Williams', 'Brown', 'Jones', 'Garcia',
    'Miller', 'Davis', 'Rodriguez', 'Martinez', 'Hernandez', 'Lopez',
    'Gonzalez', 'Wilson', 'Anderson', 'Thomas', 'Taylor', 'Moore',
    'Jackson', 'Martin', 'Lee', 'Perez', 'Thompson', 'White',
];

// Email domains for realistic email generation
const EMAIL_DOMAINS = [
    'gmail.com', 'yahoo.com', 'outlook.com', 'hotmail.com',
    'test.upmatches.com', 'example.com', 'mail.com',
];

/**
 * Pick a random item from an array
 * @param {Array} array
 * @returns {*}
 */
export function randomItem(array) {
    return array[randomIntBetween(0, array.length - 1)];
}

/**
 * Generate a unique identifier suffix
 * @returns {string}
 */
export function uniqueSuffix() {
    return `${Date.now()}_${randomString(6)}`;
}

/**
 * Generate a realistic name
 * @returns {string}
 */
export function generateName() {
    return `${randomItem(FIRST_NAMES)} ${randomItem(LAST_NAMES)}`;
}

/**
 * Generate a unique email address
 * @param {string} prefix - Optional prefix for the email
 * @returns {string}
 */
export function generateEmail(prefix = 'k6user') {
    const suffix = uniqueSuffix();
    const domain = randomItem(EMAIL_DOMAINS);
    return `${prefix}_${suffix}@${domain}`;
}

/**
 * Generate a unique Auth0 ID
 * @param {string} prefix - Optional prefix
 * @returns {string}
 */
export function generateAuth0Id(prefix = 'k6') {
    return `auth0|${prefix}_${uniqueSuffix()}`;
}

/**
 * Generate a random gender
 * @returns {string}
 */
export function generateGender() {
    return randomItem(GENDERS);
}

/**
 * Generate a random skill ID
 * @returns {number}
 */
export function generateSkillId() {
    return randomItem(SKILL_LEVELS);
}

/**
 * Generate a Telegram ID
 * @param {string} prefix - Optional prefix
 * @returns {string}
 */
export function generateTelegramId(prefix = 'k6user') {
    return `@${prefix}_${randomString(8)}`;
}

/**
 * Generate a Messenger ID
 * @param {string} prefix - Optional prefix
 * @returns {string}
 */
export function generateMessengerId(prefix = 'k6user') {
    return `${prefix}_${randomString(10)}`;
}

/**
 * Generate a WhatsApp ID (phone number format)
 * @returns {string}
 */
export function generateWhatsappId() {
    const countryCode = randomItem(['+1', '+44', '+61', '+65', '+60']);
    const number = randomIntBetween(1000000000, 9999999999);
    return `${countryCode}${number}`;
}

/**
 * Generate complete user data for creation
 * @param {Object} overrides - Optional field overrides
 * @returns {Object} User creation payload
 */
export function generateUserData(overrides = {}) {
    return {
        auth0Id: generateAuth0Id(),
        name: generateName(),
        email: generateEmail(),
        gender: generateGender(),
        telegramId: generateTelegramId(),
        skillId: generateSkillId(),
        ...overrides,
    };
}

/**
 * Generate user data with all optional fields
 * @param {Object} overrides - Optional field overrides
 * @returns {Object} Complete user payload
 */
export function generateFullUserData(overrides = {}) {
    return {
        auth0Id: generateAuth0Id(),
        name: generateName(),
        email: generateEmail(),
        gender: generateGender(),
        telegramId: generateTelegramId(),
        messengerId: generateMessengerId(),
        whatsappId: generateWhatsappId(),
        skillId: generateSkillId(),
        ...overrides,
    };
}

/**
 * Generate user update data
 * @param {Object} existingUser - Existing user data to preserve
 * @param {Object} updates - Fields to update
 * @returns {Object} Updated user payload
 */
export function generateUserUpdateData(existingUser, updates = {}) {
    return {
        auth0Id: existingUser.auth0Id,
        name: updates.name || `Updated ${existingUser.name}`,
        email: existingUser.email,
        gender: updates.gender || (existingUser.gender === 'MALE' ? 'FEMALE' : 'MALE'),
        telegramId: updates.telegramId || existingUser.telegramId,
        skillId: updates.skillId || generateSkillId(),
        ...updates,
    };
}

/**
 * Generate a batch of users
 * @param {number} count - Number of users to generate
 * @returns {Array<Object>} Array of user data objects
 */
export function generateUserBatch(count) {
    const users = [];
    for (let i = 0; i < count; i++) {
        users.push(generateUserData());
    }
    return users;
}

/**
 * Generate invalid user data for negative testing
 * @param {string} invalidType - Type of invalid data to generate
 * @returns {Object}
 */
export function generateInvalidUserData(invalidType) {
    switch (invalidType) {
        case 'missing_auth0Id':
            return {
                name: generateName(),
                email: generateEmail(),
                gender: generateGender(),
            };
        case 'missing_name':
            return {
                auth0Id: generateAuth0Id(),
                email: generateEmail(),
                gender: generateGender(),
            };
        case 'missing_email':
            return {
                auth0Id: generateAuth0Id(),
                name: generateName(),
                gender: generateGender(),
            };
        case 'invalid_email':
            return {
                auth0Id: generateAuth0Id(),
                name: generateName(),
                email: 'not-a-valid-email',
                gender: generateGender(),
            };
        case 'empty_name':
            return {
                auth0Id: generateAuth0Id(),
                name: '',
                email: generateEmail(),
                gender: generateGender(),
            };
        case 'invalid_gender':
            return {
                auth0Id: generateAuth0Id(),
                name: generateName(),
                email: generateEmail(),
                gender: 'INVALID_GENDER',
            };
        case 'empty_body':
            return {};
        default:
            return generateUserData();
    }
}

/**
 * Generate pagination parameters
 * @param {Object} options
 * @returns {Object}
 */
export function generatePaginationParams(options = {}) {
    return {
        page: options.page ?? randomIntBetween(0, 5),
        size: options.size ?? randomItem([10, 20, 50]),
        sortBy: options.sortBy ?? randomItem(['createdAt', 'name', 'email']),
        sortDirection: options.sortDirection ?? randomItem(['ASC', 'DESC']),
    };
}

/**
 * Format pagination params as query string
 * @param {Object} params
 * @returns {string}
 */
export function formatPaginationQuery(params) {
    return `page=${params.page}&size=${params.size}&sortBy=${params.sortBy}&sortDirection=${params.sortDirection}`;
}
