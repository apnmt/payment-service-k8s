package de.apnmt.payment.web.rest;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import javax.persistence.EntityManager;
import com.stripe.exception.StripeException;
import de.apnmt.payment.CommonIT;
import de.apnmt.payment.IntegrationTest;
import de.apnmt.payment.common.domain.Customer;
import de.apnmt.payment.common.domain.Price;
import de.apnmt.payment.common.domain.Product;
import de.apnmt.payment.common.domain.enumeration.Currency;
import de.apnmt.payment.common.domain.enumeration.Interval;
import de.apnmt.payment.common.repository.PriceRepository;
import de.apnmt.payment.common.repository.ProductRepository;
import de.apnmt.payment.common.service.PriceService;
import de.apnmt.payment.common.service.dto.PriceDTO;
import de.apnmt.payment.common.service.mapper.PriceMapper;
import de.apnmt.payment.common.service.stripe.PriceStripeService;
import de.apnmt.payment.common.web.rest.PriceResource;
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
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the {@link PriceResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
class PriceResourceIT extends CommonIT {

    private static final String DEFAULT_NICKNAME = "AAAAAAAAAA";
    private static final String UPDATED_NICKNAME = "BBBBBBBBBB";

    private static final Currency DEFAULT_CURRENCY = Currency.eur;
    private static final Currency UPDATED_CURRENCY = Currency.usd;

    private static final Long DEFAULT_AMOUNT = 1L;
    private static final Long UPDATED_AMOUNT = 2L;

    private static final Interval DEFAULT_INTERVAL = Interval.month;
    private static final Interval UPDATED_INTERVAL = Interval.year;

    private static final String ENTITY_API_URL = "/api/prices";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static final Random random = new Random();
    private static final AtomicLong count = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private PriceMapper priceMapper;

    @MockBean
    private PriceStripeService priceStripeService;

    @InjectMocks
    @Autowired
    private PriceService priceService;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restPriceMockMvc;

    private Price price;

    /**
     * Create an entity for this test.
     * <p>
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Price createEntity(EntityManager em) {
        Price price = new Price().nickname(DEFAULT_NICKNAME).currency(DEFAULT_CURRENCY).amount(DEFAULT_AMOUNT).interval(DEFAULT_INTERVAL);
        return price;
    }

    /**
     * Create an updated entity for this test.
     * <p>
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Price createUpdatedEntity(EntityManager em) {
        Price price = new Price().nickname(UPDATED_NICKNAME).currency(UPDATED_CURRENCY).amount(UPDATED_AMOUNT).interval(UPDATED_INTERVAL);
        return price;
    }

    @BeforeEach
    public void initTest() throws StripeException {
        MockitoAnnotations.initMocks(this);
        Product product = TestUtil.createProduct();
        this.productRepository.saveAndFlush(product);
        this.price = createEntity(this.em);
        this.price.setProduct(product);

        when(this.priceStripeService.save(any())).thenReturn(this.priceMapper.toDto(TestUtil.createPrice()));
        when(this.priceStripeService.update(any())).thenReturn(this.priceMapper.toDto(TestUtil.createPrice()));
    }

    @Test
    @Transactional
    void createPrice() throws Exception {
        int databaseSizeBeforeCreate = this.priceRepository.findAll().size();
        // Create the Price
        PriceDTO priceDTO = this.priceMapper.toDto(this.price);
        this.restPriceMockMvc.perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(priceDTO))).andExpect(status().isCreated());

        // Validate the Price in the database
        List<Price> priceList = this.priceRepository.findAll();
        assertThat(priceList).hasSize(databaseSizeBeforeCreate + 1);
        Price testPrice = priceList.get(priceList.size() - 1);
        assertThat(testPrice.getNickname()).isEqualTo(DEFAULT_NICKNAME);
        assertThat(testPrice.getCurrency()).isEqualTo(DEFAULT_CURRENCY);
        assertThat(testPrice.getAmount()).isEqualTo(DEFAULT_AMOUNT);
        assertThat(testPrice.getInterval()).isEqualTo(DEFAULT_INTERVAL);
    }

    @Test
    @Transactional
    void createPriceWithExistingId() throws Exception {
        // Create the Price with an existing ID
        this.price.setId("price_1");
        PriceDTO priceDTO = this.priceMapper.toDto(this.price);

        int databaseSizeBeforeCreate = this.priceRepository.findAll().size();

        // An entity with an existing ID cannot be created, so this API call must fail
        this.restPriceMockMvc.perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(priceDTO))).andExpect(status().isBadRequest());

        // Validate the Price in the database
        List<Price> priceList = this.priceRepository.findAll();
        assertThat(priceList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void getAllPrices() throws Exception {
        // Initialize the database
        this.price.setId("price");
        this.priceRepository.saveAndFlush(this.price);

        // Get all the priceList
        this.restPriceMockMvc.perform(get(ENTITY_API_URL + "/product/{id}?sort=id,desc", this.price.getProduct().getId())).andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE)).andExpect(jsonPath("$.[*].id").value(hasItem(this.price.getId()))).andExpect(jsonPath("$.[*].nickname").value(hasItem(DEFAULT_NICKNAME))).andExpect(jsonPath("$.[*].currency").value(hasItem(DEFAULT_CURRENCY.toString()))).andExpect(jsonPath("$.[*].amount").value(hasItem(DEFAULT_AMOUNT.intValue()))).andExpect(jsonPath("$.[*].interval").value(hasItem(DEFAULT_INTERVAL.toString())));
    }

    @Test
    @Transactional
    void getPrice() throws Exception {
        // Initialize the database
        this.price.setId("price");
        this.priceRepository.saveAndFlush(this.price);

        // Get the price
        this.restPriceMockMvc.perform(get(ENTITY_API_URL_ID, this.price.getId())).andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE)).andExpect(jsonPath("$.id").value(this.price.getId())).andExpect(jsonPath("$.nickname").value(DEFAULT_NICKNAME)).andExpect(jsonPath("$.currency").value(DEFAULT_CURRENCY.toString())).andExpect(jsonPath("$.amount").value(DEFAULT_AMOUNT)).andExpect(jsonPath("$.interval").value(DEFAULT_INTERVAL.toString()));
    }

    @Test
    @Transactional
    void getNonExistingPrice() throws Exception {
        // Get the price
        this.restPriceMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putNewPrice() throws Exception {
        // Initialize the database
        this.price.setId("price");
        this.priceRepository.saveAndFlush(this.price);

        int databaseSizeBeforeUpdate = this.priceRepository.findAll().size();

        // Update the price
        Price updatedPrice = this.priceRepository.findById(this.price.getId()).get();
        // Disconnect from session so that the updates on updatedPrice are not directly saved in db
        this.em.detach(updatedPrice);
        updatedPrice.nickname(UPDATED_NICKNAME).currency(UPDATED_CURRENCY).amount(UPDATED_AMOUNT).interval(UPDATED_INTERVAL);
        PriceDTO priceDTO = this.priceMapper.toDto(updatedPrice);

        this.restPriceMockMvc.perform(put(ENTITY_API_URL_ID, priceDTO.getId()).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(priceDTO))).andExpect(status().isOk());

        // Validate the Price in the database
        List<Price> priceList = this.priceRepository.findAll();
        assertThat(priceList).hasSize(databaseSizeBeforeUpdate);
        Price testPrice = priceList.get(priceList.size() - 1);
        assertThat(testPrice.getNickname()).isEqualTo(UPDATED_NICKNAME);
        assertThat(testPrice.getCurrency()).isEqualTo(UPDATED_CURRENCY);
        assertThat(testPrice.getAmount()).isEqualTo(UPDATED_AMOUNT);
        assertThat(testPrice.getInterval()).isEqualTo(UPDATED_INTERVAL);
    }

    @Test
    @Transactional
    void putNonExistingPrice() throws Exception {
        int databaseSizeBeforeUpdate = this.priceRepository.findAll().size();
        this.price.setId("price_" + count.incrementAndGet());

        // Create the Price
        PriceDTO priceDTO = this.priceMapper.toDto(this.price);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        this.restPriceMockMvc.perform(put(ENTITY_API_URL_ID, priceDTO.getId()).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(priceDTO))).andExpect(status().isBadRequest());

        // Validate the Price in the database
        List<Price> priceList = this.priceRepository.findAll();
        assertThat(priceList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchPrice() throws Exception {
        int databaseSizeBeforeUpdate = this.priceRepository.findAll().size();
        this.price.setId("price_" + count.incrementAndGet());

        // Create the Price
        PriceDTO priceDTO = this.priceMapper.toDto(this.price);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        this.restPriceMockMvc.perform(put(ENTITY_API_URL_ID, "price_" + count.incrementAndGet()).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(priceDTO))).andExpect(status().isBadRequest());

        // Validate the Price in the database
        List<Price> priceList = this.priceRepository.findAll();
        assertThat(priceList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamPrice() throws Exception {
        int databaseSizeBeforeUpdate = this.priceRepository.findAll().size();
        this.price.setId("price_" + count.incrementAndGet());

        // Create the Price
        PriceDTO priceDTO = this.priceMapper.toDto(this.price);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        this.restPriceMockMvc.perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(priceDTO))).andExpect(status().isMethodNotAllowed());

        // Validate the Price in the database
        List<Price> priceList = this.priceRepository.findAll();
        assertThat(priceList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteAll() throws Exception {
        // Initialize the database
        Product product = ProductResourceIT.createEntity(this.em);
        product.setId("prod_LQ9MM3UEDGiaJg");
        this.productRepository.saveAndFlush(product);
        this.price.setId("price_" + count.incrementAndGet());
        this.priceRepository.saveAndFlush(this.price);

        int databaseSizeBeforeDelete = this.priceRepository.findAll().size();

        // Delete the appointment
        this.restPriceMockMvc.perform(delete(ENTITY_API_URL).accept(MediaType.APPLICATION_JSON)).andExpect(status().isNoContent());

        // Validate the database contains no more item
        List<Price> list = this.priceRepository.findAll();
        assertThat(list).hasSize(databaseSizeBeforeDelete - 1);
    }
}
