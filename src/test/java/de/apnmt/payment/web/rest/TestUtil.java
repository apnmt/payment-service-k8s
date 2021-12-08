package de.apnmt.payment.web.rest;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.stripe.model.SubscriptionItemCollection;
import de.apnmt.payment.common.domain.Customer;
import de.apnmt.payment.common.domain.Price;
import de.apnmt.payment.common.domain.Product;
import de.apnmt.payment.common.domain.Subscription;
import de.apnmt.payment.common.domain.SubscriptionItem;
import de.apnmt.payment.common.domain.enumeration.Currency;
import de.apnmt.payment.common.domain.enumeration.Interval;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.hamcrest.TypeSafeMatcher;
import org.springframework.format.datetime.standard.DateTimeFormatterRegistrar;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.format.support.FormattingConversionService;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Utility class for testing REST controllers.
 */
public final class TestUtil {

    private static final ObjectMapper mapper = createObjectMapper();

    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS, false);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    /**
     * Convert an object to JSON byte array.
     *
     * @param object the object to convert.
     * @return the JSON byte array.
     * @throws IOException
     */
    public static byte[] convertObjectToJsonBytes(Object object) throws IOException {
        return mapper.writeValueAsBytes(object);
    }

    /**
     * Create a byte array with a specific size filled with specified data.
     *
     * @param size the size of the byte array.
     * @param data the data to put in the byte array.
     * @return the JSON byte array.
     */
    public static byte[] createByteArray(int size, String data) {
        byte[] byteArray = new byte[size];
        for (int i = 0; i < size; i++) {
            byteArray[i] = Byte.parseByte(data, 2);
        }
        return byteArray;
    }

    /**
     * A matcher that tests that the examined string represents the same instant as the reference datetime.
     */
    public static class ZonedDateTimeMatcher extends TypeSafeDiagnosingMatcher<String> {

        private final ZonedDateTime date;

        public ZonedDateTimeMatcher(ZonedDateTime date) {
            this.date = date;
        }

        @Override
        protected boolean matchesSafely(String item, Description mismatchDescription) {
            try {
                if (!this.date.isEqual(ZonedDateTime.parse(item))) {
                    mismatchDescription.appendText("was ").appendValue(item);
                    return false;
                }
                return true;
            } catch (DateTimeParseException e) {
                mismatchDescription.appendText("was ").appendValue(item).appendText(", which could not be parsed as a ZonedDateTime");
                return false;
            }
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("a String representing the same Instant as ").appendValue(this.date);
        }
    }

    /**
     * Creates a matcher that matches when the examined string represents the same instant as the reference datetime.
     *
     * @param date the reference datetime against which the examined string is checked.
     */
    public static ZonedDateTimeMatcher sameInstant(ZonedDateTime date) {
        return new ZonedDateTimeMatcher(date);
    }

    /**
     * A matcher that tests that the examined number represents the same value - it can be Long, Double, etc - as the reference BigDecimal.
     */
    public static class NumberMatcher extends TypeSafeMatcher<Number> {

        final BigDecimal value;

        public NumberMatcher(BigDecimal value) {
            this.value = value;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("a numeric value is ").appendValue(this.value);
        }

        @Override
        protected boolean matchesSafely(Number item) {
            BigDecimal bigDecimal = asDecimal(item);
            return bigDecimal != null && this.value.compareTo(bigDecimal) == 0;
        }

        private static BigDecimal asDecimal(Number item) {
            if (item == null) {
                return null;
            }
            if (item instanceof BigDecimal) {
                return (BigDecimal) item;
            } else if (item instanceof Long) {
                return BigDecimal.valueOf((Long) item);
            } else if (item instanceof Integer) {
                return BigDecimal.valueOf((Integer) item);
            } else if (item instanceof Double) {
                return BigDecimal.valueOf((Double) item);
            } else if (item instanceof Float) {
                return BigDecimal.valueOf((Float) item);
            } else {
                return BigDecimal.valueOf(item.doubleValue());
            }
        }
    }

    /**
     * Creates a matcher that matches when the examined number represents the same value as the reference BigDecimal.
     *
     * @param number the reference BigDecimal against which the examined number is checked.
     */
    public static NumberMatcher sameNumber(BigDecimal number) {
        return new NumberMatcher(number);
    }

    /**
     * Verifies the equals/hashcode contract on the domain object.
     */
    public static <T> void equalsVerifier(Class<T> clazz) throws Exception {
        T domainObject1 = clazz.getConstructor().newInstance();
        assertThat(domainObject1.toString()).isNotNull();
        assertThat(domainObject1).isEqualTo(domainObject1);
        assertThat(domainObject1).hasSameHashCodeAs(domainObject1);
        // Test with an instance of another class
        Object testOtherObject = new Object();
        assertThat(domainObject1).isNotEqualTo(testOtherObject);
        assertThat(domainObject1).isNotEqualTo(null);
        // Test with an instance of the same class
        T domainObject2 = clazz.getConstructor().newInstance();
        assertThat(domainObject1).isNotEqualTo(domainObject2);
        // HashCodes are equals because the objects are not persisted yet
        assertThat(domainObject1).hasSameHashCodeAs(domainObject2);
    }

    /**
     * Create a {@link FormattingConversionService} which use ISO date format, instead of the localized one.
     *
     * @return the {@link FormattingConversionService}.
     */
    public static FormattingConversionService createFormattingConversionService() {
        DefaultFormattingConversionService dfcs = new DefaultFormattingConversionService();
        DateTimeFormatterRegistrar registrar = new DateTimeFormatterRegistrar();
        registrar.setUseIsoFormat(true);
        registrar.registerFormatters(dfcs);
        return dfcs;
    }

    /**
     * Makes a an executes a query to the EntityManager finding all stored objects.
     *
     * @param <T>  The type of objects to be searched
     * @param em   The instance of the EntityManager
     * @param clss The class type to be searched
     * @return A list of all found objects
     */
    public static <T> List<T> findAll(EntityManager em, Class<T> clss) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<T> cq = cb.createQuery(clss);
        Root<T> rootEntry = cq.from(clss);
        CriteriaQuery<T> all = cq.select(rootEntry);
        TypedQuery<T> allQuery = em.createQuery(all);
        return allQuery.getResultList();
    }

    // PaymentService specific TestUtils

    public static String DEFAULT_CUSTOMER_ID = "customer_1";

    public static String DEFAULT_PRODUCT_ID = "product_1";

    public static String DEFAULT_PRICE_ID = "price_1";

    public static Customer createCustomer() {
        Customer customer = new Customer();
        customer.setId(DEFAULT_CUSTOMER_ID);
        customer.setOrganizationId(1L);
        return customer;
    }

    public static com.stripe.model.Customer createStripeCustomer() {
        com.stripe.model.Customer customer = new com.stripe.model.Customer();
        customer.setId(DEFAULT_CUSTOMER_ID);
        return customer;
    }

    public static Product createProduct() {
        Product product = new Product();
        product.setId(DEFAULT_PRODUCT_ID);
        product.setName("product");
        product.setDescription("description");
        return product;
    }

    public static Price createPrice() {
        Price price = new Price();
        price.setId(DEFAULT_PRICE_ID);
        price.setAmount(5000L);
        price.setNickname("nickname");
        price.setCurrency(Currency.eur);
        price.setInterval(Interval.day);
        return price;
    }

    public static SubscriptionItem createSubscriptionItem(Price price) {
        SubscriptionItem subscriptionItem = new SubscriptionItem();
        subscriptionItem.setPrice(price);
        subscriptionItem.setQuantity(1);
        return subscriptionItem;
    }

    public static com.stripe.model.Subscription createSubscription(Subscription subscription) {
        com.stripe.model.Subscription s = new com.stripe.model.Subscription();
        s.setId("subscription_1");
        s.setStatus("active");
        int i = 1;
        SubscriptionItemCollection collection = new SubscriptionItemCollection();
        List<com.stripe.model.SubscriptionItem> subscriptionItems = new ArrayList<>();
        for (SubscriptionItem item : subscription.getSubscriptionItems()) {
            com.stripe.model.SubscriptionItem subscriptionItem = new com.stripe.model.SubscriptionItem();
            subscriptionItem.setId("subscriptionItem_" + i);
            subscriptionItem.setQuantity(item.getQuantity().longValue());
            com.stripe.model.Price price = new com.stripe.model.Price();
            price.setId(item.getPrice().getId());
            subscriptionItem.setPrice(price);
            subscriptionItems.add(subscriptionItem);
            i++;
        }
        collection.setData(subscriptionItems);
        s.setItems(collection);
        return s;
    }

    private TestUtil() {
    }
}
