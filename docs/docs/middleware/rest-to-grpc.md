---
sidebar_position: 10
---

# Rest To Grpc (Experimental)

The **REST to gRPC Middleware** enables seamless integration between RESTful HTTP endpoints and gRPC services within JetProxy. This middleware allows forwarding incoming REST requests to a backend gRPC service, handling the necessary transformation of HTTP requests into gRPC calls.

By configuring this middleware, developers can leverage JetProxy to act as a bridge between REST and gRPC, ensuring compatibility between different clients and services.

## Header-Based gRPC Configuration

Instead of defining method mappings, you can configure the REST request to dynamically call any gRPC method by passing the service and method name in headers. Additionally, **gRPC Server Reflection** can be used as an alternative to requiring pre-defined .proto files, allowing JetProxy to dynamically discover available services and methods at runtime.

Use the **X-Grpc-Service-Name** and **X-Grpc-Method-Name** headers to define the target gRPC service and method.

## Configuring Example

To configure a REST to gRPC proxy, define a gRPC service in **grpcServices** and reference it within a proxy rule:

```yaml
proxies:
  - path: /v2/grpc
    service: userGrpcApi
    ttl: 10000

grpcServices:
  - name: userGrpcApi
    host: localhost                   # Host for gRPC server
    port: 50051                       # Port for gRPC server

```

### Sample CURL

```
curl --location --request GET 'localhost:8080/v2/grpc' \
--header 'Content-Type: application/json' \
--header 'X-Grpc-Service-Name: userservice.UserService' \
--header 'X-Grpc-Method-Name: GetUser' \
--data '{
    "id": "d8c05992-428e-477e-960c-be34e2cec538"
}'
```

**Explanation**:

X-Grpc-Service-Name: Specifies the gRPC service to be called (userservice.UserService).
X-Grpc-Method-Name: Defines the specific gRPC method to invoke (GetUser).
Content-Type: Ensures that the request is sent as JSON.

## How It Works

1. JetProxy intercepts the REST request and extracts the X-Grpc-Service-Name and X-Grpc-Method-Name headers.
2. The extracted values determine which gRPC service and method to invoke.
3. gRPC Server Reflection must be enabled, allowing JetProxy to dynamically discover available services and methods without requiring predefined .proto files.
4. The request payload is transformed into a gRPC request and forwarded to the specified gRPC server.
5. The gRPC response is received, converted into a JSON response, and returned to the client.