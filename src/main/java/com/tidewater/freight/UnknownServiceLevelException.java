package com.tidewater.freight;

/** The shipment names a service level that Tidewater does not offer. */
public final class UnknownServiceLevelException extends InvalidShipmentException {

  private final String code;

  /**
   * Creates the exception.
   *
   * <p>Unlike {@link UnknownZoneException}, this one always has a cause: an unknown code is
   * discovered by Java's own enum lookup failing, and that original failure is kept.
   */
  public UnknownServiceLevelException(String code, Throwable cause) {
    super("Unknown service level: " + code, cause);
    this.code = code;
  }

  public String code() {
    return code;
  }
}
