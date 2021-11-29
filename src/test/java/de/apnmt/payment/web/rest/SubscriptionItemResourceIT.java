package de.apnmt.payment.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import de.apnmt.payment.IntegrationTest;
import de.apnmt.payment.domain.SubscriptionItem;
import de.apnmt.payment.repository.SubscriptionItemRepository;
import de.apnmt.payment.service.dto.SubscriptionItemDTO;
import de.apnmt.payment.service.mapper.SubscriptionItemMapper;
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
 * Integration tests for the {@link SubscriptionItemResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class SubscriptionItemResourceIT {

    private static final Integer DEFAULT_QUANTITY = 1;
    private static final Integer UPDATED_QUANTITY = 2;

    private static final String ENTITY_API_URL = "/api/subscription-items";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong count = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private SubscriptionItemRepository subscriptionItemRepository;

    @Autowired
    private SubscriptionItemMapper subscriptionItemMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restSubscriptionItemMockMvc;

    private SubscriptionItem subscriptionItem;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static SubscriptionItem createEntity(EntityManager em) {
        SubscriptionItem subscriptionItem = new SubscriptionItem().quantity(DEFAULT_QUANTITY);
        return subscriptionItem;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static SubscriptionItem createUpdatedEntity(EntityManager em) {
        SubscriptionItem subscriptionItem = new SubscriptionItem().quantity(UPDATED_QUANTITY);
        return subscriptionItem;
    }

    @BeforeEach
    public void initTest() {
        subscriptionItem = createEntity(em);
    }

    @Test
    @Transactional
    void createSubscriptionItem() throws Exception {
        int databaseSizeBeforeCreate = subscriptionItemRepository.findAll().size();
        // Create the SubscriptionItem
        SubscriptionItemDTO subscriptionItemDTO = subscriptionItemMapper.toDto(subscriptionItem);
        restSubscriptionItemMockMvc
            .perform(
                post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(subscriptionItemDTO))
            )
            .andExpect(status().isCreated());

        // Validate the SubscriptionItem in the database
        List<SubscriptionItem> subscriptionItemList = subscriptionItemRepository.findAll();
        assertThat(subscriptionItemList).hasSize(databaseSizeBeforeCreate + 1);
        SubscriptionItem testSubscriptionItem = subscriptionItemList.get(subscriptionItemList.size() - 1);
        assertThat(testSubscriptionItem.getQuantity()).isEqualTo(DEFAULT_QUANTITY);
    }

    @Test
    @Transactional
    void createSubscriptionItemWithExistingId() throws Exception {
        // Create the SubscriptionItem with an existing ID
        subscriptionItem.setId(1L);
        SubscriptionItemDTO subscriptionItemDTO = subscriptionItemMapper.toDto(subscriptionItem);

        int databaseSizeBeforeCreate = subscriptionItemRepository.findAll().size();

        // An entity with an existing ID cannot be created, so this API call must fail
        restSubscriptionItemMockMvc
            .perform(
                post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(subscriptionItemDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the SubscriptionItem in the database
        List<SubscriptionItem> subscriptionItemList = subscriptionItemRepository.findAll();
        assertThat(subscriptionItemList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkQuantityIsRequired() throws Exception {
        int databaseSizeBeforeTest = subscriptionItemRepository.findAll().size();
        // set the field null
        subscriptionItem.setQuantity(null);

        // Create the SubscriptionItem, which fails.
        SubscriptionItemDTO subscriptionItemDTO = subscriptionItemMapper.toDto(subscriptionItem);

        restSubscriptionItemMockMvc
            .perform(
                post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(subscriptionItemDTO))
            )
            .andExpect(status().isBadRequest());

        List<SubscriptionItem> subscriptionItemList = subscriptionItemRepository.findAll();
        assertThat(subscriptionItemList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllSubscriptionItems() throws Exception {
        // Initialize the database
        subscriptionItemRepository.saveAndFlush(subscriptionItem);

        // Get all the subscriptionItemList
        restSubscriptionItemMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(subscriptionItem.getId().intValue())))
            .andExpect(jsonPath("$.[*].quantity").value(hasItem(DEFAULT_QUANTITY)));
    }

    @Test
    @Transactional
    void getSubscriptionItem() throws Exception {
        // Initialize the database
        subscriptionItemRepository.saveAndFlush(subscriptionItem);

        // Get the subscriptionItem
        restSubscriptionItemMockMvc
            .perform(get(ENTITY_API_URL_ID, subscriptionItem.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(subscriptionItem.getId().intValue()))
            .andExpect(jsonPath("$.quantity").value(DEFAULT_QUANTITY));
    }

    @Test
    @Transactional
    void getNonExistingSubscriptionItem() throws Exception {
        // Get the subscriptionItem
        restSubscriptionItemMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putNewSubscriptionItem() throws Exception {
        // Initialize the database
        subscriptionItemRepository.saveAndFlush(subscriptionItem);

        int databaseSizeBeforeUpdate = subscriptionItemRepository.findAll().size();

        // Update the subscriptionItem
        SubscriptionItem updatedSubscriptionItem = subscriptionItemRepository.findById(subscriptionItem.getId()).get();
        // Disconnect from session so that the updates on updatedSubscriptionItem are not directly saved in db
        em.detach(updatedSubscriptionItem);
        updatedSubscriptionItem.quantity(UPDATED_QUANTITY);
        SubscriptionItemDTO subscriptionItemDTO = subscriptionItemMapper.toDto(updatedSubscriptionItem);

        restSubscriptionItemMockMvc
            .perform(
                put(ENTITY_API_URL_ID, subscriptionItemDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtil.convertObjectToJsonBytes(subscriptionItemDTO))
            )
            .andExpect(status().isOk());

        // Validate the SubscriptionItem in the database
        List<SubscriptionItem> subscriptionItemList = subscriptionItemRepository.findAll();
        assertThat(subscriptionItemList).hasSize(databaseSizeBeforeUpdate);
        SubscriptionItem testSubscriptionItem = subscriptionItemList.get(subscriptionItemList.size() - 1);
        assertThat(testSubscriptionItem.getQuantity()).isEqualTo(UPDATED_QUANTITY);
    }

    @Test
    @Transactional
    void putNonExistingSubscriptionItem() throws Exception {
        int databaseSizeBeforeUpdate = subscriptionItemRepository.findAll().size();
        subscriptionItem.setId(count.incrementAndGet());

        // Create the SubscriptionItem
        SubscriptionItemDTO subscriptionItemDTO = subscriptionItemMapper.toDto(subscriptionItem);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restSubscriptionItemMockMvc
            .perform(
                put(ENTITY_API_URL_ID, subscriptionItemDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtil.convertObjectToJsonBytes(subscriptionItemDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the SubscriptionItem in the database
        List<SubscriptionItem> subscriptionItemList = subscriptionItemRepository.findAll();
        assertThat(subscriptionItemList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchSubscriptionItem() throws Exception {
        int databaseSizeBeforeUpdate = subscriptionItemRepository.findAll().size();
        subscriptionItem.setId(count.incrementAndGet());

        // Create the SubscriptionItem
        SubscriptionItemDTO subscriptionItemDTO = subscriptionItemMapper.toDto(subscriptionItem);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restSubscriptionItemMockMvc
            .perform(
                put(ENTITY_API_URL_ID, count.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtil.convertObjectToJsonBytes(subscriptionItemDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the SubscriptionItem in the database
        List<SubscriptionItem> subscriptionItemList = subscriptionItemRepository.findAll();
        assertThat(subscriptionItemList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamSubscriptionItem() throws Exception {
        int databaseSizeBeforeUpdate = subscriptionItemRepository.findAll().size();
        subscriptionItem.setId(count.incrementAndGet());

        // Create the SubscriptionItem
        SubscriptionItemDTO subscriptionItemDTO = subscriptionItemMapper.toDto(subscriptionItem);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restSubscriptionItemMockMvc
            .perform(
                put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(subscriptionItemDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the SubscriptionItem in the database
        List<SubscriptionItem> subscriptionItemList = subscriptionItemRepository.findAll();
        assertThat(subscriptionItemList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateSubscriptionItemWithPatch() throws Exception {
        // Initialize the database
        subscriptionItemRepository.saveAndFlush(subscriptionItem);

        int databaseSizeBeforeUpdate = subscriptionItemRepository.findAll().size();

        // Update the subscriptionItem using partial update
        SubscriptionItem partialUpdatedSubscriptionItem = new SubscriptionItem();
        partialUpdatedSubscriptionItem.setId(subscriptionItem.getId());

        partialUpdatedSubscriptionItem.quantity(UPDATED_QUANTITY);

        restSubscriptionItemMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedSubscriptionItem.getId())
                    .contentType("application/merge-patch+json")
                    .content(TestUtil.convertObjectToJsonBytes(partialUpdatedSubscriptionItem))
            )
            .andExpect(status().isOk());

        // Validate the SubscriptionItem in the database
        List<SubscriptionItem> subscriptionItemList = subscriptionItemRepository.findAll();
        assertThat(subscriptionItemList).hasSize(databaseSizeBeforeUpdate);
        SubscriptionItem testSubscriptionItem = subscriptionItemList.get(subscriptionItemList.size() - 1);
        assertThat(testSubscriptionItem.getQuantity()).isEqualTo(UPDATED_QUANTITY);
    }

    @Test
    @Transactional
    void fullUpdateSubscriptionItemWithPatch() throws Exception {
        // Initialize the database
        subscriptionItemRepository.saveAndFlush(subscriptionItem);

        int databaseSizeBeforeUpdate = subscriptionItemRepository.findAll().size();

        // Update the subscriptionItem using partial update
        SubscriptionItem partialUpdatedSubscriptionItem = new SubscriptionItem();
        partialUpdatedSubscriptionItem.setId(subscriptionItem.getId());

        partialUpdatedSubscriptionItem.quantity(UPDATED_QUANTITY);

        restSubscriptionItemMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedSubscriptionItem.getId())
                    .contentType("application/merge-patch+json")
                    .content(TestUtil.convertObjectToJsonBytes(partialUpdatedSubscriptionItem))
            )
            .andExpect(status().isOk());

        // Validate the SubscriptionItem in the database
        List<SubscriptionItem> subscriptionItemList = subscriptionItemRepository.findAll();
        assertThat(subscriptionItemList).hasSize(databaseSizeBeforeUpdate);
        SubscriptionItem testSubscriptionItem = subscriptionItemList.get(subscriptionItemList.size() - 1);
        assertThat(testSubscriptionItem.getQuantity()).isEqualTo(UPDATED_QUANTITY);
    }

    @Test
    @Transactional
    void patchNonExistingSubscriptionItem() throws Exception {
        int databaseSizeBeforeUpdate = subscriptionItemRepository.findAll().size();
        subscriptionItem.setId(count.incrementAndGet());

        // Create the SubscriptionItem
        SubscriptionItemDTO subscriptionItemDTO = subscriptionItemMapper.toDto(subscriptionItem);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restSubscriptionItemMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, subscriptionItemDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(TestUtil.convertObjectToJsonBytes(subscriptionItemDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the SubscriptionItem in the database
        List<SubscriptionItem> subscriptionItemList = subscriptionItemRepository.findAll();
        assertThat(subscriptionItemList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchSubscriptionItem() throws Exception {
        int databaseSizeBeforeUpdate = subscriptionItemRepository.findAll().size();
        subscriptionItem.setId(count.incrementAndGet());

        // Create the SubscriptionItem
        SubscriptionItemDTO subscriptionItemDTO = subscriptionItemMapper.toDto(subscriptionItem);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restSubscriptionItemMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, count.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(TestUtil.convertObjectToJsonBytes(subscriptionItemDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the SubscriptionItem in the database
        List<SubscriptionItem> subscriptionItemList = subscriptionItemRepository.findAll();
        assertThat(subscriptionItemList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamSubscriptionItem() throws Exception {
        int databaseSizeBeforeUpdate = subscriptionItemRepository.findAll().size();
        subscriptionItem.setId(count.incrementAndGet());

        // Create the SubscriptionItem
        SubscriptionItemDTO subscriptionItemDTO = subscriptionItemMapper.toDto(subscriptionItem);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restSubscriptionItemMockMvc
            .perform(
                patch(ENTITY_API_URL)
                    .contentType("application/merge-patch+json")
                    .content(TestUtil.convertObjectToJsonBytes(subscriptionItemDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the SubscriptionItem in the database
        List<SubscriptionItem> subscriptionItemList = subscriptionItemRepository.findAll();
        assertThat(subscriptionItemList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteSubscriptionItem() throws Exception {
        // Initialize the database
        subscriptionItemRepository.saveAndFlush(subscriptionItem);

        int databaseSizeBeforeDelete = subscriptionItemRepository.findAll().size();

        // Delete the subscriptionItem
        restSubscriptionItemMockMvc
            .perform(delete(ENTITY_API_URL_ID, subscriptionItem.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        List<SubscriptionItem> subscriptionItemList = subscriptionItemRepository.findAll();
        assertThat(subscriptionItemList).hasSize(databaseSizeBeforeDelete - 1);
    }
}
