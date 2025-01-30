package io.jetproxy.middleware.grpc;

import io.grpc.CallCredentials;
import io.grpc.Metadata;
import io.grpc.Status;

import java.util.concurrent.Executor;

public class GrpcMetadataCredentials extends CallCredentials {
    private final Metadata metadata;

    public GrpcMetadataCredentials(Metadata metadata) {
        this.metadata = metadata;
    }

    @Override
    public void applyRequestMetadata(RequestInfo requestInfo, Executor appExecutor, MetadataApplier applier) {
        appExecutor.execute(() -> {
            try {
                applier.apply(metadata);
            } catch (Exception e) {
                applier.fail(Status.UNAUTHENTICATED.withCause(e));
            }
        });
    }

    @Override
    public void thisUsesUnstableApi() {
        // No-op
    }
}
