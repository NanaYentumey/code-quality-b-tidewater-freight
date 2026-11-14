package com.tidewater.freight;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class InMemoryRateRepositoryTest {

    private static final ZonePricing ANY_PRICING = PerKilogramPricing.of("1.00", "0.00");

    @ParameterizedTest(name = "{0}: {1} per kg plus {2}")
    @CsvSource({
        "DOMESTIC,      5.00,  0.00",
        "REGIONAL,      8.00,  10.00",
        "INTERNATIONAL, 15.00, 25.00"
    })
    void holdsThePricingOfEachCurrentZone(String zone, String ratePerKg, String handlingFee) {
        RateRepository rates = InMemoryRateRepository.withCurrentZones();

        assertEquals(PerKilogramPricing.of(ratePerKg, handlingFee), rates.findPricing(zone));
    }

    @ParameterizedTest
    @ValueSource(strings = {"MOON", "domestic", " DOMESTIC", ""})
    void rejectsAZoneItHasNoPricingFor(String zone) {
        RateRepository rates = InMemoryRateRepository.withCurrentZones();

        UnknownZoneException failure =
                assertThrows(UnknownZoneException.class, () -> rates.findPricing(zone));

        assertAll(
                () -> assertEquals("Unknown zone: " + zone, failure.getMessage()),
                () -> assertEquals(zone, failure.zone()));
    }

    @Test
    void treatsAMissingZoneAsUnknown() {
        RateRepository rates = InMemoryRateRepository.withCurrentZones();

        UnknownZoneException failure =
                assertThrows(UnknownZoneException.class, () -> rates.findPricing(null));

        assertNull(failure.zone());
    }

    @Test
    void returnsThePricingItWasGivenForAZone() {
        RateRepository rates = new InMemoryRateRepository(Map.of("COASTAL", ANY_PRICING));

        assertSame(ANY_PRICING, rates.findPricing("COASTAL"));
    }

    @Test
    void ignoresChangesMadeToTheMapAfterConstruction() {
        Map<String, ZonePricing> pricingByZone = new HashMap<>();
        pricingByZone.put("COASTAL", ANY_PRICING);
        RateRepository rates = new InMemoryRateRepository(pricingByZone);

        pricingByZone.put("ISLAND", ANY_PRICING);

        assertThrows(UnknownZoneException.class, () -> rates.findPricing("ISLAND"));
    }
}
