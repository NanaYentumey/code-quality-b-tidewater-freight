package com.tidewater.freight;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Works out what a shipment costs: the zone's base cost, plus the heavy-item surcharge, times the
 * service level's multiplier, rounded to whole pesewas.
 */
public final class ShippingCalculator {

  private static final BigDecimal HEAVY_ITEM_THRESHOLD_KG = new BigDecimal("30");
  private static final BigDecimal HEAVY_ITEM_SURCHARGE = new BigDecimal("20.00");
  private static final int CURRENCY_SCALE = 2;

  private final RateRepository rates;

  public ShippingCalculator(RateRepository rates) {
    this.rates = Objects.requireNonNull(rates, "rates");
  }

  /**
   * Calculates the cost of a shipment in GHS.
   *
   * @throws UnknownZoneException if the shipment's zone has no pricing
   * @throws RateLookupException if the rate source fails or returns nothing
   */
  public BigDecimal calculateCost(Shipment shipment) {
    Objects.requireNonNull(shipment, "shipment");
    BigDecimal baseCost = findPricing(shipment.zone()).baseCost(shipment.weightKg());
    BigDecimal costBeforeService = baseCost.add(heavyItemSurcharge(shipment.weightKg()));
    return shipment
        .serviceLevel()
        .applyTo(costBeforeService)
        .setScale(CURRENCY_SCALE, RoundingMode.HALF_UP);
  }

  private ZonePricing findPricing(String zone) {
    ZonePricing pricing;
    try {
      pricing = rates.findPricing(zone);
    } catch (FreightException e) {
      throw e;
    } catch (RuntimeException e) {
      throw new RateLookupException("Could not look up pricing for zone " + zone, e);
    }
    if (pricing == null) {
      throw new RateLookupException("Rate source returned no pricing for zone " + zone);
    }
    return pricing;
  }

  private BigDecimal heavyItemSurcharge(BigDecimal weightKg) {
    boolean isHeavy = weightKg.compareTo(HEAVY_ITEM_THRESHOLD_KG) > 0;
    return isHeavy ? HEAVY_ITEM_SURCHARGE : BigDecimal.ZERO;
  }
}
