# BofA Demo Monorepo

Three microservices representing a fictional digital banking platform. Current test coverage: approximately 30% overall. Compliance-critical paths (authentication flows, fraud notification dispatch, PII masking, audit logging) have minimal or zero test coverage. This repository is used to demonstrate AI-assisted test coverage improvement.

## Services

| Service | Language | Location | Port |
|---------|----------|----------|------|
| [Authentication Service](services/authentication-service/) | Java (Spring Boot) | `/services/authentication-service` | 8081 |
| [Notification Service](services/notification-service/) | TypeScript (Node.js) | `/services/notification-service` | 3001 |
| [PII Data Handler](services/pii-data-handler/) | Python (Flask) | `/services/pii-data-handler` | 5001 |

## Getting Started

Each service can be built and tested independently. See the individual service READMEs for instructions.

## CI

Tests for all three services run automatically on pull request via GitHub Actions.
