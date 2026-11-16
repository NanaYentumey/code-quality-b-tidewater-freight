package com.tidewater.freight;

import java.math.BigDecimal;

/** Zone pricing made of a rate per kilogram plus a fixed handling fee. */
public record PerKilogramPricing(BigDecimal ratePerKg, BigDecimal handlingFee)
    implements ZonePricing {

  /**
   * Checks the pricing before it can exist.
   *
   * @throws InvalidPricingException if the rate is missing or not positive, or the handling fee is
   *     missing or negative
   */
  public PerKilogramPricing {
    if (ratePerKg == null || ratePerKg.signum() <= 0) {
      throw new InvalidPricingException(
          "Rate per kg must be greater than zero, but was " + ratePerKg);
    }
    if (handlingFee == null || handlingFee.signum() < 0) {
      throw new InvalidPricingException(
          "Handling fee must not be negative, but was " + handlingFee);
    }
  }

  /**
   * Creates pricing from amounts written as decimal text, such as {@code "8.00"}.
   *
   * <p>Text is used on purpose: {@code new BigDecimal("8.10")} is exactly 8.10, whereas a {@code
   * double} literal 8.10 is not, and money must be exact.
   */
  public static PerKilogramPricing of(String ratePerKg, String handlingFee) {
    return new PerKilogramPricing(new BigDecimal(ratePerKg), new BigDecimal(handlingFee));
  }

  @Override
  public BigDecimal baseCost(BigDecimal weightKg) {
    if (weightKg == null) {
      throw new InvalidShipmentException("Weight is required");
    }
    return ratePerKg.multiply(weightKg).add(handlingFee);
  }
}
