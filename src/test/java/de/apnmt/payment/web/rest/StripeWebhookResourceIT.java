package de.apnmt.payment.web.rest;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.gson.JsonObject;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventData;
import com.stripe.model.Invoice;
import com.stripe.net.ApiResource;
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
import de.apnmt.payment.common.service.StripeWebhookService;
import de.apnmt.payment.common.service.stripe.SubscriptionStripeService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.MediaType;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@EnableKafka
@EmbeddedKafka(ports = {58255}, topics = {TopicConstants.ORGANIZATION_ACTIVATION_CHANGED_TOPIC})
@IntegrationTest
@AutoConfigureMockMvc
@ContextConfiguration(classes = {StripeWebhookResourceIT.SyncTestConfig.class})
class StripeWebhookResourceIT extends AbstractEventSenderIT {

    private static final Instant DEFAULT_INSTANT = Instant.now().plusSeconds(864000L);

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PriceRepository priceRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @MockBean
    private SubscriptionStripeService subscriptionStripeService;

    @InjectMocks
    @Autowired
    private StripeWebhookService stripeWebhookService;

    @Autowired
    private MockMvc restSubscriptionMockMvc;

    @Override
    protected String getTopic() {
        return TopicConstants.ORGANIZATION_ACTIVATION_CHANGED_TOPIC;
    }

    @BeforeEach
    public void init() throws StripeException {
        MockitoAnnotations.initMocks(this);
        com.stripe.model.Subscription subscription = new com.stripe.model.Subscription();
        subscription.setCurrentPeriodEnd(DEFAULT_INSTANT.getEpochSecond());
        when(this.subscriptionStripeService.getSubscription(any())).thenReturn(subscription);
    }

    @Test
    void handleInvoiceSucceededTest() throws Exception {
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
        subscription.setExpirationDate(LocalDateTime.now().plusHours(1));
        subscription.addSubscriptionItem(subscriptionItem);
        subscription.setCustomer(customer);

        this.subscriptionRepository.saveAndFlush(subscription);
        Event event = this.createEvent(subscription);

        this.restSubscriptionMockMvc.perform(post("/api/stripe/events").contentType(MediaType.TEXT_PLAIN).content(event.toJson())).andExpect(status().isNoContent());

        Optional<Subscription> maybe = this.subscriptionRepository.findById(subscription.getId());
        assertThat(maybe).isPresent();

        Subscription testSubscription = maybe.get();
        LocalDateTime testDateTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(DEFAULT_INSTANT.getEpochSecond()), ZoneId.systemDefault())
            .plusDays(3)
            .truncatedTo(ChronoUnit.MILLIS);
        assertThat(testSubscription.getExpirationDate()).isEqualTo(testDateTime);

        ConsumerRecord<String, Object> message = this.records.poll(500, TimeUnit.MILLISECONDS);
        Assertions.assertThat(message).isNotNull();
        Assertions.assertThat(message.value()).isNotNull();

        TypeReference<ApnmtEvent<OrganizationActivationEventDTO>> eventType = new TypeReference<>() {
        };
        ApnmtEvent<OrganizationActivationEventDTO> eventResult = this.objectMapper.readValue(message.value().toString(), eventType);
        Assertions.assertThat(eventResult.getType()).isEqualTo(ApnmtEventType.organizationActivationChanged);
        Assertions.assertThat(eventResult.getValue().getOrganizationId()).isEqualTo(customer.getOrganizationId());
        Assertions.assertThat(eventResult.getValue().isActive()).isTrue();
    }

    private Event createEvent(Subscription subscription) {
        Event event = new Event();
        event.setApiVersion("2020-08-27");
        event.setType("invoice.payment_succeeded");
        event.setObject("event");

        Invoice invoice = new Invoice();
        invoice.setSubscription(subscription.getId());
        invoice.setObject("invoice");
        EventData eventData = new EventData();
        eventData.setObject(ApiResource.GSON.fromJson(invoice.toJson(), JsonObject.class));
        event.setData(eventData);
        return event;
    }

    // because handleInvoiceSucceeded is executed asynchronously, disable async for testing purpose
    @TestConfiguration
    public static class SyncTestConfig {

        @Bean
        public TaskExecutor taskExecutor() {
            return new SyncTaskExecutor();
        }

    }

}
