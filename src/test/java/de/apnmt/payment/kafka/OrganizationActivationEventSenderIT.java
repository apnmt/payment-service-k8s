package de.apnmt.payment.kafka;

import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import de.apnmt.common.ApnmtTestUtil;
import de.apnmt.common.TopicConstants;
import de.apnmt.common.event.ApnmtEvent;
import de.apnmt.common.event.value.OrganizationActivationEventDTO;
import de.apnmt.k8s.common.test.AbstractEventSenderIT;
import de.apnmt.payment.IntegrationTest;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import static org.assertj.core.api.Assertions.assertThat;

@EnableKafka
@EmbeddedKafka(ports = {58255}, topics = {TopicConstants.ORGANIZATION_ACTIVATION_CHANGED_TOPIC})
@IntegrationTest
@AutoConfigureMockMvc
@DirtiesContext
public class OrganizationActivationEventSenderIT extends AbstractEventSenderIT {

    @Autowired
    private OrganizationActivationEventSender organizationActivationEventSender;

    @Override
    public String getTopic() {
        return TopicConstants.ORGANIZATION_ACTIVATION_CHANGED_TOPIC;
    }

    @Test
    public void appointmentEventSenderTest() throws InterruptedException, JsonProcessingException {
        ApnmtEvent<OrganizationActivationEventDTO> event = ApnmtTestUtil.createOrganizationActivationEvent();
        this.organizationActivationEventSender.send(TopicConstants.ORGANIZATION_ACTIVATION_CHANGED_TOPIC, event);

        ConsumerRecord<String, Object> message = this.records.poll(500, TimeUnit.MILLISECONDS);
        assertThat(message).isNotNull();
        assertThat(message.value()).isNotNull();

        TypeReference<ApnmtEvent<OrganizationActivationEventDTO>> eventType = new TypeReference<>() {
        };
        ApnmtEvent<OrganizationActivationEventDTO> eventResult = this.objectMapper.readValue(message.value().toString(), eventType);
        assertThat(eventResult).isEqualTo(event);
    }

}
