package com.tidewater.freight;

import java.math.BigDecimal;

/**
 * Decides what a shipment pays on top of its zone's base cost.
 *
 * <p>A change to the surcharge rules is a new implementation handed to the calculator; the
 * calculator itself does not change.
 */
public interface SurchargePolicy {

  /** The surcharge for this shipment: zero when none applies, never negative and never null. */
  BigDecimal surchargeFor(Shipment shipment);
}
