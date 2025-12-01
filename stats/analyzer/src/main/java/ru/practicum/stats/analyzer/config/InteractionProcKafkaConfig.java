package ru.practicum.stats.analyzer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Properties;

@ConfigurationProperties("analyzer.interaction.kafka.consumer")
public record InteractionProcKafkaConfig(Properties properties, String topic, Duration poolTimeout) {
}
