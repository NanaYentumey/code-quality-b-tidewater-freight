package com.tidewater.freight;

import static com.tidewater.freight.MoneyAssertions.assertAmount;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ServiceLevelTest {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100.00");

    @Test
    void standardLeavesTheCostUnchanged() {
        assertAmount("100.00", ServiceLevel.STANDARD.applyTo(ONE_HUNDRED));
    }

    @Test
    void expressAddsHalfAgain() {
        assertAmount("150.00", ServiceLevel.EXPRESS.applyTo(ONE_HUNDRED));
    }

    @Test
    void readsAServiceLevelFromItsCode() {
        assertAll(
                () -> assertEquals(ServiceLevel.STANDARD, ServiceLevel.fromCode("STANDARD")),
                () -> assertEquals(ServiceLevel.EXPRESS, ServiceLevel.fromCode("EXPRESS")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"EXPRES", "express", "OVERNIGHT", ""})
    void rejectsACodeThatIsNotAServiceLevel(String code) {
        UnknownServiceLevelException failure = assertThrows(UnknownServiceLevelException.class,
                () -> ServiceLevel.fromCode(code));

        assertAll(
                () -> assertEquals("Unknown service level: " + code, failure.getMessage()),
                () -> assertEquals(code, failure.code()),
                () -> assertInstanceOf(IllegalArgumentException.class, failure.getCause()));
    }

    @Test
    void rejectsAMissingCode() {
        InvalidShipmentException failure = assertThrows(InvalidShipmentException.class,
                () -> ServiceLevel.fromCode(null));

        assertEquals("Service level is required", failure.getMessage());
    }
}
