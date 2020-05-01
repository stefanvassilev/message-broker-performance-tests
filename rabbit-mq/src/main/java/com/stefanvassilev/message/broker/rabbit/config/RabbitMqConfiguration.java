package com.stefanvassilev.message.broker.rabbit.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class RabbitMqConfiguration {


    public static final String PERFORMANCE_TESTS_QUEUE_NAME = "performance-tests-queue";
    public static final String TOPIC_EXCHANGE = "perf-exchange";

    @Bean
    Queue queue() {
        return new Queue(PERFORMANCE_TESTS_QUEUE_NAME, false);
    }

    @Bean
    FanoutExchange exchange() {
        return new FanoutExchange(TOPIC_EXCHANGE);
    }

    @Bean
    Binding binding(Queue queue, FanoutExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange);
    }


}
