package org.starling.identity;

import io.grpc.stub.StreamObserver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.starling.contracts.AuthenticatePasswordRequest;
import org.starling.contracts.AuthenticateResponse;
import org.starling.contracts.AuthenticateSsoRequest;
import org.starling.contracts.GetPlayerBootstrapRequest;
import org.starling.contracts.IdentityServiceGrpc;
import org.starling.storage.dao.UserDao;
import org.starling.storage.entity.UserEntity;

/**
 * gRPC façade for authentication and player bootstrap queries.
 */
public final class IdentityGrpcService extends IdentityServiceGrpc.IdentityServiceImplBase {

    private static final Logger log = LogManager.getLogger(IdentityGrpcService.class);

    /**
     * Authenticates password.
     * @param request the request value
     * @param responseObserver the response observer value
     */
    @Override
    public void authenticatePassword(
            AuthenticatePasswordRequest request,
            StreamObserver<AuthenticateResponse> responseObserver
    ) {
        UserEntity user = UserDao.findByUsername(request.getUsername());
        if (user == null || !user.getPassword().equals(request.getPassword())) {
            log.warn("Password login failed for {}", request.getUsername());
            responseObserver.onNext(IdentityMappings.authenticationFailure("login incorrect"));
            responseObserver.onCompleted();
            return;
        }

        log.info("Password login succeeded for {} (id={})", user.getUsername(), user.getId());
        responseObserver.onNext(AuthenticateResponse.newBuilder()
                .setAuthenticated(true)
                .setBootstrap(IdentityMappings.toBootstrap(user))
                .build());
        responseObserver.onCompleted();
    }

    /**
     * Authenticates sso.
     * @param request the request value
     * @param responseObserver the response observer value
     */
    @Override
    public void authenticateSso(
            AuthenticateSsoRequest request,
            StreamObserver<AuthenticateResponse> responseObserver
    ) {
        UserEntity user = UserDao.findBySsoTicket(request.getTicket());
        if (user == null) {
            log.warn("SSO login failed");
            responseObserver.onNext(IdentityMappings.authenticationFailure("login incorrect"));
            responseObserver.onCompleted();
            return;
        }

        log.info("SSO login succeeded for {} (id={})", user.getUsername(), user.getId());
        responseObserver.onNext(AuthenticateResponse.newBuilder()
                .setAuthenticated(true)
                .setBootstrap(IdentityMappings.toBootstrap(user))
                .build());
        responseObserver.onCompleted();
    }

    /**
     * Gets player bootstrap.
     * @param request the request value
     * @param responseObserver the response observer value
     */
    @Override
    public void getPlayerBootstrap(
            GetPlayerBootstrapRequest request,
            StreamObserver<org.starling.contracts.PlayerBootstrapResponse> responseObserver
    ) {
        UserEntity user = UserDao.findById(request.getPlayerId());
        responseObserver.onNext(IdentityMappings.bootstrapResponse(user));
        responseObserver.onCompleted();
    }
}
