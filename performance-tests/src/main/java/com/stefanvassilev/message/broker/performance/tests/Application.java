package com.stefanvassilev.message.broker.performance.tests;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.stefanvassilev.message.*")
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }


}
