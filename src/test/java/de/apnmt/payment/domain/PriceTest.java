package de.apnmt.payment.domain;

import static org.assertj.core.api.Assertions.assertThat;

import de.apnmt.payment.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class PriceTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Price.class);
        Price price1 = new Price();
        price1.setId(1L);
        Price price2 = new Price();
        price2.setId(price1.getId());
        assertThat(price1).isEqualTo(price2);
        price2.setId(2L);
        assertThat(price1).isNotEqualTo(price2);
        price1.setId(null);
        assertThat(price1).isNotEqualTo(price2);
    }
}
