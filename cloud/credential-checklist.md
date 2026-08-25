## Findings

- [FIXED] **F-01 — Critical — Email/SMTP Credential**
  - File: [backend/src/main/resources/application.properties](../backend/src/main/resources/application.properties#L32-L33) (lines 32-33)
  - Finding: Real Gmail account + live app password hardcoded in tracked source file (`spring.mail.username` / `spring.mail.password`)
  - Redacted value: `eventapp.cluj.test@gmail.com` / `rzpf***********`
  - Recommendation: Remove the plaintext value from the file; load via environment variable (e.g. `${MAIL_PASSWORD}`) or a secrets manager (Azure Key Vault / AWS Secrets Manager).

- [FIXED] **F-02 — High — JWT Signing Secret**
  - File: [JwtTokenProvider.java](../backend/src/main/java/com/cluj1/eventapp/security/JwtTokenProvider.java#L16) (line 16)
  - Finding: Hardcoded default fallback value for `jwt.secret` used to sign/verify all auth tokens if the property/env var is not supplied. Anyone with source access can forge valid JWTs against any deployment still using the default.
  - Redacted value: `SuperSecretKeyThatIsAtLeast32BytesLongForHS256Algorithm`
  - Recommendation: Remove the hardcoded default. Require `jwt.secret` via environment variable / secrets manager and fail application startup if it is missing (no fallback).

- [FIXED] **F-03 — Medium — Database Credential**
  - File: [backend/src/main/resources/application.properties](../backend/src/main/resources/application.properties#L6-L8) (lines 6-8)
  - Finding: Plaintext Postgres datasource username/password committed to source (`spring.datasource.url` / `username` / `password`)
  - Redacted value: `postgres` / `postgres`
  - Recommendation: Externalize via environment variables (`SPRING_DATASOURCE_USERNAME`/`PASSWORD`) or a secrets manager.

- [FIXED] **F-04 — Informational — Frontend Hardcoded Endpoints**
  - File: `frontend/src/app/core/services/*.ts`, [frontend/proxy.conf.json](../frontend/proxy.conf.json)
  - Finding: Backend API base URLs are hardcoded to `http://localhost:8080` across services (auth, event, registration, user). Not a secret, but not environment-configurable, which is a maintainability/config concern rather than a credential leak.
  - Redacted value: `http://localhost:8080`
  - Recommendation: move to Angular `environment.ts` / `environment.prod.ts` files for per-environment configuration. No credential exposure.
