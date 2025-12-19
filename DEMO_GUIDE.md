# Инструкция по демонстрации микросервисной архитектуры

## Обзор системы

Проект представляет собой микросервисную архитектуру на **Spring Boot 3**, включающую:

| Сервис | Порт | Описание |
|--------|------|----------|
| **API Gateway** | 8085 | Единая точка входа, маршрутизация, JWT валидация, rate limiting |
| **Auth Service** | 8081 | Аутентификация, регистрация, JWT токены |
| **User Service** | 8082 | Управление пользователями |
| **Sender Service** | 8083 | Отправка запросов пользователем |
| **Request Service** | 8084 | Асинхронная обработка запросов |

---

## Шаг 1: Запуск системы

```bash
cd "d:\Beta Final"
mvn clean package -DskipTests
docker-compose up -d
```

**Swagger UI:** http://localhost:8085/swagger-ui.html

---

## Шаг 2: Регистрация пользователя (Auth Service)

**Endpoint:** `POST /api/v1/auth/register`

```json
{
  "email": "student@example.com",
  "password": "password123",
  "firstName": "Иван",
  "lastName": "Петров"
}
```

**Ответ:** Система вернёт `accessToken` и `refreshToken`

---

## Шаг 3: Авторизация (Auth Service)

**Endpoint:** `POST /api/v1/auth/login`

```json
{
  "email": "student@example.com",
  "password": "password123"
}
```

**Скопируйте `accessToken` из ответа** — он понадобится для следующих запросов.

---

## Шаг 4: Авторизация в Swagger

1. Нажмите кнопку **"Authorize"** (справа вверху)
2. Введите: `Bearer <ваш_accessToken>`
3. Нажмите **"Authorize"**

---

## Шаг 5: Отправка запроса (Sender Service)

Переключитесь на **sender-service** в выпадающем списке.

**Endpoint:** `POST /api/v1/sender/send`

```json
{
  "category": 1,
  "message": "Мне нужна помощь с заказом"
}
```

**Ответ:**
```json
{
  "success": true,
  "message": "Запрос успешно отправлен",
  "data": {
    "requestId": "uuid-запроса",
    "status": "ACCEPTED",
    "message": "Ваш запрос принят и обрабатывается"
  }
}
```

---

## Шаг 6: Проверка статуса запроса

**Endpoint:** `GET /api/v1/sender/requests`

Сразу после отправки статус будет `PROCESSING`.

**Подождите 10 секунд** и повторите запрос — статус изменится на `PROCESSED` с ответным сообщением.

---

## Шаг 7: Демонстрация асинхронной обработки

**Логика Request Service:**
1. Получает запрос от Sender Service
2. Сохраняет в БД со статусом `PROCESSING`
3. **Ждёт 10 секунд** (имитация обработки)
4. Генерирует ответ в зависимости от категории
5. Обновляет статус на `PROCESSED`

**Категории ответов:**

| Категория | Тема |
|-----------|------|
| 1 | Техническая поддержка |
| 2 | Финансовые вопросы |
| 3 | Общие вопросы |
| 4 | Жалобы и предложения |
| 5 | Партнерство |
| 6 | Возврат товара |
| 7 | Доставка |
| 8 | Гарантия |
| 9 | Консультация |
| 10 | Другое |

---

## Архитектурные особенности

### 1. API Gateway Pattern
- Единая точка входа для всех запросов
- JWT валидация на уровне gateway
- Rate limiting (60 запросов/мин на IP)
- CORS настройка

### 2. JWT Authentication
- Access Token (15 мин) + Refresh Token (7 дней)
- Blacklist токенов в Redis при logout
- Stateless аутентификация

### 3. Асинхронная обработка (@Async)
- Request Service обрабатывает запросы в фоне
- Использует ThreadPoolTaskExecutor
- Не блокирует основной поток

### 4. Межсервисная коммуникация
- REST API через RestTemplate
- Sender → Request (отправка на обработку)
- Request → Sender (обновление статуса)

### 5. База данных
- Отдельная PostgreSQL для каждого сервиса (Database per Service pattern)
- Redis для кэширования и rate limiting

### 6. Контейнеризация
- Docker + Docker Compose
- Health checks для всех сервисов
- Изолированная сеть микросервисов

---

## Диаграмма потока запроса

```
Пользователь
    │
    ▼
┌─────────────────┐
│   API Gateway   │ ◄── JWT валидация, Rate Limiting
│    (8085)       │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Sender Service  │ ◄── Создаёт запрос, сохраняет в БД
│    (8083)       │
└────────┬────────┘
         │ HTTP POST
         ▼
┌─────────────────┐
│ Request Service │ ◄── Асинхронная обработка (10 сек)
│    (8084)       │
└────────┬────────┘
         │ HTTP PUT (обновление статуса)
         ▼
┌─────────────────┐
│ Sender Service  │ ◄── Обновляет статус запроса
│    (8083)       │
└─────────────────┘
```

---

## Структура проекта

```
Beta Final/
├── api-gateway/           # API Gateway (Spring Cloud Gateway)
├── auth-service/          # Сервис аутентификации
├── user-service/          # Сервис пользователей
├── sender-service/        # Сервис отправки запросов
├── request-service/       # Сервис обработки запросов
├── shared-lib/            # Общая библиотека (DTO, утилиты)
├── init-scripts/          # SQL скрипты инициализации БД
├── docker-compose.yml     # Конфигурация Docker
└── pom.xml               # Родительский Maven POM
```

---

## Технологии

- **Java 17**
- **Spring Boot 3.2.1**
- **Spring Cloud Gateway**
- **Spring Security + JWT**
- **Spring Data JPA + PostgreSQL**
- **Spring Data Redis**
- **Docker & Docker Compose**
- **MapStruct** (маппинг DTO)
- **Lombok** (уменьшение boilerplate)
- **SpringDoc OpenAPI** (Swagger UI)

---

## Полезные команды

```bash
# Статус контейнеров
docker-compose ps

# Логи сервиса
docker logs sender-service -f
docker logs request-service -f

# Перезапуск всех сервисов
docker-compose restart

# Перезапуск конкретного сервиса
docker-compose restart sender-service

# Остановка
docker-compose down

# Полная пересборка
docker-compose down
mvn clean package -DskipTests
docker-compose up -d --build
```

---

## Возможные вопросы на защите

**Q: Почему используется API Gateway?**
> A: Единая точка входа упрощает управление безопасностью, логированием, rate limiting. Клиенту не нужно знать адреса всех сервисов.

**Q: Зачем отдельная БД для каждого сервиса?**
> A: Database per Service pattern обеспечивает слабую связанность сервисов. Каждый сервис может использовать оптимальную для него СУБД.

**Q: Как работает асинхронная обработка?**
> A: @Async аннотация запускает метод в отдельном потоке из пула ThreadPoolTaskExecutor. Основной поток сразу возвращает ответ клиенту.

**Q: Что будет при падении Request Service?**
> A: Запрос сохранится в БД Sender Service со статусом PENDING. При восстановлении можно реализовать retry механизм.

**Q: Как масштабировать систему?**
> A: Можно запустить несколько экземпляров каждого сервиса за load balancer. Gateway поддерживает service discovery (Eureka, Consul).
