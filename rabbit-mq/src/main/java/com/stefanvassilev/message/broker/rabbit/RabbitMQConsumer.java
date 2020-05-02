package com.stefanvassilev.message.broker.rabbit;

import com.stefanvassilev.message.broker.lib.aspect.Metered;
import com.stefanvassilev.message.broker.rabbit.config.RabbitMqConfiguration;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;

@Component
@Getter
@Setter
public class RabbitMQConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitMQConsumer.class);
    private CountDownLatch expectedMessagesCount;


    @RabbitListener(queues = RabbitMqConfiguration.PERFORMANCE_TESTS_QUEUE_NAME)
    @Metered
    public void receiveMessage(String message) {
        if (expectedMessagesCount == null) {
            throw new IllegalStateException("expected message count has to be set before running method");
        }

        LOGGER.debug("Received RabbitMQ: {}", message);
        expectedMessagesCount.countDown();
    }


}
