package com.tidewater.freight;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * The calculator wired to the real in-memory rates, with no mocks: Tidewater's published prices
 * end to end. A change to any rate, fee, surcharge or multiplier fails a row here.
 */
class ShippingCostIntegrationTest {

    private final ShippingCalculator calculator =
            new ShippingCalculator(InMemoryRateRepository.withCurrentZones());

    @ParameterizedTest(name = "{0} {1} kg {2} costs GHS {3}")
    @CsvSource({
        "DOMESTIC,      10,   STANDARD, 50.00",
        "DOMESTIC,      10,   EXPRESS,  75.00",
        "DOMESTIC,      31,   STANDARD, 175.00",
        "REGIONAL,      5,    STANDARD, 50.00",
        "REGIONAL,      31,   EXPRESS,  417.00",
        "INTERNATIONAL, 30,   STANDARD, 475.00",
        "INTERNATIONAL, 30.5, STANDARD, 502.50",
        "INTERNATIONAL, 40,   EXPRESS,  967.50"
    })
    void quotesThePublishedPrice(
            String zone, String weightKg, String serviceLevel, String expectedCost) {
        Shipment shipment =
                new Shipment(zone, new BigDecimal(weightKg), ServiceLevel.fromCode(serviceLevel));

        assertEquals(new BigDecimal(expectedCost), calculator.calculateCost(shipment));
    }

    @Test
    void refusesToQuoteAnUnknownZoneInsteadOfQuotingZero() {
        Shipment toTheMoon = new Shipment("MOON", new BigDecimal("10"), ServiceLevel.STANDARD);

        UnknownZoneException failure = assertThrows(UnknownZoneException.class,
                () -> calculator.calculateCost(toTheMoon));

        assertEquals("MOON", failure.zone());
    }

    @Test
    void quotesANewZoneOnceItsPricingIsRegistered() {
        RateRepository ratesWithCoastal = new InMemoryRateRepository(Map.of(
                "DOMESTIC", PerKilogramPricing.of("5.00", "0.00"),
                "COASTAL", PerKilogramPricing.of("6.50", "4.00")));
        ShippingCalculator coastalCalculator = new ShippingCalculator(ratesWithCoastal);
        Shipment toTheCoast = new Shipment("COASTAL", new BigDecimal("10"), ServiceLevel.EXPRESS);

        assertEquals(new BigDecimal("103.50"), coastalCalculator.calculateCost(toTheCoast));
    }
}
