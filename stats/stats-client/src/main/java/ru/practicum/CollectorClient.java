package ru.practicum;

import com.google.protobuf.Timestamp;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.proto.ActionTypeProto;
import ru.practicum.ewm.stats.proto.UserActionProto;
import ru.practicum.grpc.stats.collector.UserActionControllerGrpc;

import java.time.Instant;

@Service
public class CollectorClient {

    @GrpcClient("collector")
    private UserActionControllerGrpc.UserActionControllerBlockingStub userActionClient;

    public void saveView(long userId, long eventId) {
        saveUserAction(userId, eventId, ActionTypeProto.ACTION_VIEW);
    }

    public void saveLike(long userId, long eventId) {
        saveUserAction(userId, eventId, ActionTypeProto.ACTION_LIKE);
    }

    public void saveRegister(long userId, long eventId) {
        saveUserAction(userId, eventId, ActionTypeProto.ACTION_REGISTER);
    }

    private void saveUserAction(long userId, long eventId, ActionTypeProto actionType) {
        Instant now = Instant.now();
        UserActionProto request = UserActionProto.newBuilder()
                .setUserId(userId)
                .setEventId(eventId)
                .setActionType(actionType)
                .setTimestamp(Timestamp.newBuilder()
                        .setSeconds(now.getEpochSecond())
                        .setNanos(now.getNano()))
                .build();
        userActionClient.collectUserAction(request);
    }
}
