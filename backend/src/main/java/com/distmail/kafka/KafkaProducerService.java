package com.distmail.kafka;

import com.distmail.config.DistMailProperties;
import com.distmail.domain.MailTask;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final DistMailProperties properties;
    private final MailTaskMapper mapper;

    public KafkaProducerService(KafkaTemplate<String, Object> kafkaTemplate, DistMailProperties properties, MailTaskMapper mapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
        this.mapper = mapper;
    }

    public void publishTask(MailTask task) {
        kafkaTemplate.send(properties.topic(), task.getId().toString(), mapper.toMessage(task));
    }
}
