package de.apnmt.payment;

import de.apnmt.payment.common.repository.CustomerRepository;
import de.apnmt.payment.common.repository.PriceRepository;
import de.apnmt.payment.common.repository.ProductRepository;
import de.apnmt.payment.common.repository.SubscriptionItemRepository;
import de.apnmt.payment.common.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

public class CommonIT {

    @Autowired
    protected SubscriptionItemRepository subscriptionItemRepository;

    @Autowired
    protected SubscriptionRepository subscriptionRepository;

    @Autowired
    protected ProductRepository productRepository;

    @Autowired
    protected PriceRepository priceRepository;

    @Autowired
    protected CustomerRepository customerRepository;

    @BeforeEach
    public void setUp() {
        subscriptionItemRepository.deleteAll();
        subscriptionRepository.deleteAll();
        customerRepository.deleteAll();
        priceRepository.deleteAll();
        productRepository.deleteAll();
    }

}
