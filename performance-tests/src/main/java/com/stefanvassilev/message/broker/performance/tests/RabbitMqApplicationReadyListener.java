package com.stefanvassilev.message.broker.performance.tests;

import com.stefanvassilev.message.broker.PerformanceTestsRabbitMqEueueReceiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.stream.IntStream;

import static com.stefanvassilev.message.broker.config.RabbitMqConfiguration.TOPIC_EXCHANGE;

@Component
public class RabbitMqApplicationReadyListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitMqApplicationReadyListener.class);

    private final RabbitTemplate rabbitTemplate;

    @Autowired
    private PerformanceTestsRabbitMqEueueReceiver performanceTestsRabbitMqEueueReceiver;

    public RabbitMqApplicationReadyListener(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationEvent() {
        IntStream.range(0, 10).forEach(i -> {
                    LOGGER.info("Sending bytes");
                    rabbitTemplate.convertAndSend(TOPIC_EXCHANGE, "perf", "Hello ");
                }

        );


        LOGGER.info("received #:{}", performanceTestsRabbitMqEueueReceiver.getReceivedMessagesCount());

        // TODO: 4/29/2020 DO work here


    }
}
