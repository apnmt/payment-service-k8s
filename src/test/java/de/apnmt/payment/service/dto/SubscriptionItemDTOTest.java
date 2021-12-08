package de.apnmt.payment.service.dto;

import de.apnmt.payment.common.service.dto.SubscriptionItemDTO;
import de.apnmt.payment.web.rest.TestUtil;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SubscriptionItemDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(SubscriptionItemDTO.class);
        SubscriptionItemDTO subscriptionItemDTO1 = new SubscriptionItemDTO();
        subscriptionItemDTO1.setId("subscriptionItem_1");
        SubscriptionItemDTO subscriptionItemDTO2 = new SubscriptionItemDTO();
        assertThat(subscriptionItemDTO1).isNotEqualTo(subscriptionItemDTO2);
        subscriptionItemDTO2.setId(subscriptionItemDTO1.getId());
        assertThat(subscriptionItemDTO1).isEqualTo(subscriptionItemDTO2);
        subscriptionItemDTO2.setId("subscriptionItem_2");
        assertThat(subscriptionItemDTO1).isNotEqualTo(subscriptionItemDTO2);
        subscriptionItemDTO1.setId(null);
        assertThat(subscriptionItemDTO1).isNotEqualTo(subscriptionItemDTO2);
    }
}
