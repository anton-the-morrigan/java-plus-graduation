package ru.practicum.analyzer.processor;

import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.practicum.analyzer.entity.UserAction;
import ru.practicum.analyzer.handler.UserActionHandler;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
public class UserActionProcessor implements Runnable {

    private final Duration CONSUME_ATTEMPT_TIMEOUT = Duration.ofMillis(1000);

    @Autowired
    @Qualifier("kafkaConsumerUserAction")
    private KafkaConsumer<String, SpecificRecordBase> consumer;
    @Value("${kafka.topics.user-action}")
    private String userActionTopic;
    @Autowired
    UserActionHandler userActionHandler;

    public void run() {
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));
            consumer.subscribe(List.of(userActionTopic));
            log.info("Подписка на топик " + userActionTopic);

            while (true) {
                ConsumerRecords<String, SpecificRecordBase> records = consumer.poll(CONSUME_ATTEMPT_TIMEOUT);

                for (ConsumerRecord<String, SpecificRecordBase> record : records) {
                    if(!(record.value() instanceof UserActionAvro userActionAvro)) {
                        log.warn("Неизвестное значение записи: {}", record.value().getClass().getSimpleName());
                        continue;
                    }
                    UserAction userAction = userActionHandler.processUserAction(userActionAvro);
                }
                consumer.commitAsync();
            }
        } catch (WakeupException ignore) {
            // игнорируем - закрываем консьюмер и продюсер в блоке finally
        } catch (Exception e) {
            log.error("Ошибка во время обработки активности пользователя", e);
        } finally {
            try {
                consumer.commitSync();
            } finally {
                log.info("Закрываем консьюмер");
                consumer.close();
            }
        }
    }
}
