package de.apnmt.payment.web.rest;

import java.util.List;

import javax.persistence.EntityManager;
import de.apnmt.payment.CommonIT;
import de.apnmt.payment.IntegrationTest;
import de.apnmt.payment.common.domain.Customer;
import de.apnmt.payment.common.repository.CustomerRepository;
import de.apnmt.payment.common.repository.PriceRepository;
import de.apnmt.payment.common.repository.ProductRepository;
import de.apnmt.payment.common.repository.SubscriptionRepository;
import de.apnmt.payment.common.service.CustomerService;
import de.apnmt.payment.common.service.dto.CustomerDTO;
import de.apnmt.payment.common.service.mapper.CustomerMapper;
import de.apnmt.payment.common.service.stripe.CustomerStripeService;
import de.apnmt.payment.common.web.rest.CustomerResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the {@link CustomerResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
class CustomerResourceIT extends CommonIT {

    private static final Long DEFAULT_ORGANIZATION_ID = 1L;
    private static final Long UPDATED_ORGANIZATION_ID = 2L;

    private static final String ENTITY_API_URL = "/api/customers";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private CustomerMapper customerMapper;

    @MockBean
    protected CustomerStripeService customerStripeService;

    @InjectMocks
    @Autowired
    private CustomerService customerService;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restCustomerMockMvc;

    private Customer customer;

    /**
     * Create an entity for this test.
     * <p>
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Customer createEntity(EntityManager em) {
        Customer customer = new Customer().organizationId(DEFAULT_ORGANIZATION_ID);
        return customer;
    }

    @BeforeEach
    public void initTest() {
        subscriptionRepository.deleteAll();
        priceRepository.deleteAll();
        productRepository.deleteAll();
        customerRepository.deleteAll();
        MockitoAnnotations.initMocks(this);
        this.customer = createEntity(this.em);
    }

    @Test
    @Transactional
    void createCustomer() throws Exception {
        when(this.customerStripeService.createCustomer(any())).thenReturn(TestUtil.createStripeCustomer());

        int databaseSizeBeforeCreate = this.customerRepository.findAll().size();
        // Create the Customer
        CustomerDTO customerDTO = this.customerMapper.toDto(this.customer);
        customerDTO.setEmail("test@test.de");
        this.restCustomerMockMvc.perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(customerDTO))).andExpect(status().isCreated());

        // Validate the Customer in the database
        List<Customer> customerList = this.customerRepository.findAll();
        assertThat(customerList).hasSize(databaseSizeBeforeCreate + 1);
        Customer testCustomer = customerList.get(customerList.size() - 1);
        assertThat(testCustomer.getId()).isEqualTo(TestUtil.DEFAULT_CUSTOMER_ID);
        assertThat(testCustomer.getOrganizationId()).isEqualTo(DEFAULT_ORGANIZATION_ID);
    }

    @Test
    @Transactional
    void createCustomerWithExistingId() throws Exception {
        // Create the Customer with an existing ID
        this.customer.setId(TestUtil.DEFAULT_CUSTOMER_ID);
        CustomerDTO customerDTO = this.customerMapper.toDto(this.customer);

        int databaseSizeBeforeCreate = this.customerRepository.findAll().size();

        // An entity with an existing ID cannot be created, so this API call must fail
        this.restCustomerMockMvc.perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(customerDTO))).andExpect(status().isBadRequest());

        // Validate the Customer in the database
        List<Customer> customerList = this.customerRepository.findAll();
        assertThat(customerList).hasSize(databaseSizeBeforeCreate);
    }
}
