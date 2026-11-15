package com.tidewater.freight;

import java.math.BigDecimal;

/** Zone pricing made of a rate per kilogram plus a fixed handling fee. */
public record PerKilogramPricing(BigDecimal ratePerKg, BigDecimal handlingFee)
    implements ZonePricing {

  /**
   * Checks the pricing before it can exist.
   *
   * @throws IllegalArgumentException if the rate is missing or not positive, or the handling fee is
   *     missing or negative
   */
  public PerKilogramPricing {
    if (ratePerKg == null || ratePerKg.signum() <= 0) {
      throw new IllegalArgumentException(
          "Rate per kg must be greater than zero, but was " + ratePerKg);
    }
    if (handlingFee == null || handlingFee.signum() < 0) {
      throw new IllegalArgumentException(
          "Handling fee must not be negative, but was " + handlingFee);
    }
  }

  public static PerKilogramPricing of(String ratePerKg, String handlingFee) {
    return new PerKilogramPricing(new BigDecimal(ratePerKg), new BigDecimal(handlingFee));
  }

  @Override
  public BigDecimal baseCost(BigDecimal weightKg) {
    return ratePerKg.multiply(weightKg).add(handlingFee);
  }
}
