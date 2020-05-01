package com.stefanvassilev.message.broker.lib.aspect;


import lombok.Getter;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Aspect
@Component
@Getter
public class MeteredAspect {

    private final Map<String, List<Long>> executionTimeByMethodName = new HashMap<>();


    @Around("@annotation(com.stefanvassilev.message.broker.lib.aspect.Metered)")
    public Object meterMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = ((MethodSignature) joinPoint.getSignature()).getMethod().getName();

        long startTime = System.nanoTime();
        var returnVal = joinPoint.proceed();
        long end = System.nanoTime();


        var duration = TimeUnit.NANOSECONDS.toMillis(end - startTime);

        if (executionTimeByMethodName.containsKey(methodName)) {
            executionTimeByMethodName.get(methodName).add(duration);
        } else {
            executionTimeByMethodName.put(methodName, new ArrayList<>(List.of(duration)));
        }

        return returnVal;

    }

    public void reset() {
        executionTimeByMethodName.clear();
    }
}
