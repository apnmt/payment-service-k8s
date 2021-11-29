package de.apnmt.payment.service.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PriceMapperTest {

    private PriceMapper priceMapper;

    @BeforeEach
    public void setUp() {
        priceMapper = new PriceMapperImpl();
    }
}
