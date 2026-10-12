# demo-orders-service

Order placement for the GitOps delivery demo. It calls
[`demo-catalog-service`](https://github.com/vehkiya/demo-catalog-service) for prices and "charges" a
simulated payment provider using a secret API key.

Java 25 · Spring Boot 4.1 · Maven · Jib

## Run locally

```sh
sdk env install                                      # Java 25 + Maven from .sdkmanrc (or use ./mvnw)
(cd ../demo-catalog-service && ./mvnw spring-boot:run) &
./mvnw spring-boot:run                               # no profile, no secret, no extra config
```

```sh
curl -s localhost:8080/api/orders -H 'content-type: application/json' \
  -d '{"items":[{"sku":"BOOK-001","quantity":2},{"sku":"MUG-001","quantity":1}]}' | jq
curl -s localhost:9080/actuator/configsources | jq   # every orders.* value + where it came from
```

| | Local | Kubernetes |
|---|---|---|
| API | `:8080` | `:8080` |
| Actuator | `:9080` | `:9000` |

## API

| Method | Path | |
|---|---|---|
| `POST` | `/api/orders` | `{"items":[{"sku","quantity"}], "express": false}` → `201` |
| `GET` | `/api/orders` | Orders placed on this instance (in memory) |
| `GET` | `/api/orders/{id}` | |

Errors are RFC 9457 problem details: `too-many-items` (400), `express-unavailable` (400),
`unknown-sku` (404), `catalog-unavailable` (503, after retries).

## Configuration

There is exactly **one** config file: [`src/main/resources/application.yaml`](src/main/resources/application.yaml).
It holds every property, with defaults that work locally, plus a small block that is active only on
Kubernetes (`spring.config.activate.on-cloud-platform: kubernetes`) and points the catalog URL at
`http://catalog:8080`.

Per-environment values live in the GitOps repo, never here:

| Env | Overrides | Secret |
|---|---|---|
| dev | [`envs/dev/orders/application.yaml`](https://github.com/vehkiya/demo-gitops/blob/main/envs/dev/orders/application.yaml) | `demo/dev/orders` |
| stage | [`envs/stage/orders/application.yaml`](https://github.com/vehkiya/demo-gitops/blob/main/envs/stage/orders/application.yaml) | `demo/stage/orders` |
| prod | [`envs/prod/orders/application.yaml`](https://github.com/vehkiya/demo-gitops/blob/main/envs/prod/orders/application.yaml) | `demo/prod/orders` |

| Property | Default | Notes |
|---|---|---|
| `orders.max-items-per-order` | `10` | 1–100 |
| `orders.catalog.base-url` | `http://localhost:8090` (`http://catalog:8080` on k8s) | Same in every env |
| `orders.catalog.timeout` | `2s` | 100ms–10s, connect + read |
| `orders.catalog.max-retries` | `2` | 0–5, only I/O errors and 5xx |
| `orders.express-shipping.enabled` | `false` | Feature flag |
| `orders.delivery.standard-days` | `5` | 1–30, days until `estimatedDelivery` |
| `orders.delivery.express-days` | `1` | 0–10, used when `express: true` |
| `orders.payment.mode` | `simulated` | `simulated` \| `sandbox` \| `live` |
| `orders.payment.provider-url` | – | Required unless `simulated` |
| `orders.payment.api-key` | – | **Secret**, injected as `ORDERS_PAYMENT_APIKEY` by External Secrets. Required unless `simulated` |

Properties are validated, and unknown keys under `orders.*` are rejected. A typo, an invalid
value, or a missing secret in sandbox/live mode stops the app at startup, so it never takes traffic.

Run with an environment's overrides (needs a `demo-gitops` checkout next to this repo):

```sh
ORDERS_PAYMENT_APIKEY=dummy ./mvnw spring-boot:run -Dspring-boot.run.arguments=\
"--spring.config.additional-location=optional:file:../demo-gitops/envs/stage/application.yaml,optional:file:../demo-gitops/envs/stage/orders/application.yaml"
```

## Build & deploy

- `./mvnw verify` runs the tests.
- `./mvnw jib:dockerBuild -Dimage=demo-orders-service:local` builds a local image without a Dockerfile.
- On merge to `main`, [CI](.github/workflows/ci.yml) calls the platform's reusable workflow. It builds and pushes
  `ghcr.io/vehkiya/demo-orders-service:sha-<commit>`, attests it, and updates
  `envs/dev/orders` in `demo-gitops`. ArgoCD deploys it to dev. Stage and prod are promoted by PR in `demo-gitops`.
