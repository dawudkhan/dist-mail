package com.distmail.kafka;

import com.distmail.domain.MailTaskMessage;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic mailTopic(@Value("${distmail.topic}") String topic) {
        return TopicBuilder.name(topic)
            .partitions(12)
            .replicas(1)
            .build();
    }
}
