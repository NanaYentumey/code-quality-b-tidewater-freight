package com.tidewater.freight;

import java.math.BigDecimal;

/** Prints a few sample quotes, including one for a zone Tidewater does not ship to. */
public final class Demo {

  private Demo() {}

  /** Prices three sample shipments and prints each quote, or the reason there is none. */
  public static void main(String[] args) {
    ShippingCalculator calculator =
        new ShippingCalculator(InMemoryRateRepository.withCurrentZones());

    printQuote(calculator, shipment("DOMESTIC", "10", ServiceLevel.STANDARD));
    printQuote(calculator, shipment("INTERNATIONAL", "40", ServiceLevel.EXPRESS));
    printQuote(calculator, shipment("MOON", "10", ServiceLevel.STANDARD));
  }

  private static Shipment shipment(String zone, String weightKg, ServiceLevel serviceLevel) {
    return new Shipment(zone, new BigDecimal(weightKg), serviceLevel);
  }

  private static void printQuote(ShippingCalculator calculator, Shipment shipment) {
    String description =
        shipment.zone() + " " + shipment.weightKg() + "kg " + shipment.serviceLevel();
    try {
      System.out.println(description + " -> GHS " + calculator.calculateCost(shipment));
    } catch (FreightException e) {
      // The demo is the top of the program: here a failed quote can be reported to the user.
      System.out.println(description + " -> no quote: " + e.getMessage());
    }
  }
}
