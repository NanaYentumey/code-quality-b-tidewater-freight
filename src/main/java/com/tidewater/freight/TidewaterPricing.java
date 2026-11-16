package com.tidewater.freight;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Tidewater's current prices, and the one place the parts are wired together.
 *
 * <p>The values live in code because this service has no configuration source yet. In production
 * they would be loaded from configuration or a database behind another {@link RateRepository};
 * nothing outside this class would have to change.
 */
public final class TidewaterPricing {

  private TidewaterPricing() {}

  /** A calculator using today's zones and surcharge. */
  public static ShippingCalculator newCalculator() {
    return new ShippingCalculator(new InMemoryRateRepository(currentZones()), heavyItemSurcharge());
  }

  /** The zones Tidewater ships to today. A new zone is one more entry here. */
  public static Map<String, ZonePricing> currentZones() {
    return Map.of(
        "DOMESTIC", PerKilogramPricing.of("5.00", "0.00"),
        "REGIONAL", PerKilogramPricing.of("8.00", "10.00"),
        "INTERNATIONAL", PerKilogramPricing.of("15.00", "25.00"));
  }

  /** Shipments over 30 kg pay GHS 20.00 extra. */
  public static SurchargePolicy heavyItemSurcharge() {
    return new HeavyItemSurcharge(new BigDecimal("30"), new BigDecimal("20.00"));
  }
}
