package com.stefanvassilev.message.broker.performance.tests.controller;

import com.stefanvassilev.message.broker.lib.aspect.MeteredAspect;
import com.stefanvassilev.message.broker.performance.tests.domain.PerfTestResults;
import com.stefanvassilev.message.broker.rabbit.RabbitMQConsumer;
import com.stefanvassilev.message.broker.rabbit.kafka.configuration.consumer.KafkaConsumer;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.stream.LongStream;

import static com.stefanvassilev.message.broker.rabbit.config.RabbitMqConfiguration.TOPIC_EXCHANGE;
import static com.stefanvassilev.message.broker.rabbit.kafka.configuration.producer.KafkaProducerConfiguration.PERFORMANCE_TOPIC_NAME;

@RestController
@RequestMapping("/perf")
public class PerformanceTestsController {


    private final RabbitMQConsumer rabbitMQConsumer;
    private final KafkaConsumer kafkaConsumer;
    private final MeteredAspect meteredAspect;
    private KafkaTemplate<String, String> kafkaTemplate;
    private RabbitTemplate rabbitTemplate;


    @Autowired
    public PerformanceTestsController(KafkaTemplate<String, String> kafkaTemplate, RabbitTemplate rabbitTemplate, RabbitMQConsumer rabbitMQConsumer, KafkaConsumer kafkaConsumer, MeteredAspect meteredAspect) {
        this.kafkaTemplate = kafkaTemplate;
        this.rabbitTemplate = rabbitTemplate;
        this.rabbitMQConsumer = rabbitMQConsumer;
        this.kafkaConsumer = kafkaConsumer;
        this.meteredAspect = meteredAspect;
    }

    @PostMapping("/kafka/{messageCount}")
    public ResponseEntity<PerfTestResults> triggerKafkaTests(@PathVariable("messageCount") Integer messageCount) throws InterruptedException {
        var res = new PerfTestResults();


        var start = System.currentTimeMillis();
        synchronized (kafkaConsumer) {
            kafkaConsumer.setExpectedMessagesCount(new CountDownLatch(messageCount));
            LongStream.range(0, messageCount).forEach(i ->
                    kafkaTemplate.send(PERFORMANCE_TOPIC_NAME, "Hello kafka"));

            kafkaConsumer.getExpectedMessagesCount().await();
            var end = System.currentTimeMillis();

            setPerfTestResults(res, start, end, "kafka");
            kafkaConsumer.setExpectedMessagesCount(null);
        }


        return ResponseEntity.ok(res);
    }

    @PostMapping("/rabbitMQ/{messageCount}")
    public ResponseEntity<PerfTestResults> triggerRabbitMqTests(@PathVariable("messageCount") Integer messageCount) throws InterruptedException {
        var res = new PerfTestResults();

        var start = System.currentTimeMillis();
        synchronized (rabbitMQConsumer) {
            rabbitMQConsumer.setExpectedMessagesCount(new CountDownLatch(messageCount));

            LongStream.range(0, messageCount).forEach(i -> rabbitTemplate.convertAndSend(TOPIC_EXCHANGE, "perf", "Hello", new CorrelationData(UUID.randomUUID().toString())));
            rabbitMQConsumer.getExpectedMessagesCount().await();

            var end = System.currentTimeMillis();
            setPerfTestResults(res, start, end, "RabbitMQ");
            rabbitMQConsumer.setExpectedMessagesCount(null);
        }


        return ResponseEntity.ok(res);
    }

    private void setPerfTestResults(PerfTestResults res, long start, long end, String messageBrokerUsed) {
        res.setTimeTaken(end - start);

        String methodName = switch (messageBrokerUsed) {
            case "kafka" -> "listen";
            case "RabbitMQ" -> "receiveMessage";
            default -> throw new IllegalStateException("Unknown message broker: " + messageBrokerUsed);
        };

        List<Long> executionTimeList = meteredAspect.getExecutionTimeByMethodName().get(methodName);
        System.out.println(executionTimeList.size());
        long avgTime = executionTimeList.stream().reduce(0L, Long::sum) / executionTimeList.size();
        res.setAvgRoundTripTime(avgTime);
        res.setMessageBrokerUsed(messageBrokerUsed);
        res.setMessagesSent((long) executionTimeList.size());

        meteredAspect.reset();
    }


}
