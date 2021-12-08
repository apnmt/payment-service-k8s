package de.apnmt.payment.service.mapper;

import de.apnmt.payment.common.service.mapper.SubscriptionItemMapper;
import de.apnmt.payment.common.service.mapper.SubscriptionItemMapperImpl;
import org.junit.jupiter.api.BeforeEach;

class SubscriptionItemMapperTest {

    private SubscriptionItemMapper subscriptionItemMapper;

    @BeforeEach
    public void setUp() {
        this.subscriptionItemMapper = new SubscriptionItemMapperImpl();
    }
}
