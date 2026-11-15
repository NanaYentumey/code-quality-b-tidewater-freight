package com.tidewater.freight;

/** The rate source failed or gave no usable answer; the request may have been fine. */
public final class RateLookupException extends FreightException {

  public RateLookupException(String message) {
    super(message);
  }

  public RateLookupException(String message, Throwable cause) {
    super(message, cause);
  }
}
