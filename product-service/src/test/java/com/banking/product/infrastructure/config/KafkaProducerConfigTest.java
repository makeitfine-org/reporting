package com.banking.product.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaProducerConfigTest {

    @Test
    void testBeans() {
        KafkaProducerConfig config = new KafkaProducerConfig();
        ReflectionTestUtils.setField(config, "bootstrapServers", "localhost:9092");

        ProducerFactory<String, Object> factory = config.producerFactory();
        assertThat(factory).isNotNull();

        KafkaTemplate<String, Object> template = config.kafkaTemplate();
        assertThat(template).isNotNull();
    }
}
