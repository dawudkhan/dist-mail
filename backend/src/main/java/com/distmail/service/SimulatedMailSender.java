package com.distmail.service;

import com.distmail.config.DistMailProperties;
import com.distmail.domain.MailTask;
import org.springframework.stereotype.Component;

@Component
public class SimulatedMailSender {

    private final DistMailProperties properties;

    public SimulatedMailSender(DistMailProperties properties) {
        this.properties = properties;
    }

    public void send(MailTask task) throws InterruptedException {
        int min = properties.smtpDelayMinMs();
        int max = properties.smtpDelayMaxMs();
        Thread.sleep(java.util.concurrent.ThreadLocalRandom.current().nextInt(min, max + 1));

        double failChance = java.util.concurrent.ThreadLocalRandom.current().nextDouble();
        if (failChance <= properties.failureRate()) {
            throw new IllegalStateException("Simulated SMTP failure");
        }
    }
}
