package ru.practicum.stats.analyzer.service.interaction;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Service;
import ru.practicum.stats.analyzer.config.InteractionProcKafkaConfig;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
public class InteractionProcessor implements Runnable {
    private final InteractionService interactionService;
    private final KafkaConsumer<String, UserActionAvro> kafkaConsumer;
    private final Duration poolTimeout;

    public InteractionProcessor(InteractionProcKafkaConfig kafkaConfig, InteractionService interactionService) {
        this.interactionService = interactionService;
        kafkaConsumer = new KafkaConsumer<>(kafkaConfig.properties());
        kafkaConsumer.subscribe(List.of(kafkaConfig.topic()));
        poolTimeout = kafkaConfig.poolTimeout();
    }

    @Override
    public void run() {
        try {
            while (true) {
                ConsumerRecords<String, UserActionAvro> records = kafkaConsumer.poll(poolTimeout);
                for (ConsumerRecord<String, UserActionAvro> record : records) {
                    interactionService.handleInteraction(record.value());
                }
            }
        } catch (WakeupException ignored) {
            // игнорируем - закрываем консьюмер и продюсер в блоке finally
        } catch (Exception e) {
            log.error("Ошибка", e);
        } finally {
            kafkaConsumer.close(Duration.ofSeconds(10));
        }
    }
}
