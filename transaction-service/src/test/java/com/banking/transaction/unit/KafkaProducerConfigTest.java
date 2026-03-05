package com.banking.transaction.unit;

import com.banking.transaction.infrastructure.config.KafkaProducerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaProducerConfigTest {

    @Test
    void producerFactory_returnsNonNull() {
        KafkaProducerConfig config = new KafkaProducerConfig();
        ReflectionTestUtils.setField(config, "bootstrapServers", "localhost:9092");

        ProducerFactory<String, Object> factory = config.producerFactory();

        assertThat(factory).isNotNull();
    }

    @Test
    void kafkaTemplate_returnsNonNull() {
        KafkaProducerConfig config = new KafkaProducerConfig();
        ReflectionTestUtils.setField(config, "bootstrapServers", "localhost:9092");

        KafkaTemplate<String, Object> template = config.kafkaTemplate();

        assertThat(template).isNotNull();
    }
}
