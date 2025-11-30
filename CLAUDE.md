# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Spring Boot 3.5.8 + Vaadin 24.5.4 application that uses Spring AI (OpenAI integration) to analyze receipt images using vision models. The application allows users to upload receipt images, which are then analyzed using an AI model to extract structured data (merchant name, total amount, and line items).

## Technology Stack

- **Java 21** with Lombok for boilerplate reduction
- **Spring Boot 3.5.8** with Spring Actuator
- **Vaadin 24.5.4** for the web UI (server-side rendering)
- **Spring AI 1.0.0-M6** with OpenAI integration
- **Maven** for build management

## Common Commands

### Build and Run
```bash
./mvnw clean install          # Full build with tests
./mvnw spring-boot:run        # Run the application
./mvnw test                   # Run all tests
./mvnw test -Dtest=ClassName  # Run a specific test class
```

### Development
```bash
./mvnw clean package                    # Package without running tests
./mvnw clean package -DskipTests        # Package, skip tests
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"  # Debug mode
```

The application runs on `http://localhost:8080` by default.

## Architecture

### AI Integration Pattern

The application uses a **custom OpenAI-compatible endpoint** configured to point to a local LLM server:

- Base URL: `http://localhost:12434/engines/llama.cpp`
- Model: `ai/gemma3`
- The Spring AI OpenAI client is configured with a dummy API key (`_`) since the local server doesn't require authentication

See `application.properties` for the complete configuration.

### UI Architecture (Vaadin)

The application uses **Vaadin Flow** (server-side Java UI framework):

- All UI logic is in Java - no separate frontend JavaScript code to write
- The `@Route("")` annotation on `ReceiptView` makes it the root view
- Vaadin automatically generates the frontend code in `src/main/frontend/generated/` (excluded from git)
- UI updates happen via server-client synchronization

### Domain Model

The application has a simple data structure for receipts:

- `Receipt`: record with `merchant` (String), `total` (BigDecimal), and `lineItems` (List)
- `LineItem`: record with `name` (String), `quantity` (int), and `price` (BigDecimal)
- Both use Java records for immutability and concise syntax

### Image Processing Flow

1. User uploads image via `Upload` component → stored in `MemoryBuffer`
2. Image bytes extracted and displayed in `Image` preview component
3. "Analyze" button sends image + prompt to AI model via `ChatClient`
4. AI response is deserialized directly into `Receipt` object using `.entity(Receipt.class)`
5. Results displayed in a `Grid<LineItem>` component

## Configuration Notes

### File Upload Limits

The application supports receipt images up to 10MB:
```properties
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB
```

### Vaadin Development

- Vaadin auto-generates frontend resources during build
- The `src/main/frontend/generated/` directory should not be modified manually
- Hot reload is available in development mode

### Lombok Configuration

Lombok is configured in the Maven compiler plugin to process annotations at compile time. The annotation processor path is explicitly set in `pom.xml` to ensure proper IDE integration.

## Important Dependencies

- **Spring AI BOM**: Manages Spring AI library versions (milestone repository required)
- **Vaadin BOM**: Manages Vaadin component versions
- Lombok is excluded from the final JAR via the Spring Boot Maven Plugin configuration
