package com.stefanvassilev.message.broker.performance.tests.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class PerfTestResults {

    private String messageBrokerUsed;
    private Long messagesSent;
    private Long avgRoundTripTime;
    private Long timeTaken;

    public PerfTestResults() {
    }

}
