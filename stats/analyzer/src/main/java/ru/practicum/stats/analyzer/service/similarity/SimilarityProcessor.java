package ru.practicum.stats.analyzer.service.similarity;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Service;
import ru.practicum.stats.analyzer.config.SimilarityProcKafkaConfig;
import ru.practicum.ewm.stats.avro.EventsSimilarityAvro;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
public class SimilarityProcessor implements Runnable {
    private final SimilarityService similarityService;
    private final KafkaConsumer<String, EventsSimilarityAvro> kafkaConsumer;
    private final Duration poolTimeout;

    public SimilarityProcessor(SimilarityProcKafkaConfig kafkaConfig, SimilarityService similarityService) {
        this.similarityService = similarityService;
        kafkaConsumer = new KafkaConsumer<>(kafkaConfig.properties());
        kafkaConsumer.subscribe(List.of(kafkaConfig.topic()));
        poolTimeout = kafkaConfig.poolTimeout();
    }

    @Override
    public void run() {
        try {
            while (true) {
                ConsumerRecords<String, EventsSimilarityAvro> records = kafkaConsumer.poll(poolTimeout);
                for (ConsumerRecord<String, EventsSimilarityAvro> record : records) {
                    similarityService.handleSimilarity(record.value());
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
