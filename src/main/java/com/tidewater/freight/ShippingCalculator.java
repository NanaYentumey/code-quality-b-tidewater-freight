package com.tidewater.freight;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Works out what a shipment costs: the zone's base cost, plus any surcharge, times the service
 * level's multiplier, rounded to whole pesewas.
 *
 * <p>It only puts the parts together. The zone rules come from the {@link RateRepository} and the
 * surcharge rules from the {@link SurchargePolicy}, so neither kind of change edits this class.
 */
public final class ShippingCalculator {

  private static final int CURRENCY_SCALE = 2;

  private final RateRepository rates;
  private final SurchargePolicy surchargePolicy;

  public ShippingCalculator(RateRepository rates, SurchargePolicy surchargePolicy) {
    this.rates = Objects.requireNonNull(rates, "rates");
    this.surchargePolicy = Objects.requireNonNull(surchargePolicy, "surchargePolicy");
  }

  /**
   * Calculates the cost of a shipment in GHS.
   *
   * @throws UnknownZoneException if the shipment's zone has no pricing
   * @throws RateLookupException if the rate source fails or returns nothing, or a pricing rule
   *     gives no amount or a negative one
   */
  public BigDecimal calculateCost(Shipment shipment) {
    Objects.requireNonNull(shipment, "shipment");
    BigDecimal costBeforeService = baseCostOf(shipment).add(surchargeOf(shipment));
    return shipment
        .serviceLevel()
        .applyTo(costBeforeService)
        .setScale(CURRENCY_SCALE, RoundingMode.HALF_UP);
  }

  private BigDecimal baseCostOf(Shipment shipment) {
    BigDecimal baseCost = findPricing(shipment.zone()).baseCost(shipment.weightKg());
    if (isNotAnAmount(baseCost)) {
      throw new RateLookupException(
          "Pricing for zone " + shipment.zone() + " gave an invalid base cost: " + baseCost);
    }
    return baseCost;
  }

  private BigDecimal surchargeOf(Shipment shipment) {
    BigDecimal surcharge = surchargePolicy.surchargeFor(shipment);
    if (isNotAnAmount(surcharge)) {
      throw new RateLookupException("Surcharge policy gave an invalid surcharge: " + surcharge);
    }
    return surcharge;
  }

  // Zone pricing and the surcharge policy are plug-in points; a faulty rule must not turn into a
  // negative quote.
  private boolean isNotAnAmount(BigDecimal amount) {
    return amount == null || amount.signum() < 0;
  }

  private ZonePricing findPricing(String zone) {
    ZonePricing pricing;
    try {
      pricing = rates.findPricing(zone);
    } catch (RuntimeException e) {
      throw describeLookupFailure(zone, e);
    }
    if (pricing == null) {
      throw new RateLookupException("Rate source returned no pricing for zone " + zone);
    }
    return pricing;
  }

  private FreightException describeLookupFailure(String zone, RuntimeException failure) {
    if (failure instanceof FreightException alreadyDescribed) {
      return alreadyDescribed;
    }
    return new RateLookupException("Could not look up pricing for zone " + zone, failure);
  }
}
