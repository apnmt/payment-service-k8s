package de.apnmt.payment.service.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ASubscriptionMapperTest {

    private ASubscriptionMapper aSubscriptionMapper;

    @BeforeEach
    public void setUp() {
        aSubscriptionMapper = new ASubscriptionMapperImpl();
    }
}
