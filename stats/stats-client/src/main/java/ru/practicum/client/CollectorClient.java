package ru.practicum.client;

import com.google.protobuf.Timestamp;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.grpc.collector.UserActionControllerGrpc;
import ru.practicum.ewm.stats.grpc.user.ActionTypeProto;
import ru.practicum.ewm.stats.grpc.user.UserActionProto;

import java.time.Instant;

@Component
public class CollectorClient {

    @GrpcClient("collector")
    private UserActionControllerGrpc.UserActionControllerBlockingStub collectorStub;


    private void sendUserAction(long userId, long eventId, ActionTypeProto actionType) {
        UserActionProto request = UserActionProto.newBuilder()
                .setUserId(userId)
                .setEventId(eventId)
                .setActionType(actionType)
                .setTimestamp(Timestamp.newBuilder()
                        .setSeconds(Instant.now().getEpochSecond())
                        .setNanos(Instant.now().getNano())
                        .build())
                .build();

        collectorStub.collectUserAction(request);
    }

    public void sendEventView(long userId, long eventId) {
        sendUserAction(userId, eventId, ActionTypeProto.ACTION_VIEW);
    }

    public void sendEventLike(long userId, long eventId) {
        sendUserAction(userId, eventId, ActionTypeProto.ACTION_LIKE);
    }

    public void sendEventRegistration(long userId, long eventId) {
        sendUserAction(userId, eventId, ActionTypeProto.ACTION_REGISTER);
    }
}
