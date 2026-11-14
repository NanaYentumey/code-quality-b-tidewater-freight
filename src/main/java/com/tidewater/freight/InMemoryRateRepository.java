package com.tidewater.freight;

import java.util.Map;

/** Zone pricing held in memory. Zone codes are matched exactly. */
public final class InMemoryRateRepository implements RateRepository {

    private final Map<String, ZonePricing> pricingByZone;

    public InMemoryRateRepository(Map<String, ZonePricing> pricingByZone) {
        this.pricingByZone = Map.copyOf(pricingByZone);
    }

    /** The zones Tidewater ships to today. A new zone is one more entry here. */
    public static InMemoryRateRepository withCurrentZones() {
        return new InMemoryRateRepository(Map.of(
                "DOMESTIC", PerKilogramPricing.of("5.00", "0.00"),
                "REGIONAL", PerKilogramPricing.of("8.00", "10.00"),
                "INTERNATIONAL", PerKilogramPricing.of("15.00", "25.00")));
    }

    @Override
    public ZonePricing findPricing(String zone) {
        // An immutable map throws NullPointerException on get(null); a null zone is just unknown.
        ZonePricing pricing = zone == null ? null : pricingByZone.get(zone);
        if (pricing == null) {
            throw new UnknownZoneException(zone);
        }
        return pricing;
    }
}
