# OnceTold 🧠

**Tell us once. We'll remember.**

*Hangover Hackathon submission — WeMakeDevs × Cognee*

**[Open the live demo](https://oncetold-hrma.onrender.com)**

OnceTold is a customer-support prototype that recalls customer-scoped history before an AI reply and writes ticket resolutions back to a Cognee memory graph.

## Try it

Use fictional information only. Register a customer, create a ticket, and send a message. Register a separate support-agent account to review and resolve the ticket with a summary. Return as the same customer and open a related ticket to explore recalled history.

The public demo uses Render Free, Neon PostgreSQL, Groq and Cognee Cloud. A sleeping instance can take several minutes to start, and memory queries can be slow. Availability depends on the providers' free allowances and API quotas. This is a portfolio demonstration, not a production support system: the demo permits agent registration.

## How the memory loop works

| Operation | Internal bridge endpoint | Purpose |
| --- | --- | --- |
| Remember | `POST /remember` | Add customer ticket messages to that customer's dataset |
| Recall | `GET /recall` | Retrieve history for the AI prompt and memory panel |
| Improve | `POST /improve` | Add an agent's resolution to the customer's graph |
| Forget | `POST /forget` | Internal capability to remove a dataset; not scheduled or exposed as a user-facing control |

Writes run asynchronously after application data is saved. Recall is synchronous with a bounded timeout; if the memory provider fails or times out, the bot can respond without recalled history. Graph ingestion is eventually consistent, so a new fact may not appear immediately.

The browser accesses memory through authenticated `GET /api/tickets/{id}/memory`. The backend checks ticket access and uses the ticket owner's persisted ID. A customer cannot select another customer's dataset by changing a browser-supplied customer ID.

## Architecture

One Render Docker web service runs two processes:

- Java 21 / Spring Boot serves the React interface, authentication, tickets and AI orchestration.
- A packaged FastAPI bridge listens only on `127.0.0.1:8000` and calls Cognee Cloud.

Only the Java HTTP port is public. Application records live in a dedicated Neon PostgreSQL database. A stable, deployment-specific Cognee dataset prefix separates this fresh database's customer IDs from the previous installation.

| Layer | Technology |
| --- | --- |
| Backend | Java 21, Spring Boot 4.0.7, Spring Security, JWT |
| AI orchestration | Spring AI 2.0.0 `ChatClient` |
| Model | Groq OpenAI-compatible API, `openai/gpt-oss-20b` |
| Memory | Python, FastAPI, httpx, Cognee Cloud |
| Database | Neon PostgreSQL in the Render profile; H2 for local development |
| Frontend | React SPA served by Spring Boot, in-browser Babel |
| Hosting | Render Free Docker service |

## Local setup

Prerequisites: Java 21, Python 3, a Groq API key, and existing Cognee Cloud configuration.

```bash
git clone https://github.com/OM2412/OnceTold.git
cd OnceTold
python3 -m venv .venv
. .venv/bin/activate
pip install -r deploy/memory/requirements.txt
```

Set `COGNEE_API_KEY`, `COGNEE_BASE_URL` and a unique `COGNEE_DATASET_PREFIX` in your shell, then start the packaged bridge:

```bash
uvicorn main:app --app-dir deploy/memory --host 127.0.0.1 --port 8000
```

In another terminal, set `SPRING_AI_OPENAI_API_KEY` to your Groq key and set a private `APP_JWT_SECRET` of at least 32 characters, then run:

```bash
sh mvnw spring-boot:run
```

Open `http://localhost:8080`. The default local H2 database is in memory and resets when the application restarts. Do not use the local defaults for a public deployment.

## Render deployment

See [deploy/RENDER.md](deploy/RENDER.md) and [render.yaml](render.yaml) for the cloud profile, required environment variables and verification notes. Secrets belong in Render's environment settings, never in source control.

The Render deployment uses PostgreSQL with TLS, a small connection pool, schema updates that fail startup on DDL errors, and a disabled H2 console. Both container processes stop if either exits.

The previous Railway trial expired. This deployment starts with a new database; old Railway accounts and tickets were **not imported**.

## Validation

On 19 October 2026, the hosted Docker build passed four backend tests: the application context and three ticket-memory access tests covering owner access, cross-customer rejection and agent lookup of the ticket owner's memory.

Hosted checks used fictional customer/agent accounts to exercise registration, login, ticket creation, an actual Groq reply, resolution, Cognee history retrieval and persistence across redeploys. These checks validate the tested scenario, not general response accuracy or an uptime guarantee.

## Engineering notes

- PostgreSQL requires `TEXT` instead of the original H2-specific `CLOB` column declaration for image data.
- A fixed eight-second recall budget dropped useful cloud memory; the Render profile now allows a bounded 90-second read.
- The retired Groq model was replaced with its documented successor.
- The old browser-to-Railway memory URL was replaced with an authenticated backend route.
- The bridge under `deploy/memory` originated in [OnceTold-Memory](https://github.com/OM2412/OnceTold-Memory); subsequent packaged changes are maintained here deliberately.

## AI disclosure

I used Claude for project suggestions and improvements. ChatGPT assisted with the Render/Neon migration, troubleshooting, validation and deployment documentation.

## Credits

Cognee — memory graphs · Groq — model inference · Render — application hosting · Neon — PostgreSQL
