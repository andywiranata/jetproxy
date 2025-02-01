package io.jetproxy.middleware.grpc;

import io.grpc.Status;
import java.util.HashMap;
import java.util.Map;

public class GrpcToHttpStatusMapper {
    private static final Map<Status.Code, Integer> GRPC_TO_HTTP_MAP = new HashMap<>();

    static {
        GRPC_TO_HTTP_MAP.put(Status.Code.OK, 200); // HTTP 200 OK
        GRPC_TO_HTTP_MAP.put(Status.Code.CANCELLED, 499); // Client closed request
        GRPC_TO_HTTP_MAP.put(Status.Code.UNKNOWN, 500); // Generic server error
        GRPC_TO_HTTP_MAP.put(Status.Code.INVALID_ARGUMENT, 400); // Bad request
        GRPC_TO_HTTP_MAP.put(Status.Code.DEADLINE_EXCEEDED, 504); // Gateway timeout
        GRPC_TO_HTTP_MAP.put(Status.Code.NOT_FOUND, 404); // Not found
        GRPC_TO_HTTP_MAP.put(Status.Code.ALREADY_EXISTS, 409); // Conflict
        GRPC_TO_HTTP_MAP.put(Status.Code.PERMISSION_DENIED, 403); // Forbidden
        GRPC_TO_HTTP_MAP.put(Status.Code.UNAUTHENTICATED, 401); // Unauthorized
        GRPC_TO_HTTP_MAP.put(Status.Code.RESOURCE_EXHAUSTED, 429); // Too many requests
        GRPC_TO_HTTP_MAP.put(Status.Code.FAILED_PRECONDITION, 412); // Precondition failed
        GRPC_TO_HTTP_MAP.put(Status.Code.ABORTED, 409); // Conflict
        GRPC_TO_HTTP_MAP.put(Status.Code.OUT_OF_RANGE, 400); // Bad request
        GRPC_TO_HTTP_MAP.put(Status.Code.UNIMPLEMENTED, 501); // Not implemented
        GRPC_TO_HTTP_MAP.put(Status.Code.INTERNAL, 500); // Internal server error
        GRPC_TO_HTTP_MAP.put(Status.Code.UNAVAILABLE, 503); // Service unavailable
        GRPC_TO_HTTP_MAP.put(Status.Code.DATA_LOSS, 500); // Internal server error
    }

    public static int mapGrpcStatusToHttp(Status.Code grpcStatusCode) {
        return GRPC_TO_HTTP_MAP.getOrDefault(grpcStatusCode, 500); // Default to 500
    }
}