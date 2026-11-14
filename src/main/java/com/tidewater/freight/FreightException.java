package com.tidewater.freight;

/**
 * Base type for every failure this service raises on purpose.
 *
 * <p>Unchecked, because a caller cannot repair a bad shipment or a broken rate source in code;
 * it can only report it. Catch a subtype to tell the two apart: {@link InvalidShipmentException}
 * means the request was wrong, {@link RateLookupException} means the service could not answer.
 */
public abstract class FreightException extends RuntimeException {

    protected FreightException(String message) {
        super(message);
    }

    protected FreightException(String message, Throwable cause) {
        super(message, cause);
    }
}
