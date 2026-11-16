package com.tidewater.freight;

import java.math.BigDecimal;
import java.util.Objects;

/** How quickly a shipment travels, and what that does to its price. */
public enum ServiceLevel {
  STANDARD(new BigDecimal("1.0")),
  EXPRESS(new BigDecimal("1.5"));

  private final BigDecimal priceMultiplier;

  ServiceLevel(BigDecimal priceMultiplier) {
    this.priceMultiplier = priceMultiplier;
  }

  /**
   * Turns a code from outside the service, such as {@code "EXPRESS"}, into a service level.
   *
   * @throws InvalidShipmentException if the code is missing
   * @throws UnknownServiceLevelException if the code is not an exact service level name
   */
  public static ServiceLevel fromCode(String code) {
    if (code == null) {
      throw new InvalidShipmentException("Service level is required");
    }
    try {
      return valueOf(code);
    } catch (IllegalArgumentException e) {
      throw new UnknownServiceLevelException(code, e);
    }
  }

  /** Multiplies a cost by this service level's multiplier. */
  public BigDecimal applyTo(BigDecimal cost) {
    return Objects.requireNonNull(cost, "cost").multiply(priceMultiplier);
  }
}
