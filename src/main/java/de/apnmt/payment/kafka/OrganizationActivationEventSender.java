package de.apnmt.payment.kafka;

import de.apnmt.common.event.ApnmtEvent;
import de.apnmt.common.event.value.OrganizationActivationEventDTO;
import de.apnmt.common.sender.ApnmtEventSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class OrganizationActivationEventSender implements ApnmtEventSender<OrganizationActivationEventDTO> {

    private static final Logger log = LoggerFactory.getLogger(OrganizationActivationEventSender.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public OrganizationActivationEventSender(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void send(String topic, ApnmtEvent<OrganizationActivationEventDTO> event) {
        log.info("Send event {} to topic {}", event, topic);
        this.kafkaTemplate.send(topic, event);
    }

}
