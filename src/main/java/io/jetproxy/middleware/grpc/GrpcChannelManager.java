package io.jetproxy.middleware.grpc;

import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.util.JsonFormat;
import io.grpc.*;
import io.grpc.reflection.v1alpha.ServerReflectionGrpc;
import io.grpc.reflection.v1alpha.ServerReflectionRequest;
import io.grpc.reflection.v1alpha.ServerReflectionResponse;
import io.grpc.stub.ClientCalls;
import io.grpc.stub.StreamObserver;
import io.jetproxy.context.AppConfig;
import com.google.protobuf.DescriptorProtos;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import io.grpc.stub.MetadataUtils;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class GrpcChannelManager {

    private static GrpcChannelManager instance;
    private final ConcurrentHashMap<String, ManagedChannel> channelMap;

    // Private constructor to enforce singleton pattern
    private GrpcChannelManager() {
        this.channelMap = new ConcurrentHashMap<>();
    }

    /**
     * Get the singleton instance of GrpcChannelManager.
     *
     * @return The singleton instance.
     */
    public static synchronized GrpcChannelManager getInstance() {
        if (instance == null) {
            instance = new GrpcChannelManager();
        }
        return instance;
    }

    /**
     * Configure gRPC channels based on the provided configuration.
     * This method remains static to initialize the singleton instance.
     *
     * @param grpcServices List of gRPC services to configure.
     */
    public static synchronized void configureGrpcChannel(List<AppConfig.GrpcService> grpcServices) {
        if (grpcServices == null) {
            return;
        }
        GrpcChannelManager manager = getInstance();
        for (AppConfig.GrpcService grpcService : grpcServices) {
            manager.channelMap.computeIfAbsent(grpcService.getName(), k -> ManagedChannelBuilder
                    .forAddress(grpcService.getHost(), grpcService.getPort())
                    .usePlaintext() // Use plaintext for testing; use TLS for production
                    .build());
        }
    }

    /**
     * Get or create a gRPC channel by service name.
     *
     * @param serviceName The name of the gRPC service.
     * @return The gRPC ManagedChannel.
     * @throws IllegalArgumentException if the service is not configured.
     */
    public ManagedChannel getGrpcChannel(String serviceName) {
        ManagedChannel channel = channelMap.get(serviceName);
        if (channel == null) {
            throw new IllegalArgumentException("Service not configured: " + serviceName);
        }
        return channel;
    }

    /**
     * Shutdown a specific gRPC channel.
     *
     * @param serviceName The name of the gRPC service.
     */
    public void shutdown(String serviceName) {
        ManagedChannel channel = channelMap.remove(serviceName);
        if (channel != null && !channel.isShutdown()) {
            channel.shutdown();
        }
    }

    /**
     * Shutdown all gRPC channels gracefully.
     */
    public void shutdownAll() {
        channelMap.forEach((key, channel) -> {
            if (!channel.isShutdown()) {
                channel.shutdown();
            }
        });
        channelMap.clear();
    }

    /**
     * Build a DynamicMessage from JSON input.
     *
     * @param jsonBody The JSON string representing the input.
     * @param descriptor The gRPC message descriptor.
     * @return The built DynamicMessage.
     * @throws Exception if parsing fails.
     */
    public DynamicMessage buildGrpcRequest(String jsonBody, Descriptors.Descriptor descriptor) throws Exception {
        DynamicMessage.Builder messageBuilder = DynamicMessage.newBuilder(descriptor);
        JsonFormat.parser().merge(jsonBody, messageBuilder);
        return messageBuilder.build();
    }

    /**
     * Invoke a gRPC method dynamically.
     *
     * @param fullMethodName The full gRPC method name (e.g., myservice.UserService/CreateUser).
     * @param grpcRequest The gRPC request DynamicMessage.
     * @param channel The gRPC ManagedChannel.
     * @return The DynamicMessage response.
     * @throws Exception if invocation fails.
     */
    public DynamicMessage invokeGrpcMethod(String fullMethodName, DynamicMessage grpcRequest, ManagedChannel channel, Map<String, String> metadataMap) throws Exception {

        String[] parts = fullMethodName.split("/");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid fullMethodName format. Expected 'ServiceName/MethodName'.");
        }

        String serviceName = parts[0];
        String methodName = parts[1];

        // "Connection" not allowed per RFC 7230 section 6.1.
        // This header is typically used in HTTP/1.x for connection-specific actions and
        // should not be forwarded in an HTTP/2 (gRPC) request.
        // Forbidden headers (case-insensitive)
        Set<String> forbiddenHeaders = Set.of(
                "proxy-authorization",
                "proxy-authenticate",
                "te",
                "trailer",
                "connection",
                "keep-alive", "proxy-connection", "transfer-encoding", "upgrade",
                "host", "accept", "accept-encoding", "cache-control", "content-length", "user-agent"
        );

        // Remove forbidden headers from metadataMap before converting
        metadataMap.entrySet().removeIf(entry -> forbiddenHeaders.contains(entry.getKey().toLowerCase()));

        // Fetch Service Descriptor
        Descriptors.ServiceDescriptor serviceDescriptor = fetchServiceDescriptor(channel, serviceName);
        Descriptors.MethodDescriptor methodDescriptor = serviceDescriptor.findMethodByName(methodName);

        if (methodDescriptor == null) {
            throw new IllegalArgumentException("Method not found: " + methodName);
        }

        MethodDescriptor<DynamicMessage, DynamicMessage> dynamicMethod = MethodDescriptor.<DynamicMessage, DynamicMessage>newBuilder()
                .setType(MethodDescriptor.MethodType.UNARY)
                .setFullMethodName(fullMethodName)
                .setRequestMarshaller(
                        io.grpc.protobuf.ProtoUtils.marshaller(
                                DynamicMessage.getDefaultInstance(methodDescriptor.getInputType())))
                .setResponseMarshaller(
                        io.grpc.protobuf.ProtoUtils.marshaller(
                                DynamicMessage.getDefaultInstance(methodDescriptor.getOutputType())))
                .build();

        // Create Metadata and populate it
        Metadata metadata = new Metadata();
        for (Map.Entry<String, String> entry : metadataMap.entrySet()) {
            Metadata.Key<String> key = Metadata.Key.of(entry.getKey(), Metadata.ASCII_STRING_MARSHALLER);
            metadata.put(key, entry.getValue());
        }

        // Use the custom CallCredentials to pass metadata
        io.grpc.CallOptions callOptions = io.grpc.CallOptions.DEFAULT.withCallCredentials(
                new GrpcMetadataCredentials(metadata));

        // Invoke the method with metadata
        try {
            // Invoke the gRPC method
            return ClientCalls.blockingUnaryCall(
                    channel.newCall(dynamicMethod, callOptions),
                    grpcRequest
            );
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.NOT_FOUND) {
                System.err.println("Error: gRPC method not found - " + e.getStatus().getDescription());
            } else if (e.getStatus().getCode() == Status.Code.UNAVAILABLE) {
                System.err.println("Error: gRPC service is unavailable. Check server status.");
            } else {
                System.err.println("gRPC call failed: " + e.getMessage());
            }
            throw e; // Rethrow exception to propagate error handling
        }
    }

    /**
     * Convert a gRPC DynamicMessage to JSON.
     *
     * @param grpcMessage The gRPC DynamicMessage.
     * @return The JSON representation of the message.
     * @throws Exception if conversion fails.
     */
    public String convertGrpcResponseToJson(DynamicMessage grpcMessage) throws Exception {
        return JsonFormat.printer().print(grpcMessage);
    }


    public Descriptors.ServiceDescriptor fetchServiceDescriptor(ManagedChannel channel, String serviceName) throws Exception {
        CompletableFuture<Descriptors.ServiceDescriptor> future = new CompletableFuture<>();
        ServerReflectionGrpc.ServerReflectionStub reflectionStub = ServerReflectionGrpc.newStub(channel);
        StreamObserver<ServerReflectionRequest> requestObserver = reflectionStub.serverReflectionInfo(new StreamObserver<>() {
            @Override
            public void onNext(ServerReflectionResponse response) {
                if (response.hasFileDescriptorResponse()) {
                    try {
                        // Convert ByteString to FileDescriptorProto
                        DescriptorProtos.FileDescriptorProto fileDescriptorProto =
                                DescriptorProtos.FileDescriptorProto.parseFrom(
                                        response.getFileDescriptorResponse().getFileDescriptorProto(0)
                                );

                        // Build FileDescriptor from FileDescriptorProto
                        Descriptors.FileDescriptor fileDescriptor = Descriptors.FileDescriptor.buildFrom(
                                fileDescriptorProto,
                                new Descriptors.FileDescriptor[]{}
                        );
                        String simpleServiceName = extractSimpleServiceName(serviceName);
                        // Find and complete with the ServiceDescriptor
                        Descriptors.ServiceDescriptor serviceDescriptor = fileDescriptor.findServiceByName(simpleServiceName);
                        if (serviceDescriptor != null) {
                            future.complete(serviceDescriptor);
                        } else {
                            future.completeExceptionally(new RuntimeException("Service descriptor not found for: " + serviceName));
                        }
                    } catch (Exception e) {
                        future.completeExceptionally(new RuntimeException("Failed to parse service descriptor", e));
                    }
                }
            }

            @Override
            public void onError(Throwable t) {
                future.completeExceptionally(new RuntimeException("Error fetching service descriptor: " + t.getMessage(), t));
            }

            @Override
            public void onCompleted() {
                // Do nothing here
            }
        });

        // Send the request to fetch the service descriptor
        requestObserver.onNext(ServerReflectionRequest.newBuilder()
                .setFileContainingSymbol(serviceName)
                .build());
        requestObserver.onCompleted();

        // Wait for the result synchronously with a timeout
        return future.get(5, TimeUnit.SECONDS); // Timeout of 5 seconds
    }
    /**
     * Extract the simple service name from a fully qualified service name.
     * Example: "userservice.UserService" -> "UserService"
     */
    private String extractSimpleServiceName(String fullServiceName) {
        int lastDotIndex = fullServiceName.lastIndexOf('.');
        return lastDotIndex != -1 ? fullServiceName.substring(lastDotIndex + 1) : fullServiceName;
    }

}
