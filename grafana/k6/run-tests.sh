#!/bin/bash

# =============================================================================
# Upmatches API Stress Test Runner
# =============================================================================
# Usage: ./run-tests.sh [scenario] [options]
#
# Scenarios: smoke, load, stress, spike, soak, breakpoint, crud
# Options:
#   --local     Run with local k6 instead of Docker
#   --no-auth   Skip authentication (for public endpoints only)
#   --help      Show this help message
# =============================================================================

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# =============================================================================
# CONFIGURATION - Update these values or set as environment variables
# =============================================================================
BASE_URL="${BASE_URL:-http://localhost:8080}"
API_VERSION="${API_VERSION:-v1}"

# Auth0 Configuration
AUTH0_DOMAIN="${AUTH0_DOMAIN:-dev-buildapp.jp.auth0.com}"
AUTH0_AUDIENCE="${AUTH0_AUDIENCE:-https://dev-api.upmatches.com/api/v1}"
AUTH0_CLIENT_ID="${AUTH0_CLIENT_ID:-kvXP5j3OfuuYMt4qxzuUtFjJQCio0iym}"
AUTH0_CLIENT_SECRET="${AUTH0_CLIENT_SECRET:-}"
AUTH0_ADMIN_USERNAME="${AUTH0_ADMIN_USERNAME:-}"
AUTH0_ADMIN_PASSWORD="${AUTH0_ADMIN_PASSWORD:-}"
AUTH0_REALM="${AUTH0_REALM:-Username-Password-Authentication}"

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# =============================================================================
# FUNCTIONS
# =============================================================================

show_help() {
    echo -e "${BLUE}Upmatches API Stress Test Runner${NC}"
    echo ""
    echo "Usage: $0 [scenario] [options]"
    echo ""
    echo -e "${YELLOW}Scenarios:${NC}"
    echo "  smoke       Quick validation test (1 min, 1 VU)"
    echo "  load        Normal expected load (9 min, 50 VUs)"
    echo "  stress      Push beyond normal (17 min, 150 VUs)"
    echo "  spike       Sudden traffic spike (5 min, 200 VUs)"
    echo "  soak        Extended duration test (30 min, 30 VUs)"
    echo "  breakpoint  Find breaking point (12 min, 300+ VUs)"
    echo "  crud        User CRUD operations test (9 min, 50 VUs)"
    echo ""
    echo -e "${YELLOW}Options:${NC}"
    echo "  --local     Run with local k6 instead of Docker"
    echo "  --no-auth   Skip authentication"
    echo "  --help      Show this help message"
    echo ""
    echo -e "${YELLOW}Environment Variables:${NC}"
    echo "  BASE_URL              API base URL (default: http://localhost:8080)"
    echo "  AUTH0_ADMIN_USERNAME  Admin email for authentication"
    echo "  AUTH0_ADMIN_PASSWORD  Admin password for authentication"
    echo "  AUTH0_CLIENT_SECRET   Auth0 client secret"
    echo ""
    echo -e "${YELLOW}Examples:${NC}"
    echo "  $0 smoke"
    echo "  $0 load --local"
    echo "  $0 stress --no-auth"
    echo "  AUTH0_ADMIN_USERNAME=admin@example.com AUTH0_ADMIN_PASSWORD=secret $0 load"
    echo ""
}

check_docker() {
    if ! command -v docker &> /dev/null; then
        echo -e "${RED}Error: Docker is not installed or not in PATH${NC}"
        echo "Install Docker or use --local flag with k6 installed"
        exit 1
    fi
}

check_k6() {
    if ! command -v k6 &> /dev/null; then
        echo -e "${RED}Error: k6 is not installed or not in PATH${NC}"
        echo "Install k6: https://k6.io/docs/getting-started/installation/"
        echo "Or remove --local flag to use Docker"
        exit 1
    fi
}

check_api_health() {
    echo -e "${BLUE}Checking API health...${NC}"

    local health_url="${BASE_URL}/actuator/health"
    local response

    if command -v curl &> /dev/null; then
        response=$(curl -s -o /dev/null -w "%{http_code}" "$health_url" 2>/dev/null || echo "000")
    else
        echo -e "${YELLOW}Warning: curl not found, skipping health check${NC}"
        return 0
    fi

    if [ "$response" = "200" ]; then
        echo -e "${GREEN}✓ API is healthy${NC}"
        return 0
    else
        echo -e "${RED}✗ API health check failed (HTTP $response)${NC}"
        echo -e "${YELLOW}Make sure the API is running at: $BASE_URL${NC}"
        read -p "Continue anyway? (y/N) " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            exit 1
        fi
    fi
}

print_config() {
    echo -e "${BLUE}Configuration:${NC}"
    echo "  Base URL:     $BASE_URL"
    echo "  API Version:  $API_VERSION"
    echo "  Auth0 Domain: $AUTH0_DOMAIN"
    echo "  Scenario:     $SCENARIO"
    if [ -n "$AUTH0_ADMIN_USERNAME" ]; then
        echo "  Admin User:   $AUTH0_ADMIN_USERNAME"
    else
        echo -e "  Admin User:   ${YELLOW}Not configured (set AUTH0_ADMIN_USERNAME)${NC}"
    fi
    echo ""
}

run_with_docker() {
    local test_file=$1

    echo -e "${BLUE}Running with Docker...${NC}"

    # Build docker run command
    # --add-host is required on Linux to resolve host.docker.internal
    local docker_cmd="docker run --rm -i --add-host=host.docker.internal:host-gateway"

    # Add environment variables
    docker_cmd+=" -e BASE_URL=http://host.docker.internal:8080"
    docker_cmd+=" -e API_VERSION=$API_VERSION"
    docker_cmd+=" -e AUTH0_DOMAIN=$AUTH0_DOMAIN"
    docker_cmd+=" -e AUTH0_AUDIENCE=$AUTH0_AUDIENCE"
    docker_cmd+=" -e AUTH0_CLIENT_ID=$AUTH0_CLIENT_ID"
    docker_cmd+=" -e AUTH0_REALM=$AUTH0_REALM"

    if [ "$NO_AUTH" != "true" ]; then
        docker_cmd+=" -e AUTH0_CLIENT_SECRET=$AUTH0_CLIENT_SECRET"
        docker_cmd+=" -e AUTH0_ADMIN_USERNAME=$AUTH0_ADMIN_USERNAME"
        docker_cmd+=" -e AUTH0_ADMIN_PASSWORD=$AUTH0_ADMIN_PASSWORD"
    fi

    if [ "$SCENARIO" != "crud" ]; then
        docker_cmd+=" -e K6_SCENARIO=$SCENARIO"
    fi

    # Add volume mount and image
    docker_cmd+=" -v $SCRIPT_DIR:/scripts"
    docker_cmd+=" grafana/k6 run /scripts/$test_file"

    # Execute
    eval $docker_cmd
}

run_with_local_k6() {
    local test_file=$1

    echo -e "${BLUE}Running with local k6...${NC}"

    # Build k6 command
    local k6_cmd="k6 run"

    # Add environment variables
    k6_cmd+=" -e BASE_URL=$BASE_URL"
    k6_cmd+=" -e API_VERSION=$API_VERSION"
    k6_cmd+=" -e AUTH0_DOMAIN=$AUTH0_DOMAIN"
    k6_cmd+=" -e AUTH0_AUDIENCE=$AUTH0_AUDIENCE"
    k6_cmd+=" -e AUTH0_CLIENT_ID=$AUTH0_CLIENT_ID"
    k6_cmd+=" -e AUTH0_REALM=$AUTH0_REALM"

    if [ "$NO_AUTH" != "true" ]; then
        k6_cmd+=" -e AUTH0_CLIENT_SECRET=$AUTH0_CLIENT_SECRET"
        k6_cmd+=" -e AUTH0_ADMIN_USERNAME=$AUTH0_ADMIN_USERNAME"
        k6_cmd+=" -e AUTH0_ADMIN_PASSWORD=$AUTH0_ADMIN_PASSWORD"
    fi

    if [ "$SCENARIO" != "crud" ]; then
        k6_cmd+=" -e K6_SCENARIO=$SCENARIO"
    fi

    k6_cmd+=" $SCRIPT_DIR/$test_file"

    # Execute
    eval $k6_cmd
}

# =============================================================================
# MAIN
# =============================================================================

# Default values
SCENARIO="smoke"
USE_LOCAL=false
NO_AUTH=false

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        smoke|load|stress|spike|soak|breakpoint|crud)
            SCENARIO=$1
            shift
            ;;
        --local)
            USE_LOCAL=true
            shift
            ;;
        --no-auth)
            NO_AUTH=true
            shift
            ;;
        --help|-h)
            show_help
            exit 0
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            show_help
            exit 1
            ;;
    esac
done

# Determine test file
if [ "$SCENARIO" = "crud" ]; then
    TEST_FILE="scenarios/user-crud-test.js"
else
    TEST_FILE="stress-test.js"
fi

# Print banner
echo ""
echo -e "${GREEN}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║          Upmatches API Stress Test Runner                  ║${NC}"
echo -e "${GREEN}╚════════════════════════════════════════════════════════════╝${NC}"
echo ""

# Print configuration
print_config

# Check prerequisites
if [ "$USE_LOCAL" = true ]; then
    check_k6
else
    check_docker
fi

# Check API health
check_api_health

# Confirm before running long tests
case $SCENARIO in
    stress|spike|soak|breakpoint)
        echo -e "${YELLOW}Warning: This is a long-running test. It may take several minutes.${NC}"
        read -p "Continue? (y/N) " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            exit 0
        fi
        ;;
esac

echo ""
echo -e "${GREEN}Starting $SCENARIO test...${NC}"
echo ""

# Run the test
if [ "$USE_LOCAL" = true ]; then
    run_with_local_k6 "$TEST_FILE"
else
    run_with_docker "$TEST_FILE"
fi

echo ""
echo -e "${GREEN}Test completed!${NC}"
