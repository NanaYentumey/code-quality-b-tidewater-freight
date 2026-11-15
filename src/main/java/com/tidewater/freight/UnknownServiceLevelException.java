package com.tidewater.freight;

/** The shipment names a service level that Tidewater does not offer. */
public final class UnknownServiceLevelException extends InvalidShipmentException {

  private final String code;

  public UnknownServiceLevelException(String code, Throwable cause) {
    super("Unknown service level: " + code, cause);
    this.code = code;
  }

  public String code() {
    return code;
  }
}
