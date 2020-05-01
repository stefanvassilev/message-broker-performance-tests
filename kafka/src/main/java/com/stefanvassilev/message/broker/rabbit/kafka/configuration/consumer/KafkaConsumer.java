package com.stefanvassilev.message.broker.rabbit.kafka.configuration.consumer;

import com.stefanvassilev.message.broker.lib.aspect.Metered;
import com.stefanvassilev.message.broker.rabbit.kafka.configuration.producer.KafkaProducerConfiguration;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;

@Component
@Getter
@Setter
public class KafkaConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaConsumer.class);
    private CountDownLatch expectedMessagesCount;

    @Metered
    @KafkaListener(topics = KafkaProducerConfiguration.PERFORMANCE_TOPIC_NAME, groupId = "perf")
    public void listen(String msg) {
        if (expectedMessagesCount == null) {
            throw new IllegalStateException("expected message count has to be set before running method");
        }
        LOGGER.info("Received msg: {}", msg);
        expectedMessagesCount.countDown();

    }

}
