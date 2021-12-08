package de.apnmt.payment.domain;

import de.apnmt.payment.common.domain.Subscription;
import de.apnmt.payment.web.rest.TestUtil;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SubscriptionTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Subscription.class);
        Subscription subscription1 = new Subscription();
        subscription1.setId("subscription_1");
        Subscription subscription2 = new Subscription();
        subscription2.setId(subscription1.getId());
        assertThat(subscription1).isEqualTo(subscription2);
        subscription2.setId("subscription_2");
        assertThat(subscription1).isNotEqualTo(subscription2);
        subscription1.setId(null);
        assertThat(subscription1).isNotEqualTo(subscription2);
    }
}
