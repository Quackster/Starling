package org.starling.support.grpc;

import io.grpc.Metadata;
import org.apache.logging.log4j.ThreadContext;

import java.util.UUID;

/**
 * Utilities for propagating request identifiers across gRPC calls and logs.
 */
public final class RequestIds {

    public static final String THREAD_CONTEXT_KEY = "requestId";
    public static final Metadata.Key<String> METADATA_KEY =
            Metadata.Key.of("x-request-id", Metadata.ASCII_STRING_MARSHALLER);

    /**
     * Creates a new RequestIds.
     */
    private RequestIds() {}

    /**
     * Generates a new request id.
     * @return the result of this operation
     */
    public static String generate() {
        return UUID.randomUUID().toString();
    }

    /**
     * Returns the current request id or creates one.
     * @return the result of this operation
     */
    public static String currentOrCreate() {
        String requestId = ThreadContext.get(THREAD_CONTEXT_KEY);
        if (requestId == null || requestId.isBlank()) {
            requestId = generate();
            ThreadContext.put(THREAD_CONTEXT_KEY, requestId);
        }
        return requestId;
    }
}
