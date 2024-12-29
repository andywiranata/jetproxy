---
sidebar_position: 3
---

# JWT Auth

The `JWTAuth` middleware is a designed to validate and process JSON Web Tokens (JWTs). It ensures that only authorized requests are forwarded to your backend services by validating the authenticity and integrity of the tokens.

[Learn more about JSON Web Tokens (JWT)](https://jwt.io/)

## Features

- **Claim Verification**:
  - Supports dynamic claim validation (e.g., `iss`, `aud`).
- **JWKS Support**:
  - Dynamically fetches public keys for RSA-based tokens from a JWKS endpoint.
- **Custom Header Forwarding**:
  - Extracts claims from JWTs and forwards them as headers to backend services.

## Claim Forwarding

The `JWTAuth` middleware forwards validated claims to backend services through a single header: `jetproxy-jwt-claims`.
- **Header Name**: `jetproxy-jwt-claims`
- **Header Value**: JSON representation of all claims from the validated JWT.

For the JWT payload:
```json
{
  "sub": "123456",
  "name": "John Doe",
  "iss": "auth.myapp.com",
  "iat": 1735439621,
  "exp": 1735443221
}
```

## Configuration Examples
### Example 1: Basic JWT Authentication

```yaml
proxies:
  - path: /user
    service: userApi
    middleware:
      jwtAuth:
        enabled: true

services:
  - name: userApi
    url: http://localhost:30001
    methods: ['GET']

jwtAuthSource:
  headerName: "Authorization"
  tokenPrefix: "Bearer "
  secretKey: "U2VjdXJlU3Ryb25nS2V5Rm9yVXNpbmdXaXRoSFMyNTY="
  algorithms:
    - HS256
  claimValidations:  #Optional
    iss: "auth.myapp.com"
```

Explanation:

* Header Name: Reads the JWT from the `Authorization` header.
* Token Prefix: Strips the `Bearer ` prefix from the token.
* Algorithms: Supports the `HS256` algorithm for token verification.
* Claim Validations: Ensures the iss (issuer) claim matches `auth.myapp.com`.


### Example 2: Advanced Configuration with JWKS

```yaml
proxies:
  - path: /user
    service: userApi
    middleware:
      jwtAuth:
        enabled: true

services:
  - name: userApi
    url: http://localhost:30001
    methods: ['GET']

jwtAuth:
  headerName: "Authorization"
  tokenPrefix: "Bearer "
  jwksUri: "https://auth.example.com/.well-known/jwks.json"
  algorithms:
    - RS256
  claimValidations:
    aud: "my-application"

```

Explanation:

* JWKS URI: Dynamically fetches public keys for RS256 tokens from the JWKS endpoint.
