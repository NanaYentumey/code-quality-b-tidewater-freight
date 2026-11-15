package com.tidewater.freight;

/** Where the calculator gets each zone's pricing rules from. */
public interface RateRepository {

  /**
   * Finds the pricing rules for a destination zone.
   *
   * @throws UnknownZoneException if the zone has no pricing
   */
  ZonePricing findPricing(String zone);
}
