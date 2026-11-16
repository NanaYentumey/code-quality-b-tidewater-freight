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
import org.junit.jupiter.params.provider.ValueSource;

class InMemoryRateRepositoryTest {

  private static final ZonePricing COASTAL_PRICING = PerKilogramPricing.of("6.50", "4.00");

  private final RateRepository rates =
      new InMemoryRateRepository(Map.of("COASTAL", COASTAL_PRICING));

  @Test
  void returnsThePricingItWasGivenForAZone() {
    assertSame(COASTAL_PRICING, rates.findPricing("COASTAL"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"MOON", "coastal", " COASTAL", ""})
  void rejectsAZoneItHasNoPricingFor(String zone) {
    UnknownZoneException failure =
        assertThrows(UnknownZoneException.class, () -> rates.findPricing(zone));

    assertAll(
        () -> assertEquals("Unknown zone: " + zone, failure.getMessage()),
        () -> assertEquals(zone, failure.zone()));
  }

  @Test
  void treatsAMissingZoneAsUnknown() {
    UnknownZoneException failure =
        assertThrows(UnknownZoneException.class, () -> rates.findPricing(null));

    assertAll(
        () -> assertEquals("Unknown zone: null", failure.getMessage()),
        () -> assertNull(failure.zone()));
  }

  @Test
  void ignoresChangesMadeToTheMapAfterConstruction() {
    Map<String, ZonePricing> pricingByZone = new HashMap<>();
    pricingByZone.put("COASTAL", COASTAL_PRICING);
    RateRepository builtFromAMutableMap = new InMemoryRateRepository(pricingByZone);

    pricingByZone.put("ISLAND", COASTAL_PRICING);

    assertThrows(UnknownZoneException.class, () -> builtFromAMutableMap.findPricing("ISLAND"));
  }
}
