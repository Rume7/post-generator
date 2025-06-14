# Post Generator

A Spring Boot application that generates essays and blog posts using AI models (OpenAI GPT-4 and Anthropic Claude).

## Features

- Essay generation using AI models
- Rate limiting for API endpoints
- Secure configuration management
- PostgreSQL database integration
- RESTful API endpoints
- Swagger/OpenAPI documentation

## Prerequisites

- Java 17 or higher
- Maven
- PostgreSQL
- OpenAI API key
- Anthropic API key

## Environment Variables

Create a `.env` file in the root directory with the following variables:

```env
DB_URL=jdbc:postgresql://localhost:5432/essaydb
DB_USERNAME=your_username
DB_PASSWORD=your_secure_password
OPENAI_API_KEY=your_openai_key
OPENAI_MODEL=gpt-4
ANTHROPIC_API_KEY=your_anthropic_key
ANTHROPIC_MODEL=claude-3
```

## Building the Project

```bash
mvn clean install
```

## Running the Application

```bash
mvn spring-boot:run
```

The application will start on port 8080.

## API Documentation

Once the application is running, you can access the Swagger UI at:
```
http://localhost:8080/swagger-ui.html
```

## API Endpoints

### Generate Essay
```
POST /api/essay
Content-Type: application/json

{
    "topic": "Your essay topic",
    "additionalContext": "Optional additional context"
}
```

## Rate Limiting

The API is rate-limited to 10 requests per minute per IP address.

## Security

- All sensitive configuration is managed through environment variables
- Input validation on all endpoints
- Rate limiting to prevent abuse
- Secure database configuration

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details. 