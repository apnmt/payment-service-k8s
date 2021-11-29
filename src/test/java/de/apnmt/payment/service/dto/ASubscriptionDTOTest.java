package de.apnmt.payment.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import de.apnmt.payment.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class ASubscriptionDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(ASubscriptionDTO.class);
        ASubscriptionDTO aSubscriptionDTO1 = new ASubscriptionDTO();
        aSubscriptionDTO1.setId(1L);
        ASubscriptionDTO aSubscriptionDTO2 = new ASubscriptionDTO();
        assertThat(aSubscriptionDTO1).isNotEqualTo(aSubscriptionDTO2);
        aSubscriptionDTO2.setId(aSubscriptionDTO1.getId());
        assertThat(aSubscriptionDTO1).isEqualTo(aSubscriptionDTO2);
        aSubscriptionDTO2.setId(2L);
        assertThat(aSubscriptionDTO1).isNotEqualTo(aSubscriptionDTO2);
        aSubscriptionDTO1.setId(null);
        assertThat(aSubscriptionDTO1).isNotEqualTo(aSubscriptionDTO2);
    }
}
