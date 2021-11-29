package de.apnmt.payment.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import de.apnmt.payment.IntegrationTest;
import de.apnmt.payment.domain.Price;
import de.apnmt.payment.repository.PriceRepository;
import de.apnmt.payment.service.dto.PriceDTO;
import de.apnmt.payment.service.mapper.PriceMapper;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import javax.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the {@link PriceResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class PriceResourceIT {

    private static final String DEFAULT_NICKNAME = "AAAAAAAAAA";
    private static final String UPDATED_NICKNAME = "BBBBBBBBBB";

    private static final String DEFAULT_CURRENCY = "AAAAAAAAAA";
    private static final String UPDATED_CURRENCY = "BBBBBBBBBB";

    private static final String DEFAULT_POSTAL_CODE = "AAAAAAAAAA";
    private static final String UPDATED_POSTAL_CODE = "BBBBBBBBBB";

    private static final Long DEFAULT_AMOUNT = 1L;
    private static final Long UPDATED_AMOUNT = 2L;

    private static final String DEFAULT_INTERVAL = "AAAAAAAAAA";
    private static final String UPDATED_INTERVAL = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/prices";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong count = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private PriceRepository priceRepository;

    @Autowired
    private PriceMapper priceMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restPriceMockMvc;

    private Price price;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Price createEntity(EntityManager em) {
        Price price = new Price()
            .nickname(DEFAULT_NICKNAME)
            .currency(DEFAULT_CURRENCY)
            .postalCode(DEFAULT_POSTAL_CODE)
            .amount(DEFAULT_AMOUNT)
            .interval(DEFAULT_INTERVAL);
        return price;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Price createUpdatedEntity(EntityManager em) {
        Price price = new Price()
            .nickname(UPDATED_NICKNAME)
            .currency(UPDATED_CURRENCY)
            .postalCode(UPDATED_POSTAL_CODE)
            .amount(UPDATED_AMOUNT)
            .interval(UPDATED_INTERVAL);
        return price;
    }

    @BeforeEach
    public void initTest() {
        price = createEntity(em);
    }

    @Test
    @Transactional
    void createPrice() throws Exception {
        int databaseSizeBeforeCreate = priceRepository.findAll().size();
        // Create the Price
        PriceDTO priceDTO = priceMapper.toDto(price);
        restPriceMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(priceDTO)))
            .andExpect(status().isCreated());

        // Validate the Price in the database
        List<Price> priceList = priceRepository.findAll();
        assertThat(priceList).hasSize(databaseSizeBeforeCreate + 1);
        Price testPrice = priceList.get(priceList.size() - 1);
        assertThat(testPrice.getNickname()).isEqualTo(DEFAULT_NICKNAME);
        assertThat(testPrice.getCurrency()).isEqualTo(DEFAULT_CURRENCY);
        assertThat(testPrice.getPostalCode()).isEqualTo(DEFAULT_POSTAL_CODE);
        assertThat(testPrice.getAmount()).isEqualTo(DEFAULT_AMOUNT);
        assertThat(testPrice.getInterval()).isEqualTo(DEFAULT_INTERVAL);
    }

    @Test
    @Transactional
    void createPriceWithExistingId() throws Exception {
        // Create the Price with an existing ID
        price.setId(1L);
        PriceDTO priceDTO = priceMapper.toDto(price);

        int databaseSizeBeforeCreate = priceRepository.findAll().size();

        // An entity with an existing ID cannot be created, so this API call must fail
        restPriceMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(priceDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Price in the database
        List<Price> priceList = priceRepository.findAll();
        assertThat(priceList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkNicknameIsRequired() throws Exception {
        int databaseSizeBeforeTest = priceRepository.findAll().size();
        // set the field null
        price.setNickname(null);

        // Create the Price, which fails.
        PriceDTO priceDTO = priceMapper.toDto(price);

        restPriceMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(priceDTO)))
            .andExpect(status().isBadRequest());

        List<Price> priceList = priceRepository.findAll();
        assertThat(priceList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkCurrencyIsRequired() throws Exception {
        int databaseSizeBeforeTest = priceRepository.findAll().size();
        // set the field null
        price.setCurrency(null);

        // Create the Price, which fails.
        PriceDTO priceDTO = priceMapper.toDto(price);

        restPriceMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(priceDTO)))
            .andExpect(status().isBadRequest());

        List<Price> priceList = priceRepository.findAll();
        assertThat(priceList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkPostalCodeIsRequired() throws Exception {
        int databaseSizeBeforeTest = priceRepository.findAll().size();
        // set the field null
        price.setPostalCode(null);

        // Create the Price, which fails.
        PriceDTO priceDTO = priceMapper.toDto(price);

        restPriceMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(priceDTO)))
            .andExpect(status().isBadRequest());

        List<Price> priceList = priceRepository.findAll();
        assertThat(priceList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkAmountIsRequired() throws Exception {
        int databaseSizeBeforeTest = priceRepository.findAll().size();
        // set the field null
        price.setAmount(null);

        // Create the Price, which fails.
        PriceDTO priceDTO = priceMapper.toDto(price);

        restPriceMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(priceDTO)))
            .andExpect(status().isBadRequest());

        List<Price> priceList = priceRepository.findAll();
        assertThat(priceList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkIntervalIsRequired() throws Exception {
        int databaseSizeBeforeTest = priceRepository.findAll().size();
        // set the field null
        price.setInterval(null);

        // Create the Price, which fails.
        PriceDTO priceDTO = priceMapper.toDto(price);

        restPriceMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(priceDTO)))
            .andExpect(status().isBadRequest());

        List<Price> priceList = priceRepository.findAll();
        assertThat(priceList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllPrices() throws Exception {
        // Initialize the database
        priceRepository.saveAndFlush(price);

        // Get all the priceList
        restPriceMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(price.getId().intValue())))
            .andExpect(jsonPath("$.[*].nickname").value(hasItem(DEFAULT_NICKNAME)))
            .andExpect(jsonPath("$.[*].currency").value(hasItem(DEFAULT_CURRENCY)))
            .andExpect(jsonPath("$.[*].postalCode").value(hasItem(DEFAULT_POSTAL_CODE)))
            .andExpect(jsonPath("$.[*].amount").value(hasItem(DEFAULT_AMOUNT.intValue())))
            .andExpect(jsonPath("$.[*].interval").value(hasItem(DEFAULT_INTERVAL)));
    }

    @Test
    @Transactional
    void getPrice() throws Exception {
        // Initialize the database
        priceRepository.saveAndFlush(price);

        // Get the price
        restPriceMockMvc
            .perform(get(ENTITY_API_URL_ID, price.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(price.getId().intValue()))
            .andExpect(jsonPath("$.nickname").value(DEFAULT_NICKNAME))
            .andExpect(jsonPath("$.currency").value(DEFAULT_CURRENCY))
            .andExpect(jsonPath("$.postalCode").value(DEFAULT_POSTAL_CODE))
            .andExpect(jsonPath("$.amount").value(DEFAULT_AMOUNT.intValue()))
            .andExpect(jsonPath("$.interval").value(DEFAULT_INTERVAL));
    }

    @Test
    @Transactional
    void getNonExistingPrice() throws Exception {
        // Get the price
        restPriceMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putNewPrice() throws Exception {
        // Initialize the database
        priceRepository.saveAndFlush(price);

        int databaseSizeBeforeUpdate = priceRepository.findAll().size();

        // Update the price
        Price updatedPrice = priceRepository.findById(price.getId()).get();
        // Disconnect from session so that the updates on updatedPrice are not directly saved in db
        em.detach(updatedPrice);
        updatedPrice
            .nickname(UPDATED_NICKNAME)
            .currency(UPDATED_CURRENCY)
            .postalCode(UPDATED_POSTAL_CODE)
            .amount(UPDATED_AMOUNT)
            .interval(UPDATED_INTERVAL);
        PriceDTO priceDTO = priceMapper.toDto(updatedPrice);

        restPriceMockMvc
            .perform(
                put(ENTITY_API_URL_ID, priceDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtil.convertObjectToJsonBytes(priceDTO))
            )
            .andExpect(status().isOk());

        // Validate the Price in the database
        List<Price> priceList = priceRepository.findAll();
        assertThat(priceList).hasSize(databaseSizeBeforeUpdate);
        Price testPrice = priceList.get(priceList.size() - 1);
        assertThat(testPrice.getNickname()).isEqualTo(UPDATED_NICKNAME);
        assertThat(testPrice.getCurrency()).isEqualTo(UPDATED_CURRENCY);
        assertThat(testPrice.getPostalCode()).isEqualTo(UPDATED_POSTAL_CODE);
        assertThat(testPrice.getAmount()).isEqualTo(UPDATED_AMOUNT);
        assertThat(testPrice.getInterval()).isEqualTo(UPDATED_INTERVAL);
    }

    @Test
    @Transactional
    void putNonExistingPrice() throws Exception {
        int databaseSizeBeforeUpdate = priceRepository.findAll().size();
        price.setId(count.incrementAndGet());

        // Create the Price
        PriceDTO priceDTO = priceMapper.toDto(price);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restPriceMockMvc
            .perform(
                put(ENTITY_API_URL_ID, priceDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtil.convertObjectToJsonBytes(priceDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Price in the database
        List<Price> priceList = priceRepository.findAll();
        assertThat(priceList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchPrice() throws Exception {
        int databaseSizeBeforeUpdate = priceRepository.findAll().size();
        price.setId(count.incrementAndGet());

        // Create the Price
        PriceDTO priceDTO = priceMapper.toDto(price);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restPriceMockMvc
            .perform(
                put(ENTITY_API_URL_ID, count.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtil.convertObjectToJsonBytes(priceDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Price in the database
        List<Price> priceList = priceRepository.findAll();
        assertThat(priceList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamPrice() throws Exception {
        int databaseSizeBeforeUpdate = priceRepository.findAll().size();
        price.setId(count.incrementAndGet());

        // Create the Price
        PriceDTO priceDTO = priceMapper.toDto(price);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restPriceMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(priceDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Price in the database
        List<Price> priceList = priceRepository.findAll();
        assertThat(priceList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdatePriceWithPatch() throws Exception {
        // Initialize the database
        priceRepository.saveAndFlush(price);

        int databaseSizeBeforeUpdate = priceRepository.findAll().size();

        // Update the price using partial update
        Price partialUpdatedPrice = new Price();
        partialUpdatedPrice.setId(price.getId());

        partialUpdatedPrice.interval(UPDATED_INTERVAL);

        restPriceMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedPrice.getId())
                    .contentType("application/merge-patch+json")
                    .content(TestUtil.convertObjectToJsonBytes(partialUpdatedPrice))
            )
            .andExpect(status().isOk());

        // Validate the Price in the database
        List<Price> priceList = priceRepository.findAll();
        assertThat(priceList).hasSize(databaseSizeBeforeUpdate);
        Price testPrice = priceList.get(priceList.size() - 1);
        assertThat(testPrice.getNickname()).isEqualTo(DEFAULT_NICKNAME);
        assertThat(testPrice.getCurrency()).isEqualTo(DEFAULT_CURRENCY);
        assertThat(testPrice.getPostalCode()).isEqualTo(DEFAULT_POSTAL_CODE);
        assertThat(testPrice.getAmount()).isEqualTo(DEFAULT_AMOUNT);
        assertThat(testPrice.getInterval()).isEqualTo(UPDATED_INTERVAL);
    }

    @Test
    @Transactional
    void fullUpdatePriceWithPatch() throws Exception {
        // Initialize the database
        priceRepository.saveAndFlush(price);

        int databaseSizeBeforeUpdate = priceRepository.findAll().size();

        // Update the price using partial update
        Price partialUpdatedPrice = new Price();
        partialUpdatedPrice.setId(price.getId());

        partialUpdatedPrice
            .nickname(UPDATED_NICKNAME)
            .currency(UPDATED_CURRENCY)
            .postalCode(UPDATED_POSTAL_CODE)
            .amount(UPDATED_AMOUNT)
            .interval(UPDATED_INTERVAL);

        restPriceMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedPrice.getId())
                    .contentType("application/merge-patch+json")
                    .content(TestUtil.convertObjectToJsonBytes(partialUpdatedPrice))
            )
            .andExpect(status().isOk());

        // Validate the Price in the database
        List<Price> priceList = priceRepository.findAll();
        assertThat(priceList).hasSize(databaseSizeBeforeUpdate);
        Price testPrice = priceList.get(priceList.size() - 1);
        assertThat(testPrice.getNickname()).isEqualTo(UPDATED_NICKNAME);
        assertThat(testPrice.getCurrency()).isEqualTo(UPDATED_CURRENCY);
        assertThat(testPrice.getPostalCode()).isEqualTo(UPDATED_POSTAL_CODE);
        assertThat(testPrice.getAmount()).isEqualTo(UPDATED_AMOUNT);
        assertThat(testPrice.getInterval()).isEqualTo(UPDATED_INTERVAL);
    }

    @Test
    @Transactional
    void patchNonExistingPrice() throws Exception {
        int databaseSizeBeforeUpdate = priceRepository.findAll().size();
        price.setId(count.incrementAndGet());

        // Create the Price
        PriceDTO priceDTO = priceMapper.toDto(price);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restPriceMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, priceDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(TestUtil.convertObjectToJsonBytes(priceDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Price in the database
        List<Price> priceList = priceRepository.findAll();
        assertThat(priceList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchPrice() throws Exception {
        int databaseSizeBeforeUpdate = priceRepository.findAll().size();
        price.setId(count.incrementAndGet());

        // Create the Price
        PriceDTO priceDTO = priceMapper.toDto(price);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restPriceMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, count.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(TestUtil.convertObjectToJsonBytes(priceDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Price in the database
        List<Price> priceList = priceRepository.findAll();
        assertThat(priceList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamPrice() throws Exception {
        int databaseSizeBeforeUpdate = priceRepository.findAll().size();
        price.setId(count.incrementAndGet());

        // Create the Price
        PriceDTO priceDTO = priceMapper.toDto(price);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restPriceMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(TestUtil.convertObjectToJsonBytes(priceDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Price in the database
        List<Price> priceList = priceRepository.findAll();
        assertThat(priceList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deletePrice() throws Exception {
        // Initialize the database
        priceRepository.saveAndFlush(price);

        int databaseSizeBeforeDelete = priceRepository.findAll().size();

        // Delete the price
        restPriceMockMvc
            .perform(delete(ENTITY_API_URL_ID, price.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        List<Price> priceList = priceRepository.findAll();
        assertThat(priceList).hasSize(databaseSizeBeforeDelete - 1);
    }
}
