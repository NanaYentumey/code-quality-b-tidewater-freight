package com.tidewater.freight;

import java.math.BigDecimal;

/**
 * The pricing rules of one destination zone.
 *
 * <p>A new zone is a new instance (or, if its rules are unlike the others, a new implementation)
 * registered with the rate repository. Nothing that already works has to change.
 */
public interface ZonePricing {

    /** Cost of carrying this weight to the zone, zone fees included, before any surcharge. */
    BigDecimal baseCost(BigDecimal weightKg);
}
