package de.apnmt.payment.service.mapper;

import de.apnmt.payment.common.service.mapper.CustomerMapper;
import de.apnmt.payment.common.service.mapper.CustomerMapperImpl;
import org.junit.jupiter.api.BeforeEach;

class CustomerMapperTest {

    private CustomerMapper customerMapper;

    @BeforeEach
    public void setUp() {
        this.customerMapper = new CustomerMapperImpl();
    }
}
