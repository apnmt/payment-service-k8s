/*
 * OrganizationActivationEventSender.java
 *
 * (c) Copyright AUDI AG, 2021
 * All Rights reserved.
 *
 * AUDI AG
 * 85057 Ingolstadt
 * Germany
 */
package de.apnmt.payment.kafka;

import de.apnmt.common.event.ApnmtEvent;
import de.apnmt.common.event.value.OrganizationActivationEventDTO;
import de.apnmt.common.sender.ApnmtEventSender;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class OrganizationActivationEventSender implements ApnmtEventSender<OrganizationActivationEventDTO> {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public OrganizationActivationEventSender(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void send(String topic, ApnmtEvent<OrganizationActivationEventDTO> event) {
        this.kafkaTemplate.send(topic, event);
    }

}