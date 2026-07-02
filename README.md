# OnceTold

OnceTold is a Spring Boot support-ticket backend with JWT authentication, role-based access, H2 persistence, and AI-assisted ticket replies.

## Structure

- `src/main/java/com/oncetold/oncetold/controller` - REST endpoints
- `src/main/java/com/oncetold/oncetold/service` - application logic
- `src/main/java/com/oncetold/oncetold/entity` - JPA entities and enums
- `src/main/java/com/oncetold/oncetold/dto` - request and response models
- `src/main/java/com/oncetold/oncetold/security` - JWT and Spring Security setup
- `src/main/java/com/oncetold/oncetold/config` - application configuration

## Features

- User registration and login with JWT
- Customer and agent roles
- Ticket creation, messaging, and resolution
- H2 in-memory database for local development
- AI-powered replies through Spring AI

## Run Locally

1. Set the required environment variables.
2. Start the app with Maven:

```bash
mvn spring-boot:run
```

## Environment Variables

Create a local `.env` file from `.env.example` and provide your own values for:

- `APP_JWT_SECRET`
- `SPRING_AI_OPENAI_API_KEY`
- `SPRING_AI_OPENAI_BASE_URL`
- `SPRING_AI_OPENAI_MODEL`

## Notes

- H2 console is enabled at `/h2-console` for local development.
- The repository keeps runtime secrets out of source control.