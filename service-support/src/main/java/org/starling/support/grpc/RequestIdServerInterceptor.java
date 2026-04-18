package org.starling.support.grpc;

import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import org.apache.logging.log4j.ThreadContext;

/**
 * Restores request ids from incoming metadata into the logging context.
 */
public final class RequestIdServerInterceptor implements ServerInterceptor {

    /**
     * Intercepts call.
     * @param call the call value
     * @param headers the headers value
     * @param next the next value
     * @return the result of this operation
     * @param <ReqT> the req type parameter
     * @param <RespT> the resp type parameter
     */
    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next
    ) {
        String requestId = headers.get(RequestIds.METADATA_KEY);
        ThreadContext.put(RequestIds.THREAD_CONTEXT_KEY,
                requestId == null || requestId.isBlank() ? RequestIds.generate() : requestId);

        ServerCall.Listener<ReqT> delegate = next.startCall(call, headers);
        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<>(delegate) {
            /**
             * Completes.
             */
            @Override
            public void onComplete() {
                try {
                    super.onComplete();
                } finally {
                    ThreadContext.remove(RequestIds.THREAD_CONTEXT_KEY);
                }
            }

            /**
             * Cancels.
             */
            @Override
            public void onCancel() {
                try {
                    super.onCancel();
                } finally {
                    ThreadContext.remove(RequestIds.THREAD_CONTEXT_KEY);
                }
            }
        };
    }
}
