package com.stefanvassilev.message.broker.performance.tests.controller;

import com.stefanvassilev.message.broker.lib.aspect.MeteredAspect;
import com.stefanvassilev.message.broker.performance.tests.domain.PerfTestResults;
import com.stefanvassilev.message.broker.rabbit.RabbitMQConsumer;
import com.stefanvassilev.message.broker.rabbit.kafka.configuration.consumer.KafkaConsumer;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.apache.commons.lang3.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.LongStream;

import static com.stefanvassilev.message.broker.rabbit.config.RabbitMqConfiguration.TOPIC_EXCHANGE;
import static com.stefanvassilev.message.broker.rabbit.kafka.configuration.producer.KafkaProducerConfiguration.PERFORMANCE_TOPIC_NAME;

@RestController
@RequestMapping("/perf")
public class PerformanceTestsController implements MeterBinder {

    private static final Logger LOGGER = LoggerFactory.getLogger(PerformanceTestsController.class);

    private final RabbitMQConsumer rabbitMQConsumer;
    private final KafkaConsumer kafkaConsumer;
    private final MeteredAspect meteredAspect;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final MeterRegistry meterRegistry;
    private final AtomicLong kafkaTimeTakenRatio = new AtomicLong();
    private final AtomicLong rabbitMQTimeTakenRatio = new AtomicLong();

    @Autowired
    public PerformanceTestsController(KafkaTemplate<String, String> kafkaTemplate, RabbitTemplate rabbitTemplate, RabbitMQConsumer rabbitMQConsumer, KafkaConsumer kafkaConsumer, MeteredAspect meteredAspect, MeterRegistry meterRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        this.rabbitTemplate = rabbitTemplate;
        this.rabbitMQConsumer = rabbitMQConsumer;
        this.kafkaConsumer = kafkaConsumer;
        this.meteredAspect = meteredAspect;
        this.meterRegistry = meterRegistry;
    }

    @PostMapping("/kafka/messageCount/{messageCount}/messageLength/{messageLength}")
    public ResponseEntity<PerfTestResults> triggerKafkaTests(@PathVariable("messageCount") Integer messageCount, @PathVariable("messageLength") Integer messageLength) throws InterruptedException {
        var res = new PerfTestResults();


        var start = System.currentTimeMillis();
        synchronized (kafkaConsumer) {
            kafkaConsumer.setExpectedMessagesCount(new CountDownLatch(messageCount));
            LongStream.range(0, messageCount).forEach(i ->
                    kafkaTemplate.send(PERFORMANCE_TOPIC_NAME, new String(RandomUtils.nextBytes(messageLength), StandardCharsets.UTF_8)));

            kafkaConsumer.getExpectedMessagesCount().await();
            var end = System.currentTimeMillis();

            setPerfTestResults(res, start, end, "kafka", messageCount);
            kafkaConsumer.setExpectedMessagesCount(null);
        }


        return ResponseEntity.ok(res);
    }

    @PostMapping("/rabbit/messageCount/{messageCount}/messageLength/{messageLength}")
    @Timed(description = "time taken to complete rabbitMq performance tests")
    public ResponseEntity<PerfTestResults> triggerRabbitMqTests(@PathVariable("messageCount") Integer messageCount, @PathVariable("messageLength") Integer messageLength) throws InterruptedException {
        var res = new PerfTestResults();

        var start = System.currentTimeMillis();
        synchronized (rabbitMQConsumer) {
            rabbitMQConsumer.setExpectedMessagesCount(new CountDownLatch(messageCount));

            LongStream.range(0, messageCount).forEach(i -> rabbitTemplate.convertAndSend(TOPIC_EXCHANGE, "perf", new String(RandomUtils.nextBytes(messageLength), StandardCharsets.UTF_8), new CorrelationData(UUID.randomUUID().toString())));
            rabbitMQConsumer.getExpectedMessagesCount().await();

            var end = System.currentTimeMillis();
            setPerfTestResults(res, start, end, "RabbitMQ", messageCount);
            rabbitMQConsumer.setExpectedMessagesCount(null);
        }


        return ResponseEntity.ok(res);
    }

    private void setPerfTestResults(PerfTestResults res, long start, long end, String messageBrokerUsed, Integer messageCount) {
        long timeTaken = end - start;
        res.setTimeTaken(timeTaken);

        String methodName = switch (messageBrokerUsed) {
            case "kafka" -> "listen";
            case "RabbitMQ" -> "receiveMessage";
            default -> throw new IllegalStateException("Unknown message broker: " + messageBrokerUsed);
        };

        List<Long> executionTimeList = meteredAspect.getExecutionTimeByMethodName().get(methodName);
        long avgTime = executionTimeList.stream().reduce(0L, Long::sum) / messageCount;
        res.setAvgRoundTripTime(avgTime);
        res.setMessageBrokerUsed(messageBrokerUsed);
        res.setMessagesSent((long) executionTimeList.size());

        LOGGER.info("Reporting for {} round-trip time to prometheus: {}", messageBrokerUsed, avgTime);
        meterRegistry.gauge(messageBrokerUsed + ".round.trip", avgTime);
        long ratio = messageCount / timeTaken;
        LOGGER.info("Reporting for {} processing time ratio  to prometheus: {}", messageBrokerUsed, ratio);

        switch (messageBrokerUsed) {
            case "kafka" -> kafkaTimeTakenRatio.set(ratio);
            case "RabbitMQ" -> rabbitMQTimeTakenRatio.set(ratio);
            default -> throw new IllegalStateException("unknown msg broker: " + messageBrokerUsed);
        }


        meteredAspect.reset();
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        Gauge.builder("kafka.message.time_taken.ratio", kafkaTimeTakenRatio::get)
                .description("ratio between messages count and time taken for complete round trip")
                .register(meterRegistry);

        Gauge.builder("rabbitMQ.message.time_taken.ratio", rabbitMQTimeTakenRatio::get)
                .description("ratio between messages count and time taken for complete round trip")
                .register(meterRegistry);
    }
}
