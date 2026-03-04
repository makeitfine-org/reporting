package com.banking.reporting.unit;

import com.banking.reporting.infrastructure.config.KafkaConsumerConfig;
import com.banking.reporting.infrastructure.config.KafkaProducerConfig;
import com.banking.reporting.infrastructure.config.RedisConfig;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ConfigTest {

    @Test
    void testRedisConfig() {
        RedisConfig config = new RedisConfig();
        assertThat(config.objectMapper()).isNotNull();

        RedisConnectionFactory factory = mock(RedisConnectionFactory.class);
        assertThat(config.stringRedisTemplate(factory)).isNotNull();
    }

    @Test
    void testKafkaProducerConfig() {
        KafkaProducerConfig config = new KafkaProducerConfig();
        ReflectionTestUtils.setField(config, "bootstrapServers", "localhost:9092");
        assertThat(config.producerFactory()).isNotNull();
        assertThat(config.kafkaTemplate()).isNotNull();
    }

    @Test
    void testKafkaConsumerConfig() {
        KafkaConsumerConfig config = new KafkaConsumerConfig();
        ReflectionTestUtils.setField(config, "bootstrapServers", "localhost:9092");
        ReflectionTestUtils.setField(config, "groupId", "test-group");

        var consumerFactory = config.consumerFactory();
        assertThat(consumerFactory).isNotNull();

        KafkaTemplate<String, Object> template = mock(KafkaTemplate.class);
        assertThat(config.reportingKafkaListenerFactory(consumerFactory, template)).isNotNull();
    }

}
