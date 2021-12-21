/*
 * SubscriptionExpirationService.java
 *
 * (c) Copyright AUDI AG, 2021
 * All Rights reserved.
 *
 * AUDI AG
 * 85057 Ingolstadt
 * Germany
 */
package de.apnmt.payment.service;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.type.TypeReference;
import de.apnmt.common.TopicConstants;
import de.apnmt.common.event.ApnmtEvent;
import de.apnmt.common.event.ApnmtEventType;
import de.apnmt.common.event.value.OrganizationActivationEventDTO;
import de.apnmt.k8s.common.test.AbstractEventSenderIT;
import de.apnmt.payment.IntegrationTest;
import de.apnmt.payment.common.domain.Customer;
import de.apnmt.payment.common.domain.Price;
import de.apnmt.payment.common.domain.Product;
import de.apnmt.payment.common.domain.Subscription;
import de.apnmt.payment.common.domain.SubscriptionItem;
import de.apnmt.payment.common.repository.CustomerRepository;
import de.apnmt.payment.common.repository.PriceRepository;
import de.apnmt.payment.common.repository.ProductRepository;
import de.apnmt.payment.common.repository.SubscriptionRepository;
import de.apnmt.payment.common.service.SubscriptionExpirationService;
import de.apnmt.payment.web.rest.TestUtil;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.test.context.EmbeddedKafka;

@EnableKafka
@EmbeddedKafka(ports = {58255}, topics = {TopicConstants.ORGANIZATION_ACTIVATION_CHANGED_TOPIC})
@IntegrationTest
@AutoConfigureMockMvc
class SubscriptionExpirationServiceIT extends AbstractEventSenderIT {

    @Autowired
    private SubscriptionExpirationService subscriptionExpirationService;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PriceRepository priceRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Override
    public String getTopic() {
        return TopicConstants.ORGANIZATION_ACTIVATION_CHANGED_TOPIC;
    }

    @BeforeEach
    public void initTest() {
        subscriptionRepository.deleteAll();
        customerRepository.deleteAll();
        priceRepository.deleteAll();
        productRepository.deleteAll();
    }

    @Test
    void checkExpirationTest() throws Exception {
        Customer customer = TestUtil.createCustomer();
        this.customerRepository.save(customer);

        Product product = TestUtil.createProduct();
        this.productRepository.save(product);

        Price price = TestUtil.createPrice();
        price.setProduct(product);
        this.priceRepository.save(price);

        SubscriptionItem subscriptionItem = TestUtil.createSubscriptionItem(price);
        subscriptionItem.setId("subscriptionItem_1");

        Subscription subscription = new Subscription();
        subscription.setId("subscription_1");
        subscription.setExpirationDate(LocalDateTime.now().minusDays(1));
        subscription.addSubscriptionItem(subscriptionItem);
        subscription.setCustomer(customer);

        this.subscriptionRepository.saveAndFlush(subscription);

        this.subscriptionExpirationService.checkExpirationOfSubscriptions();

        ConsumerRecord<String, Object> message = this.records.poll(500, TimeUnit.MILLISECONDS);
        Assertions.assertThat(message).isNotNull();
        Assertions.assertThat(message.value()).isNotNull();

        TypeReference<ApnmtEvent<OrganizationActivationEventDTO>> eventType = new TypeReference<>() {
        };
        ApnmtEvent<OrganizationActivationEventDTO> eventResult = this.objectMapper.readValue(message.value().toString(), eventType);
        Assertions.assertThat(eventResult.getType()).isEqualTo(ApnmtEventType.organizationActivationChanged);
        Assertions.assertThat(eventResult.getValue().getOrganizationId()).isEqualTo(customer.getOrganizationId());
        Assertions.assertThat(eventResult.getValue().isActive()).isFalse();
    }

    @Test
    void checkExpirationNotExpiredTest() throws Exception {
        Customer customer = TestUtil.createCustomer();
        this.customerRepository.save(customer);

        Product product = TestUtil.createProduct();
        this.productRepository.save(product);

        Price price = TestUtil.createPrice();
        price.setProduct(product);
        this.priceRepository.save(price);

        SubscriptionItem subscriptionItem = TestUtil.createSubscriptionItem(price);
        subscriptionItem.setId("subscriptionItem_1");

        Subscription subscription = new Subscription();
        subscription.setId("subscription_1");
        subscription.setExpirationDate(LocalDateTime.now().plusDays(1));
        subscription.addSubscriptionItem(subscriptionItem);
        subscription.setCustomer(customer);

        this.subscriptionRepository.saveAndFlush(subscription);

        this.subscriptionExpirationService.checkExpirationOfSubscriptions();

        ConsumerRecord<String, Object> message = this.records.poll(500, TimeUnit.MILLISECONDS);
        Assertions.assertThat(message).isNull();
    }

}
