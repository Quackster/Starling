package org.starling.support.grpc;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;

/**
 * Propagates the current request id to downstream gRPC calls.
 */
public final class RequestIdClientInterceptor implements ClientInterceptor {

    /**
     * Intercepts call.
     * @param method the method value
     * @param callOptions the call options value
     * @param next the next value
     * @return the result of this operation
     * @param <ReqT> the req type parameter
     * @param <RespT> the resp type parameter
     */
    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions,
            Channel next
    ) {
        ClientCall<ReqT, RespT> delegate = next.newCall(method, callOptions);
        return new ForwardingClientCall.SimpleForwardingClientCall<>(delegate) {
            /**
             * Starts.
             * @param responseListener the response listener value
             * @param headers the headers value
             */
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                headers.put(RequestIds.METADATA_KEY, RequestIds.currentOrCreate());
                super.start(responseListener, headers);
            }
        };
    }
}
