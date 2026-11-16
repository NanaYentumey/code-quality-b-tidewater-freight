package com.tidewater.freight;

/** A rate, fee or surcharge was configured with a value that can never be right. */
public final class InvalidPricingException extends FreightException {

  public InvalidPricingException(String message) {
    super(message);
  }
}
