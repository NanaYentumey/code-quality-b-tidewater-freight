package com.tidewater.freight;

import static com.tidewater.freight.MoneyAssertions.assertAmount;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Tidewater's own numbers: a changed rate, fee, threshold or surcharge fails a test here. */
class TidewaterPricingTest {

  @ParameterizedTest(name = "{0}: {1} per kg plus {2}")
  @CsvSource({
    "DOMESTIC,      5.00,  0.00",
    "REGIONAL,      8.00,  10.00",
    "INTERNATIONAL, 15.00, 25.00"
  })
  void pricesEachCurrentZone(String zone, String ratePerKg, String handlingFee) {
    ZonePricing pricing = TidewaterPricing.currentZones().get(zone);

    assertEquals(PerKilogramPricing.of(ratePerKg, handlingFee), pricing);
  }

  @Test
  void shipsToExactlyThreeZones() {
    assertEquals(
        Set.of("DOMESTIC", "REGIONAL", "INTERNATIONAL"), TidewaterPricing.currentZones().keySet());
  }

  @ParameterizedTest(name = "{0} kg pays a surcharge of {1}")
  @CsvSource({"30, 0", "30.01, 20.00"})
  void addsTwentyAboveThirtyKilograms(String weightKg, String expectedSurcharge) {
    Shipment shipment = new Shipment("DOMESTIC", new BigDecimal(weightKg), ServiceLevel.STANDARD);

    assertAmount(expectedSurcharge, TidewaterPricing.heavyItemSurcharge().surchargeFor(shipment));
  }
}
