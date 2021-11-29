package de.apnmt.payment.service.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SubscriptionItemMapperTest {

    private SubscriptionItemMapper subscriptionItemMapper;

    @BeforeEach
    public void setUp() {
        subscriptionItemMapper = new SubscriptionItemMapperImpl();
    }
}
