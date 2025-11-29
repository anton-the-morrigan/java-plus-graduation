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
import ru.practicum.analyzer.entity.EventSimilarity;
import ru.practicum.analyzer.handler.EventSimilarityHandler;
import ru.practicum.ewm.stats.avro.EventsSimilarityAvro;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
public class EventSimilarityProcessor implements Runnable {

    private final Duration CONSUME_ATTEMPT_TIMEOUT = Duration.ofMillis(1000);

    @Autowired
    @Qualifier("kafkaConsumerEventSimilarity")
    private KafkaConsumer<String, SpecificRecordBase> consumer;
    @Autowired
    private EventSimilarityHandler eventSimilarityHandler;
    @Value("${kafka.topics.event-similarity}")
    private String eventSimilarityTopic;

    @Override
    public void run() {
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));
            consumer.subscribe(List.of(eventSimilarityTopic));
            log.info("Подписка на топик " + eventSimilarityTopic);

            while (true) {
                ConsumerRecords<String, SpecificRecordBase> records = consumer.poll(CONSUME_ATTEMPT_TIMEOUT);

                for(ConsumerRecord<String, SpecificRecordBase> record : records) {
                    if(!(record.value() instanceof EventsSimilarityAvro eventSimilarityAvro)) {
                        log.warn("Неизвестное значение записи: {}", record.value().getClass().getSimpleName());
                        continue;
                    }
                    EventSimilarity eventSimilarity = eventSimilarityHandler.processEventSimilarity(eventSimilarityAvro);
                }
                consumer.commitSync();
            }
        } catch (WakeupException ignore) {
            // игнорируем - закрываем консьюмер и продюсер в блоке finally
        } catch (Exception e) {
            log.error("Ошибка во время обработки схожести событий", e);
        } finally {
            try {
                consumer.commitSync();
            } finally {
                log.info("Закрываем consumer");
                consumer.close();
            }
        }
    }
}
