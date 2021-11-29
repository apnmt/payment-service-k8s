package de.apnmt.payment.domain;

import static org.assertj.core.api.Assertions.assertThat;

import de.apnmt.payment.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class ASubscriptionTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(ASubscription.class);
        ASubscription aSubscription1 = new ASubscription();
        aSubscription1.setId(1L);
        ASubscription aSubscription2 = new ASubscription();
        aSubscription2.setId(aSubscription1.getId());
        assertThat(aSubscription1).isEqualTo(aSubscription2);
        aSubscription2.setId(2L);
        assertThat(aSubscription1).isNotEqualTo(aSubscription2);
        aSubscription1.setId(null);
        assertThat(aSubscription1).isNotEqualTo(aSubscription2);
    }
}
