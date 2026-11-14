package com.tidewater.freight;

import static com.tidewater.freight.MoneyAssertions.assertAmount;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PerKilogramPricingTest {

    private static final BigDecimal ONE = BigDecimal.ONE;

    @Test
    void chargesTheRateForEachKilogramPlusTheHandlingFee() {
        ZonePricing pricing = PerKilogramPricing.of("8.00", "10.00");

        assertAmount("50.00", pricing.baseCost(new BigDecimal("5")));
    }

    @Test
    void chargesOnlyTheRateWhenThereIsNoHandlingFee() {
        ZonePricing pricing = PerKilogramPricing.of("5.00", "0.00");

        assertAmount("12.50", pricing.baseCost(new BigDecimal("2.5")));
    }

    @Test
    void rejectsAMissingRate() {
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> new PerKilogramPricing(null, ONE));

        assertEquals("Rate per kg must be greater than zero, but was null", failure.getMessage());
    }

    @Test
    void rejectsAZeroRate() {
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> PerKilogramPricing.of("0", "1"));

        assertEquals("Rate per kg must be greater than zero, but was 0", failure.getMessage());
    }

    @Test
    void rejectsANegativeRate() {
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> PerKilogramPricing.of("-5", "1"));

        assertEquals("Rate per kg must be greater than zero, but was -5", failure.getMessage());
    }

    @Test
    void rejectsAMissingHandlingFee() {
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> new PerKilogramPricing(ONE, null));

        assertEquals("Handling fee must not be negative, but was null", failure.getMessage());
    }

    @Test
    void rejectsANegativeHandlingFee() {
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> PerKilogramPricing.of("5", "-0.01"));

        assertEquals("Handling fee must not be negative, but was -0.01", failure.getMessage());
    }
}
