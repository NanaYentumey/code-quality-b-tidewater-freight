package com.tidewater.freight;

import java.math.BigDecimal;

/**
 * A shipment to be priced. An instance is always valid: the constructor rejects anything that could
 * not be given a meaningful cost.
 */
public record Shipment(String zone, BigDecimal weightKg, ServiceLevel serviceLevel) {

  /**
   * Checks the shipment before it can exist.
   *
   * @throws InvalidShipmentException if the zone is blank, the weight is missing or not positive,
   *     or the service level is missing
   */
  public Shipment {
    if (zone == null || zone.isBlank()) {
      throw new InvalidShipmentException("Destination zone is required");
    }
    if (weightKg == null) {
      throw new InvalidShipmentException("Weight is required");
    }
    if (weightKg.signum() <= 0) {
      throw new InvalidShipmentException(
          "Weight must be greater than zero, but was " + weightKg + " kg");
    }
    if (serviceLevel == null) {
      throw new InvalidShipmentException("Service level is required");
    }
  }
}
