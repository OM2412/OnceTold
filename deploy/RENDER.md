# OnceTold on Render

Live demo: https://oncetold-hrma.onrender.com

Deployed on 19 October 2026 using one Free Docker web service in Singapore and a dedicated Neon PostgreSQL project. The application starts with a fresh database; old Railway accounts and tickets were not imported.

## Layout

Java 21 / Spring Boot serves the existing React frontend and authenticated API publicly. The packaged FastAPI Cognee bridge listens only on `127.0.0.1:8000` inside the same container. Both processes stop if either exits.

The bridge originated from OM2412/OnceTold-Memory:
- main.py: `0cdd11421fc61e328f7a77ffaeafc17b687faf90`
- cognee_client.py: `1047958fc71cdb70c1bfacbc2f95431f4f6b20e5`
- requirements.txt: `e051bdea5aab8e36da7a62227ef742ff81ddbeb0`

The packaged client requires a dataset prefix. Keep it stable for a retained database; choose a new unique prefix when starting another empty database. This avoids reusing old customer-number datasets. Changes in the original memory repository do not sync automatically.

## Configuration

Use `render.yaml` on `main`. The blueprint selects Free compute and does not create a database. The current service is [oncetold](https://dashboard.render.com/web/srv-dbb1fmeq1p3s73dg9is0).

Store these values in Render's environment settings, never in the repository or chat:

| Key | Required value |
| --- | --- |
| SPRING_PROFILES_ACTIVE | `render` |
| SPRING_DATASOURCE_URL | `jdbc:postgresql://HOST/DATABASE?sslmode=require` |
| SPRING_DATASOURCE_USERNAME | Dedicated database role |
| SPRING_DATASOURCE_PASSWORD | Database password |
| APP_JWT_SECRET | Private signing secret, at least 32 characters |
| SPRING_AI_OPENAI_API_KEY | Valid Groq key |
| SPRING_AI_OPENAI_BASE_URL | `https://api.groq.com/openai/v1` |
| SPRING_AI_OPENAI_MODEL | `openai/gpt-oss-20b` |
| COGNEE_API_KEY | Valid Cognee key |
| COGNEE_BASE_URL | Existing tenant HTTPS URL |
| COGNEE_DATASET_PREFIX | Unique stable prefix for this database |

The cloud profile uses PostgreSQL, TLS, at most three pooled connections, schema update with DDL error fail-fast, and no H2 console. Local H2 development remains separate.

Java uses Render's `PORT`; only that port is public. Authenticated ticket-memory requests resolve the customer ID server-side after checking ticket access. Do not expose the bridge as a public unauthenticated API.

Memory writes are asynchronous. Chat recall and the memory panel have bounded 90-second reads. Provider failures may cause a reply without history; ingestion and recall may take time.

## Verification

The hosted Docker build runs Maven tests with a build-only placeholder API key and makes no model call during the test suite. Four tests passed on 19 October 2026: the application context plus owner, cross-customer and agent memory-access checks.

Fictional hosted QA verified customer and agent registration/login, ticket creation, Groq replies, agent resolution, Cognee memory retrieval and PostgreSQL persistence over redeploys. The `messages.image_data` column is PostgreSQL `TEXT`; schema errors now fail startup instead of leaving an apparently live app with a missing table.

## Operating limits

This is a public portfolio demonstration. Agent registration is open, so use fictional data only. No production-security or uptime guarantee is implied.

Render Free services sleep after inactivity, share monthly instance hours with other workspace services, and have ephemeral local files. Cold starts can take several minutes. Neon and external AI quotas also apply. No paid compute, paid database, disk or keepalive monitor was added.

References:
- https://render.com/docs/free
- https://render.com/docs/blueprint-spec
- https://console.groq.com/docs/deprecations
