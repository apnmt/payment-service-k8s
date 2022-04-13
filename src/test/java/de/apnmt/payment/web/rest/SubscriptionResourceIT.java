package de.apnmt.payment.web.rest;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.persistence.EntityManager;
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
import de.apnmt.payment.common.service.CustomerService;
import de.apnmt.payment.common.service.SubscriptionService;
import de.apnmt.payment.common.service.dto.SubscriptionDTO;
import de.apnmt.payment.common.service.mapper.SubscriptionMapper;
import de.apnmt.payment.common.service.stripe.CustomerStripeService;
import de.apnmt.payment.common.service.stripe.SubscriptionStripeService;
import de.apnmt.payment.common.web.rest.SubscriptionResource;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the {@link SubscriptionResource} REST controller.
 */
@EnableKafka
@EmbeddedKafka(ports = {58255}, topics = {TopicConstants.ORGANIZATION_ACTIVATION_CHANGED_TOPIC})
@IntegrationTest
@AutoConfigureMockMvc
class SubscriptionResourceIT extends AbstractEventSenderIT {

    private static final LocalDateTime DEFAULT_EXPIRATION_DATE = LocalDateTime.of(2021, 12, 24, 0, 0, 11, 0);
    private static final LocalDateTime UPDATED_EXPIRATION_DATE = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/subscriptions";
    private static final String ENTITY_API_URL_CHECKOUT = ENTITY_API_URL + "/checkout";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";
    private static final String ENTITY_API_URL_CUSTOMER_ID = ENTITY_API_URL + "/customer/{id}";

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PriceRepository priceRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private SubscriptionMapper subscriptionMapper;

    @MockBean
    private SubscriptionStripeService subscriptionStripeService;

    @MockBean
    private CustomerStripeService customerStripeService;

    @InjectMocks
    @Autowired
    private SubscriptionService subscriptionService;

    @InjectMocks
    @Autowired
    private CustomerService customerService;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restSubscriptionMockMvc;

    private Subscription subscription;

    /**
     * Create an entity for this test.
     * <p>
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Subscription createEntity(EntityManager em) {
        Subscription subscription = new Subscription().expirationDate(DEFAULT_EXPIRATION_DATE);
        return subscription;
    }

    /**
     * Create an updated entity for this test.
     * <p>
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Subscription createUpdatedEntity(EntityManager em) {
        Subscription subscription = new Subscription().expirationDate(UPDATED_EXPIRATION_DATE);
        return subscription;
    }

    @Override
    public String getTopic() {
        return TopicConstants.ORGANIZATION_ACTIVATION_CHANGED_TOPIC;
    }

    @BeforeEach
    public void initTest() {
        subscriptionRepository.deleteAll();
        this.subscription = createEntity(this.em);
        MockitoAnnotations.initMocks(this);
    }

    @Test
    @Transactional
    void createSubscription() throws Exception {
        int databaseSizeBeforeCreate = this.subscriptionRepository.findAll().size();
        // Prepare the database
        Customer customer = TestUtil.createCustomer();
        this.customerRepository.save(customer);

        Product product = TestUtil.createProduct();
        this.productRepository.save(product);

        Price price = TestUtil.createPrice();
        price.setProduct(product);
        this.priceRepository.save(price);

        SubscriptionItem subscriptionItem = TestUtil.createSubscriptionItem(price);
        this.subscription.addSubscriptionItem(subscriptionItem);
        this.subscription.setCustomer(customer);

        // Create the subscription
        com.stripe.model.Subscription stripeSubscription = TestUtil.createSubscription(this.subscription);
        when(this.subscriptionStripeService.createSubscription(any(), any())).thenReturn(stripeSubscription);
        when(this.customerStripeService.createPaymentMethod(any(), any())).thenReturn(null);

        SubscriptionDTO subscriptionDTO = this.subscriptionMapper.toDto(this.subscription);
        this.restSubscriptionMockMvc.perform(post(ENTITY_API_URL_CHECKOUT).contentType(MediaType.APPLICATION_JSON)
            .header("X-paymentMethod", "paymentMethod")
            .content(TestUtil.convertObjectToJsonBytes(subscriptionDTO))).andExpect(status().isCreated());

        // Validate the Subscription in the database
        List<Subscription> subscriptionList = this.subscriptionRepository.findAll();
        assertThat(subscriptionList).hasSize(databaseSizeBeforeCreate + 1);
        Subscription testSubscription = subscriptionList.get(subscriptionList.size() - 1);
        assertThat(testSubscription.getExpirationDate().truncatedTo(ChronoUnit.MINUTES)).isEqualTo(LocalDateTime.now().plusHours(1L).truncatedTo(ChronoUnit.MINUTES));
        assertThat(testSubscription.getId()).isNotNull();
        assertThat(testSubscription.getCustomer()).isEqualTo(customer);
        assertThat(testSubscription.getSubscriptionItems().size()).isEqualTo(this.subscription.getSubscriptionItems().size());
        for (SubscriptionItem item : testSubscription.getSubscriptionItems()) {
            assertThat(item.getId()).isNotNull();
            assertThat(item.getQuantity()).isEqualTo(1);
            assertThat(item.getPrice()).isEqualTo(price);
        }

        ConsumerRecord<String, Object> message = this.records.poll(500, TimeUnit.MILLISECONDS);
        assertThat(message).isNotNull();
        assertThat(message.value()).isNotNull();

        TypeReference<ApnmtEvent<OrganizationActivationEventDTO>> eventType = new TypeReference<>() {
        };
        ApnmtEvent<OrganizationActivationEventDTO> eventResult = this.objectMapper.readValue(message.value().toString(), eventType);
        assertThat(eventResult.getType()).isEqualTo(ApnmtEventType.organizationActivationChanged);
        assertThat(eventResult.getValue().getOrganizationId()).isEqualTo(customer.getOrganizationId());
        assertThat(eventResult.getValue().isActive()).isTrue();
    }

    @Test
    @Transactional
    void createSubscriptionWithExistingId() throws Exception {
        // Create the Subscription with an existing ID
        this.subscription.setId("subscription_1");
        SubscriptionDTO subscriptionDTO = this.subscriptionMapper.toDto(this.subscription);

        int databaseSizeBeforeCreate = this.subscriptionRepository.findAll().size();

        // An entity without paymentMethod cannot be created, so this API call must fail
        this.restSubscriptionMockMvc.perform(post(ENTITY_API_URL_CHECKOUT).contentType(MediaType.APPLICATION_JSON)
            .header("X-paymentMethod", "paymentMethod")
            .content(TestUtil.convertObjectToJsonBytes(subscriptionDTO))).andExpect(status().isBadRequest());

        // Validate the Subscription in the database
        List<Subscription> subscriptionList = this.subscriptionRepository.findAll();
        assertThat(subscriptionList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void createSubscriptionWithPaymentMethodRequired() throws Exception {
        // Create the Subscription with an existing ID
        this.subscription.setId("subscription_1");
        SubscriptionDTO subscriptionDTO = this.subscriptionMapper.toDto(this.subscription);

        int databaseSizeBeforeCreate = this.subscriptionRepository.findAll().size();

        // An entity with an existing ID cannot be created, so this API call must fail
        this.restSubscriptionMockMvc.perform(post(ENTITY_API_URL_CHECKOUT).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(subscriptionDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Subscription in the database
        List<Subscription> subscriptionList = this.subscriptionRepository.findAll();
        assertThat(subscriptionList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void getAllSubscriptions() throws Exception {
        // Initialize the database
        Customer customer = TestUtil.createCustomer();
        this.customerRepository.saveAndFlush(customer);

        this.subscription.setCustomer(customer);

        this.subscription.setId("subscription");
        this.subscriptionRepository.saveAndFlush(this.subscription);

        // Get all the subscriptionList
        this.restSubscriptionMockMvc.perform(get(ENTITY_API_URL_CUSTOMER_ID, customer.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(this.subscription.getId())))
            .andExpect(jsonPath("$.[*].expirationDate").value(hasItem(DEFAULT_EXPIRATION_DATE.toString())));
    }

    @Test
    @Transactional
    void getAllSubscriptionsCustomerNotFound() throws Exception {
        // Initialize the database
        this.subscription.setId("subscription");
        this.subscriptionRepository.saveAndFlush(this.subscription);

        // Get all the subscriptionList
        this.restSubscriptionMockMvc.perform(get(ENTITY_API_URL_CUSTOMER_ID, "customer_2")).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void getSubscription() throws Exception {
        // Initialize the database
        this.subscription.setId("subscription");
        this.subscriptionRepository.saveAndFlush(this.subscription);

        // Get the subscription
        this.restSubscriptionMockMvc.perform(get(ENTITY_API_URL_ID, this.subscription.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(this.subscription.getId()))
            .andExpect(jsonPath("$.expirationDate").value(DEFAULT_EXPIRATION_DATE.toString()));
    }

    @Test
    @Transactional
    void getNonExistingSubscription() throws Exception {
        // Get the subscription
        this.restSubscriptionMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void deleteAll() throws Exception {
        // Initialize the database
        this.subscription.setId("subscription");
        this.subscriptionRepository.saveAndFlush(this.subscription);

        // Delete the appointment
        this.restSubscriptionMockMvc.perform(delete(ENTITY_API_URL).accept(MediaType.APPLICATION_JSON)).andExpect(status().isNoContent());

        // Validate the database contains no more item
        List<Subscription> list = this.subscriptionRepository.findAll();
        assertThat(list).hasSize(0);
    }
}
