# Contributing to Kitchen

Thank you for your interest in contributing to **Kitchen**! We welcome bug reports, feature requests, documentation improvements, and pull requests.

---

## 📋 Code of Conduct

Please review and adhere to our [Code of Conduct](./CODE_OF_CONDUCT.md) in all project interactions.

---

## 🛠️ Development Setup

### Prerequisites
- **Java 17+** (JDK 17 or 21)
- **Maven 3.9+**
- **Node.js 20+** and **npm**
- **Docker & Docker Compose** (optional, recommended for PostgreSQL)

### 1. Database
Run PostgreSQL locally or start via Docker:
```bash
docker compose up -d db
```

### 2. Backend
Run the Spring Boot application:
```bash
mvn spring-boot:run
```
Run tests:
```bash
mvn test -q
```

### 3. Frontend
Navigate to the frontend directory:
```bash
cd frontend
npm install
npm run dev
```
Run type check and build verification:
```bash
npx vue-tsc --noEmit
npm run build
```

---

## 🔀 Workflow & Pull Requests

1. **Fork** the repository and create your branch from `main`:
   ```bash
   git checkout -b feature/my-new-feature
   # or
   git checkout -b fix/issue-description
   ```
2. **Make your changes**:
   - Backend logic belongs in services (`@Service`), keep controllers thin.
   - Frontend components use Vue 3 `<script setup lang="ts">` and Tailwind CSS.
3. **Verify tests & build**:
   - Ensure `mvn test -q` passes without errors.
   - Ensure `npx vue-tsc --noEmit` and `npm run build` pass in `frontend/`.
4. **Commit your changes**:
   - Write clear, concise commit messages.
5. **Open a Pull Request**:
   - Fill out the PR template completely and explain the rationale for the change.

---

## 🐛 Reporting Bugs & Suggesting Features

- Check existing [Issues](https://github.com/10almazoff10/kitchen/issues) before opening a new one.
- Use our [Bug Report Template](.github/ISSUE_TEMPLATE/bug_report.md) or [Feature Request Template](.github/ISSUE_TEMPLATE/feature_request.md).

Thank you for helping make Kitchen better!
