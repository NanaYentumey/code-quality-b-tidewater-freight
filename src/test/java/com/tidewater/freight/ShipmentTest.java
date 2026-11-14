package com.tidewater.freight;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class ShipmentTest {

    private static final BigDecimal TEN_KG = new BigDecimal("10");

    @Test
    void keepsTheDetailsOfAValidShipment() {
        Shipment shipment = new Shipment("DOMESTIC", TEN_KG, ServiceLevel.EXPRESS);

        assertAll(
                () -> assertEquals("DOMESTIC", shipment.zone()),
                () -> assertEquals(TEN_KG, shipment.weightKg()),
                () -> assertEquals(ServiceLevel.EXPRESS, shipment.serviceLevel()));
    }

    @Test
    void acceptsTheSmallestPositiveWeight() {
        BigDecimal tenGrams = new BigDecimal("0.01");

        Shipment shipment = new Shipment("DOMESTIC", tenGrams, ServiceLevel.STANDARD);

        assertEquals(tenGrams, shipment.weightKg());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void rejectsAMissingZone(String zone) {
        InvalidShipmentException failure = assertThrows(InvalidShipmentException.class,
                () -> new Shipment(zone, TEN_KG, ServiceLevel.STANDARD));

        assertEquals("Destination zone is required", failure.getMessage());
    }

    @Test
    void rejectsAMissingWeight() {
        InvalidShipmentException failure = assertThrows(InvalidShipmentException.class,
                () -> new Shipment("DOMESTIC", null, ServiceLevel.STANDARD));

        assertEquals("Weight is required", failure.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "0.00", "-0.01", "-10"})
    void rejectsAWeightThatIsNotPositive(String weightKg) {
        InvalidShipmentException failure = assertThrows(InvalidShipmentException.class,
                () -> new Shipment("DOMESTIC", new BigDecimal(weightKg), ServiceLevel.STANDARD));

        assertEquals("Weight must be greater than zero, but was " + weightKg + " kg",
                failure.getMessage());
    }

    @Test
    void rejectsAMissingServiceLevel() {
        InvalidShipmentException failure = assertThrows(InvalidShipmentException.class,
                () -> new Shipment("DOMESTIC", TEN_KG, null));

        assertEquals("Service level is required", failure.getMessage());
    }
}
