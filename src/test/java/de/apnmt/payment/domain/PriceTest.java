package de.apnmt.payment.domain;

import de.apnmt.payment.common.domain.Price;
import de.apnmt.payment.web.rest.TestUtil;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PriceTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Price.class);
        Price price1 = new Price();
        price1.setId("price_1");
        Price price2 = new Price();
        price2.setId(price1.getId());
        assertThat(price1).isEqualTo(price2);
        price2.setId("price_2");
        assertThat(price1).isNotEqualTo(price2);
        price1.setId(null);
        assertThat(price1).isNotEqualTo(price2);
    }
}
