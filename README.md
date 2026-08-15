# URL Shortener

A production-oriented URL shortening service built with **Java** and **Spring Boot**.

The project is being developed incrementally with a focus on clean architecture, validation, persistence, exception handling, testing, and practical backend engineering principles.

## Features

* Create shortened URLs
* Redirect short URLs to their original URLs
* Detect and reuse existing shortened URLs
* URL validation using Apache Commons Validator
* URL normalization

  * Lowercase scheme
  * Lowercase hostname
  * Preserve case-sensitive path, query, and fragment
* Configurable short-code length
* Collision detection and retry mechanism
* Protection against short-code generation failures
* Cryptographically stronger short-code generation using `SecureRandom`
* Input validation for short codes
* Centralized exception handling
* Unit and integration testing

## Tech Stack

* **Java**
* **Spring Boot**
* **Spring Data JPA**
* **Hibernate**
* **Maven**
* **H2 Database**
* **JUnit 5**
* **Mockito**
* **AssertJ**
* **Apache Commons Validator**
* **Lombok**

## API

### Create a Short URL

**POST** `/api/v1/urls`

Request:

```json
{
  "url": "https://example.com"
}
```

Response:

```json
{
  "shortenedUrl": "http://localhost/Ab3x9K"
}
```

The response also includes a `Location` header containing the generated shortened URL.

### Redirect

**GET** `/{shortCode}`

Example:

```text
GET /Ab3x9K
```

The service resolves the short code and redirects the client to the original URL.

## URL Normalization

Before storing a URL, the service normalizes its scheme and hostname while preserving case-sensitive components.

For example:

```text
HTTPS://Example.COM/Products?Name=Java#Details
```

is normalized to:

```text
https://example.com/Products?Name=Java#Details
```

This allows semantically equivalent URLs to be detected and prevents unnecessary duplicate short URLs.

## Short-Code Generation

Short codes are generated using a configurable length and a defined set of allowed characters.

The service uses `SecureRandom` instead of `java.util.Random` to provide a cryptographically stronger source of randomness, making generated short codes significantly harder to predict.

The service also handles collisions in two ways:

1. It checks whether a generated short code already exists.
2. It handles database constraint violations caused by concurrent collisions.

A maximum number of generation attempts prevents an infinite retry loop.

## Configuration

Application configuration is kept in `application.properties`.

Example:

```properties
url.short.code.length=6
```

Additional configuration will be introduced as the project evolves.

## Testing

The project contains both unit and integration tests.

### Unit Tests

The service layer is tested with Mockito and covers scenarios including:

* URL creation
* Existing URL detection
* URL validation
* URL normalization
* Short-code generation
* Short-code collisions
* Database constraint violations
* Short-code generation failures
* Short-code validation
* Missing short URLs

### Integration Tests

The application layer is tested with Spring's `MockMvc` and an in-memory database to verify behavior across the HTTP, service, and persistence layers.

## Project Structure

```text
src
├── main
│   └── java
│       └── nvb.dev.urlshortener
│           ├── controller
│           ├── domain
│           ├── dto
│           ├── exception
│           ├── repository
│           └── service
│
└── test
    └── java
        └── nvb.dev.urlshortener
            ├── controller
            ├── repository
            └── service
```

## Running the Application

### Prerequisites

* Java
* Maven

Clone the repository:

```bash
git clone https://github.com/erfannvb/url-shortener.git
cd url-shortener
```

Run the application:

```bash
./mvnw spring-boot:run
```

On Windows:

```bash
mvnw.cmd spring-boot:run
```

## Running Tests

Run the complete test suite with:

```bash
./mvnw test
```

On Windows:

```bash
mvnw.cmd test
```

## Development Approach

This project is intentionally developed incrementally.

Each improvement is implemented as a separate assignment and branch, followed by:

1. Implementation
2. Automated tests
3. Code review
4. Git commit
5. Merge into the main branch

The goal is not simply to build a working URL shortener, but to use the project as a practical exercise in **professional Java and Spring Boot development**.

## Future Improvements

Potential future improvements include:

* Rate limiting
* URL expiration
* Custom short codes
* Click analytics
* Distributed ID generation
* Caching
* Database migration management
* Docker support
* API documentation with OpenAPI
* Observability and metrics
* Concurrency and load testing

## License

This project is intended primarily as a learning and portfolio project.
