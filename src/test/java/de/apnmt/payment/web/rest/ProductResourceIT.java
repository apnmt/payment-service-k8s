package de.apnmt.payment.web.rest;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import javax.persistence.EntityManager;
import com.stripe.exception.StripeException;
import de.apnmt.payment.IntegrationTest;
import de.apnmt.payment.common.domain.Product;
import de.apnmt.payment.common.repository.ProductRepository;
import de.apnmt.payment.common.service.ProductService;
import de.apnmt.payment.common.service.dto.ProductDTO;
import de.apnmt.payment.common.service.mapper.ProductMapper;
import de.apnmt.payment.common.service.stripe.ProductStripeService;
import de.apnmt.payment.common.web.rest.ProductResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the {@link ProductResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class ProductResourceIT {

    private static final String DEFAULT_NAME = "AAAAAAAAAA";
    private static final String UPDATED_NAME = "BBBBBBBBBB";

    private static final String DEFAULT_DESCRIPTION = "AAAAAAAAAA";
    private static final String UPDATED_DESCRIPTION = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/products";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static final Random random = new Random();
    private static final AtomicLong count = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductMapper productMapper;

    @MockBean
    private ProductStripeService productStripeService;

    @InjectMocks
    @Autowired
    private ProductService productService;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restProductMockMvc;

    private Product product;

    /**
     * Create an entity for this test.
     * <p>
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Product createEntity(EntityManager em) {
        Product product = new Product().name(DEFAULT_NAME).description(DEFAULT_DESCRIPTION);
        return product;
    }

    /**
     * Create an updated entity for this test.
     * <p>
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Product createUpdatedEntity(EntityManager em) {
        Product product = new Product().name(UPDATED_NAME).description(UPDATED_DESCRIPTION);
        return product;
    }

    @BeforeEach
    public void initTest() throws StripeException {
        this.product = createEntity(this.em);
        MockitoAnnotations.initMocks(this);
        when(this.productStripeService.save(any())).thenReturn(this.productMapper.toDto(TestUtil.createProduct()));
        when(this.productStripeService.update(any())).thenReturn(this.productMapper.toDto(TestUtil.createProduct()));
    }

    @Test
    @Transactional
    void createProduct() throws Exception {
        int databaseSizeBeforeCreate = this.productRepository.findAll().size();
        // Create the Product
        ProductDTO productDTO = this.productMapper.toDto(this.product);
        this.restProductMockMvc.perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(productDTO))).andExpect(status().isCreated());

        // Validate the Product in the database
        List<Product> productList = this.productRepository.findAll();
        assertThat(productList).hasSize(databaseSizeBeforeCreate + 1);
        Product testProduct = productList.get(productList.size() - 1);
        assertThat(testProduct.getName()).isEqualTo(DEFAULT_NAME);
        assertThat(testProduct.getDescription()).isEqualTo(DEFAULT_DESCRIPTION);
    }

    @Test
    @Transactional
    void createProductWithExistingId() throws Exception {
        // Create the Product with an existing ID
        this.product.setId("product_1");
        ProductDTO productDTO = this.productMapper.toDto(this.product);

        int databaseSizeBeforeCreate = this.productRepository.findAll().size();

        // An entity with an existing ID cannot be created, so this API call must fail
        this.restProductMockMvc.perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(productDTO))).andExpect(status().isBadRequest());

        // Validate the Product in the database
        List<Product> productList = this.productRepository.findAll();
        assertThat(productList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void getAllProducts() throws Exception {
        // Initialize the database
        this.product.setId("product");
        this.productRepository.saveAndFlush(this.product);

        // Get all the productList
        this.restProductMockMvc.perform(get(ENTITY_API_URL + "?sort=id,desc")).andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE)).andExpect(jsonPath("$.[*].id").value(hasItem(this.product.getId()))).andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME))).andExpect(jsonPath("$.[*].description").value(hasItem(DEFAULT_DESCRIPTION)));
    }

    @Test
    @Transactional
    void getProduct() throws Exception {
        // Initialize the database
        this.product.setId("product");
        this.productRepository.saveAndFlush(this.product);

        // Get the product
        this.restProductMockMvc.perform(get(ENTITY_API_URL_ID, this.product.getId())).andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE)).andExpect(jsonPath("$.id").value(this.product.getId())).andExpect(jsonPath("$.name").value(DEFAULT_NAME)).andExpect(jsonPath("$.description").value(DEFAULT_DESCRIPTION));
    }

    @Test
    @Transactional
    void getNonExistingProduct() throws Exception {
        // Get the product
        this.restProductMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putNewProduct() throws Exception {
        // Initialize the database
        this.product.setId("product");
        this.productRepository.saveAndFlush(this.product);

        int databaseSizeBeforeUpdate = this.productRepository.findAll().size();

        // Update the product
        Product updatedProduct = this.productRepository.findById(this.product.getId()).get();
        // Disconnect from session so that the updates on updatedProduct are not directly saved in db
        this.em.detach(updatedProduct);
        updatedProduct.name(UPDATED_NAME).description(UPDATED_DESCRIPTION);
        ProductDTO productDTO = this.productMapper.toDto(updatedProduct);

        this.restProductMockMvc.perform(put(ENTITY_API_URL_ID, productDTO.getId()).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(productDTO))).andExpect(status().isOk());

        // Validate the Product in the database
        List<Product> productList = this.productRepository.findAll();
        assertThat(productList).hasSize(databaseSizeBeforeUpdate);
        Product testProduct = productList.get(productList.size() - 1);
        assertThat(testProduct.getName()).isEqualTo(UPDATED_NAME);
        assertThat(testProduct.getDescription()).isEqualTo(UPDATED_DESCRIPTION);
    }

    @Test
    @Transactional
    void putNonExistingProduct() throws Exception {
        int databaseSizeBeforeUpdate = this.productRepository.findAll().size();
        this.product.setId("product_" + count.incrementAndGet());

        // Create the Product
        ProductDTO productDTO = this.productMapper.toDto(this.product);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        this.restProductMockMvc.perform(put(ENTITY_API_URL_ID, productDTO.getId()).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(productDTO))).andExpect(status().isBadRequest());

        // Validate the Product in the database
        List<Product> productList = this.productRepository.findAll();
        assertThat(productList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchProduct() throws Exception {
        int databaseSizeBeforeUpdate = this.productRepository.findAll().size();
        this.product.setId("product_" + count.incrementAndGet());

        // Create the Product
        ProductDTO productDTO = this.productMapper.toDto(this.product);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        this.restProductMockMvc.perform(put(ENTITY_API_URL_ID, count.incrementAndGet()).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(productDTO))).andExpect(status().isBadRequest());

        // Validate the Product in the database
        List<Product> productList = this.productRepository.findAll();
        assertThat(productList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamProduct() throws Exception {
        int databaseSizeBeforeUpdate = this.productRepository.findAll().size();
        this.product.setId("product_" + count.incrementAndGet());

        // Create the Product
        ProductDTO productDTO = this.productMapper.toDto(this.product);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        this.restProductMockMvc.perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(productDTO))).andExpect(status().isMethodNotAllowed());

        // Validate the Product in the database
        List<Product> productList = this.productRepository.findAll();
        assertThat(productList).hasSize(databaseSizeBeforeUpdate);
    }
}
