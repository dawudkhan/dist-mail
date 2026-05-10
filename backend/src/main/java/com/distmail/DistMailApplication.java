package com.distmail;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableKafka
@EnableScheduling
@SpringBootApplication
public class DistMailApplication {

    public static void main(String[] args) {
        SpringApplication.run(DistMailApplication.class, args);
    }
}
