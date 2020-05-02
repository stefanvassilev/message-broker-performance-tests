# Kafka vs RabbitMQ analysis [![Build Status](https://travis-ci.com/stefanvassilev/message-broker-performance-tests.svg?branch=master)](https://travis-ci.com/stefanvassilev/message-broker-performance-tests)


## Test Setup 

Both Apache Kafka and RabbitMQ are ran from Docker containers. 
* https://hub.docker.com/_/rabbitmq - link for official `RabbitMQ` image
* https://hub.docker.com/r/wurstmeister/kafka/ - link for `Apache kafka`image provided by `wurstmeister`, 
however in order for kafka to work `Zookeper` service is required as well: https://hub.docker.com/r/wurstmeister/zookeeper/

### Notes to configuration

* kafka is set up to run with a single kafka broker only

## Build

Running the spring boot test application can be done in a few simple steps: 

1. In order to run Spring Boot application you need to have docker installed. 
Running the following command would set up both kafka and rabbitMq dependencies: 
    ```shell script
    docker-compose up -d
    ```
*Note: in `docker-compose.yml` prometheus and grafana are included as well, however data reported to spring-actuator
did not include anything that could be used for benchmarking purposes. However, feel free to explore if you want :) 
prometheus is available under `localhost:9090`,
grafana is available under `localhost:3000`*

   
2. Maven and JDK 14 are required as well
    ```shell script
    mvn clean install 
    cd ./performance-tests/target
    java -jar performance-tests-0.0.1-SNAPSHOT.jar
    ```
    Alternatively this project is available as docker image: https://hub.docker.com/r/stefanvassilev/message-broker-performance-tests

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

[vegetta](https://github.com/tsenart/vegeta) was the chosen http client for running the tests. 
### Test Case description
Running same test case (10000 messages with different message lengths consisting random bytes as payload) repeatedly for 10 minutes on both single-node
RabbitMQ and Kafka yielded the following vegetta report.


### Bulk data of Kafka's results
```shell script

#message length of 1000 
$echo "POST http://localhost:8080/perf/kafka/messageCount/10000/messageLength/1000" | vegeta attack -duration=600s -max-workers=1 | tee results_kafka_big_length.bin | vegeta report
Requests      [total, rate, throughput]         236, 0.39, 0.39
Duration      [total, attack, wait]             10m5s, 10m2s, 2.763s
Latencies     [min, mean, 50, 90, 95, 99, max]  1.243s, 2.562s, 2.612s, 2.96s, 3.007s, 3.092s, 3.146s
Bytes In      [total, mean]                     20609, 87.33
Bytes Out     [total, mean]                     0, 0.00
Success       [ratio]                           100.00%
Status Codes  [code:count]                      200:236

#message length of 2500 
$ echo "POST http://localhost:8080/perf/kafka/messageCount/10000/messageLength/2500" | vegeta attack -duration=600s -max-workers=1 | tee results_kafka_big_length.bin | vegeta report
Requests      [total, rate, throughput]         79, 0.13, 0.13
Duration      [total, attack, wait]             10m12s, 10m4s, 8.129s
Latencies     [min, mean, 50, 90, 95, 99, max]  3.362s, 7.749s, 8.102s, 8.519s, 8.623s, 9.06s, 9.084s
Bytes In      [total, mean]                     6868, 86.94
Bytes Out     [total, mean]                     0, 0.00
Success       [ratio]                           100.00%
Status Codes  [code:count]                      200:79
Error Set:


#message length of 5000 
echo "POST http://localhost:8080/perf/kafka/messageCount/10000/messageLength/5000" | vegeta attack -duration=600s -max-workers=1 | tee results_kafka_big_length.bin | vegeta report
Requests      [total, rate, throughput]         24, 0.04, 0.04
Duration      [total, attack, wait]             10m29s, 10m9s, 20.084s
Latencies     [min, mean, 50, 90, 95, 99, max]  9.752s, 26.192s, 27.485s, 28.594s, 28.9s, 29.45s, 29.45s
Bytes In      [total, mean]                     2098, 87.42
Bytes Out     [total, mean]                     0, 0.00
Success       [ratio]                           100.00%
Status Codes  [code:count]                      200:24


#message length of 10000 
$ echo "POST http://localhost:8080/perf/kafka/messageCount/10000/messageLength/10000" | vegeta attack -duration=600s -max-workers=1 | tee results_kafka_big_length.bin | vegeta report
  Requests      [total, rate, throughput]         55, 0.09, 0.09
  Duration      [total, attack, wait]             10m17s, 10m7s, 9.971s
  Latencies     [min, mean, 50, 90, 95, 99, max]  9.922s, 11.215s, 10.33s, 13.656s, 15.67s, 18.486s, 18.564s
  Bytes In      [total, mean]                     4887, 88.85
  Bytes Out     [total, mean]                     0, 0.00
  Success       [ratio]                           100.00%
  Status Codes  [code:count]                      200:55
  Error Set:

# message length of 10000 and 200000 messages
$ echo "POST http://localhost:8080/perf/kafka/messageCount/20000/messageLength/10000" | vegeta attack -duration=600s -max-workers=1 | tee results_kafka_big_length.bin | vegeta report
Requests      [total, rate, throughput]         28, 0.05, 0.04
Duration      [total, attack, wait]             10m41s, 10m12s, 29.345s
Latencies     [min, mean, 50, 90, 95, 99, max]  21.3s, 22.897s, 21.988s, 27.58s, 29.41s, 30s, 30s
Bytes In      [total, mean]                     2403, 85.82
Bytes Out     [total, mean]                     0, 0.00
Success       [ratio]                           96.43%
Status Codes  [code:count]                      0:1  200:27
Error Set:
Post http://localhost:8080/perf/kafka/messageCount/20000/messageLength/10000: net/http: request canceled (Client.Timeout exceeded while awaiting headers)

```

### Bulk data of RabbitMQ's results
```shell script
#message length of 1000 
$ echo "POST http://localhost:8080/perf/rabbit/messageCount/10000/messageLength/1000" | vegeta attack -duration=600s -max-workers=1 | tee results_kafka_big_length.bin | vegeta report
Requests      [total, rate, throughput]         146, 0.24, 0.24
Duration      [total, attack, wait]             10m4s, 10m2s, 2.169s
Latencies     [min, mean, 50, 90, 95, 99, max]  2.169s, 4.135s, 4.146s, 4.475s, 5.059s, 5.551s, 5.795s
Bytes In      [total, mean]                     13135, 89.97
Bytes Out     [total, mean]                     0, 0.00
Success       [ratio]                           100.00%
Status Codes  [code:count]                      200:146
Error Set:

# message length of 2500
echo "POST http://localhost:8080/perf/rabbit/messageCount/10000/messageLength/2500" | vegeta attack -duration=600s -max-workers=1 | tee results_kafka_big_length.bin | vegeta report
Requests      [total, rate, throughput]         130, 0.22, 0.22
Duration      [total, attack, wait]             10m3s, 10m1s, 2.607s
Latencies     [min, mean, 50, 90, 95, 99, max]  2.607s, 4.64s, 4.58s, 5.249s, 5.688s, 6.03s, 6.071s
Bytes In      [total, mean]                     11748, 90.37
Bytes Out     [total, mean]                     0, 0.00
Success       [ratio]                           100.00%
Status Codes  [code:count]                      200:130
Error Set:

# message length of 5000
5000 rabbit
echo "POST http://localhost:8080/perf/rabbit/messageCount/10000/messageLength/5000" | vegeta attack -duration=600s -max-workers=1 | tee results_kafka_big_length.bin | vegeta report
Requests      [total, rate, throughput]         113, 0.19, 0.19
Duration      [total, attack, wait]             10m10s, 10m5s, 5.486s
Latencies     [min, mean, 50, 90, 95, 99, max]  4.786s, 5.403s, 5.329s, 5.827s, 6.156s, 6.446s, 6.449s
Bytes In      [total, mean]                     10260, 90.80
Bytes Out     [total, mean]                     0, 0.00
Success       [ratio]                           100.00%
Status Codes  [code:count]                      200:113
Error Set:
 

# message length of 10000 
$ echo "POST http://localhost:8080/perf/rabbit/messageCount/10000/messageLength/10000" | vegeta attack -duration=600s -max-workers=1 | tee results_rabbit.bin | vegeta report
Requests      [total, rate, throughput]         109, 0.18, 0.18
Duration      [total, attack, wait]             10m11s, 10m5s, 5.791s
Latencies     [min, mean, 50, 90, 95, 99, max]  4.669s, 5.606s, 5.501s, 5.909s, 6.286s, 11.812s, 13.265s
Bytes In      [total, mean]                     9922, 91.03
Bytes Out     [total, mean]                     0, 0.00
Success       [ratio]                           100.00%
Status Codes  [code:coun]


# message length of 10000 and 200000 messages
$ echo "POST http://localhost:8080/perf/rabbit/messageCount/20000/messageLength/10000" | vegeta attack -duration=600s -max-workers=1 | tee results_rabbit_big_length.bin | vegeta report
Requests      [total, rate, throughput]         58, 0.10, 0.09
Duration      [total, attack, wait]             10m17s, 10m4s, 13.313s
Latencies     [min, mean, 50, 90, 95, 99, max]  9.62s, 10.64s, 10.148s, 13.015s, 13.294s, 13.935s, 13.963s
Bytes In      [total, mean]                     5322, 91.76
Bytes Out     [total, mean]                     0, 0.00
Success       [ratio]                           100.00%
Status Codes  [code:count]                      200:58
Error Set:
# message length of 10000 and 200000 messages

```

Giving the upper-hand to `RabbitMQ` as per latency's mean of 5.606s, versus 11.215s.


Doing the same test again however with 20000 messages: 
```shell script
$ echo "POST http://localhost:8080/perf/kafka/messageCount/20000/messageLength/10000" | vegeta attack -duration=600s -max-workers=1 | tee results_kafka_big_length.bin | vegeta report
Requests      [total, rate, throughput]         28, 0.05, 0.04
Duration      [total, attack, wait]             10m41s, 10m12s, 29.345s
Latencies     [min, mean, 50, 90, 95, 99, max]  21.3s, 22.897s, 21.988s, 27.58s, 29.41s, 30s, 30s
Bytes In      [total, mean]                     2403, 85.82
Bytes Out     [total, mean]                     0, 0.00
Success       [ratio]                           96.43%
Status Codes  [code:count]                      0:1  200:27
Error Set:
Post http://localhost:8080/perf/kafka/messageCount/20000/messageLength/10000: net/http: request canceled (Client.Timeout exceeded while awaiting headers)




$ echo "POST http://localhost:8080/perf/rabbit/messageCount/20000/messageLength/10000" | vegeta attack -duration=600s -max-workers=1 | tee results_rabbit_big_length.bin | vegeta report
Requests      [total, rate, throughput]         58, 0.10, 0.09
Duration      [total, attack, wait]             10m17s, 10m4s, 13.313s
Latencies     [min, mean, 50, 90, 95, 99, max]  9.62s, 10.64s, 10.148s, 13.015s, 13.294s, 13.935s, 13.963s
Bytes In      [total, mean]                     5322, 91.76
Bytes Out     [total, mean]                     0, 0.00
Success       [ratio]                           100.00%
Status Codes  [code:count]                      200:58
Error Set:
```

