# TaskHub Platform

## 1) Overview

TaskHub is a backend service for managing tasks. A **task** is the basic unit, and the API supports the usual CRUD operations. The **list tasks** endpoint is **paginated** so it behaves well when the dataset grows.

The goal of this project is less about building a feature-heavy product and more about showing how I ship a service end-to-end: **build/test**, **image publish**, **Kubernetes deployment for dev and prod**, **monitoring with Prometheus/Grafana**, and a **clean release flow**. After the DevOps submission, I’ll expand the app with **OAuth2**, an additional microservice, and **async/event-driven** processing.

---

## 2) Quick Start 

### Prerequisites
- Git
- Java 17+
- Docker + Docker Compose
- kubectl
- Terraform + Helm (for cluster deployments)
- Jenkins (already configured on the server for CI/CD)

### Run locally (Docker Compose)
From the repo root:

```bash
cd infra/docker
docker compose up --build
```

Then open:
- Swagger UI: http://localhost:8080/swagger-ui/index.html
- Actuator (health): http://localhost:8080/actuator/health
- Prometheus metrics: http://localhost:8080/actuator/prometheus

> Local DB credentials in `docker-compose.yml` are intentionally simple for development.

### Deploy to DEV (automatic)
- Push to `develop` → Jenkins CI/CD runs automatically → deploys to the **dev** namespace.
- You can also trigger the Jenkins job manually (same result).

### Deploy to PROD (release-driven)
1. Merge `develop` → `main`
2. Run Gradle release locally to create the Git tag (example `0.1.0`)
3. Trigger the dedicated **PROD release** Jenkins job and provide the tag as a parameter

### URLs (running environments)

| Component | DEV | PROD |
|---|---|---|
| Swagger UI | http://51.158.200.80:30080/swagger-ui/index.html#/ | http://51.158.200.80:30081/swagger-ui/index.html#/ |
| Grafana (Golden Signals) | http://51.158.200.80:30030/d/taskhub-gold-v1/taskhub?orgId=1&refresh=5s | Same Grafana (use env dropdown: `dev` / `prod`) |
| Jenkins | http://51.158.200.80:8080/ | Same |

### Credentials (where they live)
- **Jenkins** uses its internal Credentials Store (DockerHub creds, DB creds, Slack webhook, etc.).
- **Kubernetes** secrets are created by Terraform/Helm using values provided from Jenkins credentials.
- No Vault is used in this project; Jenkins is the source of truth for secrets in the current setup.

---

## 3) Key Features

### Product features
- Task CRUD API: create, list (paginated), update, delete tasks via REST endpoints.
- Validation + clear error responses: rejects invalid input with meaningful 4xx responses.
- Robust exception handling: consistent error model for faster client-side debugging.
- Production-style observability: key runtime metrics exported via Actuator/Micrometer for monitoring.

### Platform / DevOps features
- Automated CI/CD: build → test → Docker image publish → Trivy scan gate → deploy.
- Dev auto-deploy on `develop`: every push triggers a pipeline run and updates the dev namespace.
- Release-driven prod deploy: production deploy requires an explicit Git tag + dedicated PROD job.
- Infra as Code (Terraform + Helm): “one-click” provisioning/re-deploy of cluster resources once prerequisites exist.

---

## 4) API / Domain

A **Task** is the core entity.

### Fields
- `id` (string) — generated identifier
- `title` (string) — task title
- `dueDate` (ISO-8601 timestamp) — due date/time
- `status` (enum) — current state (default: `OPEN`)

### Task Request

```json
{
  "title": "string",
  "dueDate": "2026-02-19T10:20:25.080Z"
}
```

### Task Response

```json
{
  "id": "string",
  "title": "string",
  "dueDate": "2026-02-19T10:20:25.085Z",
  "status": "OPEN"
}
```

### Endpoints and status codes

#### `POST /tasks`
Creates a new task.
- Success: `201 Created`
- Validation failure: `400 Bad Request` (Bean Validation via `@Valid`)

#### `GET /tasks`
Returns a paginated list of tasks.
- Success: `200 OK`
- Pagination: Spring `Pageable` is supported (`page`, `size`, `sort`)
- Default: `size=20`, `sort=dueDate` (`@PageableDefault(size = 20, sort = "dueDate")`)

Response type: `Page<TaskResponse>` (standard Spring Page fields like `content`, `totalElements`, `totalPages`, etc.)

#### `GET /tasks/{id}`
Fetches a single task by id.
- Success: `200 OK`
- Not found: `404 Not Found` (via `TaskNotFoundException`)

#### `PUT /tasks/{id}`
Updates an existing task.
- Success: `200 OK`
- Validation failure: `400 Bad Request`
- Not found: `404 Not Found`

#### `DELETE /tasks/{id}`
Deletes a task.
- Success: `204 No Content`
- Not found: `404 Not Found`

### Error model (what clients receive)

**Validation errors (400)**

Handled by `MethodArgumentNotValidException`. Response body is a simple field-to-message map:

```json
{
  "title": "must not be blank",
  "dueDate": "must be a future date"
}
```

**Not found (404)**

Handled by `TaskNotFoundException`. Response body is plain text (exception message).

**Unexpected server error (500)**

A generic message is returned and a `server_error` counter is recorded.

### Observability hooks in the API layer
- Each endpoint is instrumented with Micrometer `@Timed`:
    - `api.tasks.create`, `api.tasks.list`, `api.tasks.get`, `api.tasks.update`, `api.tasks.delete`
- API error counter is emitted from the global handler:
    - Metric: `api.errors.total`
    - Tag: `type=validation | not_found | server_error`

---

## 5) Architecture

## Architecture Diagram

![TaskHub Architecture](docs/architecture/taskhub-architecture.png)

### High-level view

TaskHub runs as a single backend service (`task-service`) with a PostgreSQL database in both environments: **dev** and **prod**.

Infrastructure and app deployments are automated via Jenkins using Terraform + Helm. Monitoring is provided by a shared monitoring stack (Prometheus + Grafana) scraping metrics from both namespaces.

### Kubernetes layout (namespaces)

**dev**
- App
    - `deployment/task-service` (2 replicas)
    - `service/task-service` (NodePort **30080** → port 80 → targetPort 8080)
- Database
    - `statefulset/postgres-postgresql` (1 replica)
    - `service/postgres-postgresql` (ClusterIP 5432)

**prod**
- App
    - `deployment/task-service` (2 replicas)
    - `service/task-service` (NodePort **30081** → port 80 → targetPort 8080)
- Database
    - `statefulset/postgres-postgresql` (1 replica)
    - `service/postgres-postgresql` (ClusterIP 5432)

**monitoring**
- Grafana
    - `deployment/monitoring-stack-grafana`
    - `service/monitoring-stack-grafana` (NodePort **30030**)
- Prometheus
    - `statefulset/prometheus-monitoring-stack-kube-prom-prometheus`
    - `service/monitoring-stack-kube-prom-prometheus` (ClusterIP)
- Operator + exporters
    - `deployment/monitoring-stack-kube-prom-operator`
    - `deployment/monitoring-stack-kube-state-metrics`
    - `service/monitoring-stack-prometheus-node-exporter`

### Access model (how traffic reaches the app)

This project uses **NodePort** services for simplicity and predictability during evaluation:
- DEV Swagger/UI: http://51.158.200.80:30080/...
- PROD Swagger/UI: http://51.158.200.80:30081/...
- Grafana: http://51.158.200.80:30030/...

Traefik exists in the cluster (`kube-system/traefik`), but TaskHub does not depend on Ingress for access in the current setup.

### Request/data flow (runtime)

Client → NodePort → Kubernetes Service → task-service Pod → PostgreSQL Service → PostgreSQL Pod

Examples:
- DEV: `51.158.200.80:30080` → `dev/task-service` → `dev/task-service` pods → `dev/postgres-postgresql:5432`
- PROD: `51.158.200.80:30081` → `prod/task-service` → `prod/task-service` pods → `prod/postgres-postgresql:5432`

### Observability flow (metrics pipeline)

task-service → `/actuator/prometheus` → Prometheus (ServiceMonitor scrape) → Grafana dashboard
- App exports metrics via Spring Boot Actuator + Micrometer
- Prometheus scrapes the app using a ServiceMonitor
- Grafana dashboard uses an environment dropdown (`dev` / `prod`) to filter series

---

## 6) Tech Stack

### Application
- **Language/Runtime:** Java **17.0.17** (LTS)
- **Framework:** Spring Boot **3.5.9**
- **Build:** Gradle Wrapper (**Gradle 8.14.3**)
- **Database:** PostgreSQL **15**
- **API:** REST + OpenAPI/Swagger UI
- **Observability in-app:** Spring Boot Actuator + Micrometer

### Platform / Infra
- **Kubernetes distribution:** k3s **v1.34.3+k3s1**
- **Node OS:** Debian GNU/Linux **12 (bookworm)**
- **Container runtime:** containerd **2.1.5-k3s1**
- **Containers:** Docker (multi-stage build)
- **IaC:** Terraform **1.14.4**
- **Package manager:** Helm **v3.20.0**

### CI/CD
- **CI/CD engine:** Jenkins (Pipeline as code)
- **Container registry:** Docker Hub (`tsingh38/taskhub`)
- **Security gate:** Trivy (HIGH/CRITICAL scan gate)

### Monitoring / Alerting
- **Stack:** kube-prometheus-stack (Helm chart **56.6.2**)
- **Prometheus Operator / stack app version:** **v0.71.2**
- **Components:** Prometheus, Grafana, Alertmanager

---

## 7) Local Setup

### Local prerequisites
- Java 17+
- Docker + Docker Compose
- Git

### Clone

```bash
git clone https://github.com/tsingh38/taskhub-platform.git
cd taskhub-platform
```

### Run

```bash
cd infra/docker
docker compose up --build
```

### Useful local URLs
- Swagger UI: http://localhost:8080/swagger-ui/index.html
- Metrics: http://localhost:8080/actuator/prometheus

---

## 8) Testing (TODO)
- How to run unit tests
- Whether there are integration tests
- Where to find the test reports
- What makes the pipeline fail (quality gates)

---

## 9) Deployment (Dev/Prod)

### Dev
- Trigger: push to `develop` (or manual run of the Jenkins CI job)
- Target namespace: `dev`
- Access (NodePort):
    - Swagger UI: http://51.158.200.80:30080/swagger-ui/index.html#/
- IaC execution: handled by Jenkins (Terraform + Helm)

### Prod
- Trigger model: **manual intent** via release tag
- Steps:
    1) Merge `develop` → `main`
    2) Run `./gradlew release` locally to create a version tag (example: `0.1.0`) and push it
    3) Trigger the Jenkins **PROD release** job and provide `RELEASE=true` and `RELEASE_TAG=0.1.0`
- Target namespace: `prod`
- Access (NodePort):
    - Swagger UI: http://51.158.200.80:30081/swagger-ui/index.html#/
- Rollback (quick): re-run the PROD job with a previous known-good `RELEASE_TAG` (or use Helm rollback)

---

## 10) CI/CD

### Branch / release strategy
- `develop` → DEV auto-deploy (continuous delivery to the `dev` namespace)
- `main` + Git tag → PROD deploy (manual intent, tag-driven)
- Release tags are created via `./gradlew release` (example: `0.1.0`) and pushed to origin

### Pipelines (high-level flow)

**DEV pipeline** (trigger: push to `develop`)
1. Checkout `develop`
2. Resolve version (snapshot)
3. Run tests: `./gradlew clean test`
4. Build Docker image and push to Docker Hub (snapshot tag)
5. Trivy scan (HIGH/CRITICAL gate)
6. Deploy monitoring stack (Terraform + Helm) — idempotent/no-op if already applied
7. Deploy app + DB to `dev` (Terraform + Helm), set image tag accordingly

**PROD pipeline** (trigger: dedicated job + `RELEASE_TAG`)
1. Checkout `main` at the provided `RELEASE_TAG`
2. Run tests: `./gradlew clean test`
3. Build Docker image and push to Docker Hub using the release tag (e.g., `0.1.0`)
4. Trivy scan (HIGH/CRITICAL gate)
5. Deploy app + DB to `prod` (Terraform + Helm) with `TF_VAR_app_version=<tag>`

### Artifacts produced
- JUnit test reports (JUnit publisher)
- `trivy-report.json` (archived per build)

---

## 11) Monitoring

### Stack
Monitoring is deployed via `kube-prometheus-stack` (Helm) in the `monitoring` namespace:
- Prometheus, Grafana, Alertmanager, kube-state-metrics, node-exporter

### Grafana access
- URL: http://51.158.200.80:30030/
- User: `admin`
- Password: stored in Kubernetes Secret: `monitoring-stack-grafana`

### Dashboards
- “Golden Signals” dashboard (dev/prod filter via dropdown):
    - http://51.158.200.80:30030/d/taskhub-gold-v1/taskhub?orgId=1&refresh=5s

### Prometheus scraping model (ServiceMonitor)
- The app exposes metrics at: `/actuator/prometheus`
- Prometheus discovers targets via `ServiceMonitor`
- Important label requirement (Prometheus selector):
    - `serviceMonitorSelector.matchLabels.release=monitoring-stack`
    - Therefore the ServiceMonitor must include: `metadata.labels.release: monitoring-stack`

### Generate traffic (to see metrics)

```bash
cd scripts
chmod +x traffic.generator.sh
./traffic.generator.sh
```

---

## 12) Security

### Secrets handling (current setup)
- Jenkins Credentials Store is the source of truth (DockerHub creds, DB creds, Slack webhook, etc.)
- Jenkins passes secrets into Terraform via environment variables (`TF_VAR_*`)
- Terraform/Helm creates Kubernetes Secrets in the target namespace (`dev` / `prod`)
- No Vault is used in this project (kept intentionally simple for evaluation)

### Image security gate
- Trivy scan runs in the pipeline and fails the build on HIGH/CRITICAL findings
- Trivy report is archived as `trivy-report.json`

### Network exposure
- App is exposed via NodePort (simple and deterministic for evaluation):
    - DEV: `dev/task-service` → NodePort `30080`
    - PROD: `prod/task-service` → NodePort `30081`
- Grafana: `monitoring/monitoring-stack-grafana` → NodePort `30030`
- Jenkins: http://51.158.200.80:8080/ (project CI)

> In a production setup, I would reduce public exposure (Ingress + auth), enforce least-privilege RBAC, and use a dedicated secret manager (Vault / cloud KMS).

---

## 13) Disaster Recovery (DR) (TODO)
- What can go wrong (cluster wipe, Terraform state loss, DB loss)
- Recovery steps (IaC re-apply, re-deploy, restore DB)
- Backups: where stored, how often, how to restore
- RTO/RPO statement (simple + realistic)

---

## 14) Troubleshooting (TODO)
- Jenkins: common failures + quick fixes
- Terraform: state lock / permission issues
- Prometheus not scraping ServiceMonitor (label selector)
- Pod crashloop checklist (`kubectl logs`, `kubectl describe`, probes)

---

## 15) Roadmap

Upcoming features (post DevOps submission):
- OAuth2
- Pub/sub + async processing
- Audit logging
- Scaling, policies, and a remote Terraform backend