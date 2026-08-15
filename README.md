# URL Shortener

A URL shortening service built with **Java and Spring Boot**, developed as a practical backend engineering project.

## Features

* Create shortened URLs
* Redirect short URLs to original URLs
* Reuse existing short URLs
* URL validation and normalization
* Configurable short-code length
* Collision detection and retry mechanism
* Secure short-code generation using `SecureRandom`
* Unit and integration tests

## Tech Stack

* Java
* Spring Boot
* Spring Data JPA / Hibernate
* Maven
* H2
* JUnit 5
* Mockito
* AssertJ
* Apache Commons Validator
* Lombok

## API

### Create Short URL

`POST /api/v1/urls`

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

The response also contains the generated URL in the `Location` header.

### Redirect

`GET /{shortCode}`

Example:

```text
GET /Ab3x9K
```

Redirects the client to the original URL.

## Configuration

Example `application.properties`:

```properties
url.short.code.length=6
```

## Running

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

Run tests:

```bash
./mvnw test
```

## Development

The project is developed incrementally through separate feature branches and assignments. Each assignment is implemented, tested, reviewed, and then merged into the main branch.
