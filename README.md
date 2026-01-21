# kitchen



[![Version](https://img.shields.io/badge/version-1.2.1-blue.svg)](https://github.com/10almazoff10/kitchen)


**Kitchen** is a web application for **ordering food together** with features such as restaurant selection, adding dishes, payment marking, rating, and Telegram notifications.

## 🧩 Features

- **User registration and authentication**.
- **Order creation** with restaurant selection.
- **Adding dishes** to an order.
- **Marking items as added and paid**.
- **Rating dishes** from 1 to 5 stars.
- **User ratings**:
  - Spent the most money this month.
  - Ordered the most times this month.
  - Spent the most money of all time.
  - Ordered the most times of all time.
- **Telegram notifications**:
  - When a new order is created.
  - 5 minutes before the deadline.
  - When an order closes.
- **Pagination** of the order list.
- **Long-lived sessions** (one month).
- **Menu parsing** from a restaurant's website (e.g., `shampur-brn.ru`).

## 🛠️ Technologies

- **Java 17+**
- **Spring Boot 3.x**
- **Spring Security**
- **Spring Data JPA**
- **Thymeleaf**
- **PostgreSQL** (or another database)
- **Hibernate**
- **JSoup** (for menu parsing)
- **Bootstrap 5**
- **Redis** (optional, for sessions)
- **Telegram Bot API**

## 🚀 Installation and Running

### 1. Clone the repository

```bash
git clone https://github.com/10almazoff10/kitchen.git
cd kitchen
```

### 2. Environment Setup

Create a `.env` file or add variables to `application.yaml`:

```yaml
spring:
  application:
    name: kitchen
  datasource:
    url: jdbc:postgresql://localhost:5432/kitchen
    username: kitchen
    password: kitchenpass
    driver-class-name: org.postgresql.Driver
  session:
    store-type: jdbc
    jdbc:
      initialize-schema: always
    timeout: 2592000s
  # JPA
  jpa:
    hibernate.ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true

logging:
  level:
    ru.prokin.kitchen: DEBUG
    org.springframework.security: DEBUG
    org.springframework.web: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
    org.hibernate.type: TRACE
    org.springframework.orm.jpa: DEBUG
    org.springframework.transaction: DEBUG
    org.springframework: INFO
    com.zaxxer.hikari: DEBUG

  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"

telegram:
  bot:
    token: "<TelegramBot Token>"
  chat:
    id: <Group ID>

kitchen:
  base-url: https://YourAddress.com

app:
  headerName: KITCHEN
  version: @project.version@
```

### 3. Run the application

```bash
./mvnw spring-boot:run
```

or build a JAR:

```bash
./mvnw clean package
java -jar target/kitchen.jar
```

### 4. Telegram Bot Setup

1. Create a bot via [@BotFather](https://t.me/BotFather).
2. Get the token and chat ID.
3. Specify them in `application.yaml`.

---

## 🧠 Architecture

- **Controllers**: `MainController`, `AuthController`, `RatingController`, `TelegramController`.
- **Services**: `KitchenService`, `UserService`, `TelegramService`, `RatingService`, `MenuParserService`.
- **Repositories**: `OrderRepository`, `UserOrderRepository`, `UserRepository`, `RestaurantRepository`, `DishRepository`.
- **Entities**: `Order`, `UserOrder`, `User`, `Restaurant`, `Dish`.
- **Templates**: `Thymeleaf` (HTML templates).
- **Sessions**: via `Spring Session` (Redis/JDBC).

---

## 📁 Project Structure

```
src/
├── main/
│   ├── java/
│   │   └── ru/prokin/kitchen/
│   │       ├── controller/
│   │       ├── entity/
│   │       ├── repository/
│   │       ├── service/
│   │       └── config/
│   ├── resources/
│   │   ├── templates/
│   │   ├── static/
│   │   └── application.yaml
└── test/
```

---

## 🧪 Testing

Tests can be added using `JUnit`, `Mockito`, `Testcontainers`.

---

## 🧩 Possible Improvements

- Add **dish categories**.
- Implement **reviews for dishes**.
- Integrate with **restaurant APIs**.
- Add **browser notifications**.
- Mobile version (React Native / Flutter).

---

## 📄 License

MIT License. See the [LICENSE](./LICENSE) file.

---

## 👨‍💻 Author

Oleg Prokin
