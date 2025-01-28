package io.jetproxy.middleware.grpc;

import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.util.JsonFormat;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.MethodDescriptor;
import io.grpc.reflection.v1alpha.ServerReflectionGrpc;
import io.grpc.reflection.v1alpha.ServerReflectionRequest;
import io.grpc.reflection.v1alpha.ServerReflectionResponse;
import io.grpc.stub.ClientCalls;
import io.grpc.stub.StreamObserver;
import io.jetproxy.context.AppConfig;
import com.google.protobuf.DescriptorProtos;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

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
    public DynamicMessage invokeGrpcMethod(String fullMethodName, DynamicMessage grpcRequest, ManagedChannel channel) throws Exception {
        String[] parts = fullMethodName.split("/");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid fullMethodName format. Expected 'ServiceName/MethodName'.");
        }

        String serviceName = parts[0];
        String methodName = parts[1];

        // Use reflection to get the ServiceDescriptor
        Descriptors.ServiceDescriptor serviceDescriptor = fetchServiceDescriptor(channel, serviceName);
        Descriptors.MethodDescriptor methodDescriptor = serviceDescriptor.findMethodByName(methodName);

        if (methodDescriptor == null) {
            throw new IllegalArgumentException("Method not found: " + methodName);
        }

        MethodDescriptor<DynamicMessage, DynamicMessage> dynamicMethod = MethodDescriptor.<DynamicMessage, DynamicMessage>newBuilder()
                .setType(MethodDescriptor.MethodType.UNARY)
                .setFullMethodName(fullMethodName)
                .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(DynamicMessage.getDefaultInstance(methodDescriptor.getInputType())))
                .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(DynamicMessage.getDefaultInstance(methodDescriptor.getOutputType())))
                .build();

        // Invoke the method
        return ClientCalls.blockingUnaryCall(
                channel.newCall(dynamicMethod, io.grpc.CallOptions.DEFAULT),
                grpcRequest
        );
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
