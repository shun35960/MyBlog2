# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## YOU MUST
- すべての回答は日本語で行って下さい
- ユーザーのアクションが必要な場合は通知して下さい
- 複数の手順を要する作業は、必要に応じてタスクに分割してから実行して下さい
  - 3ステップ以上を要する作業、または複数ファイルにまたがる変更が対象
  - 着手前にタスク一覧を作成し、進捗が分かるよう順次ステータスを更新して下さい
  - 完全に完了したタスクのみ完了扱いとし、失敗・未検証のものは完了にしないで下さい
  - 作業中に新たな作業が判明した場合はタスクとして追加して下さい

## Project Overview

MyBlog is a Japanese blog application built with Spring Boot 4.1.1 and Java 25 (LTS). It supports Markdown content creation, user authentication, and has a Scrapbox-inspired design. The application is containerized and deployed using Docker Compose with Nginx as a reverse proxy.

## Development Commands

### Build and Run
```bash
# Make gradlew executable (if needed)
chmod +x gradlew

# Clean and build
./gradlew clean build

# Run application locally
./gradlew bootRun

# Run tests
./gradlew test

# Build without tests (for CI)
./gradlew build -x test --no-daemon

# Run single test class
./gradlew test --tests "com.example.MyBlog.Service.MyBlogServiceTest"
```

### Docker Operations
```bash
# Build and start all services
docker compose up -d

# View logs
docker compose logs -f app

# Stop all services
docker compose down

# Full deployment using script
./deploy.sh deploy
```

### Database Connection
- **Development**: MongoDB connection via `MONGODB_URI` environment variable
- **Production**: MongoDB Atlas with connection pooling and timeout settings in `application-prod.yml`

## Architecture

### Package Structure
```
src/main/java/com/example/MyBlog/
├── Config/           # Configuration classes (Security, MongoDB, Markdown)
├── Controller/       # Web controllers (MVC pattern)
├── Entity/           # Domain models (Article, Users - MongoDB documents)
├── Repository/       # Data access layer (Spring Data MongoDB)
├── Service/          # Business logic layer
└── MyBlogApplication.java  # Main Spring Boot application
```

### Key Architectural Patterns

**MVC Architecture**: Clean separation between Controller → Service → Repository layers

**Entity Design**: Uses Java 17+ records for immutable entities:
```java
@Document(collection = "Articles")
public record Article(String id, String title, String content, boolean published, Date createdAt)
```

**Security Layer**: Spring Security with:
- BCrypt password encoding
- Form-based authentication with custom login page
- Route protection for `/Hello/**` endpoints
- OWASP HTML sanitization for XSS prevention

**Template Engine**: Thymeleaf with layout dialect for consistent page structure

### Database Schema
- **Articles Collection**: `{id, title, content, published, createdAt}`
- **Users Collection**: Basic user authentication data
- **Indexing**: Auto-indexing enabled in production, recommend compound index on `{published: 1, createdAt: -1}`

## Technology Stack

**Backend**: Java 25 (LTS), Spring Boot 4.1.1, Spring Security, Spring Data MongoDB
**Frontend**: Thymeleaf, Bootstrap 5.3.0, Custom CSS (Scrapbox-inspired)
**Database**: MongoDB (local dev) / MongoDB Atlas (production)
**Build**: Gradle 9.7.1 with Java 25 toolchain
**Deployment**: Docker multi-stage builds, Docker Compose, Nginx reverse proxy
**CI/CD**: GitHub Actions with self-hosted runner, Trivy security scanning

## Configuration Files

### Environment-Specific
- `application.properties`: Base configuration (dev port 8081, debug logging)
- `application-prod.yml`: Production settings (port 8080, INFO logging, file logging)

### Docker Configuration
- `dockerfile`: Multi-stage build (builder + runtime with Alpine)
- `compose.yml`: Application + Nginx setup with volume mounts
- Environment variables: `MONGODB_URI`, `SPRING_PROFILES_ACTIVE`, `JAVA_OPTS`

## Testing Strategy

**Unit Tests**: Service layer testing with Mockito
**CI Testing**: Excludes MongoDB-dependent tests in CI environment (see `build.gradle` CI conditions)
**Test Structure**: Mirror main package structure under `src/test/java`

### Running Tests
```bash
# All tests (requires MongoDB)
./gradlew test

# CI-safe tests only
CI=true ./gradlew test
```

## Deployment Process

### CI/CD Pipeline (`.github/workflows/deploy.yml`)
1. **Test**: Run unit tests, generate reports
2. **Security**: Trivy vulnerability scanning on Docker image
3. **Deploy**: Self-hosted runner deploys to production server via `deploy.sh`

### Production Deployment
- **Target**: Self-hosted server at `192.168.10.106`
- **Process**: Rsync to production directory → Docker Compose deployment
- **Monitoring**: Application logs in `/app/logs/myblog.log`

## Security Considerations

**Input Sanitization**: OWASP HTML sanitizer for Markdown content
**Authentication**: BCrypt password hashing, session management
**XSS Protection**: HTML sanitization policy in controllers
**Container Security**: Non-root user, Alpine Linux base image
**Secrets**: Environment variables for sensitive data, no hardcoded credentials

## Development Notes

### Markdown Processing
- **Parser**: Flexmark with GFM extensions (tables, strikethrough)
- **Sanitization**: OWASP sanitizer with comprehensive policy (formatting, links, images, tables)
- **Content Flow**: Raw Markdown → Flexmark parsing → OWASP sanitization → HTML rendering

### Japanese Language Support
- **Fonts**: Proper Japanese font stack in CSS
- **Encoding**: UTF-8 throughout application
- **Timezone**: Asia/Tokyo in Docker environment
- **Template**: Japanese language template structure

### Performance Considerations
- **Current**: No caching implemented
- **Recommendation**: Add `@Cacheable` annotations on article queries
- **Database**: Consider compound indexes for published articles sorted by date
- **Container**: G1GC enabled, 75% max RAM usage in Docker

### Code Style
- **Modern Java**: Uses records, Java 25 features
- **Dependency Injection**: Constructor injection with `@RequiredArgsConstructor` (Lombok)
- **Error Handling**: Basic exception handling, could be enhanced with `@ExceptionHandler`
- **Logging**: Debug level in development, structured logging in production