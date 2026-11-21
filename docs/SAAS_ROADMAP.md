# OnceTold — from portfolio prototype to pilot-ready SaaS

**Status:** roadmap, not shipped capabilities. Do not market the current public app as production-grade.

## Product thesis

Avoid positioning OnceTold as another generic AI support chatbot. A more testable wedge is **continuity and evidence-backed handoff for small support teams**: a customer should not need to repeat facts, and an agent should know which earlier statement or resolution supports a suggested next step.

Before building paid features, interview at least 10 operators in one vertical (e.g., small ecommerce shops with repeated delivery/returns tickets). Validate repeated-context workload and willingness to pay rather than assuming either.

## Release gates

1. **Tenant model and authorization:** introduce organizations, memberships, role scopes, and per-tenant isolation in SQL and memory namespaces. Test IDOR, role changes and invitation revocation. Current per-customer isolation is not sufficient for multiple paying businesses.
2. **Agent workflow:** assignment, priority, categories, SLA clock, internal notes, explicit escalation, immutable audit events and reopen flow. Ticket writes must be idempotent or protected from duplicate submissions.
3. **Memory trust:** show sourced, timestamped facts with customer/agent corrections and deletion controls. Never auto-run high-impact actions from AI output. Handle unavailable Cognee/Groq as an ordinary state, not an outage of ticketing.
4. **Reliability:** move from sleeping free runtime to a funded service before promising business uptime. Add health metrics, backup/restore exercises, migration strategy, rate limits and alerting. Free Render cannot be sold as an always-on SLA.
5. **Privacy/security:** data retention and deletion policies, access audit, secret rotation, encryption at rest/in transit, incident response runbook, file-upload malware/size checks and privacy notices. Obtain legal review before processing real customer records at scale.
6. **Commercial operations:** tenant onboarding, owner/admin console, usage metering and quotas; only then evaluate a payment provider and subscription plans. Never activate billing against fictional demo data.

## Customer discovery experiment

| Hypothesis | Test | Pass signal |
|---|---|---|
| Repeated context costs time | Observe 10 support escalations with consent | At least 5 show repeated explanation or lookup |
| Source-backed recap is useful | Show a manual brief of a real anonymized case | 3 pilot users say they would reuse it weekly |
| A small team will pay | Offer a 2-week assisted pilot | At least one operator requests a paid continuation |

These are suggested decision thresholds, not measured business traction.

## Demo separation

`/demo.html` is a static simulation with fictional customers and tickets; it deliberately has **no calls to production APIs and no persistence**. It can demonstrate interaction speed and UX, but cannot establish security, uptime, or business value. The live authenticated product must be verified separately, using test accounts and a documented acceptance suite.

## Engineering implementation order

- Phase A: reviewer-friendly demo, secure staff provisioning and customer/agent smoke tests.
- Phase B: organization schema, tenant-scoped repositories, migration tests and immutable audit events.
- Phase C: triage/assignment/SLA workflow, source-backed memory corrections and role-specific UX.
- Phase D: metrics, backup/restore, load testing, monitored deployment, pilot onboarding.
- Phase E: pricing experiment and billing **after** customer validation and sustainable hosting.

## Business constraints

The personal portfolio can stay at ₹0/month, but a real always-on SaaS cannot honestly promise an uptime SLA, data durability and AI inference capacity on expiring free quotas. Separate portfolio spend from pilot/customer operations and let a validated paying customer fund infrastructure.
