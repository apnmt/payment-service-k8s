package de.apnmt.payment.service.mapper;

import de.apnmt.payment.common.service.mapper.SubscriptionMapper;
import de.apnmt.payment.common.service.mapper.SubscriptionMapperImpl;
import org.junit.jupiter.api.BeforeEach;

class SubscriptionMapperTest {

    private SubscriptionMapper subscriptionMapper;

    @BeforeEach
    public void setUp() {
        this.subscriptionMapper = new SubscriptionMapperImpl();
    }
}
