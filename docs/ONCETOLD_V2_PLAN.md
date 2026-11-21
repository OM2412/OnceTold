# OnceTold V2 — secure customer-support workspace

## Product positioning

OnceTold is a ticket-based support workspace with durable conversation history,
human escalation, and optional AI-assisted replies. Its value proposition is
continuity across support conversations, not a generic chatbot.

The public portfolio demo must be trustworthy when the AI memory provider is
unavailable or the Java service is sleeping. A good demo must not depend on
spending money or keeping a laptop running.

## Release gates (required before changing public resume links)

1. **P0 identity isolation**: public signup creates only customers; staff roles
   require explicit provisioning and `APP_AGENT_ALLOWLIST`; old staff JWTs
   become unusable when access is revoked.
2. **P0 memory isolation**: provider records without the exact customer dataset
   metadata must not reach the chat prompt or memory panel. Never fall back to
   a global result list.
3. **P0 API privacy**: no API keys, provider tenancy URLs, conversation content,
   or raw exceptions in user-facing errors or application logs.
4. **P1 honest degradation**: tickets and messages work if Cognee is rate-limited;
   AI cannot invent product navigation or claim that missing provider history
   is available.
5. **P1 verification**: Java security tests, Python dataset-isolation tests,
   browser smoke tests as customer and trusted agent, and cold-start latency
   checks must pass before production rollout.

## V2 delivery stages

| Stage | What changes | Evidence / acceptance criteria |
|---|---|---|
| 1. Foundation | Tenant isolation, role provisioning, safer AI prompt, provider quota fallback | Attempts to register an agent or fetch another customer's ticket fail |
| 2. Product UI | Consistent navy/teal design system; clear tickets, context, and human handoff | Usable on mobile; clear empty/loading/error states; keyboard access |
| 3. Workflow | Ticket priority, categories, assignee, searchable inbox, audit log, conversation timeline | Agent can triage with a reproducible activity trail |
| 4. AI quality | Source-grounded suggestions, confidence/uncertainty, human approval for risky actions | No invented navigation or fabricated order/refund claims in test set |
| 5. Analytics | Response time, resolution rate, reopened tickets, provider availability | Metrics backed by DB queries; no synthetic dashboard figures |
| 6. Reliability | Lightweight health endpoint, bounded request latency, CI tests, error budget | Cold start measured separately from warm requests; failed dependency is isolated |

## Recommended architecture for ₹0/month

- **Frontend:** static Cloudflare Pages (`oncetold.pages.dev`), cached at the edge.
- **Backend:** existing Render free Spring Boot service until a sustainable
  free always-on alternative with adequate resources is verified. Expect
  sleeping instances; frequent synthetic pings are not a reliability design.
- **Database:** use a persistent managed free database only if its quota,
  suspension policy, and backup requirements are understood. Do not rely on
  an ephemeral container filesystem for customer records.
- **Memory:** Cognee is optional and currently quota-constrained. A future
  upgrade should store auditable per-customer summaries in the main database
  and use a retrieval provider only for richer enrichment.
- **Secrets:** `GROQ_API_KEY`, `COGNEE_API_KEY`, database credentials and
  `APP_AGENT_ALLOWLIST` are backend environment variables; never bundle them
  into the static frontend.

## Honest portfolio demo requirement

Show a useful page instantly on Cloudflare even when the Render API is cold.
Explain that the live support workspace can take time to connect on the free
backend; this limitation is not solved by another email or an always-on ping.
Use a short product walkthrough/demo video as a fallback for recruiters.

## Pending deployment configuration

`APP_AGENT_ALLOWLIST` must contain only emails of **trusted existing AGENT
records** in the database, comma-separated. With an empty list all AGENT
accounts are intentionally denied; CUSTOMER accounts still work.

### Promoting a trusted existing customer to an agent

There is deliberately no public role-switch endpoint. For a one-time operator
provisioning deployment:

1. Verify the user's identity and ownership of the email address.
2. Add that email to `APP_AGENT_ALLOWLIST`.
3. Set `APP_AGENT_BOOTSTRAP_ENABLED=true` and
   `APP_AGENT_BOOTSTRAP_EMAIL=<verified-email>` on the server.
4. Deploy, check successful startup, then **remove the two bootstrap variables**
   and deploy again. The account's password is unchanged.
5. Test that this account sees agent tickets and an unrelated customer account
   cannot see them. Audit existing AGENT accounts created before the fix.

Bootstrap is off by default and refuses an email outside the allowlist. It is
not an admin self-service UI and should never be left enabled permanently.

### Free-tier resource discipline

The frontend now suppresses ticket polling in hidden tabs, refreshes when the
tab returns to the foreground, and uses a 30-second list / 12-second selected
conversation cadence. This reduces unnecessary backend requests, but does not
remove the Render free plan's cold-start delay or guarantee uptime.

Never add an email merely because it was previously registered as an agent:
public registration used to permit anyone to acquire that role.

This plan is a staged implementation roadmap. Stages 3–6 are **not yet built**;
do not describe them as shipped resume features.
