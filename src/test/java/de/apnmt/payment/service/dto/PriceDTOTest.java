package de.apnmt.payment.service.dto;

import de.apnmt.payment.common.service.dto.PriceDTO;
import de.apnmt.payment.web.rest.TestUtil;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PriceDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(PriceDTO.class);
        PriceDTO priceDTO1 = new PriceDTO();
        priceDTO1.setId("price_1");
        PriceDTO priceDTO2 = new PriceDTO();
        assertThat(priceDTO1).isNotEqualTo(priceDTO2);
        priceDTO2.setId(priceDTO1.getId());
        assertThat(priceDTO1).isEqualTo(priceDTO2);
        priceDTO2.setId("price_2");
        assertThat(priceDTO1).isNotEqualTo(priceDTO2);
        priceDTO1.setId(null);
        assertThat(priceDTO1).isNotEqualTo(priceDTO2);
    }
}
