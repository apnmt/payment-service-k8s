package de.apnmt.payment.service.mapper;

import de.apnmt.payment.common.service.mapper.ProductMapper;
import de.apnmt.payment.common.service.mapper.ProductMapperImpl;
import org.junit.jupiter.api.BeforeEach;

class ProductMapperTest {

    private ProductMapper productMapper;

    @BeforeEach
    public void setUp() {
        this.productMapper = new ProductMapperImpl();
    }
}
