package com.tidewater.freight;

import java.math.BigDecimal;

/** How quickly a shipment travels, and what that does to its price. */
public enum ServiceLevel {
    STANDARD("1.0"),
    EXPRESS("1.5");

    private final BigDecimal priceMultiplier;

    ServiceLevel(String priceMultiplier) {
        this.priceMultiplier = new BigDecimal(priceMultiplier);
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

    public BigDecimal applyTo(BigDecimal cost) {
        return cost.multiply(priceMultiplier);
    }
}
