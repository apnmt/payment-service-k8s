package de.apnmt.payment.service.dto;

import de.apnmt.payment.common.service.dto.SubscriptionDTO;
import de.apnmt.payment.web.rest.TestUtil;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SubscriptionDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(SubscriptionDTO.class);
        SubscriptionDTO subscriptionDTO1 = new SubscriptionDTO();
        subscriptionDTO1.setId("subscription_1");
        SubscriptionDTO subscriptionDTO2 = new SubscriptionDTO();
        assertThat(subscriptionDTO1).isNotEqualTo(subscriptionDTO2);
        subscriptionDTO2.setId(subscriptionDTO1.getId());
        assertThat(subscriptionDTO1).isEqualTo(subscriptionDTO2);
        subscriptionDTO2.setId("subscription_2");
        assertThat(subscriptionDTO1).isNotEqualTo(subscriptionDTO2);
        subscriptionDTO1.setId(null);
        assertThat(subscriptionDTO1).isNotEqualTo(subscriptionDTO2);
    }
}
