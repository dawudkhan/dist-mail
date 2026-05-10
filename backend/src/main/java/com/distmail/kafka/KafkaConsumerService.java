package com.distmail.kafka;

import com.distmail.domain.MailTaskMessage;
import com.distmail.service.DispatcherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import com.distmail.config.DistMailProperties;

@Service
public class KafkaConsumerService {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerService.class);

    private final DispatcherService dispatcherService;
    private final MailTaskMapper mapper;
    private final DistMailProperties properties;

    public KafkaConsumerService(DispatcherService dispatcherService, MailTaskMapper mapper, DistMailProperties properties) {
        this.dispatcherService = dispatcherService;
        this.mapper = mapper;
        this.properties = properties;
    }

    @KafkaListener(
        topics = "${distmail.topic}",
        groupId = "${distmail.consumer-group}",
        concurrency = "${distmail.consumer-concurrency}"
    )
    public void consume(MailTaskMessage message) {
        dispatcherService.submitTask(mapper.fromMessage(message));
        log.debug("Consumed task {} from Kafka (concurrency={})", message.taskId(), properties.consumerConcurrency());
    }
}
