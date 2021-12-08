package de.apnmt.payment.domain;

import de.apnmt.payment.common.domain.SubscriptionItem;
import de.apnmt.payment.web.rest.TestUtil;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SubscriptionItemTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(SubscriptionItem.class);
        SubscriptionItem subscriptionItem1 = new SubscriptionItem();
        subscriptionItem1.setId("subscriptionItem_1");
        SubscriptionItem subscriptionItem2 = new SubscriptionItem();
        subscriptionItem2.setId(subscriptionItem1.getId());
        assertThat(subscriptionItem1).isEqualTo(subscriptionItem2);
        subscriptionItem2.setId("subscriptionItem_2");
        assertThat(subscriptionItem1).isNotEqualTo(subscriptionItem2);
        subscriptionItem1.setId(null);
        assertThat(subscriptionItem1).isNotEqualTo(subscriptionItem2);
    }
}
