package com.stefanvassilev.message.broker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

import static com.stefanvassilev.message.broker.config.RabbitMqConfiguration.PERFORMANCE_TESTS_QUEUE_NAME;

@Component
public class PerformanceTestsRabbitMqEueueReceiver {

    private static final Logger LOGGER = LoggerFactory.getLogger(PerformanceTestsRabbitMqEueueReceiver.class);
    private final AtomicInteger receivedMessagesCount = new AtomicInteger(0);

    @RabbitListener(queues = PERFORMANCE_TESTS_QUEUE_NAME)
    public void receiveMessage(String message) {
        LOGGER.info("Received <{}>", message);
        receivedMessagesCount.incrementAndGet();
    }


    public AtomicInteger getReceivedMessagesCount() {
        return receivedMessagesCount;
    }
}
