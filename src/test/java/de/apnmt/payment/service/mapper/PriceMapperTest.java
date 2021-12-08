package de.apnmt.payment.service.mapper;

import de.apnmt.payment.common.service.mapper.PriceMapper;
import de.apnmt.payment.common.service.mapper.PriceMapperImpl;
import org.junit.jupiter.api.BeforeEach;

class PriceMapperTest {

    private PriceMapper priceMapper;

    @BeforeEach
    public void setUp() {
        this.priceMapper = new PriceMapperImpl();
    }
}
