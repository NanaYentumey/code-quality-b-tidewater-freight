package com.tidewater.freight;

import java.math.BigDecimal;

/** A fixed surcharge for any shipment heavier than a threshold. */
public final class HeavyItemSurcharge implements SurchargePolicy {

  private final BigDecimal thresholdKg;
  private final BigDecimal amount;

  /**
   * Creates the policy.
   *
   * @param thresholdKg the weight a shipment must exceed to pay the surcharge
   * @param amount what a heavy shipment pays
   * @throws InvalidPricingException if either value is missing or negative
   */
  public HeavyItemSurcharge(BigDecimal thresholdKg, BigDecimal amount) {
    if (thresholdKg == null || thresholdKg.signum() < 0) {
      throw new InvalidPricingException(
          "Heavy-item threshold must not be negative, but was " + thresholdKg);
    }
    if (amount == null || amount.signum() < 0) {
      throw new InvalidPricingException(
          "Heavy-item surcharge must not be negative, but was " + amount);
    }
    this.thresholdKg = thresholdKg;
    this.amount = amount;
  }

  @Override
  public BigDecimal surchargeFor(Shipment shipment) {
    boolean isHeavy = shipment.weightKg().compareTo(thresholdKg) > 0;
    return isHeavy ? amount : BigDecimal.ZERO;
  }
}
