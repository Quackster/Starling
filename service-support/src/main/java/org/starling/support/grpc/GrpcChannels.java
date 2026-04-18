package org.starling.support.grpc;

import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.ClientInterceptors;

/**
 * Builds plaintext gRPC channels with the standard interceptor chain.
 */
public final class GrpcChannels {

    /**
     * Creates a new GrpcChannels.
     */
    private GrpcChannels() {}

    /**
     * Opens channel.
     * @param host the host value
     * @param port the port value
     * @param interceptors the interceptors value
     * @return the result of this operation
     */
    public static ManagedChannel open(String host, int port, ClientInterceptor... interceptors) {
        ManagedChannel baseChannel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        if (interceptors == null || interceptors.length == 0) {
            return baseChannel;
        }
        return (ManagedChannel) ClientInterceptors.intercept(baseChannel, interceptors);
    }
}
