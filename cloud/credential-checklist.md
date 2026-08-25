## Findings

- [ ] **F-01 — Critical — Email/SMTP Credential**
  - File: [backend/src/main/resources/application.properties](../backend/src/main/resources/application.properties#L32-L33) (lines 32-33)
  - Finding: Real Gmail account + live app password hardcoded in tracked source file (`spring.mail.username` / `spring.mail.password`)
  - Redacted value: `eventapp.cluj.test@gmail.com` / `rzpf***********`
  - Recommendation: Remove the plaintext value from the file; load via environment variable (e.g. `${MAIL_PASSWORD}`) or a secrets manager (Azure Key Vault / AWS Secrets Manager).

- [ ] **F-02 — Critical — Duplicated Secret (Build Artifact)**
  - File: [backend/target/classes/application.properties](../backend/target/classes/application.properties#L32-L33) (lines 32-33)
  - Finding: Same Gmail credential duplicated in compiled/build output on disk (`target/` is git-ignored but the plaintext secret is still present locally and in any build artifact/CI cache)
  - Redacted value: `eventapp.cluj.test@gmail.com` / `rzpf***********`

- [ ] **F-03 — High — JWT Signing Secret**
  - File: [JwtTokenProvider.java](../backend/src/main/java/com/cluj1/eventapp/security/JwtTokenProvider.java#L16) (line 16)
  - Finding: Hardcoded default fallback value for `jwt.secret` used to sign/verify all auth tokens if the property/env var is not supplied. Anyone with source access can forge valid JWTs against any deployment still using the default.
  - Redacted value: `SuperSecretKeyThatIsAtLeast32BytesLongForHS256Algorithm`
  - Recommendation: Remove the hardcoded default. Require `jwt.secret` via environment variable / secrets manager and fail application startup if it is missing (no fallback).

- [ ] **F-04 — Medium — Database Credential**
  - File: [backend/src/main/resources/application.properties](../backend/src/main/resources/application.properties#L6-L8) (lines 6-8)
  - Finding: Plaintext Postgres datasource username/password committed to source (`spring.datasource.url` / `username` / `password`)
  - Redacted value: `postgres` / `postgres`
  - Recommendation: Externalize via environment variables (`SPRING_DATASOURCE_USERNAME`/`PASSWORD`) or a secrets manager.

- [ ] **F-05 — Medium — Duplicated Credential (Build Artifact)**
  - File: [backend/target/classes/application.properties](../backend/target/classes/application.properties#L6-L8) (lines 6-8)
  - Finding: Same Postgres credential duplicated in compiled build output
  - Redacted value: `postgres` / `postgres`

- [ ] **F-06 — Informational — .gitignore Coverage Gap**
  - File: [.gitignore](../.gitignore#L65-L71) (lines 65-71)
  - Finding: `.gitignore` already excludes `.env*` and `application-local.properties`/`.yml` (good practice), but the primary `application.properties` containing the real mail and DB credentials (F-01/F-04) is NOT excluded and IS committed to the repository history.
  - Redacted value: N/A
  - Recommendation: Move real secrets out of `application.properties` into `application-local.properties` (already git-ignored) or environment variables, so the tracked file only contains placeholders/defaults.

- [ ] **F-09 — Informational — Frontend Hardcoded Endpoints**
  - File: `frontend/src/app/core/services/*.ts`, [frontend/proxy.conf.json](../frontend/proxy.conf.json)
  - Finding: Backend API base URLs are hardcoded to `http://localhost:8080` across services (auth, event, registration, user). Not a secret, but not environment-configurable, which is a maintainability/config concern rather than a credential leak.
  - Redacted value: `http://localhost:8080`
  - Recommendation: Optional: move to Angular `environment.ts` / `environment.prod.ts` files for per-environment configuration. No credential exposure.
  - Status: Accepted / No Action
