# Kafka vs RabbitMQ analysis [![Build Status](https://travis-ci.com/stefanvassilev/message-broker-performance-tests.svg?branch=master)](https://travis-ci.com/stefanvassilev/message-broker-performance-tests)


## Test Setup 

Both Apache Kafka and RabbitMQ are ran from Docker containers. 
* https://hub.docker.com/_/rabbitmq - link for official `RabbitMQ` image
* https://hub.docker.com/r/wurstmeister/kafka/ - link for `Apache kafka`image provided by `wurstmeister`, 
however in order for kafka to work `Zookeper` service is required as well: https://hub.docker.com/r/wurstmeister/zookeeper/

### Notes to configuration

* kafka is set up to run with a single kafka broker only

## Build

Running the spring boot test application can be done in simple steps: 

1. In order to run spring boot application you need to have docker installed. 
Running the following command would set up both kafka and rabbitMq dependencies: 
    ```shell script
    docker-compose up -d
    ```
*Note: in `docker-compose.yml` prometheus and grafana is included as well, however data reported to spring-actuator
did not include anything that could be used for benchmarking purposes. However, feel free to explore if you want :) 
prometheus is available under `localhost:9090`
grafana is available under `localhost:3000`*

   
2. Maven and JDK 14 is required as well
    ```shell script
    mvn clean install 
    cd ./performance-tests/target
    java -jar performance-tests-0.0.1-SNAPSHOT.jar
    ```
    Alternatively this project is available as docker image as well: https://hub.docker.com/r/stefanvassilev/message-broker-performance-tests

## Test results 

Test application exposes a single REST controller with two endpoints: 
* perf/kafka/messageCount/{messageCount}/messageLength/{messageLength}
* perf/rabbit/messageCount/{messageCount}/messageLength/{messageLength}

#### Returned is a json object containing following structure: 
* messageBrokerUsed: Name of broker which was used for given test case, either RabbitMQ or kafka.
* messagesSent: Number of messages that did a round-trip from application to message broker and back.
* avgRoundTripTime: Average round trip time for a single message.
* timeTaken: Elapsed time of all messages going through the round trip. 

*Note: This test does not aim to be a full-blown comparison between Kafka and RabbitMQ as the size of 
the topics and queues is limited.* 

[vegetta](https://github.com/tsenart/vegeta) was used as http client. 
Running same test case (10000 messages with 10000 random bytes as payload) for 10 minutes on both single-node
RabbitMQ and Kafka yielded the following vegetta report.
```shell script
$ echo "POST http://localhost:8080/perf/kafka/messageCount/10000/messageLength/10000" | vegeta attack -duration=600s -max-workers=1 | tee results_kafka_big_length.bin | vegeta report
  Requests      [total, rate, throughput]         55, 0.09, 0.09
  Duration      [total, attack, wait]             10m17s, 10m7s, 9.971s
  Latencies     [min, mean, 50, 90, 95, 99, max]  9.922s, 11.215s, 10.33s, 13.656s, 15.67s, 18.486s, 18.564s
  Bytes In      [total, mean]                     4887, 88.85
  Bytes Out     [total, mean]                     0, 0.00
  Success       [ratio]                           100.00%
  Status Codes  [code:count]                      200:55
  Error Set:
```

```shell script
$ echo "POST http://localhost:8080/perf/rabbit/messageCount/10000/messageLength/10000" | vegeta attack -duration=600s -max-workers=1 | tee results_rabbit.bin | vegeta report
Requests      [total, rate, throughput]         109, 0.18, 0.18
Duration      [total, attack, wait]             10m11s, 10m5s, 5.791s
Latencies     [min, mean, 50, 90, 95, 99, max]  4.669s, 5.606s, 5.501s, 5.909s, 6.286s, 11.812s, 13.265s
Bytes In      [total, mean]                     9922, 91.03
Bytes Out     [total, mean]                     0, 0.00
Success       [ratio]                           100.00%
Status Codes  [code:coun
```

Giving the upper-hand to `RabbitMQ` as per latency's mean of 5.606s, versus 11.215s. `



