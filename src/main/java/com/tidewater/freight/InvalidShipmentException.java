package com.tidewater.freight;

/** The shipment request itself is wrong, so no cost can be worked out for it. */
public class InvalidShipmentException extends FreightException {

    public InvalidShipmentException(String message) {
        super(message);
    }

    public InvalidShipmentException(String message, Throwable cause) {
        super(message, cause);
    }
}
