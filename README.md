# Spring Boot Auth0 Project

A complete Spring Boot 3.x application with Auth0 authentication and user management API.

## Overview

This project demonstrates a production-ready implementation of Auth0 integration with Spring Boot, including:
- JWT-based authentication using Auth0
- Role-based authorization (USER, ADMIN)
- User management REST API
- Integration with Auth0 Management API
- H2 in-memory database for development
- Global exception handling
- Input validation
- CORS configuration for frontend integration

## Technologies Used

- **Java 17**
- **Spring Boot 3.2.1**
- **Spring Security** with Auth0 integration
- **Spring Data JPA**
- **H2 Database**
- **Lombok**
- **Maven**
- **Auth0 Management API**

## Prerequisites

- Java 17 or higher
- Maven 3.6 or higher
- Auth0 account (free tier available at [auth0.com](https://auth0.com))

## Auth0 Setup

Before running the application, you need to set up an Auth0 application and API:

### 1. Create an Auth0 Application

1. Log in to your [Auth0 Dashboard](https://manage.auth0.com/)
2. Go to **Applications** → **Applications**
3. Click **Create Application**
4. Choose **Single Page Web Applications** or **Regular Web Applications**
5. Note your **Domain**, **Client ID**, and **Client Secret**

### 2. Create an Auth0 API

1. In the Auth0 Dashboard, go to **Applications** → **APIs**
2. Click **Create API**
3. Set a name (e.g., "User Management API")
4. Set an identifier (e.g., "https://api.myapp.com")
5. Note the **API Audience** (the identifier you just set)

### 3. Configure Permissions (Optional)

1. In your API settings, go to the **Permissions** tab
2. Add permissions like:
   - `read:users`
   - `write:users`
   - `delete:users`

### 4. Set up Roles

1. Go to **User Management** → **Roles**
2. Create roles:
   - **ADMIN** - Full access
   - **USER** - Basic access

## Configuration

### Environment Variables

Set the following environment variables or update `application.properties`:

```bash
export AUTH0_DOMAIN=your-domain.auth0.com
export AUTH0_CLIENT_ID=your-client-id
export AUTH0_CLIENT_SECRET=your-client-secret
export AUTH0_AUDIENCE=your-api-audience
```

Or create a `.env` file in the project root:

```properties
AUTH0_DOMAIN=your-domain.auth0.com
AUTH0_CLIENT_ID=your-client-id
AUTH0_CLIENT_SECRET=your-client-secret
AUTH0_AUDIENCE=your-api-audience
```

### Application Properties

The application uses two profiles:

- **Default**: Uses in-memory H2 database
- **Dev**: Uses file-based H2 database with more verbose logging

To run with the dev profile:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## Building and Running

### Build the Project

```bash
mvn clean install
```

### Run the Application

```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

### Access H2 Console

- URL: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:testdb`
- Username: `sa`
- Password: (leave blank)

## API Documentation

### Authentication

All API endpoints (except public ones) require a valid Auth0 JWT token in the Authorization header:

```
Authorization: Bearer <your-jwt-token>
```

### Endpoints

#### 1. Create User (Admin Only)

**POST** `/api/users`

Creates a new user in both the local database and Auth0.

**Request:**
```json
{
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "roles": ["USER"]
}
```

**Response:** `201 Created`
```json
{
  "id": 1,
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "roles": ["USER"],
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:30:00"
}
```

**Required Role:** `ADMIN`

#### 2. Get All Users (Admin Only)

**GET** `/api/users`

Retrieves all users from the database.

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "email": "user@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "roles": ["USER"],
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T10:30:00"
  }
]
```

**Required Role:** `ADMIN`

#### 3. Get User by ID

**GET** `/api/users/{id}`

Retrieves a specific user by ID. Users can view their own profile; admins can view any profile.

**Response:** `200 OK`
```json
{
  "id": 1,
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "roles": ["USER"],
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:30:00"
}
```

**Required:** Authenticated user (owner or admin)

#### 4. Update User

**PUT** `/api/users/{id}`

Updates a user's information. Users can update their own profile (except roles); admins can update any profile including roles.

**Request:**
```json
{
  "firstName": "Jane",
  "lastName": "Smith",
  "roles": ["USER", "ADMIN"]
}
```

**Response:** `200 OK`
```json
{
  "id": 1,
  "email": "user@example.com",
  "firstName": "Jane",
  "lastName": "Smith",
  "roles": ["USER", "ADMIN"],
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T11:00:00"
}
```

**Required:** Authenticated user (owner or admin)

#### 5. Get Current User

**GET** `/api/users/me`

Retrieves the profile of the currently authenticated user.

**Response:** `200 OK`
```json
{
  "id": 1,
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "roles": ["USER"],
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:30:00"
}
```

**Required:** Authenticated user

### Health Check Endpoints

#### Application Health

**GET** `/actuator/health`

Returns the health status of the application.

**Response:** `200 OK`
```json
{
  "status": "UP"
}
```

## Example API Requests

### Using cURL

#### Get JWT Token from Auth0

First, obtain a token from Auth0:

```bash
curl --request POST \
  --url https://YOUR_DOMAIN.auth0.com/oauth/token \
  --header 'content-type: application/json' \
  --data '{
    "client_id":"YOUR_CLIENT_ID",
    "client_secret":"YOUR_CLIENT_SECRET",
    "audience":"YOUR_API_AUDIENCE",
    "grant_type":"client_credentials"
  }'
```

#### Create a User

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "newuser@example.com",
    "firstName": "New",
    "lastName": "User",
    "roles": ["USER"]
  }'
```

#### Get All Users

```bash
curl -X GET http://localhost:8080/api/users \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

#### Get User by ID

```bash
curl -X GET http://localhost:8080/api/users/1 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

#### Update User

```bash
curl -X PUT http://localhost:8080/api/users/1 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Updated",
    "lastName": "Name",
    "roles": ["USER"]
  }'
```

#### Get Current User

```bash
curl -X GET http://localhost:8080/api/users/me \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## Error Handling

The application includes comprehensive error handling with appropriate HTTP status codes:

- **400 Bad Request** - Validation errors
- **401 Unauthorized** - Missing or invalid authentication
- **403 Forbidden** - Insufficient permissions
- **404 Not Found** - Resource not found
- **500 Internal Server Error** - Unexpected errors
- **502 Bad Gateway** - Auth0 API errors

### Example Error Response

```json
{
  "status": 404,
  "message": "User not found with id: 123",
  "timestamp": "2024-01-15T10:30:00"
}
```

### Validation Error Response

```json
{
  "status": 400,
  "errors": {
    "email": "Email is required",
    "firstName": "First name is required"
  },
  "timestamp": "2024-01-15T10:30:00"
}
```

## Security Configuration

### CORS

The application is configured to allow CORS requests from:
- `http://localhost:3000` (React default)
- `http://localhost:4200` (Angular default)

To add more origins, update the `SecurityConfig` class.

### Role-Based Access Control

- **Public endpoints:** `/actuator/**`, `/h2-console/**`
- **Admin only:** `POST /api/users`, `GET /api/users`
- **Authenticated users:** `GET /api/users/me`, `GET /api/users/{id}`, `PUT /api/users/{id}`

## Project Structure

```
src/
├── main/
│   ├── java/com/example/auth0app/
│   │   ├── config/
│   │   │   └── SecurityConfig.java
│   │   ├── controller/
│   │   │   └── UserController.java
│   │   ├── dto/
│   │   │   ├── UserCreateRequest.java
│   │   │   ├── UserUpdateRequest.java
│   │   │   └── UserResponse.java
│   │   ├── entity/
│   │   │   └── User.java
│   │   ├── exception/
│   │   │   ├── Auth0ApiException.java
│   │   │   ├── GlobalExceptionHandler.java
│   │   │   ├── UnauthorizedException.java
│   │   │   └── UserNotFoundException.java
│   │   ├── repository/
│   │   │   └── UserRepository.java
│   │   ├── service/
│   │   │   └── UserService.java
│   │   └── Auth0Application.java
│   └── resources/
│       ├── application.properties
│       └── application-dev.properties
└── test/
    └── java/com/example/auth0app/
```

## Development

### Running Tests

```bash
mvn test
```

### Building for Production

```bash
mvn clean package
java -jar target/spring-boot-auth0-project-1.0.0.jar
```

## Troubleshooting

### Common Issues

1. **Auth0 Connection Issues**
   - Verify your Auth0 credentials are correct
   - Ensure the audience matches your Auth0 API identifier
   - Check that your Auth0 domain includes `.auth0.com`

2. **403 Forbidden Errors**
   - Ensure your JWT token includes the required roles
   - Verify roles are properly assigned in Auth0

3. **H2 Console Not Accessible**
   - Ensure `spring.h2.console.enabled=true` in properties
   - Check that you're accessing the correct path `/h2-console`

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License.

## Contact

For questions or support, please open an issue in the GitHub repository.

## Acknowledgments

- [Auth0](https://auth0.com/) for authentication services
- [Spring Boot](https://spring.io/projects/spring-boot) for the framework
- [Project Lombok](https://projectlombok.org/) for reducing boilerplate code
