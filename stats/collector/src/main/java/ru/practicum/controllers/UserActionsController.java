package ru.practicum.controllers;

import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.ewm.stats.grpc.collector.UserActionControllerGrpc;
import ru.practicum.ewm.stats.grpc.user.UserActionProto;
import ru.practicum.handlers.users.UserActionsHandler;


@GrpcService
@RequiredArgsConstructor
public class UserActionsController extends UserActionControllerGrpc.UserActionControllerImplBase {
    private final UserActionsHandler userActionsHandler;

    @Override
    public void collectUserAction(UserActionProto request, StreamObserver<Empty> responseObserver) {
        try {
            userActionsHandler.handle(request);
            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(new StatusRuntimeException(
                    Status.INTERNAL
                            .withDescription(e.getLocalizedMessage())
                            .withCause(e)
            ));
        }
    }
}
