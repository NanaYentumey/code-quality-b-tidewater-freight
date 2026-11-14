package com.tidewater.freight;

/** The shipment names a destination zone that has no pricing. */
public final class UnknownZoneException extends InvalidShipmentException {

    private final String zone;

    public UnknownZoneException(String zone) {
        super("Unknown zone: " + zone);
        this.zone = zone;
    }

    public String zone() {
        return zone;
    }
}
