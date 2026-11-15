package com.tidewater.freight;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

/** Compares amounts by value, so that 150.0 and 150.00 count as the same money. */
final class MoneyAssertions {

  private MoneyAssertions() {}

  static void assertAmount(String expected, BigDecimal actual) {
    assertEquals(
        0,
        new BigDecimal(expected).compareTo(actual),
        () -> "expected amount " + expected + " but was " + actual);
  }
}
