package com.stefanvassilev.message.broker.performance.tests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class RabbitMqApplicationReadyListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitMqApplicationReadyListener.class);

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationEvent() {

        // TODO: 4/29/2020 DO work here


    }
}
