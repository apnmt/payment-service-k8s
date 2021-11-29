package de.apnmt.payment.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import de.apnmt.payment.IntegrationTest;
import de.apnmt.payment.domain.ASubscription;
import de.apnmt.payment.repository.ASubscriptionRepository;
import de.apnmt.payment.service.dto.ASubscriptionDTO;
import de.apnmt.payment.service.mapper.ASubscriptionMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
 * Integration tests for the {@link ASubscriptionResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class ASubscriptionResourceIT {

    private static final Instant DEFAULT_EXPIRATION_DATE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_EXPIRATION_DATE = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/a-subscriptions";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong count = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ASubscriptionRepository aSubscriptionRepository;

    @Autowired
    private ASubscriptionMapper aSubscriptionMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restASubscriptionMockMvc;

    private ASubscription aSubscription;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ASubscription createEntity(EntityManager em) {
        ASubscription aSubscription = new ASubscription().expirationDate(DEFAULT_EXPIRATION_DATE);
        return aSubscription;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ASubscription createUpdatedEntity(EntityManager em) {
        ASubscription aSubscription = new ASubscription().expirationDate(UPDATED_EXPIRATION_DATE);
        return aSubscription;
    }

    @BeforeEach
    public void initTest() {
        aSubscription = createEntity(em);
    }

    @Test
    @Transactional
    void createASubscription() throws Exception {
        int databaseSizeBeforeCreate = aSubscriptionRepository.findAll().size();
        // Create the ASubscription
        ASubscriptionDTO aSubscriptionDTO = aSubscriptionMapper.toDto(aSubscription);
        restASubscriptionMockMvc
            .perform(
                post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(aSubscriptionDTO))
            )
            .andExpect(status().isCreated());

        // Validate the ASubscription in the database
        List<ASubscription> aSubscriptionList = aSubscriptionRepository.findAll();
        assertThat(aSubscriptionList).hasSize(databaseSizeBeforeCreate + 1);
        ASubscription testASubscription = aSubscriptionList.get(aSubscriptionList.size() - 1);
        assertThat(testASubscription.getExpirationDate()).isEqualTo(DEFAULT_EXPIRATION_DATE);
    }

    @Test
    @Transactional
    void createASubscriptionWithExistingId() throws Exception {
        // Create the ASubscription with an existing ID
        aSubscription.setId(1L);
        ASubscriptionDTO aSubscriptionDTO = aSubscriptionMapper.toDto(aSubscription);

        int databaseSizeBeforeCreate = aSubscriptionRepository.findAll().size();

        // An entity with an existing ID cannot be created, so this API call must fail
        restASubscriptionMockMvc
            .perform(
                post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(aSubscriptionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ASubscription in the database
        List<ASubscription> aSubscriptionList = aSubscriptionRepository.findAll();
        assertThat(aSubscriptionList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkExpirationDateIsRequired() throws Exception {
        int databaseSizeBeforeTest = aSubscriptionRepository.findAll().size();
        // set the field null
        aSubscription.setExpirationDate(null);

        // Create the ASubscription, which fails.
        ASubscriptionDTO aSubscriptionDTO = aSubscriptionMapper.toDto(aSubscription);

        restASubscriptionMockMvc
            .perform(
                post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(aSubscriptionDTO))
            )
            .andExpect(status().isBadRequest());

        List<ASubscription> aSubscriptionList = aSubscriptionRepository.findAll();
        assertThat(aSubscriptionList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllASubscriptions() throws Exception {
        // Initialize the database
        aSubscriptionRepository.saveAndFlush(aSubscription);

        // Get all the aSubscriptionList
        restASubscriptionMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(aSubscription.getId().intValue())))
            .andExpect(jsonPath("$.[*].expirationDate").value(hasItem(DEFAULT_EXPIRATION_DATE.toString())));
    }

    @Test
    @Transactional
    void getASubscription() throws Exception {
        // Initialize the database
        aSubscriptionRepository.saveAndFlush(aSubscription);

        // Get the aSubscription
        restASubscriptionMockMvc
            .perform(get(ENTITY_API_URL_ID, aSubscription.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(aSubscription.getId().intValue()))
            .andExpect(jsonPath("$.expirationDate").value(DEFAULT_EXPIRATION_DATE.toString()));
    }

    @Test
    @Transactional
    void getNonExistingASubscription() throws Exception {
        // Get the aSubscription
        restASubscriptionMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putNewASubscription() throws Exception {
        // Initialize the database
        aSubscriptionRepository.saveAndFlush(aSubscription);

        int databaseSizeBeforeUpdate = aSubscriptionRepository.findAll().size();

        // Update the aSubscription
        ASubscription updatedASubscription = aSubscriptionRepository.findById(aSubscription.getId()).get();
        // Disconnect from session so that the updates on updatedASubscription are not directly saved in db
        em.detach(updatedASubscription);
        updatedASubscription.expirationDate(UPDATED_EXPIRATION_DATE);
        ASubscriptionDTO aSubscriptionDTO = aSubscriptionMapper.toDto(updatedASubscription);

        restASubscriptionMockMvc
            .perform(
                put(ENTITY_API_URL_ID, aSubscriptionDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtil.convertObjectToJsonBytes(aSubscriptionDTO))
            )
            .andExpect(status().isOk());

        // Validate the ASubscription in the database
        List<ASubscription> aSubscriptionList = aSubscriptionRepository.findAll();
        assertThat(aSubscriptionList).hasSize(databaseSizeBeforeUpdate);
        ASubscription testASubscription = aSubscriptionList.get(aSubscriptionList.size() - 1);
        assertThat(testASubscription.getExpirationDate()).isEqualTo(UPDATED_EXPIRATION_DATE);
    }

    @Test
    @Transactional
    void putNonExistingASubscription() throws Exception {
        int databaseSizeBeforeUpdate = aSubscriptionRepository.findAll().size();
        aSubscription.setId(count.incrementAndGet());

        // Create the ASubscription
        ASubscriptionDTO aSubscriptionDTO = aSubscriptionMapper.toDto(aSubscription);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restASubscriptionMockMvc
            .perform(
                put(ENTITY_API_URL_ID, aSubscriptionDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtil.convertObjectToJsonBytes(aSubscriptionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ASubscription in the database
        List<ASubscription> aSubscriptionList = aSubscriptionRepository.findAll();
        assertThat(aSubscriptionList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchASubscription() throws Exception {
        int databaseSizeBeforeUpdate = aSubscriptionRepository.findAll().size();
        aSubscription.setId(count.incrementAndGet());

        // Create the ASubscription
        ASubscriptionDTO aSubscriptionDTO = aSubscriptionMapper.toDto(aSubscription);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restASubscriptionMockMvc
            .perform(
                put(ENTITY_API_URL_ID, count.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtil.convertObjectToJsonBytes(aSubscriptionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ASubscription in the database
        List<ASubscription> aSubscriptionList = aSubscriptionRepository.findAll();
        assertThat(aSubscriptionList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamASubscription() throws Exception {
        int databaseSizeBeforeUpdate = aSubscriptionRepository.findAll().size();
        aSubscription.setId(count.incrementAndGet());

        // Create the ASubscription
        ASubscriptionDTO aSubscriptionDTO = aSubscriptionMapper.toDto(aSubscription);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restASubscriptionMockMvc
            .perform(
                put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(aSubscriptionDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the ASubscription in the database
        List<ASubscription> aSubscriptionList = aSubscriptionRepository.findAll();
        assertThat(aSubscriptionList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateASubscriptionWithPatch() throws Exception {
        // Initialize the database
        aSubscriptionRepository.saveAndFlush(aSubscription);

        int databaseSizeBeforeUpdate = aSubscriptionRepository.findAll().size();

        // Update the aSubscription using partial update
        ASubscription partialUpdatedASubscription = new ASubscription();
        partialUpdatedASubscription.setId(aSubscription.getId());

        partialUpdatedASubscription.expirationDate(UPDATED_EXPIRATION_DATE);

        restASubscriptionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedASubscription.getId())
                    .contentType("application/merge-patch+json")
                    .content(TestUtil.convertObjectToJsonBytes(partialUpdatedASubscription))
            )
            .andExpect(status().isOk());

        // Validate the ASubscription in the database
        List<ASubscription> aSubscriptionList = aSubscriptionRepository.findAll();
        assertThat(aSubscriptionList).hasSize(databaseSizeBeforeUpdate);
        ASubscription testASubscription = aSubscriptionList.get(aSubscriptionList.size() - 1);
        assertThat(testASubscription.getExpirationDate()).isEqualTo(UPDATED_EXPIRATION_DATE);
    }

    @Test
    @Transactional
    void fullUpdateASubscriptionWithPatch() throws Exception {
        // Initialize the database
        aSubscriptionRepository.saveAndFlush(aSubscription);

        int databaseSizeBeforeUpdate = aSubscriptionRepository.findAll().size();

        // Update the aSubscription using partial update
        ASubscription partialUpdatedASubscription = new ASubscription();
        partialUpdatedASubscription.setId(aSubscription.getId());

        partialUpdatedASubscription.expirationDate(UPDATED_EXPIRATION_DATE);

        restASubscriptionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedASubscription.getId())
                    .contentType("application/merge-patch+json")
                    .content(TestUtil.convertObjectToJsonBytes(partialUpdatedASubscription))
            )
            .andExpect(status().isOk());

        // Validate the ASubscription in the database
        List<ASubscription> aSubscriptionList = aSubscriptionRepository.findAll();
        assertThat(aSubscriptionList).hasSize(databaseSizeBeforeUpdate);
        ASubscription testASubscription = aSubscriptionList.get(aSubscriptionList.size() - 1);
        assertThat(testASubscription.getExpirationDate()).isEqualTo(UPDATED_EXPIRATION_DATE);
    }

    @Test
    @Transactional
    void patchNonExistingASubscription() throws Exception {
        int databaseSizeBeforeUpdate = aSubscriptionRepository.findAll().size();
        aSubscription.setId(count.incrementAndGet());

        // Create the ASubscription
        ASubscriptionDTO aSubscriptionDTO = aSubscriptionMapper.toDto(aSubscription);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restASubscriptionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, aSubscriptionDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(TestUtil.convertObjectToJsonBytes(aSubscriptionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ASubscription in the database
        List<ASubscription> aSubscriptionList = aSubscriptionRepository.findAll();
        assertThat(aSubscriptionList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchASubscription() throws Exception {
        int databaseSizeBeforeUpdate = aSubscriptionRepository.findAll().size();
        aSubscription.setId(count.incrementAndGet());

        // Create the ASubscription
        ASubscriptionDTO aSubscriptionDTO = aSubscriptionMapper.toDto(aSubscription);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restASubscriptionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, count.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(TestUtil.convertObjectToJsonBytes(aSubscriptionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ASubscription in the database
        List<ASubscription> aSubscriptionList = aSubscriptionRepository.findAll();
        assertThat(aSubscriptionList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamASubscription() throws Exception {
        int databaseSizeBeforeUpdate = aSubscriptionRepository.findAll().size();
        aSubscription.setId(count.incrementAndGet());

        // Create the ASubscription
        ASubscriptionDTO aSubscriptionDTO = aSubscriptionMapper.toDto(aSubscription);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restASubscriptionMockMvc
            .perform(
                patch(ENTITY_API_URL)
                    .contentType("application/merge-patch+json")
                    .content(TestUtil.convertObjectToJsonBytes(aSubscriptionDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the ASubscription in the database
        List<ASubscription> aSubscriptionList = aSubscriptionRepository.findAll();
        assertThat(aSubscriptionList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteASubscription() throws Exception {
        // Initialize the database
        aSubscriptionRepository.saveAndFlush(aSubscription);

        int databaseSizeBeforeDelete = aSubscriptionRepository.findAll().size();

        // Delete the aSubscription
        restASubscriptionMockMvc
            .perform(delete(ENTITY_API_URL_ID, aSubscription.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        List<ASubscription> aSubscriptionList = aSubscriptionRepository.findAll();
        assertThat(aSubscriptionList).hasSize(databaseSizeBeforeDelete - 1);
    }
}
