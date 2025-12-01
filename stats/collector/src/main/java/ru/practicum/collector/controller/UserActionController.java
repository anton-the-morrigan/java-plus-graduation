package ru.practicum.collector.controller;

import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.collector.handler.UserActionHandler;
import ru.practicum.ewm.stats.proto.UserActionProto;
import ru.practicum.grpc.stats.collector.UserActionControllerGrpc;

@GrpcService
@RequiredArgsConstructor
public class UserActionController extends UserActionControllerGrpc.UserActionControllerImplBase  {
    private final UserActionHandler userActionHandler;

    @Override
    public void collectUserAction(UserActionProto request, StreamObserver<Empty> responseObserver) {
        try {
            userActionHandler.kafkaUserAction(request);
            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(
                    new StatusRuntimeException(Status.INTERNAL
                            .withDescription(e.getMessage())
                            .withCause(e))
            );
        }
    }
}
