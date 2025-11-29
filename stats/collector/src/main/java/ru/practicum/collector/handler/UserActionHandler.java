package ru.practicum.collector.handler;

import lombok.RequiredArgsConstructor;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.proto.ActionTypeProto;
import ru.practicum.ewm.stats.proto.UserActionProto;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class UserActionHandler {
    private final KafkaProducer<String, SpecificRecordBase> producer;

    @Value("${kafka.topics.user-actions}")
    private String topicUserActions;

    public void kafkaUserAction(UserActionProto userActionProto) {
        UserActionAvro avro = userActionToAvro(userActionProto);
        producer.send(new ProducerRecord<>(topicUserActions, avro));
    }

    private UserActionAvro userActionToAvro(UserActionProto userActionProto) {
        UserActionAvro.Builder builder = UserActionAvro.newBuilder()
                .setUserId(userActionProto.getUserId())
                .setEventId(userActionProto.getEventId())
                .setTimestamp(toInstant(userActionProto.getTimestamp()))
                .setActionType(actionTypeToAvro(userActionProto.getActionType()));

        return builder.build();
    }

    private ActionTypeAvro actionTypeToAvro(ActionTypeProto actionTypeProto) {
        return switch (actionTypeProto) {
            case ACTION_LIKE -> ActionTypeAvro.LIKE;
            case ACTION_VIEW -> ActionTypeAvro.VIEW;
            case ACTION_REGISTER -> ActionTypeAvro.REGISTER;
            case UNRECOGNIZED -> throw new IllegalArgumentException("Unrecognized action type: " + actionTypeProto);
        };
    }

    private Instant toInstant(com.google.protobuf.Timestamp timestamp) {
        return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
    }
}