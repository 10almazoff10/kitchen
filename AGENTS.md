# AGENTS.md — Kitchen Architecture & Development Guidelines

## 📌 Project Overview
**Kitchen** is a collaborative food ordering web application with restaurant management, dish tracking, payment marking, user/dish ratings, and Telegram notifications.

---

## 🛠️ Tech Stack
- **Backend:** Java 17, Spring Boot 3.x, Spring Data JPA, Hibernate, Spring Security, PostgreSQL, JSoup (menu parsing), Telegram Bot API.
- **Frontend:** Vue 3 (Composition API, `<script setup lang="ts">`), TypeScript, Vite, Pinia, Vue Router, Tailwind CSS v4, Lucide icons, Axios.
- **Infrastructure:** Docker, Docker Compose, Nginx (for frontend in prod).

---

## 📂 Architecture & Directory Layout

### Backend (`src/main/java/ru/prokin/kitchen/`)
- `entity/`: Core JPA models (`User`, `Order`, `UserOrder`, `Restaurant`, `Rating`).
- `repository/`: Spring Data JPA interfaces for DB access.
- `service/`: Business logic:
  - `OrderService`, `KitchenService`: Order lifecycle and business rules.
  - `UserOrderItemService`: Individual user order items and payment status.
  - `TelegramService`, `DeadlineNotificationService`: Telegram alerts and scheduled deadline notifications.
  - `MenuParserService`: Scrapes restaurant menus via JSoup.
  - `RatingService`, `RestaurantService`, `UserService`: Domain services.
- `controller/`: REST endpoints (`AuthController`, `OrderController`, `RestaurantController`, `UserOrderItemController`, `RatingController`, `MainController`).
- `dto/`: Request/Response payload models.
- `config/` & `security/`: Spring Security, session config, beans.
- `src/main/resources/`: `application.yaml` and static/template resources.

### Frontend (`frontend/src/`)
- `views/`: Primary pages (`HomeView.vue`, `CreateOrderView.vue`, `OrderDetailView.vue`, `RestaurantsView.vue`, `RatingsView.vue`, `LoginView.vue`).
- `components/`: Reusable UI widgets and dialogs.
- `stores/`: Pinia state management (`orders.ts`, `auth.ts`, `toast.ts`).
- `api/client.ts`: Axios instance with base URL and interceptors.
- `router/`: Route definitions and navigation guards.
- `types/`: TypeScript definitions mirroring DTOs and frontend entities.

---

## ⚡ Context & Token Conservation Rules (For AI Agents)

1. **Targeted Inspections (Do NOT read entire files unconditionally):**
   - Use line ranges when reading files (`StartLine`, `EndLine`).
   - Use `grep_search` to pinpoint exact methods/classes instead of inspecting entire directories.
   - Identify the exact layer first (e.g. Entity vs DTO vs Service) before opening source code.

2. **Delegate Exploration to Subagents:**
   - For multi-file architectural investigations, invoke the `research` subagent (`invoke_subagent`).
   - The subagent conducts broad searches in an isolated context and returns only a compact summary, preventing context bloat in the main dialog.

3. **Quiet Commands (Suppress Verbose Output):**
   - Maven tests: `mvn test -Dtest=<TestClass> -q` (avoid long ASCII banners and stack traces).
   - Frontend typecheck: `cd frontend && npx vue-tsc --noEmit` (fast, outputs only errors).
   - Frontend build: `cd frontend && npm run build`.
   - Never print raw server/application logs without filtering (`grep -E "ERROR|FAILURE"` or `tail -n 30`).

4. **Code Conventions:**
   - **Backend:** Maintain thin controllers, place logic in `@Service`, use constructor injection, validate inputs via DTOs.
   - **Frontend:** Vue 3 `<script setup lang="ts">`, centralize API calls and server state in Pinia stores, style using Tailwind CSS utilities.
