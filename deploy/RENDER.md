# OnceTold migration to Render

Prepared for a free portfolio demonstration. Render successfully built the Docker image on 19 October 2026 at commit
ab187cdfdc0ea1573328dcff46d8757d86365246. Runtime deployment remains blocked
because PostgreSQL connection settings and the application secrets are missing.
Live PostgreSQL/Cognee workflows have not been validated.

## Layout

One Docker web service runs Java 21/Spring Boot (including the existing frontend)
and the small Python Cognee bridge. Only Java listens publicly. The bridge listens
on 127.0.0.1:8000; it is not deployed as a public unauthenticated memory API.
Both processes stop if either exits.

The bridge files under deploy/memory originate from OM2412/OnceTold-Memory:
main.py blob 0cdd11421fc61e328f7a77ffaeafc17b687faf90,
cognee_client.py blob 1047958fc71cdb70c1bfacbc2f95431f4f6b20e5,
requirements.txt blob e051bdea5aab8e36da7a62227ef742ff81ddbeb0.
The packaged client requires an explicit new dataset prefix to keep a fresh
database's user IDs separate from the old Railway installation's memories.
Future bridge changes need to be copied deliberately; this is not automatic sync.

## Before deployment

1. Connect the Render account and check remaining shared free instance hours.
2. Use a dedicated empty PostgreSQL database, such as a suitable existing Neon
   project database. Do not point schema updates at an unrelated application's DB.
   This blueprint does not create a database or purchase anything.
3. Supply database credentials and existing Groq/Cognee configuration through
   Render's secret environment fields, never GitHub source or chat messages.
4. Use a new, unique COGNEE_DATASET_PREFIX for this new database, for example
   oncetold_render_20261019_customer_. Keep it stable across redeploys while
   retaining that database. Use a new prefix if creating another empty database.
5. Supply APP_JWT_SECRET with at least 32 characters; do not use the local default.

Required environment fields:

| Name | Value |
| --- | --- |
| SPRING_DATASOURCE_URL | jdbc:postgresql://HOST/DB?sslmode=require |
| SPRING_DATASOURCE_USERNAME | Dedicated database role |
| SPRING_DATASOURCE_PASSWORD | Dedicated database password |
| APP_JWT_SECRET | Private signing secret |
| SPRING_AI_OPENAI_API_KEY | Existing valid Groq key |
| COGNEE_API_KEY | Existing valid Cognee key |
| COGNEE_BASE_URL | Actual existing Cognee tenant HTTPS URL |
| COGNEE_DATASET_PREFIX | Unique prefix for this new database |

The cloud profile uses PostgreSQL, a three-connection pool, disabled H2 console
and schema update rather than create-drop. Default local H2 development stays
unchanged. Existing Railway accounts/tickets are not automatically imported.
Recover the H2 backup first if old records must be retained; this deployment
does not claim to have migrated them.

## Deploy and verify

Create the free Docker service using render.yaml and its listed branch. Review
the Free plan before creation. Do not add paid disks or automatic paid upgrades.
After merge, update the blueprint branch to main deliberately.

The local environment lacks Java 21/Maven/Docker, but Render completed Maven
packaging and Docker image creation successfully on 19 October 2026. The Docker
build skips the existing context-load test, which requires application
configuration. Do not interpret packaging success as a passed backend test suite.

Created service: https://dashboard.render.com/web/srv-dbb1fmeq1p3s73dg9is0
Deployment dep-dbb1fneq1p3s73dg9mr0 ended update_failed: the startup guard
reported SPRING_DATASOURCE_URL missing. The assigned public URL is not a working
demo yet. No paid compute or database was provisioned.

Before changing the portfolio from Hosting paused:
- Confirm the container builds and the root homepage opens.
- Register fictional demo customers and an agent; confirm ticket creation,
  chat, agent resolution and access boundaries.
- Verify an actual Cognee remember/recall cycle and a real Groq reply. A 200
  response from the bridge alone is insufficient: its status may report a
  skipped Cognee call.
- Redeploy and verify PostgreSQL records remain and the dataset prefix is stable.
- Confirm only the Java port is public and the memory service has no public URL.

## Free limits

Render free web services sleep after 15 minutes of inactivity and share 750
instance hours per workspace each month. The existing ReturnTrail service shares
that budget. Do not add another keep-alive monitor by default. Local service files
are ephemeral. Free Render Postgres expires after 30 days, so it is not selected
as the durable database for this setup. Account usage and external AI quotas
still need to be checked.

Sources: https://render.com/docs/free and https://render.com/docs/blueprint-spec
