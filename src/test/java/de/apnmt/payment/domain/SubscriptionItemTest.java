package de.apnmt.payment.domain;

import static org.assertj.core.api.Assertions.assertThat;

import de.apnmt.payment.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class SubscriptionItemTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(SubscriptionItem.class);
        SubscriptionItem subscriptionItem1 = new SubscriptionItem();
        subscriptionItem1.setId(1L);
        SubscriptionItem subscriptionItem2 = new SubscriptionItem();
        subscriptionItem2.setId(subscriptionItem1.getId());
        assertThat(subscriptionItem1).isEqualTo(subscriptionItem2);
        subscriptionItem2.setId(2L);
        assertThat(subscriptionItem1).isNotEqualTo(subscriptionItem2);
        subscriptionItem1.setId(null);
        assertThat(subscriptionItem1).isNotEqualTo(subscriptionItem2);
    }
}
