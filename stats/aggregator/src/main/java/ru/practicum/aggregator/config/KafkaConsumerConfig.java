package ru.practicum.aggregator.config;

import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.practicum.avro.deserializer.UserActionDeserializer;

import java.util.Properties;

@Configuration
public class KafkaConsumerConfig {
    @Value("${spring.kafka.bootstrap-servers}")
    private String BOOTSTRAP_SERVERS;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Bean
    public KafkaConsumer<String, SpecificRecordBase> kafkaConsumer() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, UserActionDeserializer.class);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);

        return new KafkaConsumer<>(props);
    }
}
