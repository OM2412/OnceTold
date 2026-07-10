# OnceTold 🧠

**Tell us once. We'll remember.**

*Hangover Hackathon submission — WeMakeDevs × Cognee*

🔗 **Live Demo:** [oncetold-production.up.railway.app](https://oncetold-production.up.railway.app)

---

## The Problem

Every time a customer contacts support, they repeat themselves from scratch. No continuity exists between tickets — a returning customer with a known issue gets treated like a stranger. This costs customers time and forces agents to manually dig through old threads to reconstruct context, if those threads are even findable at all.

## The Solution

OnceTold is a customer support ticketing platform where every conversation is stored as memory in **Cognee** — an AI memory graph — scoped per customer. When a customer opens a new ticket, the system automatically recalls their relevant history. When a ticket is resolved, that resolution is written back into permanent memory, so the *next* interaction — even months later — starts with full context instead of from zero.

## Real World Example

Sarah contacts support in March about a double billing charge. The issue gets resolved, and the resolution is stored in Cognee's memory graph for her account.

In June, Sarah contacts support again about her account balance. Without Sarah mentioning March at all, the bot immediately references her prior billing issue and its resolution — saving Sarah from repeating herself and giving the agent full context instantly.

## The Memory Loop

| Step | Endpoint | When it happens |
|---|---|---|
| **Remember** | `POST /remember` | Every ticket message and every resolution is ingested into that customer's Cognee dataset |
| **Recall** | `POST /recall` | On each new customer message, prior history is queried and injected into the AI's reply prompt |
| **Improve** | `POST /remember` (resolution) | Ticket resolved → the agent's resolution is written back, permanently strengthening that customer's graph |
| **Forget** | `POST /forget` | Available on-demand to delete a customer's dataset (not currently scheduled/automated) |

## Architecture

OnceTold runs as **two deployed services**:

1. **Java Spring Boot backend** — serves the React frontend directly from the same app, handles auth, tickets, and orchestrates the AI bot reply flow
2. **Python FastAPI microservice** — bridges the backend to **Cognee Cloud's REST API**, keeping all memory-graph logic isolated from the main app

We deliberately chose **Cognee Cloud** over self-hosting the local `cognee` Python library — this removes the need to manage any persistent storage or infrastructure for the memory layer itself; Cognee Cloud handles that entirely.

### Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 3.x, Spring Security + JWT |
| AI Orchestration | Spring AI (`ChatClient`) |
| Memory Bridge | Python 3.11, FastAPI, `httpx` → Cognee Cloud REST API |
| LLM Provider | Groq (`llama-3.1-8b-instant`) via OpenAI-compatible API |
| Database | H2 — file-based on a persistent volume in production (Railway), in-memory for local dev |
| Frontend | React (single-page, served from the Spring Boot backend, in-browser Babel) |
| Deployment | Railway (two services, one project) |

## Setup Instructions

### Prerequisites
- Java 21+
- Python 3.11+
- Groq API key (free at [console.groq.com](https://console.groq.com))
- Cognee Cloud API key + tenant URL

### 1. Clone both repos
```bash
git clone https://github.com/OM2412/OnceTold.git
git clone https://github.com/OM2412/OnceTold-Memory.git
```

### 2. Start the memory service (Python)
```bash
cd OnceTold-Memory
pip install -r requirements.txt
# Set COGNEE_API_KEY and COGNEE_BASE_URL as environment variables
uvicorn main:app --reload --port 8000
```

### 3. Start the Java backend (also serves the frontend)
```bash
cd OnceTold
./mvnw spring-boot:run
```
The full app — frontend included — is now available at `http://localhost:8080`.

## Demo Flow

1. Register as **CUSTOMER** → create a ticket about a billing issue
2. Chat with the AI bot → it responds using recalled context (or greets normally if there's no history yet)
3. Log in as **AGENT** → view the ticket queue → resolve the ticket with a summary
4. Log back in as **CUSTOMER** → open a second ticket about a related issue
5. The bot references the prior context unprompted — this is OnceTold's memory loop working end-to-end

## What We Learned / Engineering Challenges

Getting this reliably deployed surfaced some real, non-obvious bugs worth mentioning:
- A silent, unconfigured default HTTP timeout was cutting off memory calls at exactly 10 seconds with no visible error — only caught by inspecting actual network timing logs, not by reading error messages.
- H2's default in-memory mode wiped all data on every Railway restart — fixed with a persistent volume and environment-driven datasource configuration.
- A UTF-8 BOM character silently broke the Railway build despite compiling fine locally.
- Balancing memory recall depth against response latency — Cognee's graph queries can take up to a minute for complex context, which required separating "fire-and-forget" memory writes from the one blocking recall call needed before the bot can reply.

## AI Disclosure

I used claude for suggetions like improvements in my project.

## Credits

- **Cognee** — AI memory graph engine
- **Groq** — LLM inference (free tier)
- **Railway** — deployment platform
