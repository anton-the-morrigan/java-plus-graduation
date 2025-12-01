package ru.practicum.stats.analyzer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Properties;

@ConfigurationProperties("analyzer.similarity.kafka.consumer")
public record SimilarityProcKafkaConfig(Properties properties, String topic, Duration poolTimeout) {
}
